package com.github.eikek.sbt.openapi

case class ScalaConfig(mapping: CustomMapping = CustomMapping.none
  , json: ScalaJson = ScalaJson.none
) {

  def withJson(json: ScalaJson): ScalaConfig =
    copy(json = json)

  def addMapping(cm: CustomMapping): ScalaConfig =
    copy(mapping = mapping.andThen(cm))

  def setMapping(cm: CustomMapping): ScalaConfig =
    copy(mapping = cm)
}

object ScalaConfig {

  val default = ScalaConfig()

}
