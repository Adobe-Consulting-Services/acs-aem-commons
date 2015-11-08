/*
 * #%L
 * ACS AEM Commons Bundle
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

/*global angular: false, quickly: false, JSON: false, _: false */
quickly.factory('FavoritesOperation', ['$document', '$timeout', '$window', '$filter', '$localStorage', 'Init', 'Command', 'Result',
    function($document, $timeout, $window, $filter, $localStorage, Init, Command, Result) {

    var initialized = true,

        MAX_SIZE = 100,
        REMOVE_CMD = 'rm',

        ADD_METHOD = 'add',
        REMOVE_METHOD = 'rm',

        ADD_FAVORITE_RESULT = Result.build({
            title: 'Add Favorite',
            description: 'Add this page to your favorites',
            action: {
                method: Result.ACTION_METHODS.JS_OPERATION_ACTION,
                params: {
                    method: ADD_METHOD
                }
            }
        });

        function getStorageId() {
            return Init.getData().user + '-favorites';
        }

    return  {

        cmd: ['*'],
        clientSide: true,

        accepts: function(cmdOp) {
            return this.cmd.indexOf(cmdOp) > -1;
        },

        getResults: function(cmd) {
            var results = _.clone($localStorage.quickly.operations[getStorageId()] || [], true),
                params = Command.getParams(cmd, true, 2),
                param = '';

            results = results.slice(0);

            if(!Command.hasParam(cmd, true)) {
                results.unshift(ADD_FAVORITE_RESULT);
            }

            if(this.isRemoveFavoriteCmd(cmd)) {
                // Set filter to the 2nd Param segment
                param = params[1] || '';

                // Mark remove results as handling client side
                angular.forEach(results, function(result) {
                    _.merge(result, {
                        action: {
                            method: Result.ACTION_METHODS.JS_OPERATION_ACTION,
                            params: {
                                method: REMOVE_METHOD
                            }
                        }
                    });
                });
            } else {
                param = Command.getParam(cmd, true);
            }

            return $filter('title')(results, param.trim());
        },

        process: function(cmd, result) {
            if(Result.isJsOperationAction(result)) {
                if(this.isRemoveFavorite(result)) {
                    return this.removeFavorite(result);
                } else if(this.isAddFavorite(result)) {
                    return this.addFavorite();
                }
            }

            return false;
        },

        isAddFavorite: function(result) {
            return(result.action.params.method && result.action.params.method === ADD_METHOD);
        },

        isRemoveFavorite: function(result) {
            return(result.action.params.method && result.action.params.method === REMOVE_METHOD);
        },

        isRemoveFavoriteCmd: function(cmd) {
            var params = Command.getParams(cmd, true, 2);
            return ((params.length === 1 || params.length === 2) &&
                params[0] === REMOVE_CMD);
        },

        addFavorite: function() {
            var entry,
                uri,
                favorites = this.getFavorites(),
                i = 0,
                j = 1;

            uri = ($window.location.pathname + $window.location.search + $window.location.hash) || '';

            if(!_.isEmpty(uri)) {
                /* Create the result for the current favorite'd page */
                entry = Result.build({
                    title: $document.attr('title') || 'Favorite\'d Page',
                    description: uri,
                    action: {
                        uri: uri
                    }
                });

                /* Add to the local storage favorites */

                if(entry.title && entry.action.uri) {
                    for (i = 0; i < favorites.length && j < MAX_SIZE; i += 1) {
                        if (favorites[i].action && favorites[i].action.uri === entry.action.uri) {
                            // Remove from favorites; will add to the front of the list below
                            favorites.splice(i, 1);
                        }
                    }

                    // Add favorite onto the front
                    favorites.unshift(entry);
                }
            }

            return true;
        },

        removeFavorite: function(result) {
            var favorites = this.getFavorites(),
                i = 0;

            if(result.action.uri) {
                for (i = 0; i < favorites.length; i += 1) {
                    if (favorites[i].action && favorites[i].action.uri === result.action.uri) {
                        // Remove from favorites
                        favorites.splice(i, 1);
                    }
                }
            }

            return true;
        },

        getFavorites: function() {
            $localStorage.quickly = $localStorage.quickly || {
                operations: {}
            };

            $localStorage.quickly.operations[getStorageId()] = $localStorage.quickly.operations[getStorageId()] || [];

            return $localStorage.quickly.operations[getStorageId()];
        }

    };
}]);