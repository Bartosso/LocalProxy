# Local proxy
My solution for the Paidy test task

## Requirements

* SBT >= 1.5.0
* Assuming you already have Java for `sbt` (tested with Java 8 and 11) 
* Docker (app uses testcontainers)
* docker-compose for fast local run with Redis cache

## Local run
There are two ways to run the application locally:
1. it's just to execute ```sbt run``` application will use in-memory caffeine cache
2. using docker-compose, see [this](./docker/README.md)
