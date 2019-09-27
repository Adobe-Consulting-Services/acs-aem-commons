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

/*global angular: false, quickly: false, _: false */
quickly.factory('Results', ['$http', '$q', 'Operations', function($http, $q, Operations) {

    var lastResultTime = 0,

        selectFirstResult = function(results) {
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
        },

        getServerResults = function (cmd) {
            var requestTime = new Date().getTime();

            return $http({
                method: 'GET',
                url: '/bin/quickly.json',
                params: {
                    t: requestTime,
                    cmd: cmd
                }
            }).then(function (response) {
                if (requestTime <= lastResultTime) {
                    // This check prevents previous slow running queries from
                    // overwriting items of faster later queries
                    return;
                }

                lastResultTime = requestTime;
                return selectFirstResult(response.data.results || []);
            });
        },

        getJsOperationResults = function (cmd) {
            var operationResults = Operations.getResults(cmd);

            if(operationResults !== null) {
                return $q.when(selectFirstResult(operationResults));
            } else {
                return null;
            }
        };

    /* Service Object */

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
            var results;

            if(cmd) {
                results = getJsOperationResults(cmd);

                if(results === null) {
                    results = getServerResults(cmd);
                }
            }

            return results || $q.when([]);
        }
    };
}]);