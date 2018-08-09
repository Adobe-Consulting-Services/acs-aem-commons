(function() {

    var promptInstall,
        currentPath = window.location.pathname,
        pathName = currentPath,
        serviceWorkerJsPath = Granite.$("meta['name=pwa.root']").attr("value");

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
            }  else {
                console.error('Failed to load from PWA Service Worker.');
            }
        }, function(error){
            console.error('Failed to load PWA Service Worker', error);
        });

    }

    function loadServiceWorker(data){
        navigator.serviceWorker
            .register(serviceWorkerJsPath)
            //.register(data +'.pwa.'+'service-worker.js')
            .then(function(response) {
                //load Manifest after service worker only
                addManifestToDOM();

                console.debug('PWA Service Worker registered!');
            }).catch(function(error){
            console.error("PWA Service Worker registration failed :(", error);
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

}());
