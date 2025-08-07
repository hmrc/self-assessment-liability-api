/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models
import play.api.libs.json.*

case class CidPerson(name: Option[CidNames], ids: TaxIds, dateOfBirth: Option[String])

case class CidNames(current: Option[CidName], previous: Option[List[CidName]])

case class CidName(firstName: Option[String], lastName: Option[String])

case class TaxIds(nino: Option[String])

object CidPerson {
  implicit val format: OFormat[CidPerson] = Json.format[CidPerson]
}

object CidName {
  implicit val format: OFormat[CidName] = Json.format[CidName]
}

object CidNames {
  implicit val format: OFormat[CidNames] = Json.format[CidNames]
}

object TaxIds {
  implicit val format: OFormat[TaxIds] = Json.format[TaxIds]
}
