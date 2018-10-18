/* Global state */
 
var config = {};

/* Helpers */

function init() {
    return clearCaches()
    .then(function(){
        return getServiceWorkerConfig();    
    });
    
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
    return getStoreData()
        .then(function(storeData){
            console.log('storeData: ', storeData);
            return fetch(storeData[0].configPath)
                .then(function(response) {
                    return response.json()
                        .then(function(json) {
                            config = json;
                            console.log("In SW ", config);
                            return json;
                    });
                });         
    });
    
    
}

function getStoreData(){
    var dbPromise = idb.open('pwa-aem-db', 1, function(db){});
    return dbPromise.then(function(db) {
        console.log('db', db);
        var tx = db.transaction('aem-pwa-store', 'readonly');
        var store = tx.objectStore('aem-pwa-store');
        console.log('store', store);
        return store.getAll();
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
        init().then(function(configObject) {
            console.log('configObject', configObject);
            caches.open(configObject.cache_name)
                .then(function(cache) {
                var urlsToCache = configObject.fallback.map(function(entry) {
                    return entry.path+'.html';
                }).concat(configObject.pre_cache);
                    
                return cache.addAll(urlsToCache);
            });
        })
    );
});
self.addEventListener("activate", function(event) {

    
              event.waitUntil(function() {
                        return clearCaches();
                    });
   return self.clients.claim();

});

function clearCaches(){
    return caches.keys().then(function(keyList) {
                            return Promise.all(keyList.map(function(key) {
                                if (key !== config.cache_name) {
                                    console.log(key);
                                    return caches.delete(key);
                                }
                            }));

                        });
}

self.addEventListener('fetch', function(event) {
    if (!isCacheable(event.request)) {
        return;
    }
    if(!config.cache_name){
        getServiceWorkerConfig()
            .then(function(){
            useSWCacheStrategy(event);
        });
    }else{
        useSWCacheStrategy(event);
    }
});
function useSWCacheStrategy(event){
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

}

function cachedResponse(event) {

    return caches.match(event.request).then(function(resp) {
        if (resp) {
            console.log('Resolved response', resp);
            return resp;
        } else {
            console.log('check for fallback for ', event.request);
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