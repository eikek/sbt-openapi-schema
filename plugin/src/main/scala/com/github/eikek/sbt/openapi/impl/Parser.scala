package com.github.eikek.sbt.openapi.impl

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.oas.models.media._
import scala.collection.JavaConverters._

import StringUtil._
import com.github.eikek.sbt.openapi._

object Parser {
  private val parser = new OpenAPIV3Parser()

  def parse(file: String): Map[String, SingularSchemaClass] = {
    // see http://javadoc.io/doc/io.swagger.core.v3/swagger-models/2.0.7
    // http://javadoc.io/doc/io.swagger.parser.v3/swagger-parser-v3/2.0.9
    val oapi = parser.read(file)

    oapi.getComponents.getSchemas.asScala.toMap.
      map({ case (name, schema) =>
        name -> makeSchemaClass(name, schema)
      })
  }

  def makeSchemaClass(name: String, schema: Schema[_]): SingularSchemaClass =
    schema match {
      case cs: ComposedSchema =>
        val allOfSchemas = cs.getAllOf.asScala

        val discriminatorOpt = allOfSchemas.collectFirst {
          case s: Schema[_] if s.getDiscriminator != null => s.getName
        } orElse allOfSchemas.collectFirst { case s: Schema[_] if s.get$ref() != null => s.get$ref().split('/').last }

        val allProperties = allOfSchemas.flatMap { innerSchema =>
          innerSchema match {
            case s if s.getProperties != null =>
              val required = Option(s.getRequired).map(_.asScala.toSet).getOrElse(Set.empty)
              Option(s.getProperties.asScala.map({case (n, ps) => makeProperty(n, ps, required, None)}).toList)
            case _ =>
              None
          }
        }
        SingularSchemaClass(name, allProperties.toList.flatten, Doc(cs.getDescription.nullToEmpty), discriminatorRef = discriminatorOpt)
      case s if s.getProperties != null =>
        val required = Option(s.getRequired).map(_.asScala.toSet).getOrElse(Set.empty)
        val discriminatorName = Option(s.getDiscriminator).map(_.getPropertyName)
        val props = s.getProperties.asScala.map({case (n, ps) => makeProperty(n, ps, required, discriminatorName)}).toList
        SingularSchemaClass(name, props, Doc(s.getDescription.nullToEmpty))
      case _ =>
        val discriminatorName = Option(schema.getDiscriminator).map(_.getPropertyName)
        SingularSchemaClass(name, wrapper = true) + makeProperty("value", schema, Set("value"), discriminatorName)
    }

  def makeProperty(name: String, schema: Schema[_], required: String => Boolean, discriminatorName: Option[String]): Property = {
    val p = Property(name, schemaType(schema)
      , format = schema.getFormat.asNonEmpty
      , pattern = schema.getPattern.asNonEmpty
      , nullable = schema.getNullable == true || !required(name)
      , doc = Doc(schema.getDescription.nullToEmpty)
      , discriminator = discriminatorName.contains(name))
    p
  }

  //TODO missing: BinarySchema, ByteArraySchema, FileSchema, MapSchema
  def schemaType(sch: Schema[_]): Type =
    sch match {
      case s: ArraySchema =>
        Type.Sequence(schemaType(s.getItems))
      case _: BooleanSchema =>
        Type.Bool
      case _: DateSchema =>
        Type.Date(Type.TimeRepr.String)
      case _: DateTimeSchema =>
        Type.DateTime(Type.TimeRepr.String)
      case s: IntegerSchema =>
        if ("int64" == s.getFormat) Type.Int64
        else if ("date-time" == s.getFormat) Type.DateTime(Type.TimeRepr.Number)
        else if ("date" == s.getFormat) Type.Date(Type.TimeRepr.Number)
        else Type.Int32
      case s: NumberSchema =>
        s.getFormat.nullToEmpty.toLowerCase match {
          case "float" => Type.Float32
          case "double" => Type.Float64
          case _ => sys.error(s"Unsupported number type: $s")
        }
      case _: PasswordSchema =>
        Type.String
      case _: EmailSchema =>
        Type.String
      case s: StringSchema if "url".equalsIgnoreCase(s.getFormat) =>
        Type.Url
      case s: StringSchema if "uri".equalsIgnoreCase(s.getFormat) =>
        Type.Uri
      case _: StringSchema =>
        Type.String
      case _: UUIDSchema =>
        Type.Uuid
      case s: ObjectSchema if s.getAdditionalProperties != null =>
        s.getAdditionalProperties match {
          case ps: Schema[_] =>
            Type.Map(Type.String, schemaType(ps))
          case _ =>
            sys.error("An object schema with value types `Object` and `AnyRef`, respectively, is not supported.")
        }
      case s: MapSchema if s.getAdditionalProperties != null =>
        s.getAdditionalProperties match {
          case ps: Schema[_] =>
            Type.Map(Type.String, schemaType(ps))
          case _ =>
            sys.error("An object schema with value types `Object` and `AnyRef`, respectively, is not supported.")
        }
      case s if s.get$ref != null =>
        Type.Ref(s.get$ref.split('/').last)
      case _ =>
        sys.error(s"Unsupported schema: $sch")
    }
}
