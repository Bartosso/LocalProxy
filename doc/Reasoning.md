# Reasoning

My thought process:
1. At first glance, I thought it should be easy to implement a proxy. I started from the
   `HttpRoutesClass`. I added validation and handling of invalid currencies.


2. After completing `HttpRoutesClass` parameters validation, I found that I can ask the Open Frame API for the
   currencies only 1k times per day. Still, on the other side, I can ask the Open Frame API about more than one pair of
   currency per call. Therefore, I can't use a solution performing currencies request to the target in a lazy manner
   (on the user's request to the proxy) and then caching the response for the next 5 minutes.
   Hence, I decided to have two main components: cache updater and cache reader.

   In the original naive approach, I wanted to update the cache every _TTL_ minus 5 seconds (_TTL_ is 5 minutes by default).
   There are 24*3600=86400 seconds in one day. The hard limit of requests to the target is 1000. That means
   I can ask the target for currencies every 86.4 seconds and not exceed the token usage limit.

   Performing requests to the currencies API every 295 should be more than enough to fulfill both the 5 minutes 
   currency value TTL and 1k TPD (times-per-day) API threshold.


3. Once cache updater and reader were ready, I decided to shift my focus on the application robustness.
   There is always a possibility of an application failure due to reasons beyond my control, 
   e.g. OOM, power shortage, etc. 

   In such case, it's possible to burn the entire daily request limit due to repeating application restarts
   (for example, if k8s/docker bundle is set up to restart for every failure)

   I decided to have some persistent cache and only validate it during the application startup process: 
   - if values are outdated, update them immediately and schedule a future update task
   - otherwise, just schedule a future update task.

   Redis was chosen to be able to scale it in the future (if ever needed, of course).
   
   My initial threshold for outdated values was 200 seconds.


4. I decided to check the head pair to determine if the cache is outdated or not (at the application startup).
   If there is no value in the cache, that means
   the pair is older than 5 minutes, and we need to update the cache.
   If the head pair is there and is older than 200 seconds, we update it.

   Why only the head pair? There is a possibility that the application dies after updating the first few values in
   the cache. Because I use indexed sequence that guarantees the order for the currency pairs, it's enough
   to just validate the head pair.

   It also doesn't make sense to validate every pair, because it will use our entire daily request
   limit for the target API (in case of continuous restarts and failures).


5. I realized that thresholds I set for the initial data age validation
   (200 s) and cache refresh rate (295 s) are wrong.

   If I update the cache every 295 seconds and check that the head pair is older than 200 seconds,
   there is another possibility. 

   Suppose the application updates currency values and immediately dies. In this case, values age can be 199 seconds,
   for example, and the application will execute the scheduled task for an update only in 295 seconds:
   values will be absent for the 96 seconds after they die (because of cache TTL set to 5 minutes).


6. I changed the cache refresh rate and cache age threshold values to 100 seconds.

   Why 100? Because if values are 99
   seconds old, it's not a problem. We'll update them after 100 seconds anyway, so there wouldn't be a situation
   described above when we have an empty cache for some period of time.


7. I decided to move the cache synchronization task to a separate component to simplify
   adding more data providers in the future.


8. At the API level, I wanted to copy the target behavior, but frankly speaking, I wasn't sure if it was a good idea.
   The target returns a JSON document with an error text if you pass a wrong currency, but the status code is 200 (OK), so I decided to adjust it a little.


## Ideas for further improving

1. Move cache synchronization to separated project to be able to share Redis cache by proxies
   (to be able to scale the number of proxies).
2. Currencies loading from the config file to not recompile the whole application if there will be
   a need of adding a new currency.
3. Add overriding of the token. In other words, I believe it is worth adding the ability to pass your auth token and
   do a request the One Frame API with that token immediately. But on the other side, the user can burn out the
   application token fast if he passes what the application uses. Or maybe it is even worth adding
   an endpoint to change the token for the whole application.
4. Maybe it is worth adding a circuit breaker to the client. I'm not totally sure, since the loop runs every
   100 seconds. However, I believe it is good neutral timing.

## Possible problems
1. It's still possible to have partially updated cache or not updated at all but with a wasted call since hardware can
   go down etc. Even with relational databases, there is no guarantee that the application will complete a
   transaction before the shut-off/blackout after retrieving the data.