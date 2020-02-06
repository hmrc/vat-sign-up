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

package uk.gov.hmrc.vatsignup.helpers

import helpers.WiremockHelper
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Writes
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.vatsignup.config.featureswitch.{FeatureSwitch, FeatureSwitching}
import uk.gov.hmrc.vatsignup.utils.CurrentDateProvider

import scala.concurrent.Future
import scala.reflect.ClassTag

trait ComponentSpecBase extends WordSpec with Matchers with GuiceOneServerPerSuite with WiremockHelper
  with BeforeAndAfterAll with BeforeAndAfterEach with FeatureSwitching with DefaultAwaitTimeout with FutureAwaits with OptionValues {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  lazy val currentDateProvider = new CurrentDateProvider()

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Test))
    .bindings(bind[CurrentDateProvider].toInstance(currentDateProvider))
    .configure(config)
    .build

  private val mockHost = WiremockHelper.wiremockHost
  private val mockPort = WiremockHelper.wiremockPort.toString
  private val mockUrl = s"http://$mockHost:$mockPort"

  def config: Map[String, String] = Map(
    "microservice.services.base.host" -> mockHost,
    "microservice.services.base.port" -> mockPort,
    "microservice.services.agent-client-relationships.url" -> mockUrl,
    "microservice.services.des.url" -> mockUrl,
    "basicAuthentication.username" -> "username",
    "basicAuthentication.password" -> "password",
    "basicAuthentication.realm" -> "realm"
  ) ++ mockedServices(
    "auth",
    "tax-enrolments",
    "email-verification",
    "authenticator",
    "identity-verification-frontend",
    "vat-subscription",
    "email",
    "enrolment-store-proxy",
    "users-groups-search"
  )

  private def mockedServices(serviceNames: String*) =
    (serviceNames flatMap {
      serviceName =>
        Seq(
          s"microservice.services.$serviceName.host" -> mockHost,
          s"microservice.services.$serviceName.port" -> mockPort
        )
    }).toMap

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    FeatureSwitch.switches foreach disable
    resetWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def get[T](uri: String): WSResponse = {
    await(
      buildClient(uri).get
    )
  }

  def post[T](uri: String)(body: T, additionalHeaders: (String, String)*)(implicit writes: Writes[T]): WSResponse = {
    await(
      buildClient(uri)
        .withHttpHeaders(additionalHeaders :+ "Content-Type" -> "application/json": _*)
        .post(writes.writes(body).toString())
    )
  }

  def put[T](uri: String)(body: T)(implicit writes: Writes[T]): WSResponse = {
    await(
      buildClient(uri)
        .withHttpHeaders("Content-Type" -> "application/json")
        .put(writes.writes(body).toString())
    )
  }

  def intercept[T <: AnyRef](res: => Future[Any])(implicit classTag: ClassTag[T]): T = {
    super.intercept[T](await(res))
  }

  def buildClient(path: String): WSRequest = ws.url(s"http://localhost:$port/vat-sign-up$path").withFollowRedirects(false)

}
