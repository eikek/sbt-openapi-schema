package com.github.eikek.sbt.openapi

case class Property(name: String
  , `type`: Type
  , nullable: Boolean = false
  , format: String = ""
  , pattern: String = ""
  , doc: Doc = Doc.empty) {

  def optional: Property =
    if (nullable) this else copy(nullable = true)
}
