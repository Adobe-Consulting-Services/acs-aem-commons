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

/*global JSON: false, angular: false */

angular.module('bulkWorkflowManagerApp',[])
    .controller('MainCtrl', ['$scope', '$http', '$timeout',
    function($scope, $http, $timeout) {

    $scope.dfault = {
        pollingInterval: 5
    };

    $scope.app = {
        uri: '',
        statusInterval: 5,
        polling: false
    };

    $scope.notifications = [];

    $scope.formOptions = {};

    $scope.form = {};

    $scope.data = {};

    $scope.start = function(isValid) {
        if(!isValid) {
            $scope.addNotification('error',
                "Invalid form parameters",
                "Form is incomplete or contains invalid parameters.");

            return;
        }

        $scope.items = {};

        $http({
            method: 'POST',
            url: $scope.app.uri + '.start.json',
            data: 'params=' + encodeURIComponent(JSON.stringify($scope.form)),
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
        success(function(data, status, headers, config) {
            $scope.data.status = data || {};
            $scope.status();
            $scope.form = {};

            $scope.notifications.shift();
        }).
        error(function(data, status, headers, config) {
            $scope.addNotification('error',
                data.title || "Error starting Bulk Workflow",
                data.message);

            $scope.notifications.shift();
        });

        $scope.addNotification('notice',
            "Starting...",
            "Collecting payloads for processing. Depending on the query and number of payload items this may take some time. Please be patient.");
    };

    $scope.stop = function() {
        $http({
            method: 'POST',
            url: $scope.app.uri + '.stop.json',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.data.status = data || {};

                $timeout.cancel($scope.app.pollingPromise);
            }).
            error(function(data, status, headers, config) {
                $scope.addNotification('error',
                    data.title || 'Error stopping the bulk workflow process.',
                    data.message);
            });
    };

    $scope.resume = function() {

        $http({
            method: 'POST',
            url: $scope.app.uri + '.resume.json',
            data: 'params=' + encodeURIComponent(JSON.stringify({ interval: $scope.form.interval })),
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.data.status = data || {};
                $scope.status();
            }).
            error(function(data, status, headers, config) {
                $scope.addNotification('error',
                    data.title || 'Error resuming bulk workflow process.',
                    data.message);
            });
    };

    $scope.status = function(forceStatus) {
        var timeout = ($scope.app.pollingInterval || $scope.dfault.pollingInterval ) * 1000;
        $scope.app.polling = true;

        $http({
            method: 'GET',
            url: $scope.app.uri + '.status.json',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.data.status = data || {};

                if(!forceStatus) {
                    if ($scope.data.status.state === 'running') {
                        $scope.app.pollingPromise = $timeout(function () {
                            $scope.status();
                        }, timeout);
                    } else {
                        $timeout.cancel($scope.app.pollingPromise);
                    }
                }

                $scope.app.polling = false;
            }).
            error(function(data, status, headers, config) {
                $scope.app.polling = false;
                $scope.addNotification('error',
                    'Could not retrieve bulk workflow status.', data.message);
            });
    };


    $scope.initForm = function() {
        $http({
            method: 'GET',
            url: $scope.app.uri + '.init-form.json',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.formOptions = data || {};
            }).
            error(function(data, status, headers, config) {
                $scope.addNotification('error',
                    data.title || 'Error retrieving form values from the server.',
                    data.message);
            });
    };

    $scope.init = function() {
        $scope.initForm();
        $scope.status();
    };

    $scope.updatePollingInterval = function(interval) {
        interval = parseInt(interval, 10);

        if(!angular.isNumber(interval) || interval < 1) {
            $scope.form.pollingInterval = $scope.dfault.pollingInterval;
            $scope.app.pollingInterval = $scope.form.pollingInterval;
        } else {
            $scope.form.pollingInterval = interval;
            $scope.app.pollingInterval = interval;
            $timeout.cancel($scope.app.pollingPromise);
            $scope.status();
        }
    };

    $scope.addNotification = function (type, title, message) {
        var timeout = 30000;

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
}]);
