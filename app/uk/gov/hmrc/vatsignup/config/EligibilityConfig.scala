/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.config


case class EligibilityConfig(
                              permitBelowVatThreshold: Boolean,
                              permitAnnualStagger: Boolean,
                              permitMissingReturns: Boolean,
                              permitCentralAssessments: Boolean,
                              permitCriminalInvestigationInhibits: Boolean,
                              permitCompliancePenaltiesOrSurcharges: Boolean,
                              permitInsolvency: Boolean,
                              permitDeRegOrDeath: Boolean,
                              permitDebtMigration: Boolean,
                              permitDirectDebit: Boolean,
                              permitEuSalesOrPurchases: Boolean,
                              permitLargeBusiness: Boolean,
                              permitMissingTrader: Boolean,
                              permitMonthlyStagger: Boolean,
                              permitNonStandardTaxPeriod: Boolean,
                              permitOverseasTrader: Boolean,
                              permitPoaTrader: Boolean,
                              permitStagger1: Boolean,
                              permitStagger2: Boolean,
                              permitStagger3: Boolean,
                              permitCompany: Boolean,
                              permitDivision: Boolean,
                              permitGroup: Boolean,
                              permitPartnership: Boolean,
                              permitPublicCorporation: Boolean,
                              permitSoleTrader: Boolean,
                              permitLocalAuthority: Boolean,
                              permitNonProfit: Boolean,
                              permitDificTrader: Boolean,
                              permitAnythingUnderAppeal: Boolean,
                              permitRepaymentTrader: Boolean,
                              permitMossTrader: Boolean
                            )


