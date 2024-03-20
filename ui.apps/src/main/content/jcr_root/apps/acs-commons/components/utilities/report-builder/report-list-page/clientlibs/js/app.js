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
angular.module('acs-commons-report-list-page-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
    function ($scope, $http, $timeout, NotificationsService) {
    	
    	$scope.createReport = function (e,id) {
    		e.preventDefault();
			NotificationsService.running(true);
            var $form = $('#'+id);
            var name = $form.find('input[name="jcr:content/jcr:title"]').val().toLowerCase().replace(/\W+/g, "-");
            $.post($form.attr('action')+"/"+name, $form.serialize(), function() {
            	setTimeout(function(){
            		location.reload(true);
            	}, 500);
            });
            return false;
        };
    	
    	$scope.postValues = function (e,id) {
    		e.preventDefault();
			NotificationsService.running(true);
            var $form = $('#'+id);
            $.post($form.attr('action'), $form.serialize(), function() {
            	setTimeout(function(){
            		location.reload(true);
            	}, 500);
            });
            return false;
        };
    
        $scope.init = function () {
        	$('button[data-href]').click(function(){
        		window.location = $(this).data('href');
        	});
        };
    }]);


