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

    (config.no_cache || []).forEach(function (pattern) {
        if (cacheable && request.url.match(pattern)) {
            cacheable = false;
            //console.log("Unable to cache [ " + request.url + " ] due to pattern [ " + pattern + " ]");
        }
    });

    return cacheable;
}


function getFallback(request) {
    var fallback = null;

    (config.fallback || []).forEach(function (entry) {
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

    return fetch(configJson).then(function (response) {
        return response.json().then(function (json) {
            config = json;
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
self.addEventListener('install', function (e) {
    e.waitUntil(
        init().then(function () {
            caches.open(config.cache_name).then(function (cache) {
                var urlsToCache = config.fallback.map(function (entry) {
                    return entry.path;
                }).concat(config.pre_cache);

                return cache.addAll(urlsToCache);
            });
        })
    );
});

self.addEventListener('fetch', function (event) {
    if (!isCacheable(event.request)) {
        return;
    }

    event.respondWith(
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
    );
});