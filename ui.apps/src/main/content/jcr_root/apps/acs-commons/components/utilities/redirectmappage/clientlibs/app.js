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
        $scope.filteredEntries = [];
        $scope.invalidEntries = [];
        $scope.redirectMap = '';
        $scope.currentEntry = null;
        

        $scope.addEntry = function(){
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.entries = [];
            $scope.invalidEntries = [];
            $http({
                method: 'POST',
                url: $scope.app.uri+'.addentry.json?'+$('#entry-form').serialize()
            }).success(function (data, status, headers, config) {
                var time = new Date().getTime() - start;
                data.time=time;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                $scope.filterEntries();
                NotificationsService.running(false);
                NotificationsService.add('success', 'SUCCESS', 'Entry added!');
                $scope.loadRedirectMap();
            }).error(function (data, status, headers, config) {
                NotificationsService.running(false);
                NotificationsService.add('error', 'ERROR', 'Unable to add entry!');
            });
        };
        
        $scope.filterEntries = function(){
            $scope.filteredEntries = [];
            
            var term = $('#filter-form').find('input[name=filter]').val().toLowerCase();
            if(term.trim() !== ''){
                var count = 0;
                $scope.entries.forEach(function(el, idx){
                    var found = (term.trim() === '*');
                    Object.values(el).forEach(function(val, idx2){
                        if(val.toString().toLowerCase().indexOf(term) !== -1){
                            found = true;
                        }
                    });
                    if (found) {
                        $scope.filteredEntries.push(el);
                        count++;
                    }
                });
                
                NotificationsService.add('success', 'SUCCESS', 'Found '+count+' entries for '+$('#filter-form').find('input[name=filter]').val()+'!');
            }
            
            return false;
        };

        $scope.init = function () {
            $('.endor-Crumbs-item[href="/miscadmin"]').html('Redirects').attr('href','/miscadmin#/etc/acs-commons/redirect-maps');
            $scope.load();
        };
        
        $scope.load = function () {
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.filteredEntries = [];
            $scope.entries = [];
            $scope.invalidEntries = [];
            $http({
                method: 'GET',
                url: $scope.app.uri+'.redirectentries.json'
            }).success(function (data, status, headers, config) {
                var time = new Date().getTime() - start;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                NotificationsService.running(false);
                NotificationsService.add('success', 'SUCCESS', 'Found '+data.length+' entries in '+time+'ms!');
                $scope.loadRedirectMap();
                $scope.filterEntries();
            }).error(function (data, status, headers, config) {
                NotificationsService.running(false);
                NotificationsService.add('error', 'ERROR', 'Unable load redirect entries!');
            });
        };
        
        $scope.loadRedirectMap = function(){
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.redirectMap = '';
            $http({
                method: 'GET',
                url: $scope.app.uri+'.redirectmap.txt'
            }).success(function (data, status, headers, config) {
                var time = new Date().getTime() - start;
                $scope.redirectMap = data || '';
                NotificationsService.running(false);
                NotificationsService.add('success', 'SUCCESS', 'Loaded redirect map in '+time+'ms!');
            }).error(function (data, status, headers, config) {
                NotificationsService.running(false);
                NotificationsService.add('error', 'ERROR', 'Unable load redirect map!');
            });
        };

        $scope.openEditor = function(path){
            if(path.indexOf('/content/dam') === -1){
                window.open('/editor.html'+path+'.html','_blank');
            } else {
                window.open('/mnt/overlay/dam/gui/content/assets/metadataeditor.external.html?_charset_=utf-8&item='+path,'_blank');
            }
        };

        $scope.editItem = function(id){
            $scope.entries.forEach(function(el){
                if(el.id == id){
                    console.log('Editing entry: '+id);
                    document.querySelector('input[name=edit-source]').value = el.source;
                    document.querySelector('input[name=edit-target]').value = el.target;
                    document.querySelector('input[name=edit-id]').value = id;
                }
            });
            
            var dialog = document.querySelector('#edit-entry');
            dialog.show();
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
        
        $scope.removeAlert = function(id){
            $scope.currentEntry = id;

            var dialog = document.querySelector('#remove-confirm');
            dialog.show();
        };

        $scope.removeLine = function(){
            
            var idx = $scope.currentEntry;
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.entries = [];
            $scope.filteredEntries = [];
            $scope.invalidEntries = [];
            $http({
                method: 'POST',
                url: $scope.app.uri+'.removeentry.json?idx='+idx
            }).success(function (data, status, headers, config) {
                var time = new Date().getTime() - start;
                data.time=time;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                NotificationsService.running(false);
                NotificationsService.add('success', 'SUCCESS', 'Redirect map updated!');
                $scope.loadRedirectMap();
                $scope.filterEntries();
            }).error(function (data, status, headers, config) {
                NotificationsService.running(false);
                NotificationsService.add('error', 'ERROR', 'Unable remove entry '+idx+'!');
            });
        };
        
        $scope.saveLine = function() {
            var dialog = document.querySelector('#edit-entry');
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.entries = [];
            $scope.invalidEntries = [];
            $http({
                method: 'POST',
                url: $scope.app.uri+'.updateentry.json?'+$('#update-form').serialize()
            }).success(function (data, status, headers, config) {
                var time = new Date().getTime() - start;
                data.time=time;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                $scope.filterEntries();
                NotificationsService.running(false);
                NotificationsService.add('success', 'SUCCESS', 'Entry updated!');
                $scope.loadRedirectMap();
                dialog.hide();
            }).error(function (data, status, headers, config) {
                NotificationsService.running(false);
                NotificationsService.add('error', 'ERROR', 'Unable to update entry!');
                dialog.hide();
            });
        };

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

    }]);

