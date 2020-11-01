package com.github.eikek.sbt.openapi

case class JavaConfig(
    mapping: CustomMapping = CustomMapping.none,
    json: JavaJson = JavaJson.none,
    builderParents: List[Superclass] = Nil
) {

  def withJson(json: JavaJson): JavaConfig =
    copy(json = json)

  def addBuilderParent(sc: Superclass): JavaConfig =
    copy(builderParents = sc :: builderParents)

  def addMapping(cm: CustomMapping): JavaConfig =
    copy(mapping = mapping.andThen(cm))

  def setMapping(cm: CustomMapping): JavaConfig =
    copy(mapping = cm)
}

object JavaConfig {

  val default = JavaConfig()

}
