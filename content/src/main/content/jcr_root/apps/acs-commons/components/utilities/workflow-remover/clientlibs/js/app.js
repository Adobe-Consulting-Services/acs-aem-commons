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

/*global angular: false, moment: false, JSON: false */

angular.module('acs-commons-workflow-remover-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
        function ($scope, $http, $timeout, NotificationsService) {

            $scope.app = {
                resource: '',
                running: false,
                refresh: ''
            };

            $scope.form = {
                payloads: [
                    { pattern: '' }
                ],
                models: [],
                statuses: [],
                batchSize: 1000
            };

            $scope.status = {};

            $scope.formOptions = {};

            /* Methods */

            $scope.init = function () {
                $http({
                    method: 'GET',
                    url: encodeURI($scope.app.resource + '.init.json')
                }).
                    success(function (data, status, headers, config) {
                        $scope.formOptions = data.form || {};
                    }).
                    error(function (data, status, headers, config) {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to initialize form');
                    });

                $scope.getStatus();
            };

            $scope.getStatus = function (forceRunning) {
                $http({
                    method: 'GET',
                    url: encodeURI($scope.app.resource + '.status.json')
                }).
                    success(function (data, status, headers, config) {

                        $scope.status = data || { running: false };

                        if(forceRunning) {
                            $scope.app.running = NotificationsService.running(forceRunning);
                        } else {
                            $scope.app.running =
                                NotificationsService.running(($scope.status && $scope.status.running) || false);
                        }

                        if ($scope.app.running) {
                            $scope.app.refresh = $timeout(function () {
                                $scope.getStatus();
                            }, 2000);

                        } else if ($scope.status.erredAt) {

                            NotificationsService.add('error',
                                'ERROR', 'Workflow removal resulted in an error. Please check the AEM logs.');

                        }
                     }).
                    error(function (data, status, headers, config) {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to retrieve status');
                    });

                document.getElementById("scroll-top").scrollTop = 0;
            };

            $scope.remove = function () {
                var payload;

                if ($scope.app.running) {
                    return;
                }

                payload = angular.copy($scope.form);
                $scope.app.running = NotificationsService.running(true);

                if (payload.olderThan) {
                    payload.olderThan = moment(payload.olderThan, "YYYY-MM-DD HH:MM").valueOf();
                }

                $http({
                    method: 'POST',
                    url: encodeURI($scope.app.resource + '.remove.json'),
                    data: 'params=' + JSON.stringify(payload),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).
                    success(function (data, status, headers, config) {
                        $scope.getStatus();
                        $scope.app.running = NotificationsService.running(false);
                        NotificationsService.add('info',
                            'INFO', 'Workflow removal complete');
                    }).
                    error(function (data, status, headers, config) {
                        if (status === 599) {
                            $scope.app.running = NotificationsService.running(false);
                        } else {
                            $scope.getStatus();
                            $scope.app.running = NotificationsService.running(false);
                            NotificationsService.add('error',
                                'ERROR', 'Workflow removal failed due to: ' + data);
                        }
                    });

                $scope.getStatus(true);
            };


            $scope.forceQuit = function () {
                $http({
                    method: 'POST',
                    url: encodeURI($scope.app.resource + '.force-quit.json'),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).
                    success(function (data, status, headers, config) {
                        $scope.app.running = NotificationsService.running(false);

                        $scope.status = data || { running: false };

                        NotificationsService.add('info',
                            'INFO', 'Workflow removal has been force quit');
                    }).
                    error(function (data, status, headers, config) {
                        $scope.getStatus();
                        NotificationsService.add('error',
                            'ERROR', 'Workflow removal failed to force quit: ' + data);
                    });
            };


            /* Form Methods */
            $scope.toggleStatusSelection = function (status) {
                var idx = $scope.form.statuses.indexOf(status);

                if (idx > -1) {
                    $scope.form.statuses.splice(idx, 1);
                } else {
                    $scope.form.statuses.push(status);
                }
            };

            $scope.toggleModelSelection = function (workflowModelID) {
                var idx = $scope.form.models.indexOf(workflowModelID);

                if (idx > -1) {
                    $scope.form.models.splice(idx, 1);
                } else {
                    $scope.form.models.push(workflowModelID);
                }
            };
        }]);


