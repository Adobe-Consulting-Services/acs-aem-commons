var promptInstall;

if ('serviceWorker' in navigator) {
    //The search params would be read from the content resource
    navigator.serviceWorker
    .register('/content/we-retail-pwa/us/service-worker.js?html=/content/we-retail-pwa/us/en|/content/we-retail-pwa/us/en/men&fallback=/content/we-retail-pwa/us/en/fallback')
    .then(function(response) {
        console.log('[Service Worker] registered!');
    }).catch(function(error){
        console.log("[Service Worker] Registration failed ", error);
    });
}
/*
window.addEventListener("beforeinstallprompt", function(event) {
	event.preventDefault();
    promtInstall = event;

});

 window.addEventListener("load", function(event) {
     console.log('Window loaded ', event);
     if(promtInstall){
		promtInstall.prompt()
        .then(function(outcome){
            console.log("Outcome: ", outcome);
        });

     }

  });

*/