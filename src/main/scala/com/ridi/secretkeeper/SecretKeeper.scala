package com.ridi.secretkeeper

import java.time.{OffsetDateTime, ZoneOffset}

import com.google.api.client.util.ArrayMap
import com.ridi.secretkeeper.aws.{AWSAPIClient, AWSAPIClientImpl}
import org.apache.logging.log4j.{LogManager, Logger}


object SecretKeeper {
  val secretKeeper = new SecretKeeper(new AWSAPIClientImpl())
  def tell(alias: String): String = secretKeeper.tell(alias)
  def tellSafe(alias: String): Either[Exception, String] = secretKeeper.tellSafe(alias)
}

class SecretKeeper(private val client: AWSAPIClient) {
  val log: Logger = LogManager.getLogger(this.getClass)
  def tell(alias: String): String = {

    val accessKey = sys.env("SECRETKEEPER_AWS_ACCESS_KEY")
    val secretKey = sys.env("SECRETKEEPER_AWS_SECRET_KEY")
    val region = sys.env("SECRETKEEPER_AWS_REGION")

    val uri = "/"
    val requestDateTime = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime

    val service = "ssm"
    val action = "GetParameter"
    val version = "2014-11-06"
    val additionalParams = Map("Name" -> alias, "WithDecryption" -> "true")
    val payload = ""

    val json = client.sendRequest(
      service, region, uri, requestDateTime,
      action, version, additionalParams, payload, accessKey, secretKey
    )

    json
      .get("GetParameterResponse").asInstanceOf[ArrayMap[String, Object]]
      .get("GetParameterResult").asInstanceOf[ArrayMap[String, Object]]
      .get("Parameter").asInstanceOf[ArrayMap[String, Object]]
      .get("Value").asInstanceOf[String]

  }

  def tellSafe(alias: String): Either[Exception, String] =
    try {
      Right(tell(alias))
    } catch {
      case e: Exception => Left(e)
    }
}

