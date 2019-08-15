/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2014 Adobe
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

/*global angular: false, ace: false qrCode: false*/

angular.module('acs-commons-instant-package-app', ['acsCoral', 'ACS.Commons.notifications']).controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService', function ($scope, $http, $timeout, NotificationsService) {

    $scope.app = {
        uri: ''
    };

    $scope.form = {
        enabled: false
    };

    $scope.init = function(appUri) {
        $scope.app.uri = appUri;

        $http({
            method: 'GET',
            url: $scope.app.uri + '/config/enabled'
        }).success(function (data) {
            $scope.form.enabled = data;
        }).error(function (data, status) {
            // Response code 404 will be when configs are not available
            if (status !== 404) {
                NotificationsService.add('error', "Error", "Something went wrong while fetching previous configurations");
            }
        });
    };

    $scope.saveConfig = function () {
        $http({
            method: 'POST',
            url: $scope.app.uri + '/config',
            data: 'enabled=' + $scope.form.enabled || 'false',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }).
        success(function (data, status, headers, config) {
            if ($scope.form.enabled) {
                $http({
                    method: 'POST',
                    url: $scope.app.uri,
                    data: './clientlib-authoring/categories@TypeHint=String[]&./clientlib-authoring/categories=cq.wcm.sites&./clientlib-authoring/categories=dam.gui.admin.coral',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    } 
                });
            } else {
                $http({
                    method: 'POST',
                    url: $scope.app.uri,
                    data: './clientlib-authoring/categories@Delete',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                });
            }

            $scope.app.running = false;
            NotificationsService.add('success', "Success", "Your configuration has been saved");
            NotificationsService.running($scope.app.running);
        }).
        error(function (data) {
            NotificationsService.add('error', 'ERROR', data.title + '. ' + data.message);
            $scope.app.running = false;
            NotificationsService.running($scope.app.running);

        });
    };
}]);
