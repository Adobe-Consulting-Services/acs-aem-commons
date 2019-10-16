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
            rootPaths: [
                { path: '' }
            ],
            datetimecal: '',
            agents: [
                { title: '' },
                { logHref: '' },
                { agentHref: '' }
            ]
        };

        $scope.results = {};
        $scope.agentsInfo = {};

        $scope.replicate = function () {
            var payload;
            if ($scope.app.running) {
                return;
            }
            payload = angular.copy($scope.form);
            console.log($('#versionReplicator').serialize());
            $scope.app.running = NotificationsService.running(true);

            $http({
                method: 'POST',
                url: $scope.app.uri,
                data: $scope.payload,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
                transformRequest: angular.identity
            }).success(function (data, status, headers, config) {
                $scope.results = data.result;
                console.log("$scope.results -> "+ JSON.stringify($scope.results));
                $scope.app.running = NotificationsService.running(false);
            }).error ;

        };

        $scope.toggleModelSelection = function (agentId) {
            var idx = $scope.form.agents.indexOf(agentId);
            if (idx > -1) {
                $scope.form.agents.splice(idx, 1);
            } else {
                $scope.form.agents.push(agentId);
            }
        };

        $scope.serializeMultiValues = function(elements) {
            var array = [];
            angular.forEach(elements, (function(element) {
                console.log($(element).val());
                //if(angular.isUndefined($(element).val())){
                if(typeof $(element).val() !== 'undefined' || !$(element).val()){
                    array.push($(element).val());
                }
            }));
            return array;
        };

        $scope.showReplicationMsg = function(msg) {
            console.log("showReplicationMsg -> " + JSON.stringify(msg.result));
            $scope.results = msg.result;

        };

        $scope.showReplicationAgentsInfo = function(agents) {
            console.log("showReplicationAgentsInfo" + JSON.stringify(agents));
            var $el = $('#replication-agents-info');
            angular.forEach(agents, (function(agent) {
                console.log($(agent).val());
          }));
        };
        $scope.showErrorMsg = function(msg) {
            var $el = $('#error-message');
            console.log("showErrorMsg ->" +JSON.stringify(msg));
            $scope.hideAll();
            if(typeof msg === undefined || !msg) {
                msg = "A system error occurred.";
            }
            // convert JSON the HTML format and add to the div
            $el.find('.message').html(msg);
            $el.show();
        };
        $scope.hideAll = function() {
            $('#error-message').hide();
            $('#results').hide();
            $('#buttons').hide();
        };

        // Show agent list in result notifications/
        // Set a 1 second timeout to allow invalid input params to be caught and return.
        $scope.requestSuccessTimeout = function() {
            setTimeout(function() {
               //$scope.showReplicationAgentsInfo($scope.buildAgentInfoHTML(agentInfo));
            }, 2000);
        };
        $scope.requestFailTimeout = function(agentInfo) {
            setTimeout(function() {
               $scope.showReplicationAgentsInfo($scope.buildAgentInfoHTML(agentInfo));
            }, 1000);
        };

        $scope.buildAgentInfoHTML = function(agentInfo) {
            // convert JSON the HTML format and add to the div
            console.log(JSON.stringify(agentInfo));
        };

    }]);



