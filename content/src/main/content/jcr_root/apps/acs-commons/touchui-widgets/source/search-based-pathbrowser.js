(function($, CUI){
    var ACS_PREFIX = "acs.granite.ui.search.pathBrowser",
        ROOT_PATH = "rootPath",
        QUERY_PARAMS = "predicates",
        QUERY = "/bin/querybuilder.json?";

    //executed when user initiates search in pathbrowser by typing in a keyword
    function searchBasedAutocompleteCallback(){
        return{
            name: ACS_PREFIX + '.autocompletecallback',
            handler: autoCompleteHandler
        };

        function autoCompleteHandler(searchTerm){
            var self = this, deferred = $.Deferred(), searchParams;

            if(_.isEmpty(searchTerm)){
                return;
            }

            searchParams = getSearchParameters(self, searchTerm);

            function callback(results){
                if(_.isEmpty(results)){
                    deferred.resolve([]);
                    return;
                }

                self.options.options = results;
                deferred.resolve(_.range(results.length));
            }

            self.optionLoader(searchParams, callback);

            return deferred.promise();
        }

        function getSearchParameters(widget,searchTerm){
            var path = widget.$element.data(ROOT_PATH), tokens,
                queryParams = widget.$element.data(QUERY_PARAMS),
                searchParams = {
                    fulltext: searchTerm
                };

            if(!_.isEmpty(path)){
                searchParams.path = path;
            }

            if(!_.isEmpty(queryParams)){
                queryParams = queryParams.split(" ");

                _.each(queryParams, function(param, index){
                    tokens = param.split("=");
                    searchParams[ (index + 1) + "_property" ] = tokens[0];
                    searchParams[ (index + 1) + "_property.value" ] = tokens[1];
                });
            }

            return searchParams;
        }
    }

    CUI.PathBrowser.register('autocompleteCallback', searchBasedAutocompleteCallback());

    //the option loader for requesting query results
    function searchBasedOptionLoader() {
        return {
            name: ACS_PREFIX + ".optionLoader",
            handler: optionLoaderHandler
        };

        function optionLoaderHandler(searchParams, callback) {
            var query = QUERY;

            _.each(searchParams, function(value, key){
                query = query + key + "=" + value + "&";
            });

            query = query.substring(0, query.length - 1);

            console.log("ACS - Search query - " + query);

            function handler(data){
                var results = [];

                if(!_.isEmpty(data.hits)){
                    results = _.pluck(data.hits, "path");
                }

                if (callback){
                    callback(results);
                }
            }

            $.get(query).done(handler);

            return false;
        }
    }

    CUI.PathBrowser.register('optionLoader', searchBasedOptionLoader());

    //option renderer for creating the option html
    function searchBasedOptionRenderer() {
        return {
            name: ACS_PREFIX + ".optionRenderer",
            handler: optionRendererHandler
        };

        function optionRendererHandler(iterator, index) {
            var value = this.options.options[index];

            return $('<li class="coral-SelectList-item coral-SelectList-item--option" data-value="' + value + '">' + value + '</li>');
        }
    }

    CUI.PathBrowser.register('optionRenderer', searchBasedOptionRenderer());
}(jQuery, window.CUI));