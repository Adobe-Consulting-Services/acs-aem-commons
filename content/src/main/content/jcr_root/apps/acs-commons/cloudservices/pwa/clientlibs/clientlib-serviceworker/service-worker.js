/* Global state */
var config = {
    cache_name: 'pwa__uninitialized-v0',
    version: 0
};

/* Helpers */

function init() {
    return getServiceWorkerConfig();
}

function isCacheable(request) {
    var cacheable = true;

    if (request.method !== "GET") {
        return false;
    }
    if (!config.no_cache || !Array.isArray(config.no_cache)) {
        config.no_cache = [];
    }
    config.no_cache.push('service-worker');
    config.no_cache.push('.pwa.');
    config.no_cache.push('manifest');
    (config.no_cache || []).forEach(function(pattern) {
        if (cacheable && request.url.match(pattern)) {
            cacheable = false;
            //console.log("Unable to cache [ " + request.url + " ] due to pattern [ " + pattern + " ]");
        }
    });

    return cacheable;
}


function getFallback(request) {
    var fallback = null;

    (config.fallback || []).forEach(function(entry) {
        if (!fallback && request.url.match(entry.pattern)) {
            fallback = entry.path;
        }
    });

    if (!fallback) {
        fallback = config.fallback_default;
    }

    return fallback;
}

function getServiceWorkerConfig() {
    var configJson = new URL(location).searchParams.get('config');

    return fetch(configJson).then(function(response) {
        return response.json().then(function(json) {
            config = json;
            console.log("In SW ", config);
            return json;

        });
    });
}

/* Events */

/**
 * Install should initialize the cache..
 * - Get the SW Config
 * - Set the cacheName
 * - Request the Fallback URLs
 * - Request the Pre-cache URLs
 */
self.addEventListener('install', function(e) {
    console.log('Registering SW');
    e.waitUntil(
        init().then(function() {
            caches.open(config.cache_name).then(function(cache) {
                var urlsToCache = config.fallback.map(function(entry) {
                    return entry.path;
                }).concat(config.pre_cache);

                return cache.addAll(urlsToCache);
            });
        })
    );
});
self.addEventListener("activate", function(event) {

    console.log("Activated SW");
              event.waitUntil(function() {
                        caches.keys().then(function(keyList) {
                            return Promise.all(keyList.map(function(key) {
                                if (key !== config.cache_name) {
                                    console.log(key);
                                    return caches.delete(key);
                                }
                            }));

                        });
                    });

    return self.clients.claim();

});

self.addEventListener('fetch', function(event) {
    if (!isCacheable(event.request)) {
        return;
    }

    // cache then network fallback
    event.respondWith(
        caches.open(config.cache_name).then(function(cache) {
            return cache.match(event.request).then(function (cachedResponse) {
                return cachedResponse || fetch(event.request)
                    .then(function(response) {
                       cache.put(event.request, response.clone());
                            return response;
                }).catch(function() {
                    return cache.match(getFallback(event.request))
                        .then(function(cachedResponse) {
                            return cachedResponse;
                    });
                });
            });
        })
   
    );

    // network then cache fallback
    /*

         fetch(event.request)
        .then(function(response) {
            return caches.open(config.cache_name)
                .then(function(cache) {
                    cache.put(event.request, response.clone());
                    return response;
                });
        }).catch(function(err) {
            return cachedResponse(event);
        })
*/

});

function cachedResponse(event) {

    return caches.match(event.request).then(function(resp) {
        if (resp) {
            console.log('Resolved respone', resp);
            return resp;
        } else {
            console.log('chek for fallback for ', event.request);
            return caches.match(getFallback(event.request));
        }

    });
}

/*
caches.open(config.cache_name).then(function(cache) {
            return cache.match(event.request).then(function (cachedResponse) {
                return cachedResponse || fetch(event.request).then(function(response) {
                    cache.put(event.request, response.clone());
                    return response;
                }).catch(function() {
                    return cache.match(getFallback(event.request)).then(function(cachedResponse) {
                        return cachedResponse;
                    });
                });
            });
        })
*/