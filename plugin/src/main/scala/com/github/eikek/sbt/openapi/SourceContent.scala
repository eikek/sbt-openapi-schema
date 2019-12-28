package com.github.eikek.sbt.openapi

sealed trait SourceContent
case class SingleSchema(fields: List[Field]) extends SourceContent
case class GroupedSchemas(schemas: List[SingleSchema]) extends SourceContent
