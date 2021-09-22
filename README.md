# Local proxy
My solution for the Paidy test task

## Table of contents

1. [Requirements](./README.md#Requirements)
2. [How to run](./README.md#How-to-run)
3. [Reasoning](./doc/Reasining.md)
4. [Application structure](./doc/ApplicationStructure.md)
5. [API description](./doc/Api.md)

## Requirements

* SBT >= 1.5.0
* Assuming you already have Java for `sbt` (application tested with Java 8 and 11) 
* Docker (application uses testcontainers)
* docker-compose for fast local run with Redis cache and one-frame dockerized image

## How to run
There are two ways to run the application locally:
1. Just execute `sbt run` application will use in-memory caffeine cache.
By default, the application will search for a target on `localhost:8081`, so if you have a different target, see [this]()
2. Using docker-compose, see [this](./docker/README.md)
