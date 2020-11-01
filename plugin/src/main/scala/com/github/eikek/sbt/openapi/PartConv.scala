package com.github.eikek.sbt.openapi

trait PartConv[A] { self =>
  def toPart(a: A): Part

  def map(f: Part => Part): PartConv[A] =
    PartConv(a => f(self.toPart(a)))

  def contramap[B](f: B => A): PartConv[B] =
    b => self.toPart(f(b))

  def concat(pc: PartConv[A], merge: (Part, Part) => Part): PartConv[A] =
    PartConv(a => merge(self.toPart(a), pc.toPart(a)))

  def ++(pc: PartConv[A]): PartConv[A] =
    concat(pc, _ ++ _)

  def ~(pc: PartConv[A]): PartConv[A] =
    concat(pc, _ ~ _)

  def +(pc: PartConv[A]): PartConv[A] =
    concat(pc, _ + _)

  def when(p: A => Boolean): PartConv[A] =
    PartConv(a => if (p(a)) self.toPart(a) else Part.empty)
}

object PartConv {
  def apply[A](f: A => Part): PartConv[A] = a => f(a)

  def ofPart[A](p: Part): PartConv[A] =
    PartConv(_ => p)

  def of[A](str: String): PartConv[A] =
    ofPart(Part(str))

  def constant[A](str: String): PartConv[A] = of(str)

  def empty[A]: PartConv[A] = PartConv.of("")

  def forList[A](pc: PartConv[A], f: (Part, Part) => Part): PartConv[List[A]] =
    PartConv { list =>
      list.map(pc.toPart).foldLeft(Part.empty)(f)
    }

  def forListSep[A](pc: PartConv[A], sep: Part): PartConv[List[A]] =
    PartConv {
      case Nil  => Part.empty
      case list => list.map(pc.toPart).reduce((a, b) => a + sep + b)
    }

  def listSplit[A](psingle: PartConv[A], plist: PartConv[List[A]]): PartConv[List[A]] =
    PartConv {
      case Nil      => Part.empty
      case a :: Nil => psingle.toPart(a)
      case a :: as  => psingle.toPart(a) ~ plist.toPart(as)
    }

  val string: PartConv[String] =
    PartConv(s => Part(s))

  val imports: PartConv[Imports] =
    forList(string, _ ++ _).contramap[Imports](_.lines.map(l => "import " + l))

  val pkg: PartConv[Pkg] =
    PartConv(p => Part(s"package ${p.name}"))

  val doc: PartConv[Doc] = PartConv(d =>
    if (d.isEmpty) Part.empty
    else Part("/**") ++ Part(d.text).prefix(" * ") ++ Part(" */")
  )

  val annotation: PartConv[Annotation] =
    PartConv(annot => Part(annot.render))

  val superclass: PartConv[Superclass] =
    PartConv(sc => Part(sc.name))

  val fieldName: PartConv[Field] =
    string.contramap(_.prop.name)

  val sourceName: PartConv[SourceFile] =
    string.contramap(_.name)

  val fieldType: PartConv[Field] =
    string.contramap(_.typeDef.name)

  val discriminantType: PartConv[SourceFile] =
    string.contramap(
      _.fields
        .collectFirst { case f if f.prop.discriminator => f.prop.name }
        .getOrElse("type")
    )

  val accessModifier: PartConv[SourceFile] =
    cond(_.isInternal, constant("private "), empty)

  def cond[A](p: A => Boolean, when: PartConv[A], otherwise: PartConv[A]): PartConv[A] =
    PartConv(a => if (p(a)) when.toPart(a) else otherwise.toPart(a))
}
