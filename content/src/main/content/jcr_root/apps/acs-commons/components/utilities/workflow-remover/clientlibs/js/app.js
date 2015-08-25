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

            $scope.getStatus = function () {
                $http({
                    method: 'GET',
                    url: encodeURI($scope.app.resource + '.status.json')
                }).
                    success(function (data, status, headers, config) {
                        var startedAtMoment, completedAtMoment;

                        $scope.status = data || {};
                        $scope.app.running = NotificationsService.running($scope.status.running || false);
                        
                        startedAtMoment = moment($scope.status.startedAt);

                        $scope.status.startedAt = startedAtMoment.format('MMMM Do YYYY, h:mm:ss a');

                        if ($scope.app.running) {
                            
                            $scope.status.timeTaken = moment().diff(startedAtMoment, 'seconds');

                            $scope.app.refresh = $timeout(function () {
                                $scope.getStatus();
                            }, 3000);
                            
                        } else if ($scope.status.status === 'complete') {

                            completedAtMoment = moment($scope.status.completedAt);

                            $scope.status.completedAt = completedAtMoment.format('MMMM Do YYYY, h:mm:ss a');
                            $scope.status.timeTaken = completedAtMoment.diff(startedAtMoment, 'seconds');
                            
                        } else {
                            
                            NotificationsService.add('error',
                                'ERROR', 'Invalid workflow removal status: ' + $scope.status.status);
                            
                        }

                     }).
                    error(function (data, status, headers, config) {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to retrieve status');
                    });

                document.getElementById("scroll-top").scrollTop = 0;
            };

            $scope.remove = function () {
                var payload = angular.copy($scope.form);
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
                            'INFO', 'Workflow removal completed');                        
                    }).
                    error(function (data, status, headers, config) {
                        $scope.app.running = NotificationsService.running(false);
                        NotificationsService.add('error',
                            'ERROR', 'Workflow removal failed due to: ' + data);
                    });

                $scope.getStatus();
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


