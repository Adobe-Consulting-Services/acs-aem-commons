/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

/*global JSON: false, angular: false, moment: false, window: false */

angular.module('acs-commons-bulk-workflow-manager-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
        function ($scope, $http, $timeout, NotificationsService) {

            $scope.dfault = {
                pollingInterval: 5
            };

            $scope.app = {
                uri: '',
                statusInterval: 5,
                polling: false
            };

            $scope.formOptions = {};

            $scope.form = {};

            $scope.data = {};

            $scope.calc = {};


            $scope.$watch('form.batchSize', function (newValues, oldValues) {
                $scope._calculateInterval();
            });

            $scope.$watch('calc.queueWidth', function (newValues, oldValues) {
                $scope._calculateInterval();
            });

            $scope.$watch('calc.avgTime', function (newValues, oldValues) {
                $scope._calculateInterval();
            });

            $scope._calculateInterval = function () {
                $scope.form.interval = Math.round(($scope.form.batchSize / ($scope.calc.queueWidth || 1)) * ($scope.calc.avgTime || 2));
            };


            $scope.start = function (isValid) {
                if (!isValid) {
                    NotificationsService.add('error',
                        "Invalid form parameters",
                        "Form is incomplete or contains invalid parameters.");

                    return;
                }

                $scope.items = {};

                $scope.form.workflowModelId = $scope.form.workflowModel.value;

                $http({
                    method: 'POST',
                    url: $scope.app.uri + '.start.json',
                    data: 'params=' + encodeURIComponent(JSON.stringify($scope.form)),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).success(function (data, status, headers, config) {
                    $scope.data.status = data || {};
                    $scope.status();
                    $scope.form = {};

                    NotificationsService.shift();
                }).error(function (data, status, headers, config) {
                    NotificationsService.shift();

                    NotificationsService.add('error',
                        data.title || "Error starting Bulk Workflow",
                        data.message);
                });

                NotificationsService.add('notice',
                    "Starting...",
                    "Collecting payloads for processing. Depending on the query and number of payload items this may take some time. Please be patient.");

                window.scrollTo(0, 0);
            };

            $scope.stop = function () {
                $http({
                    method: 'POST',
                    url: $scope.app.uri + '.stop.json',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).success(function (data, status, headers, config) {
                    $scope.data.status = data || {};

                    if ($scope.data.status.status === 'STOPPED') {
                        $timeout.cancel($scope.app.pollingPromise);
                    }
                }).error(function (data, status, headers, config) {
                    NotificationsService.add('error',
                        data.title || 'Error stopping the bulk workflow process.',
                        data.message);
                });
            };

            $scope.resume = function () {
                $http({
                    method: 'POST',
                    url: $scope.app.uri + '.resume.json',
                    data: 'params=' + encodeURIComponent(JSON.stringify({interval: $scope.form.interval})),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).success(function (data, status, headers, config) {
                    $scope.data.status = data || {};
                    $scope.status();
                }).error(function (data, status, headers, config) {
                    NotificationsService.add('error',
                        data.title || 'Error resuming bulk workflow process.',
                        data.message);
                });
            };

            $scope.status = function (forceStatus) {
                var timeout = ($scope.app.pollingInterval || $scope.dfault.pollingInterval) * 1000;
                $scope.app.polling = true;

                $http({
                    method: 'GET',
                    url: $scope.app.uri + '.status.json',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).success(function (data, status, headers, config) {
                    $scope.data.status = data || {};
                    if (!forceStatus) {
                        if ($scope.data.status.status === 'RUNNING') {
                            $scope.app.pollingPromise = $timeout(function () {
                                $scope.status();
                            }, timeout);
                        } else {
                            $timeout.cancel($scope.app.pollingPromise);
                        }
                    }

                    $scope.app.polling = false;
                }).error(function (data, status, headers, config) {
                    $scope.app.polling = false;
                    NotificationsService.add('error',
                        'Could not retrieve bulk workflow status.', data.message);
                });
            };


            $scope.initForm = function () {
                $http({
                    method: 'GET',
                    url: $scope.app.uri + '.init-form.json',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).success(function (data, status, headers, config) {
                    $scope.formOptions = data || {};
                    $scope.form.selectUserEventData = data.userEventData[0];
                }).error(function (data, status, headers, config) {
                    NotificationsService.add('error',
                        data.title || 'Error retrieving form values from the server.',
                        data.message);
                });
            };

            $scope.init = function () {
                moment.locale('en');

                $scope.initForm();
                $scope.status();
            };

            $scope.updatePollingInterval = function (interval) {
                interval = parseInt(interval, 10);

                if (!angular.isNumber(interval) || interval < 1) {
                    $scope.form.pollingInterval = $scope.dfault.pollingInterval;
                    $scope.app.pollingInterval = $scope.form.pollingInterval;
                } else {
                    $scope.form.pollingInterval = interval;
                    $scope.app.pollingInterval = interval;
                    $timeout.cancel($scope.app.pollingPromise);
                    $scope.status();
                }
            };

            $scope.showForm = function () {
                if ($scope.data && $scope.data.status) {
                    return $scope.data.status.status === 'NOT_STARTED';
                }

                return true;
            };

            $scope.isSynthetic = function () {
                return $scope.runnerCheck('com.adobe.acs.commons.workflow.bulk.execution.impl.runners.SyntheticWorkflowRunnerImpl');
            };

            $scope.isFAM = function () {
                return $scope.runnerCheck('com.adobe.acs.commons.workflow.bulk.execution.impl.runners.FastActionManagerRunnerImpl');
            };

            $scope.isWorkflow = function () {
                if ($scope.showForm()) {
                    return $scope.runnerCheck('com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMWorkflowRunnerImpl') &&
                        (!$scope.form.workflowModel || !$scope.form.workflowModel.transient);
                } else {
                    return $scope.runnerCheck('com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMWorkflowRunnerImpl');
                }
            };

            $scope.isTransientWorkflow = function () {
                if ($scope.showForm()) {
                    return $scope.runnerCheck('com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMWorkflowRunnerImpl') &&
                        $scope.form.workflowModel &&
                        $scope.form.workflowModel.transient;
                } else {
                    return $scope.runnerCheck('com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMTransientWorkflowRunnerImpl');
                }
            };

            $scope.runnerCheck = function(runnerName) {
                if ($scope.showForm()) {
                    return runnerName === $scope.form.runnerType;
                } else {
                    return runnerName === $scope.data.status.runnerType;
                }
            };

            $scope.timeTaken = function() {
                var duration;

                if (!$scope.data.status || typeof $scope.data.status.timeTakenInMillis === 'undefined') {
                    return '?';
                }

                duration = moment.duration($scope.data.status.timeTakenInMillis);
                return duration.humanize();
            };

            $scope.projectedTimeRemaining = function() {
                var total,
                    left;

                if (!$scope.data.status || typeof $scope.data.status.timeTakenInMillis === 'undefined') {
                    return '?';
                }

                total = $scope.data.status.timeTakenInMillis / ($scope.data.status.percentComplete / 100);
                left = total - $scope.data.status.timeTakenInMillis || 0;

                estimated = moment.duration(left);
                return estimated.humanize();
            };
        }]);
