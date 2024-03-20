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
/*global angular: false */

var pageCompareApp = angular.module('PageCompare', ['acsCoral']);

pageCompareApp.controller('MainCtrl', ['$scope', '$http', '$timeout', '$location',
    function($scope, $http, $timeout, $location) {

        $scope.app = {
            dirty: false,
            home : '',
            resource : '',
            resourceB : '',
            a: '',
            b: '',
            hideVersions : {},
            hideUnchanged : false
        };

        $scope.app.compareDifferentResources = function() {
            return $scope.app.resourceB !== '' && $scope.app.resourceB !== $scope.app.resource;
        };

        $scope.dirty = function() {
            $scope.app.dirty = true;
        };

        $scope.blur = function() {
            if ($scope.app.dirty) {
                $scope.analyse();
            }
        };

        $scope.analyse = function() {
            var url = $scope.app.home;
            if (window.location.pathname.substring(0, 4) === '/cqa') {
                url = '/cqa' + url;
            }
            url += "?path=" + $scope.app.resource;
            url += "&a=" + $scope.app.a;
            url += "&b=" + $scope.app.b;
            if ($scope.app.resourceB !== '') {
                url += "&pathB=" + $scope.app.resourceB;
            }
            window.location = url;
        };

        $scope.init = function() {
        };
    } 
]);
