package com.github.eikek.sbt.openapi

case class ScalaConfig(
    mapping: CustomMapping = CustomMapping.none,
    json: ScalaJson = ScalaJson.none,
    modelType: ScalaModelType = ScalaModelType.CaseClass
) {
  require(modelType == ScalaModelType.CaseClass || json == ScalaJson.none,
    "Generating traits and ScalaJson is not supported.")

  def withJson(json: ScalaJson): ScalaConfig =
    copy(json = json)

  def addMapping(cm: CustomMapping): ScalaConfig =
    copy(mapping = mapping.andThen(cm))

  def setMapping(cm: CustomMapping): ScalaConfig =
    copy(mapping = cm)

  def setModelType(mt: ScalaModelType): ScalaConfig =
    copy(modelType = mt)
}

object ScalaConfig {

  val default = ScalaConfig()

}
