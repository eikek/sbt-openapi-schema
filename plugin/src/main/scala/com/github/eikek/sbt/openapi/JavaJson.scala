package com.github.eikek.sbt.openapi

trait JavaJson {

  def resolve(src: SourceFile): SourceFile

  def builderAnnotations(src: SourceFile): List[Annotation]

}

object JavaJson {
  val none = new JavaJson {
    def resolve(src: SourceFile): SourceFile = src
    def builderAnnotations(src: SourceFile): List[Annotation] = Nil
  }

  val jackson = new JavaJson {

    def jacksonDeserializer(src: SourceFile) =
      Annotation(s"@JsonDeserialize(builder = ${src.name}.Builder.class)")
    def jacksonIgnore(src: SourceFile) = {
      val props = src.fields.
        map(_.prop).
        filter(_.nullable).
        map(_.name).
        map(n => "\"" + n + "Optional\"").mkString(", ")
      Annotation(s"@JsonIgnoreProperties({ $props })")
    }
    def jacksonAutoDetect =
      Annotation("@JsonAutoDetect(getterVisibility = Visibility.NON_PRIVATE)")

    val jackonBuilderPojo =
      Annotation("@JsonPOJOBuilder(withPrefix = \"set\")")

    def resolve(src: SourceFile): SourceFile =
      if (src.wrapper) {
        src.addImports(Imports("com.fasterxml.jackson.annotation.JsonValue")).
          copy(fields = src.fields.map(f => Field(f.prop, List(Annotation("@JsonValue")), f.typeDef)))
      } else {
        src.addAnnotation(jacksonDeserializer(src)).
          addAnnotation(jacksonIgnore(src)).
          addAnnotation(jacksonAutoDetect).
          addImports(Imports("com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder"
            , "com.fasterxml.jackson.databind.annotation.JsonDeserialize"
            , "com.fasterxml.jackson.annotation.JsonIgnoreProperties"
            , "com.fasterxml.jackson.annotation.JsonAutoDetect"
            , "com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility"))
      }

    def builderAnnotations(src: SourceFile): List[Annotation] =
      List(jackonBuilderPojo)
  }
}
