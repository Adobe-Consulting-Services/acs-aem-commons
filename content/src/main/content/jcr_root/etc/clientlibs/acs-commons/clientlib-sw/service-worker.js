(function(){
	var urlsToCache = [];
    var fallbackCache ='';
    var versionNumber = 'V1' ;

self.addEventListener('install', function(event) {
 
    //console.log('url location :: '+new URL(location));
    versionNumber = new URL(location).toString();
    var urlParams =  new URLSearchParams(new URL(location).search);
    var counter =0;

/*jshint esnext: true */

    for(var key of urlParams.keys()) {
        if(key === 'version'){
            versionNumber = urlParams.get(key);
        }else if(key === 'html'){
			var keyUrl = urlParams.get(key).split('|');
            for(var x=0; x< keyUrl.length; x++){
                urlsToCache.push(keyUrl[x]+'.'+key);
            }
        }
        else if(key === 'fallback'){
			fallbackCache = urlParams.get(key).replace('.js', '')+ '.html';
            urlsToCache.push(fallbackCache);
        }

    }

    console.log("[Service Worker] caching static urls: ", urlsToCache);
    event.waitUntil(
        caches.open("STATIC_"+versionNumber)
        .then(function(cache) {
            return cache.addAll(urlsToCache);
        })
    );
});


self.addEventListener('activate', function(event){
    event.waitUntil(function(){
		caches.keys().then(function(keyList) {
              return Promise.all(keyList.map(function(key) {
                  if (key !== "STATIC_"+versionNumber && key !== "DYNAMIC_"+versionNumber) {
                    return caches.delete(key);
                  }
              }));

    	});
    });
	//return self.clients.claim();
});


self.addEventListener('fetch', function(event) {
    if(event.request.method === 'GET'){
        fetchAndCache(event.request)
            .then(function(response){
                  return response;
                  })
            .catch(function(){
            caches.match(event.request)
                .then(function(response){
                return response;
            });
        });

    }
    if(event.request.method === 'POST'){
        if(!navigator.onLine){ //intercept the request and store for bgSync
			//TODO code here
        }
    }

});



function fetchAndCache(url) {

    if(url.method != "GET"){
		return; // cache only the GET requests or HEAD 
    }else{
          return fetch(url)
          .then(function(response) {
            // Check if we received a valid response
              if(response){
                  return caches.open("DYNAMIC_"+versionNumber)
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


})();
