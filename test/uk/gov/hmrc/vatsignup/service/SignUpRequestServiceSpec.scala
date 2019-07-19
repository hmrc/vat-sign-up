/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.service

import play.api.http.Status
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.FeatureSwitching
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEmailVerificationConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser.{EmailNotVerified, EmailVerified}
import uk.gov.hmrc.vatsignup.models.SignUpRequest.EmailAddress
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.controllist.Division
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.SignUpRequestService
import uk.gov.hmrc.vatsignup.services.SignUpRequestService.{EmailVerificationFailure, EmailVerificationRequired, InsufficientData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignUpRequestServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository with MockEmailVerificationConnector with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  object TestSignUpRequestService extends SignUpRequestService(
    mockSubscriptionRequestRepository,
    mockEmailVerificationConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testIsMigratable = true

  "getSignUpRequest" when {
    "the request is not a delegated" when {
      "there is a stored company number" when {
        "there is a stored CT reference" when {
          "there is a stored sign up email address" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" should {
                  "there is a contact preference stored in the database" should {
                    s"return a successful $SignUpRequest" in {

                      val testSubscriptionRequest =
                        SubscriptionRequest(
                          vatNumber = testVatNumber,
                          businessEntity = Some(LimitedCompany(testCompanyNumber)),
                          ctReference = Some(testCtReference),
                          email = Some(testEmail),
                          isMigratable = testIsMigratable,
                          isDirectDebit = false,
                          contactPreference = Some(Paper)
                        )

                      mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                      mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                      val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

                      val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                      await(res) shouldBe Right(
                        SignUpRequest(
                          vatNumber = testVatNumber,
                          businessEntity = LimitedCompany(testCompanyNumber),
                          signUpEmail = Some(verifiedEmail),
                          transactionEmail = verifiedEmail,
                          isDelegated = false,
                          isMigratable = testIsMigratable,
                          contactPreference = Paper
                        )
                      )
                    }
                  }
                  s"there is a $Paper contact preference stored in the database and no sign up e-mail" should {
                    s"return a successful $SignUpRequest" in {

                      val testSubscriptionRequest =
                        SubscriptionRequest(
                          vatNumber = testVatNumber,
                          businessEntity = Some(LimitedCompany(testCompanyNumber)),
                          ctReference = Some(testCtReference),
                          transactionEmail = Some(testEmail),
                          email = None,
                          isMigratable = testIsMigratable,
                          isDirectDebit = false,
                          contactPreference = Some(Paper)
                        )

                      mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                      mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                      val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

                      val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                      await(res) shouldBe Right(
                        SignUpRequest(
                          vatNumber = testVatNumber,
                          businessEntity = LimitedCompany(testCompanyNumber),
                          signUpEmail = None,
                          transactionEmail = verifiedEmail,
                          isDelegated = false,
                          isMigratable = testIsMigratable,
                          contactPreference = Paper
                        )
                      )
                    }
                  }
                  s"there is $None for contact preference stored in the database" should {
                    s"return a Left $InsufficientData" in {

                      val testSubscriptionRequest =
                        SubscriptionRequest(
                          vatNumber = testVatNumber,
                          businessEntity = Some(LimitedCompany(testCompanyNumber)),
                          ctReference = Some(testCtReference),
                          email = Some(testEmail),
                          isMigratable = testIsMigratable,
                          isDirectDebit = false,
                          contactPreference = None
                        )

                      mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                      mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                      val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

                      val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                      await(res) shouldBe Left(InsufficientData)
                    }
                  }
              }
            }
            "the sign up email address is not verified" should {
              s"return $EmailVerificationRequired" in {
                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(LimitedCompany(testCompanyNumber)),
                    ctReference = Some(testCtReference),
                    email = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false,
                    contactPreference = Some(testContactPreference)
                  )

                mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

                val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

                await(res) shouldBe Left(EmailVerificationRequired)
              }
            }
            "the call to verify e-mail fails" should {
              s"return $EmailVerificationFailure" in {
                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(LimitedCompany(testCompanyNumber)),
                    ctReference = Some(testCtReference),
                    email = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false,
                    contactPreference = Some(testContactPreference)
                  )

                mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                mockGetEmailVerificationState(testEmail)(
                  Future.successful(Left(GetEmailVerificationStateHttpParser.GetEmailVerificationStateErrorResponse(Status.INTERNAL_SERVER_ERROR, "")))
                )

                val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

                await(res) shouldBe Left(EmailVerificationFailure)
              }
            }
          }
          "there is not a stored email address" should {
            s"return $InsufficientData" in {
              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(LimitedCompany(testCompanyNumber)),
                  ctReference = Some(testCtReference),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(InsufficientData)
            }
          }
        }
        "there is not a stored CT reference" when {
          s"return a $SignUpRequest" in {
            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(LimitedCompany(testCompanyNumber)),
                ctReference = None,
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = LimitedCompany(testCompanyNumber),
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
        }
      }
      "there is a stored NINO" when {
        "there is a stored sign up email address" when {
          "the sign up email address is verified" when {
            "there is not a transaction e-mail address" when {
                s"return a successful $SignUpRequest" in {

                  val testSubscriptionRequest =
                    SubscriptionRequest(
                      vatNumber = testVatNumber,
                      businessEntity = Some(SoleTrader(testNino)),
                      ninoSource = Some(IRSA),
                      email = Some(testEmail),
                      isMigratable = testIsMigratable,
                      isDirectDebit = false,
                      contactPreference = Some(testContactPreference)
                    )

                  mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                  mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                  val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

                  val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                  await(res) shouldBe Right(
                    SignUpRequest(
                      vatNumber = testVatNumber,
                      businessEntity = SoleTrader(testNino),
                      signUpEmail = Some(verifiedEmail),
                      transactionEmail = verifiedEmail,
                      isDelegated = false,
                      isMigratable = testIsMigratable,
                      contactPreference = testContactPreference
                    )
                  )
                }
            }
          }
        }
      }
      s"there is stored $GeneralPartnership information" when {
        "the user has a partnership enrolment" when {
          "the sign up email address is verified" when {
            "there is not a transaction e-mail address" when {
                s"return a successful $SignUpRequest" in {

                  val testSubscriptionRequest =
                    SubscriptionRequest(
                      vatNumber = testVatNumber,
                      businessEntity = Some(GeneralPartnership(Some(testUtr))),
                      email = Some(testEmail),
                      isMigratable = testIsMigratable,
                      isDirectDebit = false,
                      contactPreference = Some(testContactPreference)
                    )

                  mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                  mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                  val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                  val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                  await(res) shouldBe Right(
                    SignUpRequest(
                      vatNumber = testVatNumber,
                      businessEntity = GeneralPartnership(Some(testUtr)),
                      signUpEmail = Some(verifiedEmail),
                      transactionEmail = verifiedEmail,
                      isDelegated = false,
                      isMigratable = testIsMigratable,
                      contactPreference = testContactPreference
                    )
                  )
                }
            }
          }
        }
        s"the user is a $LimitedPartnership" when {
          "the user has a partnership enrolment" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                  s"return a successful $SignUpRequest" in {

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedPartnership(Some(testUtr), testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false,
                        contactPreference = Some(testContactPreference)
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedPartnership(Some(testUtr), testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = testContactPreference
                      )
                    )
                  }
              }
            }
          }
        }
        s"the user is a $LimitedLiabilityPartnership" when {
          "the user has a partnership enrolment" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                  s"return a successful $SignUpRequest" in {

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false,
                        contactPreference = Some(testContactPreference)
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = testContactPreference
                      )
                    )
                  }
              }
            }
          }
        }
        s"the user is a $ScottishLimitedPartnership" when {
          "the user has a partnership enrolment" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                  s"return a successful $SignUpRequest" in {

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(ScottishLimitedPartnership(Some(testUtr), testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false,
                        contactPreference = Some(testContactPreference)
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = ScottishLimitedPartnership(Some(testUtr), testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = testContactPreference
                      )
                    )
                  }
              }
            }
          }
        }
        s"the user is a $ScottishLimitedPartnership" when {
          "the sign up email address is verified" when {
            "the user does not have a partnership enrolment" when {
              "there is not a transaction e-mail address" when {
                  s"return a successful $SignUpRequest" in {

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(ScottishLimitedPartnership(Some(testUtr), testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false,
                        contactPreference = Some(testContactPreference)
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))
                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = ScottishLimitedPartnership(Some(testUtr), testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = testContactPreference
                      )
                    )
                  }
              }
            }
          }
        }
        s"the user is a $LimitedLiabilityPartnership" when {
          "the sign up email address is verified" when {
            "the user does not have a partnership enrolment" when {
              "there is not a transaction e-mail address" when {
                  s"return a successful $SignUpRequest" in {

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false,
                        contactPreference = Some(testContactPreference)
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))
                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = testContactPreference
                      )
                    )
                  }
              }
            }
          }
        }
      }
      "there is not a stored NINO or company number" should {
        s"return $InsufficientData" in {
          val testSubscriptionRequest =
            SubscriptionRequest(
              vatNumber = testVatNumber,
              email = Some(testEmail),
              isMigratable = testIsMigratable,
              isDirectDebit = false,
              contactPreference = Some(testContactPreference)
            )

          mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

          val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

          await(res) shouldBe Left(InsufficientData)
        }
      }

      s"the user is a $VatGroup" when {
        "sign up email is verified" when {
            s"return a successful $SignUpRequest" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(VatGroup),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

              val verifiedEmail = EmailAddress(testEmail, isVerified = true)

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Right(
                SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = VatGroup,
                  signUpEmail = Some(verifiedEmail),
                  transactionEmail = verifiedEmail,
                  isDelegated = false,
                  isMigratable = testIsMigratable,
                  contactPreference = testContactPreference
                )
              )
            }
          "sign up email is not verified" should {
            s"return a $EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(VatGroup),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }

      s"the user is a $RegisteredSociety" when {
        "sign up email is verified" when {
            s"return a successful $SignUpRequest" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

              val verifiedEmail = EmailAddress(testEmail, isVerified = true)

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Right(
                SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = RegisteredSociety(testCompanyNumber),
                  signUpEmail = Some(verifiedEmail),
                  transactionEmail = verifiedEmail,
                  isDelegated = false,
                  isMigratable = testIsMigratable,
                  contactPreference = testContactPreference
                )
              )
            }
          "sign up email is not verified" should {
            s"return a $EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }
      s"the user is a $Charity" when {
        "sign up email is verified" when {
            s"return a successful $SignUpRequest" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(Charity),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

              val verifiedEmail = EmailAddress(testEmail, isVerified = true)

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Right(
                SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Charity,
                  signUpEmail = Some(verifiedEmail),
                  transactionEmail = verifiedEmail,
                  isDelegated = false,
                  isMigratable = testIsMigratable,
                  contactPreference = testContactPreference
                )
              )
            }
          "sign up email is not verified" should {
            s"return a $EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(Charity),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }

      s"the user is a $GovernmentOrganisation" when {
        "sign up email is verified" when {
            s"return a successful $SignUpRequest" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(GovernmentOrganisation),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

              val verifiedEmail = EmailAddress(testEmail, isVerified = true)

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Right(
                SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = GovernmentOrganisation,
                  signUpEmail = Some(verifiedEmail),
                  transactionEmail = verifiedEmail,
                  isDelegated = false,
                  isMigratable = testIsMigratable,
                  contactPreference = testContactPreference
                )
              )
            }
          "sign up email is not verified" should {
            s"return a $EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(GovernmentOrganisation),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }

      s"the user is a $JointVenture" when {
        "sign up email is verified" when {
            s"return a successful $SignUpRequest" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(JointVenture),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

              val verifiedEmail = EmailAddress(testEmail, isVerified = true)

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Right(
                SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = JointVenture,
                  signUpEmail = Some(verifiedEmail),
                  transactionEmail = verifiedEmail,
                  isDelegated = false,
                  isMigratable = testIsMigratable,
                  contactPreference = testContactPreference
                )
              )
            }
          "sign up email is not verified" should {
            s"return a $EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(VatGroup),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }

    }

    s"the request is delegated && $SoleTrader" when {
      "there is a stored NINO" when {
        "there is an unverified sign up e-mail" when {
            s"return a successful $SignUpRequest" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

              val unverifiedEmail = EmailAddress(testEmail, isVerified = false)

              await(res) shouldBe Right(
                SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = SoleTrader(testNino),
                  signUpEmail = Some(unverifiedEmail),
                  transactionEmail = unverifiedEmail,
                  isDelegated = true,
                  isMigratable = testIsMigratable,
                  contactPreference = testContactPreference
                )
              )
            }
        }
        "there is not a sign up e-mail" when {
          "there is a verified transaction e-mail" when {
              s"return a successful $SignUpRequest" in {

                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(SoleTrader(testNino)),
                    transactionEmail = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false,
                    contactPreference = Some(testContactPreference)
                  )

                mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

                val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                await(res) shouldBe Right(
                  SignUpRequest(
                    vatNumber = testVatNumber,
                    businessEntity = SoleTrader(testNino),
                    signUpEmail = None,
                    transactionEmail = verifiedEmail,
                    isDelegated = true,
                    isMigratable = testIsMigratable,
                    contactPreference = testContactPreference
                  )
                )
              }
          }
          "there is an unverified transaction e-mail" should {
            s"return $EmailVerificationRequired" in {
              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  transactionEmail = Some(testEmail),
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
          "there is not a transaction e-mail" should {
            s"return $InsufficientData" in {
              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false,
                  contactPreference = Some(testContactPreference)
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

              await(res) shouldBe Left(InsufficientData)
            }
          }
        }

        s"The request is a $LimitedCompany" when {
          "there is a verified transaction e-mail" when {
              s"return a successful $SignUpRequest" in {

                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(LimitedCompany(testCompanyNumber)),
                    email = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false,
                    contactPreference = Some(testContactPreference)
                  )

                mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

                val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                await(res) shouldBe Right(
                  SignUpRequest(
                    vatNumber = testVatNumber,
                    businessEntity = LimitedCompany(testCompanyNumber),
                    signUpEmail = Some(verifiedEmail),
                    transactionEmail = verifiedEmail,
                    isDelegated = true,
                    isMigratable = testIsMigratable,
                    contactPreference = testContactPreference
                  )
                )
              }
          }
        }
      }

      s"the user is a $VatGroup" when {
          s"return a successful $SignUpRequest" in {

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(VatGroup),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = VatGroup,
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
      }

      s"the user is a $Division" when {
          s"return a successful $SignUpRequest" in {

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(AdministrativeDivision),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = AdministrativeDivision,
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
      }

      s"the user is a $UnincorporatedAssociation" should {
          s"return a successful $SignUpRequest" in {

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(UnincorporatedAssociation),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = UnincorporatedAssociation,
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
      }

      s"the user is a $RegisteredSociety" should {
          s"return a successful $SignUpRequest" in {

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = RegisteredSociety(testCompanyNumber),
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
      }

      s"the user is a $Charity" should {
          s"return a successful $SignUpRequest" in {

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(Charity),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = Charity,
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
      }

      s"the user is a $GovernmentOrganisation" should {
          s"return a successful $SignUpRequest" in {

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(GovernmentOrganisation),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = GovernmentOrganisation,
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
      }

      s"the user is a $JointVenture" when {
          s"return a successful $SignUpRequest" in {

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(JointVenture),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false,
                contactPreference = Some(testContactPreference)
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

            val verifiedEmail = EmailAddress(testEmail, isVerified = true)

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

            await(res) shouldBe Right(
              SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = JointVenture,
                signUpEmail = Some(verifiedEmail),
                transactionEmail = verifiedEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )
            )
          }
      }

    }
  }
}
