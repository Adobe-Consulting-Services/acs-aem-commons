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

var explainQueryApp = angular.module('oakIndexManager',[]);

explainQueryApp.controller('MainCtrl', function($scope, $http, $timeout) {

    $scope.app = {
        resource: '',
        running: false
    };

    $scope.notifications = [];

    $scope.oakIndex = {
        asyncDone: '',
        asyncStatus: '',
        indexes: []
    };

    $scope.$watch('toggleChecks', function(newValue, oldValue) {
        angular.forEach($scope.oakIndex.indexes, function(index, key) {
            if(index.show) {
                index.checked = newValue;
            }
        });
    });


    $scope.list = function() {
        $http({
            method: 'GET',
            url: encodeURI($scope.app.resource + '.list.json'),
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
        success(function(data, status, headers, config) {
            $scope.oakIndex.asyncDone = data['async-done'] || 'Unknown';
            $scope.oakIndex.asyncStatus = data['async-status'] || 'Unknown';

            $scope.oakIndex.indexes = [];
            angular.forEach(data, function(value, key) {
                if(value['jcr:primaryType'] === 'oak:QueryIndexDefinition') {
                    value.name = key;
                    value.show = true;
                    $scope.oakIndex.indexes.push(value);
                }
            });

        }).
        error(function(data, status, headers, config) {
            $scope.addNotification('error', 'ERROR', 'Unable to retrieve Oak indexes; Ensure you are running with elevated permissions and are on AEM6+');
        });
    };

    /**
     * Get and refresh the status of index nodes
     *
     * @param index
     */
    $scope.get = function(index) {

        $http({
            method: 'GET',
            url: encodeURI($scope.app.resource + '.get.json'),
            params: { name: index.name },
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                index = data;
                index.name = name;
            }).
            error(function(data, status, headers, config) {
                $scope.addNotification('error', 'ERROR', 'Unable to retrieve Oak index: ' + name);
            });
    };

    $scope.reindex = function(index) {
        $scope.app.running = true;
        $http({
            method: 'POST',
            url: encodeURI($scope.app.resource + '.reindex.json'),
            data: 'name=' + index.name,
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
        success(function(data, status, headers, config) {
            $scope.app.running = false;
            $scope.addNotification('info', 'INFO', 'Reindex successfully initiated for: ' + index.name);
            $scope.reindexStatus(index);
        }).
        error(function(data, status, headers, config) {
            $scope.app.running = false;
            $scope.addNotification('error', 'ERROR', 'Reindex request failed for: ' + index.name);
        });
    };

    $scope.bulkReindex = function() {
        var data = (function() {
            var params = [];

            angular.forEach($scope.selectedIndexes, function(index, key) {
                if(index.show && index.checked) {
                    params.push('name=' + encodeURIComponent(index.name));
                }
            });

            return params.join('&');
        }());

        if(!data) {
            $scope.addNotification('notice', 'NOTICE', 'Use checkboxes to select one or more indexes for bulk reindexing');
            return;
        }

        $scope.app.running = true;

        $http({
            method: 'POST',
            url: encodeURI($scope.app.resource + '.reindex.json'),
            data: data,
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
        success(function(data, status, headers, config) {
            $scope.app.running = false;

            if(data.success) {
                $scope.addNotification('info', 'INFO', 'Bulk reindex successfully initiated for: ' + data.success.join(', '));
            }
        }).
        error(function(data, status, headers, config) {

            $scope.app.running = false;
            if(data.success) {
                $scope.addNotification('info', 'INFO', 'Bulk reindex successfully initiated for: ' + data.success.join(', '));
            }

            if(data.error) {
                $scope.addNotification('error', 'ERROR', 'Bulk reindex could not be initiated for: ' + data.error.join(', '));
            }
        });
    };

    $scope.reindexStatus = function(index) {
        $scope.get(index);

        if(index.reindex === false) {
            $scope.addNotification('success', 'SUCCESS', 'Reindex completed for: ' + index.name);
        } else {
            $timeout($scope.reindexStatus(index), 2000);
        }
    };

    $scope.addNotification = function (type, title, message) {
        var timeout = 10000;

        if(type === 'success')  {
            timeout = timeout / 2;
        }

        $scope.notifications.push({
            type: type,
            title: title,
            message: message
        });

        $timeout(function() {
            $scope.notifications.shift();
        }, timeout);
    };


    /*
    * Init
    *
    */
    $scope.init = function() {
        $scope.list();
    };
});
