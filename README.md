# Local proxy
My solution for the Paidy [test task](https://github.com/paidy/interview/blob/master/Forex.md)

## Table of contents

1. [Requirements](./README.md#Requirements)
2. [How to run](./README.md#How-to-run)
3. [Reasoning](./doc/Reasoning.md)
4. [Application structure](./doc/ApplicationStructure.md)
5. [API description](./doc/Api.md)
6. [Configuration](./doc/Configuration.md)

## Requirements

* SBT >= 1.5.0
* Assuming you already have Java for `sbt` (application tested with Java 8 and 11) 
* Docker (application uses testcontainers)
* docker-compose for fast local run with Redis cache and the One Frame dockerized image

## How to run
There are two ways to run the application locally:
1. Just execute `sbt run` application will use in-memory caffeine cache.
   By default, the application will search for a target on `localhost:8081`, if you have a different target,
   see [this](./doc/Configuration.md#client-config)
2. Using docker-compose, see [this](./docker)

## Naive load testing report

Test was performed using [vegeta](https://github.com/tsenart/vegeta)

**Report**
```text
duration=1h |tee results.bin | vegeta report"
Requests      [total, rate, throughput]         3600000, 1000.00, 1000.00
Duration      [total, attack, wait]             1h0m0s, 1h0m0s, 1.364ms
Latencies     [min, mean, 50, 90, 95, 99, max]  1.067ms, 1.353ms, 1.308ms, 1.54ms, 1.645ms, 1.881ms, 34.022ms
Bytes In      [total, mean]                     331673316, 92.13
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           100.00%
Status Codes  [code:count]                      200:3600000
Error Set:
```
**Note**: Due to hardware limitations, it was impossible to test a scenario of 10k requests per one second.
