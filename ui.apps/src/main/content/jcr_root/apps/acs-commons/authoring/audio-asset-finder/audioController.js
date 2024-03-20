/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/*global Granite: false */
(function ($, ns, channel, window) {

    var self = { searchRoot : '/content/dam' },
        name = 'Audio',
        searchPath = self.searchRoot,
        imageServlet = '/bin/wcm/contentfinder/asset/view.html',
        itemResourceType = 'cq/gui/components/authoring/assetfinder/asset';

    /**
     *
     * @param query {String} search query
     * @param lowerLimit {Number} lower bound for paging
     * @param upperLimit {Number} upper bound for paging
     * @returns {jQuery.Promise}
     */
    self.loadAssets = function (query, lowerLimit, upperLimit) {
        // the image servlet is now used
        // though a different rendering will be necessary for extensions
        var param = {
                '_dc': new Date().getTime(),  // cache killer
                'query': query,
                'mimeType': 'audio',
                'itemResourceType': itemResourceType, // single item rendering (cards)
                'limit': lowerLimit + ".." + upperLimit,
                '_charset_': 'utf-8'
            };

        return $.ajax({
            type: 'GET',
            dataType: 'html',
            url: Granite.HTTP.externalize(imageServlet) + searchPath,
            data: param
        });
    };

    /**
     * Set URL to image servlet
     * @param servlet {String} URL to image servlet
     */
    self.setServlet = function (imgServlet) {
        imageServlet = imgServlet;
    };

    self.setSearchPath = function (spath) {
        searchPath = spath || self.searchRoot;
    };

    self.setItemResourceType = function (rt) {
        itemResourceType = rt;
    };

    self.resetSearchPath = function () {
        searchPath = self.searchRoot;
    };

    // register as a asset tab
    ns.ui.assetFinder.register(name, self);

}(jQuery, Granite.author, jQuery(document), this));
