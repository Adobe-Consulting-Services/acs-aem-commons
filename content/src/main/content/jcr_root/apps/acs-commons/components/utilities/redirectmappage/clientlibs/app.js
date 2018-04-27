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
    	
    	$scope.app = {
			uri: ''
		};
    	
    	$scope.entries = [];

        $scope.updateRedirectMap = function (e) {
        	e.preventDefault();
        	
			NotificationsService.running(true);

            var $form = $('#fn-acsCommons-update-redirect');

            $.ajax({
                url: $form.attr('action'),
                data: new FormData($form[0]),
                cache: false,
                contentType: false,
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
        
        $scope.load = function () {
			var start = new Date().getTime();
			NotificationsService.running(true);
			$scope.entries = {};
			$http({
				method: 'GET',
				url: $scope.app.uri+'.redirectentries.json'
			}).success(function (data, status, headers, config) {

				var time = new Date().getTime() - start;
				data.time=time;
				$scope.entries = data || {};
				NotificationsService.running(false);
				NotificationsService.add('success', 'SUCCESS', 'Found '+data.length+' entries in '+time+'ms!');
			}).error(function (data, status, headers, config) {
				NotificationsService.running(false);
				NotificationsService.add('error', 'ERROR', 'Unable load redirect entries!');
			});
		};

        $scope.removeLine = function(idx){
            var start = new Date().getTime();
			NotificationsService.running(true);
			$scope.entries = {};
			$http({
				method: 'POST',
				url: $scope.app.uri+'.removeentry.json?idx='+idx
			}).success(function (data, status, headers, config) {
				var time = new Date().getTime() - start;
				data.time=time;
				$scope.entries = data || {};
				NotificationsService.running(false);
				NotificationsService.add('success', 'SUCCESS', 'Redirect map updated!');
			}).error(function (data, status, headers, config) {
				NotificationsService.running(false);
				NotificationsService.add('error', 'ERROR', 'Unable remove entry '+idx+'!');
			});
        };

        $scope.openEditor = function(path){
            if(path.indexOf('/content/dam') === -1){
                window.open('/editor.html'+path+'.html','_blank');
            } else {
                window.open('/mnt/overlay/dam/gui/content/assets/metadataeditor.external.html?_charset_=utf-8&item='+path,'_blank');
            }
        };
        
        $scope.filterEntries = function(){
            var found = 0;
        	var term = $('#filter-form').find('input[name=filter]').val().toLowerCase();
    		$('#entry-table tbody tr').each(function(idx,el){
    			if(term === ''){
    				$(el).show();
                    found = -1;
    			} else {
    				if($(el).text().toLowerCase().indexOf(term) != -1){
    					$(el).show();
                        found++;
    				} else {
    					$(el).hide();
    				}
    			}
    		});
            if(found > -1){
				NotificationsService.add('success', 'SUCCESS', 'Found '+found+' entries for '+$('#filter-form').find('input[name=filter]').val()+'!');
            } else {
                NotificationsService.add('success', 'SUCCESS', 'Filter reset!');
            }
    		return false;
        };

        $scope.addEntry = function(){
        	var start = new Date().getTime();
			NotificationsService.running(true);
			$scope.entries = {};
			$http({
				method: 'POST',
				url: $scope.app.uri+'.addentry.json?'+$('#entry-form').serialize()
			}).success(function (data, status, headers, config) {
				var time = new Date().getTime() - start;
				data.time=time;
				$scope.entries = data || {};
				NotificationsService.running(false);
				NotificationsService.add('success', 'SUCCESS', 'Entry added!');
			}).error(function (data, status, headers, config) {
				NotificationsService.running(false);
				NotificationsService.add('error', 'ERROR', 'Unable to add entry!');
			});
        };

        $scope.init = function () {
        	$('.endor-Crumbs-item[href="/miscadmin"]').html('Redirects').attr('href','/miscadmin#/etc/acs-commons/redirect-maps');
        	
        	$scope.load();

        };
    }]);

