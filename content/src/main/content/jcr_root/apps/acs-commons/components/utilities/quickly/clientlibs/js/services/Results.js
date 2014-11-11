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

/*global angular: false, quickly: false */
quickly.factory('Results', ['$http', '$q', 'Operations', function($http, $q, Operations) {

    var selectFirstResult = function(results) {
            var i = 0;

            if(results) {
                for(; i < results.length; i++) {
                    results[i].selected = false;
                }

                if (results[0]) {
                    results[0].selected = true;
                }
            }

            return results;
        };

    return {

        clear: function(results) {
            results.splice(0, results.length);
        },

        select: function (result, results) {
            var i = 0;
            for (; i < results.length; i++) {
                results[i].selected = false;
            }

            if(result) {
                result.selected = true;
            }
        },

        up: function(results) {
            var i = this.findSelectedIndex(results);

            if(i > 0) {
                this.select(results[i - 1], results);
            }
        },

        down: function(results) {
            var i = this.findSelectedIndex(results);

            if(i < results.length - 1) {
                this.select(results[i + 1], results);
            }
        },

        getSelected: function(results) {
            var i = this.findSelectedIndex(results);
            return results[i];
        },

        findSelectedIndex: function (results) {
            var i = 0;
            for (; i < results.length; i++) {
                if (results[i].selected) {
                    return i;
                }
            }

            return 0;
        },

        getResults: function (cmd) {
            var requestTime = new Date().getTime(),
                operationResults = null;

            if (!cmd) {
                return $q.when([]);
            } else {

                // Try Client-side Operations first

                operationResults = Operations.getResults(cmd);

                if(operationResults !== null) {
                    return $q.when(selectFirstResult(operationResults));
                }

                // Only go to Server if a client side operation did not provide results

                return $http({
                    method: 'GET',
                    url: '/bin/quickly.json',
                    params: {
                        t: requestTime,
                        cmd: cmd
                    }
                }).then(function (response) {
                    var results;

                    if (requestTime <= this.resultTime) {
                        // This check prevents previous slow running queries from
                        // overwriting items of faster later queries
                        return;
                    }

                    this.resultTime = requestTime;
                    results = response.data.results || [];

                    results = selectFirstResult(results);

                    return results;
                });
            }
        }
    };
}]);