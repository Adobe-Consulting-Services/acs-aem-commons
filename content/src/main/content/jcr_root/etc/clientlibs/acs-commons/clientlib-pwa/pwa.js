var promptInstall;

if ('serviceWorker' in navigator) {
    navigator.serviceWorker
    .register('/etc/clientlibs/acs-commons/clientlib-sw.js')
    .then(function(response) {
        console.log('[Service Worker] registered!');
    }).catch(function(error){
        console.log("Resgistration failed ", error);
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