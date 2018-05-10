const STATIC_CACHE_NAME = 'STATIC_CACHE';
const DYNAMIC_CACHE_NAME = 'DYNAMIC_CACHE';
var urlsToCache = [];
var fallbackCache ='';

self.addEventListener('install', function(event) {

    var urlParams =  new URLSearchParams(new URL(location).search);
    var counter =0;

    for(var key of urlParams.keys()) {
        if(key !== 'fallback'){
			var keyUrl = urlParams.get(key).split('|');
            for(var x=0; x< keyUrl.length; x++){
                urlsToCache.push(keyUrl[x]+'.'+key);
                counter++;
            }
        }
        else{
			fallbackCache = urlParams.get(key)+ '.html';
            urlsToCache.push(fallbackCache);
            counter++;
        }

    }

    console.log("[Service Worker] caching static urls: ", urlsToCache);
    event.waitUntil(
        caches.open(STATIC_CACHE_NAME+counter)
        .then(function(cache) {
            return cache.addAll(urlsToCache);
        })
    );
});


self.addEventListener('activate', function(event){
    event.waitUntil(function(){
		caches.keys().then(function(keyList) {
              return Promise.all(keyList.map(function(key) {
                  if (key !== STATIC_CACHE_NAME+counter && key !== DYNAMIC_CACHE_NAME) {
                    return caches.delete(key);
                  }
              }));

    	});
    });
	//return self.clients.claim();
});


self.addEventListener('fetch', function(event) {
    if(event.request.method === 'GET'){
		  event.respondWith(
            caches.match(event.request)
            .then(function(response) {
              return response || fetchAndCache(event.request);
            })
          );
    }
    if(event.request.method === 'POST'){
        if(!navigator.onLine){ //intercept the request and store for bgSync
			//TODO code here
        }
    }

});



function fetchAndCache(url) {

    if(url.method != "GET"){
		return; // cache only the GET requests or HEAD as per the tech specs
    }else{
          return fetch(url)
          .then(function(response) {
            // Check if we received a valid response
              if(response){
                  return caches.open(DYNAMIC_CACHE_NAME)
                  .then(function(cache) {
                      cache.put(url, response.clone());
                      return response;
                  });
              }

          })
          .catch(function(error){
          // Could return a custom offline 404/ Fallback page here if the "Accept" headers is "text/html", that was cached on install lifecycle
              if(url.headers.get('Accept').indexOf('text/html') !== -1){
				  console.log('Request failed:', error);
                  return caches.match(fallbackCache)
                  .then(function(offlineResponse){
                      if(offlineResponse){
                          return offlineResponse.clone();
                      }

                  });
              }


 			 });

    }

}
