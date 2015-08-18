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
angular.module('acs-commons-oak-index-manager-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
    function ($scope, $http, $timeout, NotificationsService) {

        $scope.app = {
            resource: ''
        };

        $scope.indexes = [];
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

                    $scope.async.status = data['async-status'] || '';
                    $scope.async.start = data['async-start'] || '';
                    $scope.async.reindexDone = data['async-reindex-done'] || '';
                    $scope.async.reindexStatus = data['async-reindex-status'] || '';

                    $scope.indexes = [];

                    angular.forEach(data, function (index, key) {
                        if (index['jcr:primaryType'] === 'oak:QueryIndexDefinition') {
                            $scope.put(key, index);
                        }
                    });

                }).
                error(function (data, status, headers, config) {
                    NotificationsService.add('error', 'ERROR', 'Unable to retrieve Oak indexes; Ensure you are running with elevated permissions and are on AEM6+');
                });
        };

        $scope.get = function (index) {
            var reindexing = index.reindex;
            $http({
                method: 'GET',
                url: encodeURI($scope.app.resource + '.get.json'),
                params: { name: index.name },
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).
                success(function (data, status, headers, config) {
                    $scope.put(index.name, data);

                    if(reindexing && !data.reindex) {
                        $timeout.cancel(index.timeout);
                        NotificationsService.add('success', 'SUCCESS', 'Reindex completed for: ' + index.name);
                    }
                }).
                error(function (data, status, headers, config) {
                    $scope.addNotification('error', 'ERROR', 'Unable to retrieve Oak index: ' + index.name);
                });
        };

        $scope.reindex = function (index) {
            index.reindex = true;

            $http({
                method: 'POST',
                url: encodeURI($scope.app.resource + '.reindex.json'),
                data: 'name=' + index.name,
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).
                success(function (data, status, headers, config) {
                    NotificationsService.add('info', 'INFO', 'Reindex requested for: ' + index.name);
                }).
                error(function (data, status, headers, config) {
                    index.reindex = false;
                    NotificationsService.add('error', 'ERROR', 'Reindex request failed for: ' + index.name);
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
                NotificationsService.add('help', 'HELP', 'Select one or more checkboxes in the index table to bulk reindex');
                return;
            }

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
                }).
                error(function (data, status, headers, config) {
                    var i;
                    for(i in bulkIndexes) {
                        $scope.refresh(bulkIndexes[i]);
                    }

                    if (data.success) {
                        NotificationsService.add('info', 'INFO', 'Bulk reindex request succeeded for: ' + data.success.join(', '));
                    }

                    if (data.error) {
                        NotificationsService.add('error', 'ERROR', 'Bulk reindex request failed for: ' + data.error.join(', '));
                    }
                });

        };


        $scope.put = function(name, data) {
            var i,
                checked,
                timeout;

            data.name = name;
            if(data.reindex) {
                data.reindexKeyword = 'reindexing';
            }

            for(i = 0; i < $scope.indexes.length; i++) {
                if($scope.indexes[i].name === data.name) {
                    checked = $scope.indexes[i].checked;
                    timeout = $scope.indexes[i].timeout;

                    $scope.indexes[i] = data;
                    $scope.indexes[i].checked = checked;
                    $scope.indexes[i].timeout = timeout;

                    return;
                }
            }

            // Could not find anything to update so add
            $scope.indexes.push(data);
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
                    NotificationsService.add('info', 'INFO', 'Reindex check timed out for : ' + index.name);
                    $timeout.cancel(index.timeout);
                }
            }
        };

        $scope.init = function () {
            $scope.list();
        };
    }]);


