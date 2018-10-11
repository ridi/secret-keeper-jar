package com.ridi.secretkeeper

import java.time.LocalDateTime

import com.google.api.client.http._
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.testing.http.{HttpTesting, MockHttpTransport, MockLowLevelHttpRequest, MockLowLevelHttpResponse}
import com.ridi.secretkeeper.aws.AWSAPIClient
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}


class FakeClient(secrets: Map[String, String]) extends AWSAPIClient {
  private def buildRequestByResponse(statusCode: Int, content: String): HttpRequest = {
    val tranport = new MockHttpTransport {
      override def buildRequest(method: String, url: String): LowLevelHttpRequest = {
        new MockLowLevelHttpRequest {
          override def execute(): LowLevelHttpResponse = {
            val response = new MockLowLevelHttpResponse
            response.setStatusCode(statusCode)
            response.setContentType("application/json")
            response.setContent(content)
            response
          }
        }
      }
    }
    tranport.createRequestFactory.buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL)
      .setParser(new JsonObjectParser(new JacksonFactory))
  }

  override def createRequest
  (
    service: String, region: String, uri: String,
    requestDateTimeUTC: LocalDateTime, action: String, version: String,
    additionalParams: Map[String, String], payload: String,
    accessKey: String, secretKey: String
  ): HttpRequest = {
    val alias = additionalParams("Name")

    val secretOpt = secrets.get(alias)
    val (statusCode, content) =
      if (secretOpt.isDefined) {
        val secret = secretOpt.get
        (200, s"""{
          |"GetParameterResponse":{
            |"GetParameterResult":{
              |"Parameter":{
              |"ARN":"arn:aws:ssm:us-east-1:123456789012:parameter/sample.secret",
              |"LastModifiedDate":1538721167.781,
              |"Name":"$alias",
              |"Type":"SecureString",
              |"Value":"$secret",
              |"Version":1
              |}
            |},
            |"ResponseMetadata":{
              |"RequestId":"00112233-4455-6677-8899-aabbccddeeff"
            |}
          |}
        |}""".stripMargin.replaceAllLiterally("\n", "")
        )
      }
      else {
        (404, """{
          |"Error":{
            |"Code":"ParameterNotFound",
            |"Type":"Sender",
            |"message":null
          |},
          |"RequestId":"00112233-4455-6677-8899-aabbccddeeff"
        |}""".stripMargin)
      }

    buildRequestByResponse(statusCode, content)
  }
}

class SecretKeeperSpec extends FlatSpec with Matchers with MockFactory {
  val fakeClient = new FakeClient(Map("ones" -> "11111"))
  val secretKeeper = new SecretKeeper(client=fakeClient)

  "SecretKeeper.tell" should "successfully tell a secret" in {
    secretKeeper.tell("ones") should equal ("11111")
  }

  "SecretKeeper.tell" should "raise 404 error if secret is not found" in {
    val caught = intercept[HttpResponseException] {
      secretKeeper.tell("twos")
    }
    caught.getStatusCode should equal (404)
    caught.getContent should include ("ParameterNotFound")
  }

  "SecretKeeper.tellSafe" should "return a left value if secret is not found" in {
    val either = secretKeeper.tellSafe("twos")
    either shouldBe 'Left
  }
}
