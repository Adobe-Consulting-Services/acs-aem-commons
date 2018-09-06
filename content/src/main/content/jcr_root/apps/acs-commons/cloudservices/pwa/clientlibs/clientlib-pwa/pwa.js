if ('serviceWorker' in navigator) {
    var pwaRoot = document.querySelector('meta[name="pwa.root"]').getAttribute('value'),
        configPath = encodeURI(pwaRoot + '.pwa.service-worker.json');

    var rootSW = ''; //fetchRootPath(configPath);
    fetch(configPath).then(function(response) {
        return response.json();
    }).then(function(res) {
        // read the root SW path; currently the fallback[0] is the rootPath for testing purpose
        rootSW = res.fallback[0].pattern;
        //register SW only if the cached version has changed
        if (rootSW) {
            clearCacheCheck(res, rootSW, configPath);
            /*.then(function() {
                 
                    loadSW(rootSW, configPath);
                 
            });*/

        }
    });
}

function clearCacheCheck(resp, rootSW, configPath) {
    caches.has(resp.cache_name)
        .then(function(hasCacheName) {
            if (navigator.serviceWorker.controller === null) { // no SW exists; load one 
                loadSW(rootSW, configPath);
            } else {
                if (!hasCacheName) {
                    updateServiceWorker().then(function() {

                        clearOldCache()
                            .then(function() {
                                navigator.serviceWorker.controller = null;
                                console.log('SW Cleared');
                                loadSW(rootSW, configPath);
                            });
                    });

                }
            }

        });
}

function clearOldCache() {

    return caches.keys()
        .then(function(keyList) {

            return Promise.all(keyList.map(function(key) {
                //delete cache keys
                return caches.delete(key);
            }));
        });
}

function loadSW(rootSW, configPath) {
    var time = new Date().getTime();
    navigator.serviceWorker
        .register(rootSW + '.pwa.service-worker.' + time + '.js?config=' + configPath)
        .then(function() {
            console.log('PWA Service Worker Registered');
        })
        .catch(function(error) {
            console.error('Failed to load SW')
        });
}

function updateServiceWorker(rootSW, configPath) {
    return navigator.serviceWorker.getRegistration().then(

        function(registration) {

            return registration.unregister();

        })

}