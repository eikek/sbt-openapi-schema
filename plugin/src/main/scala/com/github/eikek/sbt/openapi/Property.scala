package com.github.eikek.sbt.openapi

case class Property(name: String
  , `type`: Type
  , nullable: Boolean = false
  , format: Option[String] = None
  , pattern: Option[String] = None
  , doc: Doc = Doc.empty
  , discriminator: Boolean = false) {

  def optional: Property =
    if (nullable) this else copy(nullable = true)
}
