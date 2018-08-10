if ('serviceWorker' in navigator) {
    var pwaRoot = document.querySelector('meta[name="pwa.root"]').getAttribute('value'),
        configPath = encodeURIComponent(pwaRoot + '.pwa.service-worker.json');

    navigator.serviceWorker
        .register(pwaRoot + '.pwa.service-worker.js?config=' + configPath)
        .then(function() { console.debug('PWA Service Worker Registered'); });
}
