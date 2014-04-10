;
(function ($, ns, channel, window, undefined) {

    var self = {},
        name = 'Audio';

    // make the loadAssets fuction more flexible
    self.searchRoot = '/content/dam';

    var searchPath = self.searchRoot,
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
        searchPath = spath;
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
