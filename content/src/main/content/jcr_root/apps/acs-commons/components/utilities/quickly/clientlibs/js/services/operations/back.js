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

/*global angular: false, quickly: false, JSON: false, document: false */
quickly.factory('BackOperation', ['$timeout', '$window', '$filter', '$localStorage', 'Command', 'Result',
    function($timeout, $window, $filter, $localStorage, Command, Result) {

    var MAX_SIZE = 50,
        initialized = false;

    return  {

        cmd: ['back'],
        clientSide: true,

        accepts: function(cmdOp) {
            return this.cmd.indexOf(cmdOp) > -1;
        },

        getResults: function(cmd) {
            var results = $localStorage.quickly.operations.back || [],
                param = Command.getParam(cmd, true);

            // Remove current page
            if(results.length > 0) {
                results = results.slice(1, results.length);
            }


            return $filter('title')(results, param);
        },

        init: function() {

            $timeout(function() {

                var entry,
                    history,
                    i = 0,
                    j = 1;

                $localStorage.quickly = $localStorage.quickly || {
                    operations: {}
                };

                history = $localStorage.quickly.operations.back || [];


                /* Create the result for the current visited page */
                entry = Result.build();
                entry.title = document.title || '??? Page';
                entry.action.uri = ($window.location.pathname + $window.location.search + $window.location.hash) || '';
                entry.description = entry.action.uri;

                /* Add to the local storage history */

                if(entry.title && entry.action.uri) {
                    for(i = 0; i <  history.length && j < MAX_SIZE; i += 1) {
                        if(history[i].action && history[i].action.uri === entry.action.uri) {
                            // Remove from history; will add to the front of the list below
                            history.splice(i, 1);
                        }
                    }

                    // Add history onto the front
                    history.unshift(entry);
                }


                $localStorage.quickly.operations.back = history;

                initialized = true;

            }, 2500);
        }
    };
}]);