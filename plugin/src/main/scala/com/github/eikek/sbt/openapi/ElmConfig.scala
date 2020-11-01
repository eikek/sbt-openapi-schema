package com.github.eikek.sbt.openapi

case class ElmConfig(
    mapping: CustomMapping = CustomMapping.none,
    json: ElmJson = ElmJson.none
) {

  def withJson(json: ElmJson): ElmConfig =
    copy(json = json)

  def addMapping(cm: CustomMapping): ElmConfig =
    copy(mapping = mapping.andThen(cm))

  def setMapping(cm: CustomMapping): ElmConfig =
    copy(mapping = cm)
}

object ElmConfig {

  val default = ElmConfig()

}
