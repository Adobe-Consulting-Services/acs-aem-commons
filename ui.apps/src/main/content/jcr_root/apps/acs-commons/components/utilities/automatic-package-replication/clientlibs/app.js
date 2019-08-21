/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
angular.module('acs-commons-automatic-package-replication-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
    function ($scope, $http, $timeout, NotificationsService) {

        $scope.save = function () {
        	
			NotificationsService.running(true);
            

            var $form = $('#fn-acsCommons-APR-form');

            $.post($form.attr('action'), $form.serialize(), function() {
               location.reload(true);
            });
        };

        $scope.init = function () {
        	$('#trigger-select').change(function(){
            	$('#event-container').hide();
            	$('#cron-container').hide();
            	if($('#trigger-select').val() == 'event'){
            		$('#event-container').show();
            	}
            	if($('#trigger-select').val() == 'cron'){
            		$('#cron-container').show();
            	}
            });
        };
    }]);

