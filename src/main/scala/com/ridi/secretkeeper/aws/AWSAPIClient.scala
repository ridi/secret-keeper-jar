package com.ridi.secretkeeper.aws

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.google.api.client.http._
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.json.{GenericJson, JsonObjectParser}
import com.ridi.secretkeeper.hash._

trait AWSAPIClient {
  def createRequest
  (
    service: String, region: String, uri: String,
    requestDateTimeUTC: LocalDateTime, action: String, version: String,
    additionalParams: Map[String, String], payload: String,
    accessKey: String, secretKey: String
  ): HttpRequest

  def sendRequest
  (
    service: String, region: String, uri: String,
    requestDateTimeUTC: LocalDateTime, action: String, version: String,
    additionalParams: Map[String, String], payload: String,
    accessKey: String, secretKey: String
  ): GenericJson = {
    val request = createRequest(
      service, region, uri, requestDateTimeUTC, action, version,
      additionalParams, payload, accessKey, secretKey
    )

    val response = request.execute()
    response.parseAs(classOf[GenericJson])
  }
}

private[secretkeeper] class AWSAPIClientImpl extends AWSAPIClient {
  val TERMINATION = "aws4_request"
  val ALGORITHM = "AWS4-HMAC-SHA256"
  val HTTP_TRANSPORT = new NetHttpTransport
  val JSON_FACTORY = new JacksonFactory

  def makeCanonicalAndSignedHeaders(headers: Map[String, String]): (String, String) = {
    val canonicalHeaders = headers.map({ case (key, value) => key.toLowerCase -> value.trim })
    val canonicalHeadersString = canonicalHeaders.toArray.sortBy(_._1).map({ case (key, value) => s"$key:$value\n" }).mkString("")
    val signedHeaders = canonicalHeaders.keys.toArray.sorted.mkString(";")

    (canonicalHeadersString, signedHeaders)
  }

  def makeCanonicalRequest
  (
    method: String, host: String, uri: String,
    queryParams: Map[String, String], headers: Map[String, String], payload: String
  ): String = {
    val canonicalQueryString = queryParams.toArray.sortBy(_._1).map({ case (key, value) => s"$key=$value" }).mkString("&")
    val encodedPayload = hashSHA256(payload).toLowerCase

    val (canonicalHeaders, signedHeaders) = makeCanonicalAndSignedHeaders(headers)

    val canonicalRequest = Array(method, uri, canonicalQueryString, canonicalHeaders, signedHeaders, encodedPayload).mkString("\n")
    hashSHA256(canonicalRequest)
  }

  def makeCredentialScope(requestDate: String, region: String, service: String): String =
    s"$requestDate/$region/$service/$TERMINATION"

  def makeStringToSign(xAmzDate: String, credentialScope: String, hashedCanonicalRequest: String): String =
    Array(ALGORITHM, xAmzDate, credentialScope, hashedCanonicalRequest).mkString("\n")

  def makeSigningKey(secretKey: String, requestDate: String, region: String, service: String): Array[Byte] = {
    val kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8)
    val kDate = hmacSHA256(requestDate, kSecret)
    val kRegion = hmacSHA256(region, kDate)
    val kService = hmacSHA256(service, kRegion)
    val kSigining = hmacSHA256(TERMINATION, kService)
    kSigining
  }

  def getEndpointHost(service: String, region: String): String = service match {
    case "ssm" => s"$service.$region.amazonaws.com"
    case _ => throw new IllegalArgumentException(s"Service '$service' is not supported")
  }

  override def createRequest
  (
    service: String, region: String, uri: String,
    requestDateTimeUTC: LocalDateTime, action: String, version: String,
    additionalParams: Map[String, String], payload: String,
    accessKey: String, secretKey: String
  ): HttpRequest = {

    val method = "GET"

    val host = getEndpointHost(service, region)

    val xAmzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(requestDateTimeUTC)
    val date = DateTimeFormatter.ofPattern("yyyyMMdd").format(requestDateTimeUTC)

    val (acceptHeader, contentTypeHeader) = ("application/json", "application/x-amz-json-1.1")
    val headers = Map(
      "Accept" -> acceptHeader, "Content-Type" -> contentTypeHeader,
      "Host" -> host, "X-Amz-Date" -> xAmzDate
    )

    val queryParams = additionalParams + (
      "Action" -> action,
      "Version" -> version
    )

    val canonicalRequest = makeCanonicalRequest(method, host, uri, queryParams, headers, payload)

    val signedHeaders = makeCanonicalAndSignedHeaders(headers)._2

    val credentialScope = makeCredentialScope(date, region, service)

    val stringToSign = makeStringToSign(xAmzDate, credentialScope, canonicalRequest)

    val signingKey = makeSigningKey(secretKey, date, region, service)

    val signature = bytesToHex(hmacSHA256(stringToSign, signingKey))

    val authorizationHeader = s"$ALGORITHM Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

    val requestFactory = HTTP_TRANSPORT.createRequestFactory

    val httpHeaders = (new HttpHeaders)
      .setAccept(acceptHeader)
      .setContentType(contentTypeHeader)
      .setAuthorization(authorizationHeader)
      .set("Host", host)
      .set("X-Amz-Date", xAmzDate)

    val url = queryParams
      .foldLeft(new GenericUrl(s"https://$host$uri"))({
        case (u, (key, value)) => u.set(key, value)
      })

    requestFactory
      .buildGetRequest(url)
      .setHeaders(httpHeaders)
      .setParser(new JsonObjectParser(JSON_FACTORY))
  }

}
