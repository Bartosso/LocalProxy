# Building and running local proxy via docker compose
`docker-compose-local-dev.yml` starts Redis image, one-frame and local-proxy

#### Prerequisites
* (optional) you might need to add the right to write to the `./redis` folder since
  I set up Redis to save the snapshot every 100 seconds if there was at least one change.
  The easiest way if you use Linux is `sudo chmod -R 777 ./redis`. Otherwise, you can encounter an error from the Redis side.
  
#### How to run

To build and run the local proxy docker image, you need to execute the following steps:

1. execute `sbt docker` in the root folder of the project to build and publish locally the local proxy image
3. execute `docker-compose -f docker-compose-local-dev.yml up` in this folder to start the whole stack
(Redis + one frame + local proxy)

### Note
Redis image uses `./redis` folder as volume for configuration and dump storing, so you can
configure Redis by editing `./redis/redis.conf`.