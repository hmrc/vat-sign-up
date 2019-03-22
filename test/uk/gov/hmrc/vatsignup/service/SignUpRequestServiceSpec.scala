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
import uk.gov.hmrc.vatsignup.config.featureswitch.{CaptureContactPreference, FeatureSwitching}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEmailVerificationConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser.{EmailNotVerified, EmailVerified}
import uk.gov.hmrc.vatsignup.models.SignUpRequest.EmailAddress
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.SignUpRequestService
import uk.gov.hmrc.vatsignup.services.SignUpRequestService.{EmailVerificationFailure, EmailVerificationRequired, InsufficientData, RequestNotAuthorised}

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
                "the CaptureContactPreference feature switch is enabled" when {
                  "there is a contact preference stored in the database" should {
                    "return a successful SignUpRequest" in {
                      enable(CaptureContactPreference)

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
                          contactPreference = Some(Paper)
                        )
                      )
                    }
                  }
                  "there is a Paper contact preference stored in the database and no sign up e-mail" should {
                    "return a successful SignUpRequest" in {
                      enable(CaptureContactPreference)

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
                          contactPreference = Some(Paper)
                        )
                      )
                    }
                  }
                  "there is no contact preference stored in the database" should {
                    "return a successful SignUpRequest" in {
                      enable(CaptureContactPreference)

                      val testSubscriptionRequest =
                        SubscriptionRequest(
                          vatNumber = testVatNumber,
                          businessEntity = Some(LimitedCompany(testCompanyNumber)),
                          ctReference = Some(testCtReference),
                          email = Some(testEmail),
                          isMigratable = testIsMigratable,
                          isDirectDebit = false
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
                          contactPreference = Some(Digital)
                        )
                      )
                    }
                  }
                }
                "the CaptureContactPreference feature switch is disabled" when {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedCompany(testCompanyNumber)),
                        ctReference = Some(testCtReference),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
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
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
            "the sign up email address is not at verified" should {
              "return EmailVerificationRequired" in {
                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(LimitedCompany(testCompanyNumber)),
                    ctReference = Some(testCtReference),
                    email = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false
                  )

                mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

                val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

                await(res) shouldBe Left(EmailVerificationRequired)
              }
            }
            "the call to verify e-mail fails" should {
              "return EmailVerificationFailure" in {
                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(LimitedCompany(testCompanyNumber)),
                    ctReference = Some(testCtReference),
                    email = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false
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
            "return InsufficientData" in {
              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(LimitedCompany(testCompanyNumber)),
                  ctReference = Some(testCtReference),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(InsufficientData)
            }
          }
        }
        "there is not a stored CT reference" when {
          "return RequestNotAuthorised" in {
            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(LimitedCompany(testCompanyNumber)),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
              )

            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

            val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

            await(res) shouldBe Left(RequestNotAuthorised)
          }
        }
      }
      "there is a stored NINO" when {
        "the NINO source is IR-SA" when {
          "there is a stored sign up email address" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                "the CaptureContactPreference feature switch is enabled" should {
                  "return a successful SignUpRequest" in {
                    enable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(SoleTrader(testNino)),
                        ninoSource = Some(IRSA),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
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
                        contactPreference = Some(testContactPreference)
                      )
                    )
                  }
                }
                "the CaptureContactPreference feature switch is disabled" should {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(SoleTrader(testNino)),
                        ninoSource = Some(IRSA),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
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
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
          }
        }
        "the NINO source is auth profile" when {
          "there is a stored sign up email address" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                "the CaptureContactPreference feature switch is enabled" should {
                  "return a successful SignUpRequest" in {
                    enable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(SoleTrader(testNino)),
                        ninoSource = Some(AuthProfile),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
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
                        contactPreference = Some(testContactPreference)
                      )
                    )
                  }
                }
                "the CaptureContactPreference feature switch is disabled" should {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(SoleTrader(testNino)),
                        ninoSource = Some(AuthProfile),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
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
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
          }
        }
        "the NINO source is UserEntered" when {
          "there is a stored identity verified flag" when {
            "there is a stored sign up email address" when {
              "the sign up email address is verified" when {
                "there is not a transaction e-mail address" when {
                  "the CaptureContactPreference feature switch is enabled" should {
                    "return a successful SignUpRequest" in {
                      enable(CaptureContactPreference)

                      val testSubscriptionRequest =
                        SubscriptionRequest(
                          vatNumber = testVatNumber,
                          businessEntity = Some(SoleTrader(testNino)),
                          ninoSource = Some(UserEntered),
                          email = Some(testEmail),
                          identityVerified = true,
                          isMigratable = testIsMigratable,
                          isDirectDebit = false
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
                          contactPreference = Some(testContactPreference)
                        )
                      )
                    }
                  }
                  "the CaptureContactPreference feature switch is disabled" should {
                    "return a successful SignUpRequest" in {
                      disable(CaptureContactPreference)

                      val testSubscriptionRequest =
                        SubscriptionRequest(
                          vatNumber = testVatNumber,
                          businessEntity = Some(SoleTrader(testNino)),
                          ninoSource = Some(UserEntered),
                          email = Some(testEmail),
                          identityVerified = true,
                          isMigratable = testIsMigratable,
                          isDirectDebit = false
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
                          contactPreference = None
                        )
                      )
                    }
                  }
                }
              }
            }
          }
          "there is not a identity verified flag" should {
            "return RequestNotAuthorised" in {
              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  ninoSource = Some(UserEntered),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(RequestNotAuthorised)
            }
          }
        }
      }
      "there is stored general partnership information" when {
        "the user has a partnership enrolment" when {
          "the sign up email address is verified" when {
            "there is not a transaction e-mail address" when {
              "the CaptureContactPreference feature switch is enabled" should {
                "return a successful SignUpRequest" in {
                  enable(CaptureContactPreference)

                  val testSubscriptionRequest =
                    SubscriptionRequest(
                      vatNumber = testVatNumber,
                      businessEntity = Some(GeneralPartnership(testUtr)),
                      email = Some(testEmail),
                      isMigratable = testIsMigratable,
                      isDirectDebit = false
                    )

                  mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                  mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                  val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                  val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                  await(res) shouldBe Right(
                    SignUpRequest(
                      vatNumber = testVatNumber,
                      businessEntity = GeneralPartnership(testUtr),
                      signUpEmail = Some(verifiedEmail),
                      transactionEmail = verifiedEmail,
                      isDelegated = false,
                      isMigratable = testIsMigratable,
                      contactPreference = Some(testContactPreference)
                    )
                  )
                }
              }
              "the CaptureContactPreference feature switch is disabled" should {
                "return a successful SignUpRequest" in {
                  disable(CaptureContactPreference)

                  val testSubscriptionRequest =
                    SubscriptionRequest(
                      vatNumber = testVatNumber,
                      businessEntity = Some(GeneralPartnership(testUtr)),
                      email = Some(testEmail),
                      isMigratable = testIsMigratable,
                      isDirectDebit = false
                    )

                  mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                  mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                  val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                  val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                  await(res) shouldBe Right(
                    SignUpRequest(
                      vatNumber = testVatNumber,
                      businessEntity = GeneralPartnership(testUtr),
                      signUpEmail = Some(verifiedEmail),
                      transactionEmail = verifiedEmail,
                      isDelegated = false,
                      isMigratable = testIsMigratable,
                      contactPreference = None
                    )
                  )
                }
              }
            }
          }
        }
        "the user is a limited partnership" when {
          "the user has a partnership enrolment" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                "the CaptureContactPreference feature switch is enabled" should {
                  "return a successful SignUpRequest" in {
                    enable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = Some(testContactPreference)
                      )
                    )
                  }
                }
                "the CaptureContactPreference feature switch is disabled" should {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
          }
        }
        "the user is a limited liability partnership" when {
          "the user has a partnership enrolment" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                "the CaptureContactPreference feature switch is enabled" should {
                  "return a successful SignUpRequest" in {
                    enable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedLiabilityPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedLiabilityPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = Some(testContactPreference)
                      )
                    )
                  }
                }
                "the CaptureContactPreference feature switch is disabled" should {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedLiabilityPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedLiabilityPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
          }
        }
        "the user is a scottish limited partnership" when {
          "the user has a partnership enrolment" when {
            "the sign up email address is verified" when {
              "there is not a transaction e-mail address" when {
                "the CaptureContactPreference feature switch is enabled" should {
                  "return a successful SignUpRequest" in {
                    enable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(ScottishLimitedPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = ScottishLimitedPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = Some(testContactPreference)
                      )
                    )
                  }
                }
                "the CaptureContactPreference feature switch is disabled" should {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(ScottishLimitedPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testPartnershipEnrolment)))

                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = ScottishLimitedPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
          }
        }
        "the user is a scottish limited partnership" when {
          "the sign up email address is verified" when {
            "the user does not have a partnership enrolment" when {
              "there is not a transaction e-mail address" when {
                "the CaptureContactPreference feature switch is enabled" should {
                  "return a successful SignUpRequest" in {
                    enable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(ScottishLimitedPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))
                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = ScottishLimitedPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = Some(testContactPreference)
                      )
                    )
                  }
                }
                "the CaptureContactPreference feature switch is disabled" should {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(ScottishLimitedPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))
                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = ScottishLimitedPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
          }
        }
        "the user is a limited liability partnership" when {
          "the sign up email address is verified" when {
            "the user does not have a partnership enrolment" when {
              "there is not a transaction e-mail address" when {
                "the CaptureContactPreference feature switch is enabled" should {
                  "return a successful SignUpRequest" in {
                    enable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedLiabilityPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))
                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedLiabilityPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = Some(testContactPreference)
                      )
                    )
                  }
                }
                "the CaptureContactPreference feature switch is disabled" should {
                  "return a successful SignUpRequest" in {
                    disable(CaptureContactPreference)

                    val testSubscriptionRequest =
                      SubscriptionRequest(
                        vatNumber = testVatNumber,
                        businessEntity = Some(LimitedLiabilityPartnership(testUtr, testCompanyNumber)),
                        email = Some(testEmail),
                        isMigratable = testIsMigratable,
                        isDirectDebit = false
                      )

                    mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
                    mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))

                    val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))
                    val verifiedEmail = EmailAddress(testEmail, isVerified = true)

                    await(res) shouldBe Right(
                      SignUpRequest(
                        vatNumber = testVatNumber,
                        businessEntity = LimitedLiabilityPartnership(testUtr, testCompanyNumber),
                        signUpEmail = Some(verifiedEmail),
                        transactionEmail = verifiedEmail,
                        isDelegated = false,
                        isMigratable = testIsMigratable,
                        contactPreference = None
                      )
                    )
                  }
                }
              }
            }
          }
        }
      }
      "there is not a stored NINO or company number" should {
        "return InsufficientData" in {
          val testSubscriptionRequest =
            SubscriptionRequest(
              vatNumber = testVatNumber,
              email = Some(testEmail),
              isMigratable = testIsMigratable,
              isDirectDebit = false
            )

          mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

          val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

          await(res) shouldBe Left(InsufficientData)
        }
      }

      "the user is a VAT group" when {
        "sign up email is verified" when {
          "the CaptureContactPreference feature switch is enabled" should {
            "return a successful SignUpRequest" in {
              enable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(VatGroup),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = Some(testContactPreference)
                )
              )
            }
          }
          "the CaptureContactPreference feature switch is disabled" should {
            "return a successful SignUpRequest" in {
              disable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(VatGroup),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = None
                )
              )
            }
          }
          "sign up email is not verified" should {
            "return a EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(VatGroup),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }

      "the user is a Registered Society" when {
        "sign up email is verified" when {
          "the CaptureContactPreference feature switch is enabled" should {
            "return a successful SignUpRequest" in {
              enable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = Some(testContactPreference)
                )
              )
            }
          }
          "the CaptureContactPreference feature switch is disabled" should {
            "return a successful SignUpRequest" in {
              disable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = None
                )
              )
            }
          }
          "sign up email is not verified" should {
            "return a EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }
      "the user is a Charity" when {
        "sign up email is verified" when {
          "the CaptureContactPreference feature switch is enabled" should {
            "return a successful SignUpRequest" in {
              enable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(Charity),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = Some(testContactPreference)
                )
              )
            }
          }
          "the CaptureContactPreference feature switch is disabled" should {
            "return a successful SignUpRequest" in {
              disable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(Charity),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = None
                )
              )
            }
          }
          "sign up email is not verified" should {
            "return a EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(Charity),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }

      "the user is a Government Organisation" when {
        "sign up email is verified" when {
          "the CaptureContactPreference feature switch is enabled" should {
            "return a successful SignUpRequest" in {
              enable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(GovernmentOrganisation),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = Some(testContactPreference)
                )
              )
            }
          }
          "the CaptureContactPreference feature switch is disabled" should {
            "return a successful SignUpRequest" in {
              disable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(GovernmentOrganisation),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = None
                )
              )
            }
          }
          "sign up email is not verified" should {
            "return a EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(GovernmentOrganisation),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set.empty))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
        }
      }

      "the user is a Joint Venture" when {
        "sign up email is verified" when {
          "the CaptureContactPreference feature switch is enabled" should {
            "return a successful SignUpRequest" in {
              enable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(JointVenture),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = Some(testContactPreference)
                )
              )
            }
          }
          "the CaptureContactPreference feature switch is disabled" should {
            "return a successful SignUpRequest" in {
              disable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(JointVenture),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = None
                )
              )
            }
          }
          "sign up email is not verified" should {
            "return a EmailVerificationRequired" in {

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(VatGroup),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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

    "the request is delegated" when {
      "there is a stored NINO" when {
        "there is an unverified sign up e-mail" when {
          "the CaptureContactPreference feature switch is enabled" should {
            "return a successful SignUpRequest" in {
              enable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = Some(testContactPreference)
                )
              )
            }
          }
          "the CaptureContactPreference feature switch is disabled" should {
            "return a successful SignUpRequest" in {
              disable(CaptureContactPreference)

              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  email = Some(testEmail),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
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
                  contactPreference = None
                )
              )
            }
          }
        }
        "there is not a sign up e-mail" when {
          "there is a verified transaction e-mail" when {
            "the CaptureContactPreference feature switch is enabled" should {
              "return a successful SignUpRequest" in {
                enable(CaptureContactPreference)

                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(SoleTrader(testNino)),
                    transactionEmail = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false
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
                    contactPreference = Some(testContactPreference)
                  )
                )
              }
            }
            "the CaptureContactPreference feature switch is disabled" should {
              "return a successful SignUpRequest" in {
                disable(CaptureContactPreference)

                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(SoleTrader(testNino)),
                    transactionEmail = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false
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
                    contactPreference = None
                  )
                )
              }
            }
          }
          "there is an unverified transaction e-mail" should {
            "return EmailVerificationRequired" in {
              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  transactionEmail = Some(testEmail),
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

              await(res) shouldBe Left(EmailVerificationRequired)
            }
          }
          "there is not a transaction e-mail" should {
            "return InsufficientData" in {
              val testSubscriptionRequest =
                SubscriptionRequest(
                  vatNumber = testVatNumber,
                  businessEntity = Some(SoleTrader(testNino)),
                  isMigratable = testIsMigratable,
                  isDirectDebit = false
                )

              mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

              val res = TestSignUpRequestService.getSignUpRequest(testVatNumber, Enrolments(Set(testAgentEnrolment)))

              await(res) shouldBe Left(InsufficientData)
            }
          }
        }

        "The request is a company" when {
          "there is a verified transaction e-mail" when {
            "the CaptureContactPreference feature switch is enabled" should {
              "return a successful SignUpRequest" in {
                enable(CaptureContactPreference)

                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(LimitedCompany(testCompanyNumber)),
                    email = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false
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
                    contactPreference = Some(testContactPreference)
                  )
                )
              }
            }
            "the CaptureContactPreference feature switch is disabled" should {
              "return a successful SignUpRequest" in {
                disable(CaptureContactPreference)

                val testSubscriptionRequest =
                  SubscriptionRequest(
                    vatNumber = testVatNumber,
                    businessEntity = Some(LimitedCompany(testCompanyNumber)),
                    email = Some(testEmail),
                    isMigratable = testIsMigratable,
                    isDirectDebit = false
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
                    contactPreference = None
                  )
                )
              }
            }
          }
        }
      }

      "the user is a VAT group" when {
        "the CaptureContactPreference feature switch is enabled" should {
          "return a successful SignUpRequest" in {
            enable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(VatGroup),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = Some(testContactPreference)
              )
            )
          }
        }
        "the CaptureContactPreference feature switch is disabled" should {
          "return a successful SignUpRequest" in {
            disable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(VatGroup),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = None
              )
            )
          }
        }
      }

      "the user is a Division" when {
        "the CaptureContactPreference feature switch is enabled" should {
          "return a successful SignUpRequest" in {
            enable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(AdministrativeDivision),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = Some(testContactPreference)
              )
            )
          }
        }
        "the CaptureContactPreference feature switch is disabled" should {
          "return a successful SignUpRequest" in {
            disable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(AdministrativeDivision),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = None
              )
            )
          }
        }
      }

      "the user is a Unincorporated Association" should {
        "the CaptureContactPreference feature switch is enabled" should {
          "return a successful SignUpRequest" in {
            enable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(UnincorporatedAssociation),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = Some(testContactPreference)
              )
            )
          }
        }
        "the CaptureContactPreference feature switch is disabled" should {
          "return a successful SignUpRequest" in {
            disable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(UnincorporatedAssociation),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = None
              )
            )
          }
        }
      }

      "the user is a Registered Society" should {
        "the CaptureContactPreference feature switch is enabled" should {
          "return a successful SignUpRequest" in {
            enable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = Some(testContactPreference)
              )
            )
          }
        }
        "the CaptureContactPreference feature switch is disabled" should {
          "return a successful SignUpRequest" in {
            disable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(RegisteredSociety(testCompanyNumber)),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = None
              )
            )
          }
        }
      }

      "the user is a Charity" should {
        "the CaptureContactPreference feature switch is enabled" should {
          "return a successful SignUpRequest" in {
            enable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(Charity),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = Some(testContactPreference)
              )
            )
          }
        }
        "the CaptureContactPreference feature switch is disabled" should {
          "return a successful SignUpRequest" in {
            disable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(Charity),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = None
              )
            )
          }
        }
      }

      "the user is a Government Organisation" should {
        "the CaptureContactPreference feature switch is enabled" should {
          "return a successful SignUpRequest" in {
            enable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(GovernmentOrganisation),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = Some(testContactPreference)
              )
            )
          }
        }
        "the CaptureContactPreference feature switch is disabled" should {
          "return a successful SignUpRequest" in {
            disable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(GovernmentOrganisation),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = None
              )
            )
          }
        }
      }

      "the user is a Joint Venture" when {
        "the CaptureContactPreference feature switch is enabled" should {
          "return a successful SignUpRequest" in {
            enable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(JointVenture),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = Some(testContactPreference)
              )
            )
          }
        }
        "the CaptureContactPreference feature switch is disabled" should {
          "return a successful SignUpRequest" in {
            disable(CaptureContactPreference)

            val testSubscriptionRequest =
              SubscriptionRequest(
                vatNumber = testVatNumber,
                businessEntity = Some(JointVenture),
                email = Some(testEmail),
                isMigratable = testIsMigratable,
                isDirectDebit = false
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
                contactPreference = None
              )
            )
          }
        }
      }

    }
  }
}
