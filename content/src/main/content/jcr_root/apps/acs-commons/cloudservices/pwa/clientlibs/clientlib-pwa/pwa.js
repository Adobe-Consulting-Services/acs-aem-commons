if ('serviceWorker' in navigator) {
    var pwaRoot = document.querySelector('meta[name="pwa.root"]').getAttribute('value'),
        configPath = encodeURI(pwaRoot + '.pwa.service-worker.json');

    var rootSW = ''; //fetchRootPath(configPath);
    fetch(configPath).then(function(response) {
        return response.json();
    }).then(function(res) {
        // read the root SW path; currently the fallback[0] is the rootPath for testing purpose
         
        //register SW only if the cached version has changed
        if (res && res.scope) {
            registerSW(res, configPath);
        }
    });
}

function registerSW(resp, configPath) {
    checkIDB(resp.version).then(function() {
                        loadSW(resp.scope, configPath, resp.version);
                    });/*
    caches.has(resp.cache_name)
        .then(function(hasCacheName) {
            if (navigator.serviceWorker.controller === null) { // no SW exists; load one 
                loadSW(resp.scope, configPath, resp.version);
            } else {
                if (!hasCacheName && navigator.onLine) {
                     updateServiceWorker().then(function() {

                        clearOldCache()
                            .then(function() {
                                navigator.serviceWorker.controller = null;
                                loadSW(resp.scope, configPath, resp.version);
                            });
                    }); 
                    

                }
            }

        });*/
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

function loadSW(rootSW, configPath, version) {
    navigator.serviceWorker
        .register(rootSW + '.pwa.service-worker.V' + version + '.js')
        .then(function() {
            console.log('PWA Service Worker Registered');
        })
        .catch(function(error) {
            console.error('Failed to load SW');
        });
}

function updateServiceWorker(rootSW, configPath) {
    return navigator.serviceWorker.getRegistration().then(

        function(registration) {

            return registration.unregister();

        });

}


function checkIDB(versionNum) {
    if (!('indexedDB' in window)) {
        console.log('This browser doesn\'t support IndexedDB');
        return;
    }

    var dbPromise = idb.open('pwa-aem-db', 1, function(db) {
        console.log('database created ');
        if (!db.objectStoreNames.contains('aem-pwa-store')) {
            db.createObjectStore('aem-pwa-store', {
                keyPath: 'pwaStore'
            });
        }
    });

    return dbPromise.then(function(db) {
        var version = versionNum;
        var tx = db.transaction('aem-pwa-store', 'readwrite');
        var store = tx.objectStore('aem-pwa-store');
        var item = {
            pwaStore: 'pwa-version-store', 
            version: version, 
            configPath: configPath
        };
        return store.get('pwa-version-store').then(function(data) {
            if (!data || data.version !== version) {
                store.clear();
                store.put(item);
                updateCacheAndSW();
				return tx.complete;
            }
        });


    });




}

function updateCacheAndSW(){
	caches.delete('STATIC_CACHE');
    caches.delete('DYNAMIC_CACHE');
    navigator.serviceWorker.getRegistrations().then(function(registrations) {
 		for(let registration of registrations) {
  			registration.unregister()
		} 
    });

}