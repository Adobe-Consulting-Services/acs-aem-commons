/*
 * #%L
 * ACS AEM Commons Package
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
angular.module('acs-commons-version-replicator-app', ['acsCoral', 'ACS.Commons.notifications'])
	.controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
		function ($scope, $http, $timeout, NotificationsService) {
        $scope.app = {
            uri: ''
        };
        $scope.form = {
            rootPaths: [],
            datetimecal: '',
            agents: []
        };
        $scope.results = {};
        $scope.agentsInfo = [];
        $scope.replicate = function () {
            if ($scope.app.running) {
                return;
            }
            $scope.hideAll();
            $http({
                method: 'POST',
                url: $scope.app.uri + "?" + $('#versionReplicator').serialize(),
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                },
                transformRequest: angular.identity
            }).success(function (data, status, headers, config) {
                $scope.app.running = false;
                $scope.app.error = false;
                $scope.app.results = true;
                $scope.results = data.result;
                $('#results').show();
                $scope.buildAgentInfoHTML($('#versionReplicator').find('input[name="cmbAgent"]:checked'));
            }).error(function (data, status, headers, config) {
                $scope.app.running = false;
                $scope.app.results = false;
                $scope.app.error = true;
                $scope.showErrorMsg(data);
            });
        };
        $scope.toggleModelSelection = function (agentId) {
            var idx = $scope.form.agents.indexOf(agentId);
            if (idx > -1) {
                $scope.form.agents.splice(idx, 1);
            } else {
                $scope.form.agents.push(agentId);
            }
        };
        $scope.showErrorMsg = function (msg) {
            var $errorMessageDiv = $('#error-message');
            if (typeof msg === undefined || !msg) {
                msg = "A system error occurred.";
            }
            $errorMessageDiv.find('.message').html(msg.error);
            $errorMessageDiv.show();
        };
        $scope.hideAll = function () {
            console.log("Should hide all divs");
            $('#error-message').hide();
            $('#results').hide();
        };

        $scope.buildAgentInfoHTML = function (agentInputArray) {
            var agent, title, logHref, agentHref;
            agentInputArray.each(function() {
                $scope.agentsInfo.push(
                    {
                        title: $(this).attr('data-agent-name'),
                        logHref: $.trim($(this).attr('data-agent-path') + '.log.html#end'),
                        agentHref: $.trim($(this).attr('data-agent-path') + '.html')
                    }
                );
            });
        };
        $scope.isEmpty = function(value) {
            if( typeof value !== 'undefined' && value !== '') {
                // foo could get resolved and it's defined
                return value;
            }
        };
	}]);