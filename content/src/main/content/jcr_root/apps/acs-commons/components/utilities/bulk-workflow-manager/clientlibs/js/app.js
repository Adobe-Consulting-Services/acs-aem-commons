/*
 * #%L
 * ACS AEM Tools Package
 * %%
 * Copyright (C) 2014 Adobe
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

/*global JSON: false, angular: false */

angular.module('bulkWorkflowManagerApp',[]).controller('MainCtrl', function($scope, $http, $timeout) {

    $scope.app = {
        uri: '',
        statusInterval: 5
    };

    $scope.notifications = [];

    $scope.form = {};

    $scope.data = {};


    $scope.$watch('app.statusInterval', function(newValue, oldValue) {
        if(!angular.isNumber(newValue) || newValue <= 0) {
            $scope.app.statusInterval = 10;
        } else {
            $timeout.cancel($scope.app.statusPromise);
            $scope.status();
        }
    });

    $scope.start = function() {
        $scope.results = {};

        $http({
            method: 'POST',
            url: $scope.app.uri + '.start.json',
            data: 'params=' + encodeURIComponent(JSON.stringify($scope.form)),
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
        success(function(data, status, headers, config) {
            $scope.data.status = data || {};
            $scope.status();
        }).
        error(function(data, status, headers, config) {
            $scope.addNotification('error', 'ERROR', 'Check your params and your error logs and try again.');
        });
    };

    $scope.stop = function() {
        $http({
            method: 'POST',
            url: $scope.app.uri + '.stop.json',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.data.status = data || {};
                $timeout.cancel($scope.app.statusPromise);
            }).
            error(function(data, status, headers, config) {
                $scope.addNotification('error', 'ERROR', 'Check your params and your error logs and try again.');
            });
    };

    $scope.resume = function() {

        $http({
            method: 'POST',
            url: $scope.app.uri + '.resume.json',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.data.status = data || {};
                $scope.status();
            }).
            error(function(data, status, headers, config) {
                $scope.addNotification('error', 'ERROR', 'Check your params and your error logs and try again.');
            });
    };

    $scope.status = function() {

        $http({
            method: 'GET',
            url: $scope.app.uri + '.status.json',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.data.status = data || {};

                if ($scope.data.status.state === 'running') {

                    $scope.data.status.percentComplete =
                        Math.round(($scope.data.status.complete / $scope.data.status.total) * 100);

                    $scope.app.statusPromise = $timeout(function () {
                        $scope.status();
                    }, $scope.app.statusInterval * 1000);
                } else {
                    $timeout.cancel($scope.app.statusPromise);
                }
            }).
            error(function(data, status, headers, config) {
                $scope.addNotification('error', 'ERROR', 'Check your params and your error logs and try again.');
            });
    };

    $scope.init = function() {
        $scope.status();
    };


    $scope.addNotification = function (type, title, message) {
        var timeout = 10000;

        if(type === 'success')  {
            timeout = timeout / 2;
        }

        $scope.notifications.unshift({
            type: type,
            title: title,
            message: message
        });

        $timeout(function() {
            $scope.notifications.shift();
        }, timeout);
    };
});
