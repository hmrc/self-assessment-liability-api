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

package utils

import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.authorise.Predicate
import utils.constants.EnrolmentConstants.*

object SelfAssessmentEnrolments {
  def delegatedEnrolments(utr: String, mtdId: String): Predicate = {
    Enrolment(IR_SA_Enrolment_Key)
      .withIdentifier(IR_SA_Identifier, utr)
      .withDelegatedAuthRule(IR_SA_Delegated_Auth_Rule) or Enrolment(Mtd_Enrolment_Key)
      .withIdentifier(Mtd_Identifier, mtdId)
      .withDelegatedAuthRule(Mtd_Delegated_Auth_Rule)

  }
  val principleAgentEnrolments: Predicate =
    Enrolment(ASA_Enrolment_Key) or Enrolment(IR_SA_AGENT_Key)

  def legacySaEnrolment(utr: String): Predicate = Enrolment(IR_SA_Enrolment_Key).withIdentifier(
    IR_SA_Identifier,
    utr
  )

  def mtdSaEnrolment(mtdId: String): Predicate = Enrolment(Mtd_Enrolment_Key).withIdentifier(
    Mtd_Identifier,
    mtdId
  )
}
