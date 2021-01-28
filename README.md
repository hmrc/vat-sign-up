VAT Sign Up MicroService
====================================
[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Build Status](https://travis-ci.org/hmrc/vat-sign-up.svg?branch=master)](https://travis-ci.org/hmrc/vat-sign-up) [ ![Download](https://api.bintray.com/packages/hmrc/releases/vat-sign-up/images/download.svg) ](https://bintray.com/hmrc/releases/vat-sign-up/_latestVersion)

This is the protected backend MicroService for the Use software to submit your VAT Returns (MTD VAT) service.

This service provides the backend interactions with other backend MicroServices

### Running the subscription services locally

You will need [sbt](http://www.scala-sbt.org/)

1) **[Install Service-Manager](https://github.com/hmrc/service-manager/wiki/Install#install-service-manager)**


2) **Start the MTD VAT sign up services:**

   `sm --start VAT_SIGN_UP_ALL -f`


3) **Clone the protected service:**

  - SSH

     `git clone git@github.com:hmrc/vat-sign-up.git`

  - HTTPS

     `git clone https://github.com/hmrc/vat-sign-up.git`


4) **Start the protected service:**
   
   `sm --stop VAT_SIGN_UP`

   `sbt "run 9572 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes"`
   

5) **Go to the homepage:**

   http://localhost:9566/vat-through-software/sign-up/
   
### Bulk Migration API

#### Route
 
`/vat-sign-up/migration-notification/vat-number/:vatNumber`

where `:vatNumber` is a VRN (that has been migrated) that the calling service must provide as shown in the example below.

e.g `/vat-sign-up/migration-notification/vat-number/123456789`

#### Request Method and Body

HTTP Methods Supported: POST

Request Body: An empty json object

#### Responses

The API returns only two status codes with no response body as the failures are not recoverable.

|Status Code | Reason
|------------| -----------------
| ```204```  | Successfully assigned the MTD-VAT enrolment to all users that have the VAT-DEC enrolment for the supplied VRN.
| ```500```  | Failed to assign the enrolment / downstream service failures

## Stubs
All routes are prepended with `/vat-sign-up/test-only`

### Email Verification Stubs
#### POST /email-verification/verify-passcode

Returns a status based on the provided passcode.

**Example Request body**

```
{
    "passcode": "123456",
    "email": "test@test.com"
}
```

### Example Response

| Passcode              |  Status                                |
|-----------------------|----------------------------------------|
| 123456                | 204 (No Content) - already verified    |
| 111111                | 404 (Not Found) - passcode not found   |
| 222222                | 404 (Not Found) - passcode mismatch    |
| 333333                | 400 (Forbidden) - max attempts exceeded|
| Any other 6 digit code| 201 (Created) - successfully verified  |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html" )
