var promptInstall;

var currentPath = window.location.pathname;
var pathName = currentPath;

function addManifestToDOM(){
	var  el, head = document.getElementsByTagName("head")[0];
    el = document.createElement("link");
    el.rel = "manifest";
    el.href = pathName.replace('.html','') +'.pwa.load/manifest.json';
    head.appendChild(el);
}

if ('serviceWorker' in navigator) {
    
    var rootSWPath = pathName.replace('.html','')+'.pwa.load/root-service-worker.json';
	 Granite.$.ajax({
        type : "GET",
        url : rootSWPath,
    }).then(function(data) {

         if(data && data.path){
			loadServiceWorker(data.path);
         }
         else{
             console.log('Failed to load from Root SW');
         }
     }, function(error){
		console.log('Failed to load Root SW');
     });

}
 
function loadServiceWorker(data){
	 navigator.serviceWorker
            .register(data+'.pwa.'+'service-worker.js')
            .then(function(response) {
            //load Manifest after service worker only
                addManifestToDOM();
                 
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