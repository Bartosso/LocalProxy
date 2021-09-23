# Configuration

There are main app configuration with following entities:
1. **http** - config for the HTTP server
2. **client-config** - config of the HTTP client which will interact with the Open Frame API
3. **cache-config** - config for the cache

## http
1. **host** - string, address to which server will be bound, by default value is `"0.0.0.0"`, can be set up via
   environment by the key **SERVER_HOST**
2. **port** - int, port to which server will be bound, by default value is `8080`, can be set up via
   environment by the key **SERVER_PORT**
3. **timeout** - finite duration, timeout before the 503 response, by default value is `40 seconds`, can be set up via
   environment by the key **SERVER_TIMEOUT**

## client-config
1. **token** - string, auth token which will be used in the Open Frame API calls, by default value is
   `"10dc303535874aeccc86a8251e6992f5"`, can be set up via environment by the key **AUTH_TOKEN**
2. **target-host** - string, address of the Open Frame API, by default value is `localhost`, can be set up via
   environment by the key **TARGET_HOST**
3. **target-port** - int, port of the Open Frame API host, by default value is `8081`, can be set up via
   environment by the key **TARGET_PORT**
4. **timeout** - finite duration, time limit of how long client will read the response, by default value is `20 seconds`, can be set up via 
   environment by the key **CLIENT_TIMEOUT**
5. **idle-timeout** - finite duration, time of how long client will wait for any response, by default value is `40 seconds`, can be set up via
   environment by the key **CLIENT_IDLE_TIMEOUT**

## cache-config
1. **redis-host** - string, address of the Redis host, by default value is empty, can be set up via 
   environment by the key **REDIS_HOST**
2. **redis-port** - int, address to which server will be bound, by default value is empty, can be set up via
   environment by the key **REDIS_PORT**
3. **ttl** - finite duration, time to live in cache for values, by default value is `5 minutes`, can be set up via
   environment by the key **CACHE_TTL**

If both **redis-host** and **redis-port** are set - the application will use Redis, otherwise - in-memory cache (Caffeine).
