# API description

API itself is very basic, so I decided to describe it here. 

There are two routes:
1. **/healthcheck** - always returns 200
2. **/rates** - requires query parameters `from` and `to`

### /rates
requires two query parameters:
1. `from` - should be one of [supported](../src/main/scala/forex/domain/Currency.scala) currencies
2. `to` - should be one of [supported](../src/main/scala/forex/domain/Currency.scala) currencies

Both parameters shouldn't be the same. Otherwise, the user will get an empty list response
`[]` as from the original One Frame API.

If the user uses unsupported currencies, he will get an error response with code 400 and a list of errors in
JSON format, here is the example
```json
[
    {
        "field": "from",
        "message": "unknown currency"
    },
    {
        "field": "to",
        "message": "unknown currency"
    }
]
```

Example of the request via curl - `curl localhost:8080/rates?from=JPY&to=USD`

Example of the successful response -
```json
{
  "from": "JPY",
  "to": "USD",
  "price": 0.37498260086563734,
  "timestamp": "2021-09-22T14:13:00.967Z"
}
```

If a target (One Frame API) is unreachable, that means the cache is empty, so if a user asks proxy for the proper pair but there is no
value in the cache - the user will get an empty response with status code 502.
