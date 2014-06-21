/*
 * #%L
 * ACS AEM Tools Package
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

/*global angular: false */

angular.module('filters', []).filter('indexKeywordFilter', function () {

    return function (indexes, query) {
        var result = {};

        if (!query) {
            return indexes;
        }

        query = angular.lowercase(query);

        angular.forEach(indexes, function (index, indexName) {
            var i, key, match = false;

            for (key in index) {
                if (index.hasOwnProperty(key)) {
                    if(angular.isArray(index[key])) {
                        for(i in index[key]) {
                            match = angular.lowercase(index[key][i]).indexOf(query) > -1;
                            if(match) {
                                break;
                            }
                        }
                    } else if (angular.isString(index[key])) {
                        match = angular.lowercase(index[key]).indexOf(query) > -1;
                    }
                }

                if(match) {
                    result[indexName] = index;
                    break;
                }
            }
        });

        return result;
    };
}).filter('indexCheckedFilter', function () {

    return function (indexes, query) {
        var result = [];

        if (!query) {
            return indexes;
        }

        angular.forEach(indexes, function (index, indexName) {
            if(index.checked === query) {
                result.push(index);
            }
        });

        return result;
    };
});

