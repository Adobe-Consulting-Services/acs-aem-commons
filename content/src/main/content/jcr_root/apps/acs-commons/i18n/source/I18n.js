/*
 * ADOBE CONFIDENTIAL
 *
 * Copyright 2012 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 *
 */
/*global CQ: false */
/*global Granite: false */

(function (Granite, util, http, $) {
    /**
     * A helper class providing a set of utilities related to internationalization (i18n).
     * @static
     * @singleton
     * @class Granite.I18n
     */
    Granite.ACSI18n = (function() {

        /**
         * The map where the dictionaries are stored under their locale.
         * @private
         * @type Object
         */
        var dicts = {},

        /**
         * The initialization state of the internationalization.
         * @private
         * @type Boolean
         */
            initialized = false,

        /**
         * The prefix for the URL used to request dictionaries from the server.
         * @private
         * @type String
         */
            urlPrefix = "/bin/acs-commons/components/utilities/i18n/dict.",

        /**
         * The suffix for the URL used to request dictionaries from the server.
         * @private
         * @type String
         */
            urlSuffix = ".json",

        /**
         * The current locale as a String or a function that returns the locale as a string.
         * @private
         * @static
         * @type String
         */
            currentLocale = "en",

             /**
         * The current basename as a String or a function that returns the locale as a string.
         * @private
         * @static
         * @type String
         */
         currentBasename = "",

        /**
         * If the current locale represents pseudo translations.
         * In that case the dictionary is expected to provide just a special
         * translation pattern to automatically convert all original strings.
         */
            pseudoTranslations = false,

            languages = null,

            self = {};

        /**
         * The default locale (en).
         * @static
         * @final
         * @type String
         */
        self.LOCALE_DEFAULT = "en";

        /**
         * Language code for pseudo translations.
         * @static
         * @final
         * @type String
         */
        self.PSEUDO_LANGUAGE = "zz";

        /**
         * Dictionary key for pseudo translation pattern.
         * @static
         * @final
         * @type String
         */
        self.PSEUDO_PATTERN_KEY = "_pseudoPattern_";

        /**
         * Initializes I18n with the given config options:
         * <ul>
         * <li>locale: the current locale (defaults to "en")</li>
         * <li>urlPrefix: the prefix for the URL used to request dictionaries from
         * the server (defaults to "/libs/cq/i18n/dict.")</li>
         * <li>urlSuffix: the suffix for the URL used to request dictionaries from
         * the server (defaults to ".json")</li>
         * </ul>
         * Sample config. The dictioniary would be requested from
         * "/apps/i18n/dict.fr.json":
         <code><pre>{
         "locale": "fr",
         "urlPrefix": "/apps/i18n/dict.",
         "urlSuffix": ".json"
         }</pre></code>
         * @param {Object} config The config
         */
        self.init = function (config) {
            if (!config) {
                config = {};
            }
            if (config.locale) {
                this.setLocale(config.locale);
            }
            urlPrefix = config.urlPrefix || urlPrefix;
            urlSuffix = config.urlSuffix || urlSuffix;
            initialized = true;
        };

        /**
         * Sets the current locale.
         * @static
         * @param {String/Function} locale The locale or a function that returns the locale as a string
         */
        self.setLocale = function (locale) {
            currentLocale = locale;
        };

        /**
         * Returns the current locale or the default locale if none is defined.
         * @static
         * @return {String} The locale
         */
        self.getLocale = function () {
            if(currentLocale && $.isFunction(currentLocale)) {
                // execute function first time only and store result in currentLocale
                currentLocale = currentLocale();
            }
            return currentLocale;
        };



        /**
         * Returns the current Bundle or the default bundle if none is defined.
         * @static
         * @return {String} The Bundle
         */
         self.setBundle = function (bundle) {
         currentBasename = bundle;
         };

        /**
        * Returns the current bundle or the default bundle if none is defined.
        * @static
        * @return {String} The Bundle
        */
        self.getBundle = function () {
            if(currentBasename && $.isFunction(currentBasename)) {
                // execute function first time only and store result in currentLocale
                currentBasename = currentBasename();
            }
            return currentBasename;
        };

        /**
         * Sets the prefix for the URL used to request dictionaries from
         * the server. The locale and URL suffix will be appended.
         * @static
         * @param {String} prefix The URL prefix
         */
        self.setUrlPrefix = function (prefix) {
            urlPrefix = prefix;
        };

        /**
         * Sets the suffix for the URL used to request dictionaries from
         * the server. It will be appended to the URL prefix and locale.
         * @static
         * @param {String} suffix The URL suffix
         */
        self.setUrlSuffix = function (suffix) {
            urlSuffix = suffix;
        };

      /**
         * Returns the dictionary for the specified locale. This method
         * will request the dictionary using the URL prefix, the locale,
         * and the URL suffix. If no locale is specified, the current
         * locale is used.
         * @static
         * @param {String} locale (optional) The locale
         * @return {Object} The dictionary
         */
        self.getDictionary = function (locale, bundle) {
           locale = locale || self.getLocale() || Granite.I18n.LOCALE_DEFAULT;
                    bundle = bundle || self.getBundle();
            var bundleCall, url, response;
            bundleCall = bundle;
            if (!dicts[locale+bundle]) {
                pseudoTranslations = (locale.indexOf(self.PSEUDO_LANGUAGE) === 0);
                if (bundle === undefined || bundle === null || bundle.length <= 0){
                    bundleCall = "";
                    bundle="";
                }
                else{
                   bundleCall = "."+bundle;
                }

                url = urlPrefix + locale + bundleCall +  urlSuffix;
                
                try {
                    response = $.ajax(url, {
                        async: false,
                        dataType: "json"
                    });
                    dicts[locale+bundle] = $.parseJSON(response.responseText);
                } catch (e) {}
                if (!dicts[locale+bundle]) {
                    dicts[locale+bundle] = {};
                }
            }
            return dicts[locale+bundle];
        };

        /**
         * Translates the specified text into the current language.
         * @static
         * @param {String} text The text to translate
         * @param {String[]} snippets The snippets replacing <code>{n}</code> (optional)
         * @param {String} note A hint for translators (optional)
         * @return {String} The translated text
         */
        self.get = function (text, snippets, note) {
            var dict, newText, lookupText;
            if (initialized) {
                dict = self.getDictionary();
            }
            // note that pseudoTranslations is initialized in the getDictionary() call above
            lookupText = pseudoTranslations ? self.PSEUDO_PATTERN_KEY :
                note ? text + " ((" + note + "))" :
                    text;
            if (dict) {
                newText = dict[lookupText];
            }
            if (!newText) {
                newText = text;
            }
            if (pseudoTranslations) {
                newText = newText.replace("{string}", text).replace("{comment}", note ? note : "");
            }
            return util.patchText(newText, snippets);
        };

        /**
         * Translates the specified text into the current language and from the specified basename/resourceBundle.
         * @static
         * @param {String} text The text to translate
         * @param {String} bundleName The name of the resourceBundle
         * @param {String[]} snippets The snippets replacing <code>{n}</code> (optional)
         * @param {String} note A hint for translators (optional)
         * @return {String} The translated text
         */
        self.getBundleMessage = function (text, bundleName, snippets, note) {
            var dict, newText, lookupText;
            if (initialized) {
                dict = self.getDictionary(currentLocale, bundleName);
            }
            // note that pseudoTranslations is initialized in the getDictionary() call above
            lookupText = pseudoTranslations ? self.PSEUDO_PATTERN_KEY :
                note ? text + " ((" + note + "))" :
                    text;
            if (dict) {
                newText = dict[lookupText];
            }
            if (!newText) {
                newText = text;
            }
            if (pseudoTranslations) {
                newText = newText.replace("{string}", text).replace("{comment}", note ? note : "");
            }
            return util.patchText(newText, snippets);
        };
        /**
         * Translates the specified text into the current language. Use this
         * method to translate String variables, e.g. data from the server.
         * @static
         * @param {String} text The text to translate
         * @param {String} note A hint for translators (optional)
         * @return {String} The translated text
         */
        self.getVar = function (text, note) {
            if (!text) {
                return null;
            }
            return self.get(text, null, note);
        };

      

        /**
         * Parses a language code string such as "de_CH" and returns an object with
         * language and country extracted. The delimiter can be "_" or "-".
         * @static
         * @param {String} langCode a language code such as "de" or "de_CH" or "de-ch"
         * @return {Object} an object with "code" ("de_CH"), "language" ("de") and "country" ("CH")
         *                  (or null if langCode was null)
         */
        self.parseLocale = function (langCode) {
            if (!langCode) {
                return null;
            }
            var language, country, pos;
            pos = langCode.indexOf("_");
            if (pos < 0) {
                pos = langCode.indexOf("-");
            }

            if (pos < 0) {
                language = langCode;
                country = null;
            } else {
                language = langCode.substring(0, pos);
                country = langCode.substring(pos + 1);
            }
            return {
                code: langCode,
                language: language,
                country: country
            };
        };

        return self;

    }());

}(Granite, Granite.Util, Granite.HTTP, jQuery));

Granite.ACSI18n.init();
