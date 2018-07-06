(function() {
        var urlsToCache = [];
        var fallbackCache = '';
        var versionNumber = 'V1';

        self.addEventListener('install', function(event) {

            var urlLocation = new URL(location).toString();
            //      fetch(urlLocation+'.pwa.load/cacheDetails.json');
            var urlParams = new URLSearchParams(new URL(location).search);
            var counter = 0;

            /*jshint esnext: true */
            /*

                    for (var key of urlParams.keys()) {
                        if (key === 'version') {
                            versionNumber = urlParams.get(key);
                        } else if (key === 'html') {
                            var keyUrl = urlParams.get(key).split('|');
                            for (var x = 0; x < keyUrl.length; x++) {
                                urlsToCache.push(keyUrl[x] + '.' + key);
                            }
                        } else if (key === 'fallback') {
                            fallbackCache = urlParams.get(key).replace('.js', '') + '.html';
                            urlsToCache.push(fallbackCache);
                        }

                    }
            */

            console.log("[Service Worker] caching static urls: ", urlsToCache);
            event.waitUntil(
                caches.open("STATIC_CACHE")
                .then(function(cache) {
                    return cache.addAll(urlsToCache);
                })
            );
        });


        self.addEventListener('activate', function(event) {
            /*        event.waitUntil(function() {
                        caches.keys().then(function(keyList) {
                            return Promise.all(keyList.map(function(key) {
                                if (key !== "STATIC_CACHE"  && key !== "DYNAMIC_CACHE") {
                                    return caches.delete(key);
                                }
                            }));

                        });
                    });*/
            return self.clients.claim();
        });


        self.addEventListener('fetch', function(event) {
            if (event.request.method === 'GET' && event.request.url.indexOf('.pwa.load/root-service-worker.json') === -1) {
                event.respondWith(
                    fetch(event.request)
                    .then(function(response) {

                        return caches.open("DYNAMIC_CACHE")
                            .then(function(cache) {
                                cache.put(event.request, response.clone());
                                return response;
                            });

                    }).catch(function(err) {
                        return caches.match(event.request);
                    })
                );
            }


            if (event.request.method === 'POST') {
                if (!navigator.onLine) { //intercept the request and store for bgSync
                    //TODO code here
                }
            }

        });
})();