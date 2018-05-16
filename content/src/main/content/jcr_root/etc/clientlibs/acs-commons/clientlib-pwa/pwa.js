var promptInstall;

if ('serviceWorker' in navigator) {
    //The search params would be read from the content resource; manually place the sw next to the path below for testing
    navigator.serviceWorker
    .register('/content/we-retail-pwa/us/service-worker.js?html=/content/we-retail-pwa/us/en|/content/we-retail-pwa/us/en/men&fallback=/content/we-retail-pwa/us/en/fallback')
    .then(function(response) {
        console.log('[Service Worker] registered!');
    }).catch(function(error){
        console.log("[Service Worker] Registration failed ", error);
    });
}
/** I'm referring to load the <link> tag on the page through this js.
    Since this js will be a dependency to we-retail base it will load the manifest and
    if its's not available it will ignore it

      var  el, head = document.getElementsByTagName("head")[0];
                    el = document.createElement("link");
                    el.rel = "manifest";
                    el.href = window.location.path.replace('html','')+'.pwa.load/manifest.json';
                    head.appendChild(el);

*/

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