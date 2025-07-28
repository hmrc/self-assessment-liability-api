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

sealed abstract class ServiceErrors extends Throwable {
  override def toString: String = getClass.getSimpleName.replace("$", "")
}

object ServiceErrors {
  case object Downstream_Error extends ServiceErrors
  case object Service_Currently_Unavailable extends ServiceErrors
  case object Invalid_SAUTR extends ServiceErrors
  case object Invalid_NINO extends ServiceErrors
  case object More_Than_One_NINO_Found_For_SAUTR extends ServiceErrors
  case object No_NINO_Found_For_SAUTR extends ServiceErrors
  case object Not_Allowed extends ServiceErrors
  case object Low_Confidence extends ServiceErrors
}
