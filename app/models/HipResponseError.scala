package models

import play.api.libs.json.{Json, OFormat}

case class HipResponseError (`type`:String, reason:String)


object HipResponseError {
  implicit val format: OFormat[HipResponseError] = Json.format[HipResponseError]
}