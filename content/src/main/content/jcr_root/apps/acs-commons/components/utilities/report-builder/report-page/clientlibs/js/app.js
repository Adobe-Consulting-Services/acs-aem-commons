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
angular.module('acs-commons-report-page-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
    function ($scope, $http, $timeout, NotificationsService) {
    	
    	var loadResults = function(params){
			var dfd = jQuery.Deferred();
    		var start = new Date().getTime();
			NotificationsService.running(true);
			$('input,select,coral-select').attr('disabled','disabled');
			$('.report__result').html('');
			$scope.result = {};
			
			$http({
				method: 'GET',
				url: $scope.app.uri+ '?wcmmode=disabled&'+ params
			}).success(function (data, status, headers, config) {
				window.location.hash = '#' + params;
				var time = new Date().getTime() - start;
				data.time=time;
				$('.report__result').html(data);
				$('.report__result .pagination__link').click(function(){
					$scope.run($(this).data('page'));
					return false;
				});
				$('.report__result a[data-href]').click(function(){
					window.open($(this).data('href'),'_blank');
					return false;
				});
				NotificationsService.running(false);
				NotificationsService.add('success', 'SUCCESS', 'Ran report in '+time+'ms!');
				$('input,select,coral-select').removeAttr('disabled');
				dfd.resolve();
			}).error(function (data, status, headers, config) {
				NotificationsService.running(false);
				NotificationsService.add('error', 'ERROR', 'Unable to run report due to error!');
				$('input,select,coral-select').removeAttr('disabled');
				dfd.resolve();
			});
			return dfd.promise();
    	};

        $scope.app = {
		};
        
        $scope.download = function(path){
        	var url = path + '?' + $('#report--form').serialize();
        	window.open(url,'_blank');
        };

        $scope.init = function () {
        	$(document).ready(function(){
        		
        		if(window.location.hash !== '' && window.location.hash !== '#'){
            		var params = window.location.hash.substr(1);
            		loadResults(params).done(function(){
            			var url = new URL("http://localhost:4502"+window.location.hash.replace('#','?'));
                		url.searchParams.forEach(function(val,key){
                			$('input[name="'+key+'"]:not([type="checkbox"])').val(val);
                			var $sel = $('coral-select[name="'+key+'"]');
                			if($sel.length > 0){
                				$sel.each(function(idx, select){
                					select.items.getAll().forEach(function(item, idx){
                						if(item.value === val){
                							item.selected = true;
                						}
                					});
                				});
                			}
                			if($('input[name="'+key+'"][type="checkbox"]').val() == val){
                				$('input[name="'+key+'"][type="checkbox"],coral-checkbox[name="'+key+'"]').attr('checked','checked');
                			}
                			$('textarea[name="'+key+'"]').html(val);
                		});
            		});
            	}

            	var tools = $('.endor-Crumbs-item')[1];
            	tools.innerText = 'Reports';
            	tools.href='/etc/acs-commons/reports.html';
        	});

        	
        };
        
        $scope.run = function(page) {
        	var params = $('#report--form').serialize();
        	if(page){
        		params += '&page=' + page;
        	}
			loadResults(params);
		};
	}
]);
