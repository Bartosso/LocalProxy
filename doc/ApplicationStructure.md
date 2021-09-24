# Application structure

The application itself has three main components:
1. **CacheAlgebra** - represents cache itself, overall just a wrapper around the cache
2. **CacheSynchronizationAlgebra** - service which tries to update cache every 100 seconds
3. **OneFrameAlgebra** - service with access to the cache

And two HTTP components:
1. **RatesHttpRoutes** + **HealthCheckRoutes** - provides HTTP routes, see this [this](./Api.md) for the full description
2. **OneFrameHttpClient** - consumes the One Frame API.

### CacheAlgebra

Have one implementation

#### CacheImpl

Just a wrapper, since [scala cache](https://github.com/cb372/scalacache) have an ugly API 
(key is `Any*`, requires to have `Mode` type-class at every call).
Can contain Redis cache or Caffeine if a user didn't set up `redis-host` and `redis-port` in the config, more [here](./Configuration.md#cache-config).

### CacheSynchronizationAlgebra

Have one implementation

#### CacheSynchronizationImpl

Requires CacheAlgebra and OneFrameHttpClient on creating. Synchronizes cache every 100 seconds by default.

At the first synchronization (after the application start), checks if the cache has the head pair (AUD CAD) if the cache
is empty or the pair are older than 100 seconds - uses the One Frame API and synchronizes values, otherwise lefts cache 
as is. Then, neglecting the result of the previous step - schedules task for synchronization (the second and following 
tasks don't contain checking the head pair's age).

### OneFrameAlgebra

Have one implementation

#### OneFrameCachedImpl

Primarily a guard around the cache. Handles situations when a user asks for the pair where fields `From` and `To` are the same
and when there is no value for the key.

### RatesHttpRoutes + HealthCheckRoutes
See [this](./Api.md).

### OneFrameHttpClient

Have one implementation

#### OneFrameHttpClientImpl

HTTP client, which consumes the One Frame API. handles situations when there is an empty response, unknown error,
forbidden or some One Frame API error.