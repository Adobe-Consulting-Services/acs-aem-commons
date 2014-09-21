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

/*global angular: false */

angular.module('workflowRemover', [])
    .controller('MainCtrl', ['$scope', '$http', '$timeout',
    function ($scope, $http, $timeout) {

        $scope.app = {
            resource: '',
            running: false
        };

        $scope.form = {
            payloads: [{ pattern: '' }],
            models: [],
            statuses: []
        };

        $scope.formOptions = {};

        $scope.status = function () {
            $http({
                method: 'GET',
                url: encodeURI($scope.app.resource + '.json')
            }).
                success(function (data, status, headers, config) {
                    $scope.status = data || {};
                }).
                error(function (data, status, headers, config) {
                    $scope.addNotification('error', 'ERROR', 'Unable to retrieve Status');
                });
        };



        $scope.remove = function () {
            $scope.app.running = true;

            $http({
                method: 'POST',
                url: encodeURI($scope.app.resource + '.remove.json'),
                data: 'params=',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).
                success(function (data, status, headers, config) {
                    $scope.app.running = false;
                    $scope.addNotification('info', 'INFO', '');
                }).
                error(function (data, status, headers, config) {
                    $scope.app.running = false;
                    $scope.addNotification('error', 'ERROR', '');
                });
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
            $http({
                method: 'GET',
                url: encodeURI($scope.app.resource + '.init.json')
            }).
                success(function (data, status, headers, config) {
                    $scope.formOptions = data || {};
                }).
                error(function (data, status, headers, config) {
                    $scope.addNotification('error', 'ERROR', 'Unable to initialize form');
                });
        };
    }]);


