(function () {
    if ('serviceWorker' in navigator) {
        var pwaRoot = document.querySelector('meta[name="pwa.root"]').getAttribute('value'),
            configPath = encodeURI(pwaRoot + '.pwa.service-worker.json');

        fetch(configPath).then(function (response) {
            return response.json();
        }).then(function (res) {
            if (res && res.scope) {
                registerSW(res.scope, res.version);
            }
        });
    }

    function registerSW(scope, cacheVersion) {
        checkIDB(cacheVersion).then(function () {
            loadSW(scope, cacheVersion);
        });
    }


    function deregisterSW() {

        navigator.serviceWorker.getRegistrations()
        .then(function (registrations) {
        /*jshint esnext: true */
            for (let registration of registrations) {
                registration.unregister();
            }
        });
    }

    /* load a service worker under a given path with the version number  */
    function loadSW(swRootPath, version) {
        navigator.serviceWorker
            .register(swRootPath + '.pwa.service-worker.V' + version + '.js')
            .then(function () {
                console.log('PWA Service Worker Registered');
            })
            .catch(function () {
                console.error('Failed to load SW');
            });
    }

    /* Check the versionNum matches in IndexedDb else unregister the Service worker */
    function checkIDB(versionNum) {
        if (!('indexedDB' in window)) {
            console.log('This browser doesn\'t support IndexedDB');
            return;
        }
        var dbPromise = idb.open('pwa-aem-db', 1, function (db) {
            console.log('database created ');
            if (!db.objectStoreNames.contains('aem-pwa-store')) {
                db.createObjectStore('aem-pwa-store', {
                    keyPath: 'pwaStore'
                });
            }
        });

        return dbPromise.then(function (db) {
            var version = versionNum;
            var tx = db.transaction('aem-pwa-store', 'readwrite');
            var store = tx.objectStore('aem-pwa-store');
            var item = {
                pwaStore: 'pwa-version-store',
                version: version,
                configPath: configPath
            };
            return store.get('pwa-version-store').then(function (data) {
                if (!data || data.version !== version) {
                    store.clear();
                    store.put(item);
                    deregisterSW();
                    return tx.complete;
                }
            });


        });
    }


})();
