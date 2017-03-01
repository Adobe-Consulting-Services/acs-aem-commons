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

angular.module('PageCompare', ['acsCoral'])
.controller('MainCtrl', ['$scope', '$http', '$timeout', '$location',
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

        $scope.notifications = [];
        $scope.connections = [];
        $scope.changeStatus = [];

        $scope.addConnection = function(params) {
            $scope.connections.push(params);
        };

        $scope.addChangeStatus = function(params) {
            $scope.changeStatus.push(params);
        };

        $scope.$watch('connections', function(newValue, oldValue) {
            $scope.correctHeight();
        });

        $scope.correctHeight = function() {
            for (var i = 0; i < $scope.connections.length; i++) {
                var connection = $scope.connections[i];
                var source = $('#' + connection.source);
                var target = $('#' + connection.target);
                if (source.length && target.length) {
                    var dif = Math.floor(target.position().top) - Math.floor(source.position().top);
                    var insert = $('<div></div>');
                    insert.css({
                        'height': (Math.abs(dif)-1) + 'px',
                        'background-color': '#eee',
                        'border-top': '1px solid grey'
                    });
                    console.log(dif);
                    if (dif > 0) {
                        //source.parent().prepend(insert);
                    } else if (dif < 0) {
                        //target.parent().prepend(insert);
                    }
                    var sourceTxt = $.trim($('.coral-Popover', source).text());
                    var targetTxt = $.trim($('.coral-Popover', target).text());
                    var bg = {'background-color': '#FFCCCC'};
                    if (sourceTxt === targetTxt) {
                        bg = {'background-color': '#CBF6CB'};
                    }
                    source.css(bg);
                    target.css(bg);
                }
            }
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
    } ]);
