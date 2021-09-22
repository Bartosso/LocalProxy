# Building and running local proxy via docker compose
`docker-compose-local-dev.yml` starts one-frame image, redis and local-proxy

To build and run the local proxy docker image, you need to execute the following steps:

1. execute `sbt docker` in the root folder of the project to build and publish locally the local proxy image
2. execute `docker-compose -f docker-compose-local-dev.yml up` in this folder to start the whole stack
(redis + one frame + local proxy)

###Note
Redis image uses `./redis` folder as volume for configuration and dump, so you can
configure Redis by editing `./redis/redis.conf`.