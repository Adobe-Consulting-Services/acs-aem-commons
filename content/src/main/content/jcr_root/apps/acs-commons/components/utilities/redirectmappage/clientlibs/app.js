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
/*global angular: false */
angular.module('acs-commons-redirectmappage-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
    function ($scope, $http, $timeout, NotificationsService) {

        $scope.updateRedirectMap = function (e) {
        	e.preventDefault();
        	
			NotificationsService.running(true);

            var $form = $('#fn-acsCommons-update-redirect');

            $.ajax({
                url: $form.attr('action'),
                data: new FormData($form[0]),
                cache: false,
                contentType: 'multipart/form-data',
                processData: false,
                type: 'POST',
                success: function(data){
                	location.reload(true);
                }
            });
            return false;
        };
        
        $scope.postValues = function (e, id) {
        	e.preventDefault();
        	
			NotificationsService.running(true);

            var $form = $('#'+id);

            $.post($form.attr('action'), $form.serialize(), function() {
            	location.reload(true);
            });
            return false;
        };

        $scope.init = function () {
        	$('.endor-Crumbs-item[href=/miscadmin]').html('Reports').attr('href','/etc/acs-commons/reports.html');
        };
    }]);

