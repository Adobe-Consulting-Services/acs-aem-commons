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

angular.module('oakIndexManager', ['filters'])
    .controller('MainCtrl', function ($scope, $http, $timeout) {

        $scope.app = {
            resource: '',
            running: false
        };

        $scope.notifications = [];
        $scope.indexes = {};
        $scope.async = {};

        $scope.$watch('toggleChecks', function (newValue, oldValue) {
            angular.forEach($scope.filtered, function (index, key) {
                index.checked = newValue;
            });
        });

        $scope.list = function () {
            $http({
                method: 'GET',
                url: encodeURI($scope.app.resource + '.list.json'),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).
                success(function (data, status, headers, config) {
                    var priorState = $scope.indexes;

                    $scope.async.status = data['async-status'] || '';
                    $scope.async.start = data['async-start'] || '';
                    $scope.async.reindexDone = data['async-reindex-done'] || '';
                    $scope.async.reindexStatus = data['async-reindex-status'] || '';

                    $scope.indexes = {};

                    angular.forEach(data, function (index, key) {
                        if (index['jcr:primaryType'] === 'oak:QueryIndexDefinition') {
                            index.name = key;

                            // Don't clear any applied checkmarks
                            index.checked = (function (needle, haystack) {
                                var checked = false;
                                angular.forEach(haystack, function (value, key) {
                                    if (value.name === needle) {
                                        checked = value.checked || false;
                                    }
                                });

                                return checked;
                            }(index.name, priorState));

                            $scope.indexes[index.name] = index;
                        }
                    });

                }).
                error(function (data, status, headers, config) {
                    $scope.addNotification('error', 'ERROR', 'Unable to retrieve Oak indexes; Ensure you are running with elevated permissions and are on AEM6+');
                });
        };

        $scope.get = function (index) {
            $http({
                method: 'GET',
                url: encodeURI($scope.app.resource + '.get.json'),
                params: { name: index.name },
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).
                success(function (data, status, headers, config) {
                    data.name = index.name;
                    data.checked = index.checked;
                    $scope.indexes[index.name] = data;

                    if(index.reindex && !data.reindex) {
                        $timeout.cancel(index.timeout);
                        $scope.addNotification('success', 'SUCCESS', 'Reindex completed for: ' + index.name);
                    }
                }).
                error(function (data, status, headers, config) {
                    $scope.addNotification('error', 'ERROR', 'Unable to retrieve Oak index: ' + index.name);
                });
        };

        $scope.reindex = function (index) {
            $scope.app.running = true;
            index.reindex = true;

            $http({
                method: 'POST',
                url: encodeURI($scope.app.resource + '.reindex.json'),
                data: 'name=' + index.name,
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).
                success(function (data, status, headers, config) {
                    $scope.app.running = false;
                    $scope.addNotification('info', 'INFO', 'Reindex requested for: ' + index.name);
                }).
                error(function (data, status, headers, config) {
                    $scope.app.running = false;
                    index.reindex = false;
                    $scope.addNotification('error', 'ERROR', 'Reindex request failed for: ' + index.name);
                });

            $scope.refresh(index);
        };

        $scope.bulkReindex = function (bulkIndexes) {
            var data = (function () {
                var params = [];

                angular.forEach(bulkIndexes, function (index, key) {
                    params.push('name=' + encodeURIComponent(index.name));
                });

                return params.join('&');
            }());

            if (!data) {
                $scope.addNotification('help', 'HELP', 'Select one or more checkboxes in the index table to bulk reindex');
                return;
            }

            $scope.app.running = true;

            angular.forEach(bulkIndexes, function (index, key) {
                index.reindex = true;
            });

            $http({
                method: 'POST',
                url: encodeURI($scope.app.resource + '.reindex.json'),
                data: data,
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).
                success(function (data, status, headers, config) {
                    var i;
                    for(i in bulkIndexes) {
                        $scope.refresh(bulkIndexes[i]);
                    }

                    if (data.success) {
                        $scope.addNotification('info', 'INFO', 'Bulk reindex requested for: ' + data.success.join(', '));
                    }

                    $scope.app.running = false;
                }).
                error(function (data, status, headers, config) {
                    var i;
                    for(i in bulkIndexes) {
                        $scope.refresh(bulkIndexes[i]);
                    }

                    if (data.success) {
                        $scope.addNotification('info', 'INFO', 'Bulk reindex request succeeded for: ' + data.success.join(', '));
                    }

                    if (data.error) {
                        $scope.addNotification('error', 'ERROR', 'Bulk reindex request failed for: ' + data.error.join(', '));
                    }

                    $scope.app.running = false;
                });

        };

        $scope.refresh = function (index, count) {
            count = count || 0;

            if (index && index.reindex) {
                if (count++ < 30) {
                    index.timeout = $timeout(function () {
                        $scope.get(index);
                        $scope.refresh(index, count);
                    }, 2500 * count);
                } else {
                    $scope.addNotification('info', 'INFO', 'Reindex check timed out for : ' + index.name);
                    $timeout.cancel(index.timeout);
                }
            }
        };

        $scope.addNotification = function (type, title, message) {
            var timeout = 10000;

            if (type === 'success') {
                timeout = timeout / 2;
            }

            $scope.notifications.push({
                type: type,
                title: title,
                message: message
            });

            $timeout(function () {
                $scope.notifications.shift();
            }, timeout);
        };

        $scope.init = function () {
            $scope.list();
        };
    });


