(function ($, window, undefined) {
    var storageKey = 'cui-state',
        storageLoadEvent = 'cui-state-restore',
        store = {},
        loaded = false,
        $doc = $(document);

    /**
     * state object to enable UI page refresh stable states
     * TODO:
     *  - all states are global, lack of an auto restore mode which is aware of the URL
     *  - client side only (localStorage)
     *  - lack of an abstraction layer for the client side storage
     * @type {Object}
     */
    CUI.util.state = {

        /*saveForm: function (form, elem) {

        },*/

        config: {
            serverpersistence: true
        },

        /**
         * Persist attributes of a DOM node
         *
         * @param {String} selector
         * @param {String|Array} [attribute] single attribute or list of attributes to be saved. If null then all attributes will be saved
         * @param {Boolean} [autorestore]
         * @param {String} [customEvent] custom event name
         */
        save: function (selector, attribute, autorestore, customEvent) {
            var elem = $(selector),
                saveLoop = function (i, attr) {
                    store.global[selector] = store.global[selector] || {};
                    store.global[selector][attr] = store.global[selector][attr] || {};
                    store.global[selector][attr].val = elem.attr(attr);
                    store.global[selector][attr].autorestore = autorestore || false;
                    store.global[selector][attr].customEvent = customEvent || null;
                };

            
            if (attribute) { // save single or multiple attributes
                if ($.isArray(attribute)) { // multiple values to save
                    $.each(attribute, saveLoop);
                } else { // save all attributes
                    saveLoop(0, attribute);
                }
            } else { // save all attributes
                // TODO
                // not supported yet because the browser implementation of Node.attributes is a mess
                // https://developer.mozilla.org/en-US/docs/DOM/Node.attributes
            }

            localStorage.setItem(storageKey, JSON.stringify(store));
            
            if (CUI.util.state.config.serverpersistence) {
                $.cookie(storageKey, JSON.stringify(store), {
                    expires: 7,
                    path: '/'
                });
            }
        },

        /**
         *
         * @param {String} [selector]
         * @param {Function} [filter] filter function for the attributes of the given selector
         */
        restore: function (selector, filter) {
            var check = filter || function () {
                    return true;
                },
                sel,
                elem,
                selectorLoop = function (item, noop) {
                    sel = item;
                    elem = $(sel);

                    if (store.global[sel]) {
                        $.each(store.global[sel], restoreLoop);
                    }
                },
                restoreLoop = function (attr, obj) {
                    if (check(sel, attr, obj)) {
                        elem.attr(attr, obj.val);

                        if (obj.customEvent) {
                            $doc.trigger(obj.customEvent, [elem, obj]);
                        }

                        $doc.trigger(storageLoadEvent, [elem, obj]);
                    }
                };

            if (!loaded) {
                loaded = CUI.util.state.load();
            }

            
            if (selector) { // restore single selector
                selectorLoop(selector);
            } else { // restore everything
                $.each(store.global, selectorLoop);
            }
        },

        load: function () {
            var val = localStorage.getItem(storageKey);

            store = val ? JSON.parse(val) : {
                global: {}
            };

            return true;
        },

        // support for "temporary" storage that will be automatically cleared if
        // the browser session ends; currently uses a set/get pattern rather than
        // loading the entire thing on document ready. Also note that the data is currently
        // not sent to the server.

        setSessionItem: function(name, value, ns) {
            var key = name;
            if (ns) {
                key = name + ":" + ns;
            }
            sessionStorage.setItem(key, JSON.stringify(value));
        },

        getSessionItem: function(name, ns) {
            var key = name;
            if (ns) {
                key = name + ":" + ns;
            }
            var value = sessionStorage.getItem(key);
            if (value) {
                value = JSON.parse(value);
            }
            return value;
        },

        removeSessionItem: function(name, ns) {
            var key = name;
            if (ns) {
                key = name + ":" + ns;
            }
            sessionStorage.removeItem(key);
        },

        clearSessionItems: function(ns) {
            if (ns) {
                ns = ":" + ns;
                var keyCnt = sessionStorage.length;
                var toRemove = [ ];
                for (var k = 0; k < keyCnt; k++) {
                    var keyToCheck = sessionStorage.key(k);
                    var keyLen = keyToCheck.length;
                    if (keyLen > ns.length) {
                        if (keyToCheck.substring(keyLen - ns.length) === ns) {
                            toRemove.push(keyToCheck);
                        }
                    }
                }
                var removeCnt = toRemove.length;
                for (var r = 0; r < removeCnt; r++) {
                    sessionStorage.removeItem(toRemove[r]);
                }
            }
        }

    };

    $doc.ready(function () {
        CUI.util.state.restore(null, function (selector, attr, val) {
            if (val.autorestore) {
                return true;
            }

            return false;
        });
    });
}(jQuery, this));