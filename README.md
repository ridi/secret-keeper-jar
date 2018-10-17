# Secret Keeper - JAR

[![Build Status](https://travis-ci.com/ridi/secret-keeper-jar.svg?branch=master)](https://travis-ci.com/ridi/secret-keeper-jar)
[![Download](https://api.bintray.com/packages/ridi-data/maven/secret-keeper-jar/images/download.svg) ](https://bintray.com/ridi-data/maven/secret-keeper-jar/_latestVersion)
[![Coverage Status](https://coveralls.io/repos/github/ridi/secret-keeper-jar/badge.svg)](https://coveralls.io/github/ridi/secret-keeper-jar)

## Introduction
Without secret-keeper, you would have:
- hard-coded your secrets in your version-controlled source code (Worst!), or
- created a not-version-controlled config file and manually provide it when you deploy your code, or
- let your deployment system - Jenkins CI, etc - mananage your not-version-controlled config file, but you have as many of them as your projects.

With secret-keeper, you can:
- store your secrets in AWS and let your applications use it safely and conveniently.
- let AWS manage contents of your secrets, keeping them encoded and safe.
- version-control usage of secrets inside your applications, since secrets are referred only with their aliases.
- simply provide access key and secret key of your dedicated IAM user to your deployment system. You don't have to manage per-project config files.


## Install
Add following lines in your `build.sbt`:
```scala
resolvers += Resolver.bintrayRepo("ridi-data", "maven")
libraryDependencies += "com.ridi" %% "secret-keeper-jar" % "0.1.0"
```

## Preparation
- [Create a dedicated AWS IAM user](https://github.com/ridi/secret-keeper-python/wiki/Create-a-dedicated-AWS-IAM-user)
- [Create a dedicated encryption key in AWS KMS](https://github.com/ridi/secret-keeper-python/wiki/Create-a-dedicated-encryption-key-in-AWS-KMS)
- [Create a sample secret in AWS SSM Parameter Store](https://github.com/ridi/secret-keeper-python/wiki/Create-a-sample-secret-in-AWS-SSM-Parameter-Store)

## Usage
- Write a sample application.
  ```scala
  import com.ridi.secretkeeper.SecretKeeper

  object Sample extends App {
    val secret = SecretKeeper.tell("sample.secret")
    println(s"Secret: $secret")
  }
  ```

- Run the sample application. You must provide the dedicated user's access key and secret key, and the region as environment variables.
```bash
$ export CLASSPATH=/path/to/your/jar/files
$ export SECRETKEEPER_AWS_ACCESS_KEY="YOUR_ACCESS_KEY_ID"
$ export SECRETKEEPER_AWS_SECRET_KEY="YOUR_SECRET_ACCESS_KEY"
$ export SECRETKEEPER_AWS_REGION="us-east-1"
$ scalac Sample.scala
$ scala Sample
Secret: pa$$w@rd!
```
