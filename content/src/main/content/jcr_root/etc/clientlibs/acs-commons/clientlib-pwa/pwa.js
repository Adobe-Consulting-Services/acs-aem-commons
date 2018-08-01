var promptInstall;

var currentPath = window.location.pathname;
var pathName = currentPath;
/*
function addManifestToDOM(rootPath) {
    var el, head = document.getElementsByTagName("head")[0];
    el = document.createElement("link");
    el.rel = "manifest";
    el.href = rootPath + '.pwa.load/manifest.json'; //pathName.replace('.html','') +'.pwa.load/manifest.json';
    head.appendChild(el);
}
*/
if ('serviceWorker' in navigator) {

    var rootSWPath = pathName.replace('.html', '') + '.pwa.load/root-service-worker.json';
    Granite.$.ajax({
        type: "GET",
        url: rootSWPath,
    }).then(function(data) {

        if (data && data.path && data.version) {
            checkIDB(data.version).then(function() {
                loadServiceWorker(data.path);
            });
        } else {
            console.log('Failed to load from Root SW');
        }
    }, function(error) {
        console.log('Failed to load Root SW');
    });

}

function checkIDB(versionNum) {
    if (!('indexedDB' in window)) {
        console.log('This browser doesn\'t support IndexedDB');
        return;
    }

    var dbPromise = idb.open('pwa-db', 1, function(db) {
        console.log('database created ');
        if (!db.objectStoreNames.contains('pwa-version-store')) {
            db.createObjectStore('pwa-version-store', {
                keyPath: 'category'
            });
        }
    });

    return dbPromise.then(function(db) {
        var version = versionNum;
        var tx = db.transaction('pwa-version-store', 'readwrite');
        var store = tx.objectStore('pwa-version-store');
        var item = {
            category: 'version',
            version: version
        };
        return store.get('version').then(function(data) {
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
    /*jshint esnext: true */
    navigator.serviceWorker.getRegistrations().then(function(registrations) {
 		for(let registration of registrations) {
  			registration.unregister();
		} 
    });

}
function loadServiceWorker(dataPath) {
    navigator.serviceWorker
        .register(dataPath + '.pwa.load.service-worker.js')
        .then(function(response) {
            //load Manifest after service worker only
             // addManifestToDOM(dataPath);
            // var manifestElement = document.querySelector('link[rel="manifest"]');

            console.log('[Service Worker] registered!');
        }).catch(function(error) {
            console.log("[Service Worker] Registration failed ", error);
        });
}

/*

window.addEventListener("beforeinstallprompt", function(event) {
	event.preventDefault();
    promptInstall = event;
    var headerNav = document.querySelector(".navbar-brand");
    headerNav.addEventListener("click", function(){
		promptInstall.prompt();
        promptInstall.userChoice
    	.then(function(choiceResult){
           if (choiceResult.outcome === 'accepted') {
                console.log('Home screen added');
              } else {
                console.log('Otherwise');
              }
        
          promptInstall = null;

		});

    });


});

 window.addEventListener("load", function(event) {
     console.log('Window loaded ', event);
     if(promptInstall){
		promptInstall.prompt()
        .then(function(outcome){
            console.log("Outcome: ", outcome);
        });

     }

  });

*/