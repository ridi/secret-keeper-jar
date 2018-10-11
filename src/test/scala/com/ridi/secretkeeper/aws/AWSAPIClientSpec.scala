package com.ridi.secretkeeper.aws

import java.time.LocalDateTime
import java.util

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class AWSAPIClientSpec extends FlatSpec with Matchers {
  "AWSAPIHelper" should "correctly create and sign a request" in {
    val requestTimeUTC = LocalDateTime.of(2015, 8, 30, 12, 36, 0, 0)
    val request = (new AWSAPIClientImpl).createRequest(
      service="ssm", region="us-east-1", uri="/",
      requestDateTimeUTC=requestTimeUTC, action="GetParameter", version="2010-05-08",
      additionalParams=Map("Name"->"alias", "WithDecryption"->"true"), payload="",
      accessKey="AKIDEXAMPLE", secretKey="wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"
    )

    request.getUrl.toString should equal (
      "https://ssm.us-east-1.amazonaws.com/?Name=alias&WithDecryption=true&Action=GetParameter&Version=2010-05-08"
    )
    val headers = request.getHeaders.asScala.map({case (k, v) =>
      // flatten values of collection type
      val newVal = v match {
        case javaColl: util.Collection[Any] => javaColl.iterator.next
        case scalaColl: Iterable[Any] => scalaColl.iterator.next
        case x: Any => x
      }
      (k, newVal)
    })

    headers should contain ("accept" -> "application/json")
    headers should contain ("content-type", "application/x-amz-json-1.1")
    headers should contain ("host" -> "ssm.us-east-1.amazonaws.com")
    headers should contain ("authorization" -> (
      "AWS4-HMAC-SHA256 " +
      "Credential=AKIDEXAMPLE/20150830/us-east-1/ssm/aws4_request, " +
      "SignedHeaders=accept;content-type;host;x-amz-date, " +
      "Signature=1aec5acdb8595dccbf7df419cdb87e45090274ece914b38954eb5d65a104c373"
    ))
    headers should contain ("x-amz-date" -> "20150830T123600Z")
  }
}
