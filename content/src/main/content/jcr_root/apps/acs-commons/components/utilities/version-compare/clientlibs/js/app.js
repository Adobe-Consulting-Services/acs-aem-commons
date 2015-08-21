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

angular.module('versionComparator', ['acsCoral'])
.controller('MainCtrl', ['$scope', '$http', '$timeout', '$location',
    function($scope, $http, $timeout, $location) {

        $scope.app = {
            home : '',
            resource : '',
            paintConnections : false,
            hideVersions : {},
            hideUnchanged : false
        };

        $scope.notifications = [];
        $scope.connections = [];
        $scope.changeStatus = [];

        $scope.$watch('app.paintConnections', function(newValue,
                oldValue) {
            $scope.paintConnections(newValue);
        });

        $scope.addNotification = function(type, title, message) {
            var timeout = 10000;

            if (type === 'success') {
                timeout = timeout / 2;
            }

            $scope.notifications.push({
                type : type,
                title : title,
                message : message
            });

            $timeout(function() {
                $scope.notifications.shift();
            }, timeout);
        };

        $scope.addConnection = function(params) {
            $scope.connections.push(params);
        };

        $scope.addChangeStatus = function(params) {
            $scope.changeStatus.push(params);
        };

        $scope.paintConnections = function(doPrint) {
            var i;
            if (doPrint) {
                for (i = 0; i < $scope.connections.length; i++) {
                    jsPlumb.connect({
                        source : $scope.connections[i].source,
                        target : $scope.connections[i].target,
                        anchors : [ "Right", "Left" ],
                        paintStyle : {
                            lineWidth : 1,
                            strokeStyle : 'grey'
                        },
                        hoverPaintStyle : {
                            strokeStyle : "rgb(0, 0, 135)"
                        },
                        endpointStyle : {
                            width : 1,
                            height : 1
                        },
                        endpoint : "Rectangle",
                        connector : "Straight"
                    });
                }
            } else {
                // $timeout(jsPlumb.reset, 100);
                $('*[class^="_jsPlumb"]').remove();
            }
            jsPlumb.repaintEverything();
        };

        $scope.showVersion = function(version) {
            var property, value;
            for (property in $scope.app.hideVersions) {
                if ($scope.app.hideVersions
                        .hasOwnProperty(property)) {
                    value = $scope.app.hideVersions[property];
                    if (version === property && value === true) {
                        jsPlumb.repaintEverything();
                        return false;
                    }
                }
            }
            return true;
        };

        $scope.analyse = function() {
            window.location = $scope.app.home + "?path=" + $scope.app.resource;
        };

        $scope.init = function() {
        };
    } ]);
