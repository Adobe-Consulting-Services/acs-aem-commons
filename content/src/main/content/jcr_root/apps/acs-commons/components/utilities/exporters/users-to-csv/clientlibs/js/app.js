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

angular.module('acs-commons-users-to-csv-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
        function ($scope, $http, $timeout, NotificationsService) {

            $scope.app = {};

            $scope.form = {
                groupFilter: ''
            };

            $scope.options = {
                groups: [],
                groupFilter: ''
            };

            $scope.init = function (resourcePath) {
                $scope.app.resource = resourcePath;

                $http({
                    method: 'GET',
                    url: encodeURI($scope.app.resource + '.init.json')
                }).success(function (data, status, headers, config) {
                    $scope.options = data.options;
                    $scope.form = data.form;
                }).error(function (data, status, headers, config) {
                    NotificationsService.add('error',
                        'ERROR', 'Could not save configuration: ' + data);
                });
            };

            $scope.save = function () {
                $http({
                    method: 'POST',
                    url: encodeURI($scope.app.resource + '.save.json'),
                    data: 'params=' + JSON.stringify($scope.form),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).success(function (data, status, headers, config) {
                    NotificationsService.add('success',
                        'SUCCESS', 'Configuration saved');
                }).error(function (data, status, headers, config) {
                    NotificationsService.add('error',
                        'ERROR', 'Could not save configuration: ' + data);
                });
            };

            $scope.download = function () {
                window.open($scope.app.resource + '/users.export.csv?params=' + JSON.stringify($scope.form));
            };

            $scope.toggle = function (arr, value) {
                if (arr.indexOf(value) < 0) {
                    arr.push(value);
                } else {
                    arr.splice(arr.indexOf(value), 1);
                }
            };

            $scope.getFromIndex = function(column) {
                if (column === 1) {
                    return 0;
                } else {
                    return ((column - 1) * Math.ceil($scope.options.groups.length / 3));
                }
            };

            $scope.getToIndex = function(column) {
                if (column === 1) {
                    return Math.ceil($scope.options.groups.length / 3);
                } else {
                    if ($scope.getFromIndex(column) + Math.ceil($scope.options.groups.length / 3) > $scope.options.groups.length) {
                        return $scope.options.groups.length;
                    } else {
                        return $scope.getFromIndex(column) + Math.ceil($scope.options.groups.length / 3);
                    }
                }
            };
        }]);