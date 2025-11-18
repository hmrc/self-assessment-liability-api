# self-assessment-liability-api

More information about this service can be found in the [service guide](https://developer.service.hmrc.gov.uk/guides/self-assessment-liability-service-guide). 

## Endpoint Definition

### Request

Method: `GET`

URL: `/:utr`

Query Parameter: `fromDate`

### Example request URLs:
* `/1097172987`
* `/1097172987?2025-01-01`

### Response Status Codes

More detail can be found in the [service guide error responses](https://developer.service.hmrc.gov.uk/guides/self-assessment-liability-service-guide/documentation/errors-responses.html).

| Status Code | Description                                                                                                                                                                                                                                         |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 200         | Successful request: <br/> * The response body contains self-assessment details for the given `utr` and `fromDate`.                                                                                                                                  |
| 400         | Invalid API implementation caused by (at least) one of: <br/> * Invalid `fromDate` - either malformed or outside of the accepted range (the accepted range is 7 tax years ago to now). <br/> * Invalid `utr`. <br/> * Missing authentication token. |
| 401         | Unauthorized: <br/> * An agent may not have an active relationship with the client. <br/> * An individual may have a low confidence level.                                                                                                          |
| 403         | Forbidden: <br/> * An agent may not have registered for Self Assessment or have access to financial data. <br/> * An individual may not be registered with HMRC.                                                                                    |
| 404         | Not Found: <br/> * The request format is valid but self-assessment data cannot be found.                                                                                                                                                            |
| 500         | Internal Server Error: <br/> * This API (or another HMRC service this API relies on) has returned an unexpected error for a valid request.                                                                                                          |
| 503         | Service Unavailable: <br/> * An HMRC service this API relies on is temporarily unavailable.                                                                                                                                                         |

### Example success response body

```json
{
  "balanceDetails": {
    "totalOverdueBalance": 500,
    "totalPayableBalance": 500,
    "earliestPayableDueDate": "2025-04-30",
    "totalPendingBalance": 1500,
    "earliestPendingDueDate": "2025-07-15",
    "totalBalance": 2000,
    "totalCreditAvailable": 0,
    "codedOutDetail": [
      {
        "totalAmount": 200,
        "effectiveStartDate": "2023-04-06",
        "effectiveEndDate": "2024-04-01"
      }
    ]
  },
  "chargeDetails": [
    {
      "chargeId": "AB1234567",
      "creationDate": "2025-01-15",
      "chargeType": "ITSA",
      "chargeAmount": 1250,
      "outstandingAmount": 500,
      "taxYear": "2024-2025",
      "dueDate": "2025-04-30",
      "amendments": [
        {
          "amendmentDate": "2025-04-15",
          "amendmentAmount": 500,
          "amendmentReason": "Payment",
          "isPaymentRelated": true,
          "paymentMethod": "bank_transfer",
          "paymentDate": "2025-04-10"
        }
      ]
    },
    {
      "chargeId": "EF2345678",
      "creationDate": "2024-02-10",
      "chargeType": "NICS",
      "chargeAmount": 2200,
      "outstandingAmount": 0,
      "taxYear": "2023-2024",
      "dueDate": "2024-04-01",
      "outstandingInterestDue": 20,
      "accruingInterestPeriod": {
        "interestStartDate": "2024-05-01",
        "interestEndDate": "2024-12-01"
      },
      "accruingInterestRate": 0.05,
      "amendments": [
        {
          "amendmentDate": "2024-12-08",
          "amendmentAmount": 2058.33,
          "amendmentReason": "Payment",
          "isPaymentRelated": true,
          "paymentMethod": "bank_transfer",
          "paymentDate": "2024-12-03"
        }
      ]
    },
    {
      "chargeId": "KL3456789",
      "creationDate": "2025-05-22",
      "chargeType": "VATC",
      "chargeAmount": 1500,
      "outstandingAmount": 1500,
      "taxYear": "2025-2026",
      "dueDate": "2025-07-15"
    }
  ],
  "refundDetails": [
    {
      "refundDate": "2024-01-10",
      "refundMethod": "bank_transfer",
      "refundRequestDate": "2023-12-12",
      "refundRequestAmount": 350,
      "refundDescription": "From overpayment from return 05 APR 23",
      "interestAddedToRefund": 5.25,
      "totalRefundAmount": 355.25,
      "refundStatus": "processed"
    }
  ],
  "paymentHistoryDetails": [
    {
      "paymentAmount": 500,
      "paymentReference": "PAY123456",
      "paymentMethod": "bank_transfer",
      "paymentDate": "2025-04-11",
      "processedDate": "2025-04-15",
      "allocationReference": "AB1234567"
    },
    {
      "paymentAmount": 2058.33,
      "paymentReference": "PAY112233",
      "paymentMethod": "bank_transfer",
      "paymentDate": "2024-12-04",
      "processedDate": "2024-12-08",
      "allocationReference": "EF2345678"
    },
    {
      "paymentAmount": 200,
      "paymentReference": "PAY888233",
      "paymentMethod": "bank_transfer",
      "paymentDate": "2023-12-04",
      "processedDate": "2023-12-08",
      "allocationReference": "EF2345678"
    }
  ]
}
```

### Example failure response body

```json
{
  "failure": {
    "type": "BAD_REQUEST",
    "reason": "Invalid request format or parameters."
  }
}
```

## Stub

This API interacts with internal HMRC services to fetch relevant data. For testing purposes, this API does not interact with the live version of these services but rather a fake _stub_. This _stub_ is a single service which simulates various responses for different request URLs and parameters. The _stub_ service can be found [here](https://github.com/hmrc/self-assessment-liability-stub).

## Testing
Use the following command to run both unit and integration tests
```shell
sbt testAll
```

### API testing

With the [HMRC Service Manager](https://github.com/hmrc/sm2), this API can be further developed and tested by accessing the HMRC microservices required for this API to function. To test local changes to this repository:

1. Ensure a container runtime is active e.g. Colima:
```console
colima start
```
2. Start all microservices related to this API:
```console
sm2 --start SELF_ASSESSMENT_LIABILITY_API_ALL
```
3. Stop the main version of this API which was started in the previous command:
```console
sm2 --stop SELF_ASSESSMENT_LIABILITY_API
```
4. Run the modified version of this API from the root directory:
```console
sbt run
```
Note: ensure that `sbt clean` and `sbt compile` commands are executed before `sbt run` every time changes are made to the API.

Requests can now be sent to this API, with responses matching any changes in the local code.

## Generation, Validation and Publishing
To ensure API documentation alignment with code, we generate the OpenAPI specification (OAS) directly from the route definitions using an SBT task, reducing risk and manual effort.


To generate, validate and publish the YAML OAS, run:
```shell
sbt genValOas
```
The generated OAS will include definitions based on the application's routes. The output folder is set to 'resources/public/api/conf/1.0' so that the recent changes get deployed to the API platform.



## All in one sbt alias:

To run all tests, generate OAS and validate the OAS, you can run:
```shell
sbt testGenVal
```
## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

