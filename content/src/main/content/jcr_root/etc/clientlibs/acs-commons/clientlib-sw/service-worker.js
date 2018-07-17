(function() {
    var urlsToCache = [];
    var fallbackCache = '';
    var deniedCachePaths = [];
    var staticCachePaths = [];

    self.addEventListener('install', function(event) {
		populateAppShell();
    });
    function populateAppShell(){


        var urlLocation = new URL(location).toString();

         fetch(urlLocation + '/cacheDetails.json', {
            mode: 'no-cors'
        }).then(function(response) {
            return response.json();
        }).then(function(data) {
            //initialize variables
            fallbackCache = data.fallback + '.html';
            urlsToCache.push(fallbackCache);
            staticCachePaths = data.staticHTMLCache;
            if (!staticCachePaths) {
                staticCachePaths = [];
            }
            staticCachePaths = staticCachePaths.concat(data.staticAssetsCache);
            deniedCachePaths = data.deniedHTMLCache;
            if (!deniedCachePaths) {
                deniedCachePaths = [];
            }
            deniedCachePaths = deniedCachePaths.concat(data.deniedAssetsCache);
            urlsToCache = urlsToCache.concat(staticCachePaths);

            caches.open("STATIC_CACHE")
                .then(function(cache) {
                    console.log('[Service Worker] Precaching', urlsToCache);
                    return cache.addAll(urlsToCache);
                });
        });

    }

    self.addEventListener('activate', function(event) {

        return self.clients.claim();
    });

    function cacheNotDeniedRequest(url) {

        for (var x = 0; x < deniedCachePaths.length; x++) {
            if (url.indexOf(deniedCachePaths[x]) > -1) {
                return false;
            }
        }
        return true;

    }
    self.addEventListener('fetch', function(event) {
        if (event.request.method === 'GET' && event.request.url.indexOf('.pwa.load/root-service-worker.json') === -1 && event.request.url.indexOf('manifest.json') < 0) {
            /* Cache then Network strategy starts here */
            event.respondWith(
                caches.match(event.request)
                .then(function(response) {
                    if (response) {
                        return response;
                    } else {
                        return fetch(event.request)
                            .then(function(res) {
                                if (cacheNotDeniedRequest(event.request.url)) {
                                    return caches.open("DYNAMIC_CACHE")
                                        .then(function(cache) {
                                            cache.put(event.request.url, res.clone());
                                            return res;
                                        });

                                }
                                return res;

                            })
                            .catch(function(err) {
                                if (event.request.headers.get('accept').indexOf('text/html') > -1)
                                    return caches.match(fallbackCache);
                            });
                    }
                })
            );
            /* Cache then Network strategy ends here */

            /* Network first then cache strategy 

                event.respondWith(
                    fetch(event.request)
                    .then(function(response) {

                        return caches.open("DYNAMIC_CACHE")
                            .then(function(cache) {
                                cache.put(event.request, response.clone());
                                return response;
                            });

                    }).catch(function(err) {
                        if(event.request.headers.get('accept').indexOf('text/html') > -1  &&  caches.match(event.request)){

                            return caches.match(event.request);
                        }else{
							return caches.match(fallbackCache);
                        }
                    })
                );
            }
		*/

        }


        if (event.request.method === 'POST') {
            if (!navigator.onLine) { //intercept the request and store for bgSync
                //TODO code here
            }
        }

    });
})();