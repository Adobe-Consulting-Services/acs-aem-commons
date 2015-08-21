/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

/*global angular: false, moment: false, JSON: false */

angular.module('acs-commons-jcr-checksum-compare-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
        function ($scope, $http, $timeout, NotificationsService) {

            $scope.app = {
                servlet: '/bin/yada.json',
                running: false
            };

            $scope.form = {
                targets: {
                    self: 'self',
                    external: []
                }
            };

            
            /* Methods */

            $scope.compare = function () {
                $scope.diffSelf();
                $scope.diffRemote();
            };

            $scope.diffSelf  = function () {

                $http({
                    method: 'GET',
                    url: encodeURI($scope.app.servlet + '?path=' + $scope.form.path)
                }).
                    success(function (data, status, headers, config) {
                        $scope.results[0] = data || {};
                    }).
                    error(function (data, status, headers, config) {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to collect JCR diff data from ' + $scope.app.servlet);
                    });
            };

            
            $scope.diffRemote  = function () {

                // Get external hosts
                angular.forEach($scope.form.targets.remote, function (value, index) {
                    $http({
                        method: 'GET',
                        url: encodeURI(value + $scope.app.servlet + '?path=' + $scope.form.path)
                    }).
                        success(function (data, status, headers, config) {
                            $scope.results[index + 1] = data || {};
                        }).
                        error(function (data, status, headers, config) {
                            NotificationsService.add('error',
                                'ERROR', 'Unable to collect JCR diff data from ' + value);
                        });
                });
            };

    }]);


