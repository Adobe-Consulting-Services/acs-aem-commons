var STATIC_CACHE_NAME = 'static-cache-V4';
var DYNAMIC_CACHE_NAME = 'dynamic-cache-V4';

var urlsToCache = [

  '/content/we-retail-pwa/us/en.html',
    '/content/we-retail-pwa/us/en/fallback.html'

];  

self.addEventListener('install', function(event) {
  event.waitUntil(
    caches.open(STATIC_CACHE_NAME)
    .then(function(cache) {
      return cache.addAll(urlsToCache);
    })
  );
});


self.addEventListener('activate', function(event){
    event.waitUntil(function(){
		caches.keys().then(function(keyList) {
              return Promise.all(keyList.map(function(key) {
                  if (key !== STATIC_CACHE_NAME && key !== DYNAMIC_CACHE_NAME) {
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
            console.log('Request failed:', error);
            // Could return a custom offline 404 page here if the content-type is html, that was cached on install lifecycle
              return caches.match('/content/we-retail-pwa/us/en/fallback.html')
              .then(function(offlineresponse){
                  if(offlineresponse){
                      return offlineresponse.clone();
                  }

              });
 			 });

    }

}
