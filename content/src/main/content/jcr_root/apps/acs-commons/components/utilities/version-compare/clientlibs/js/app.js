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
        $scope.nodes = [];
        $scope.nodesMap = {};
        $scope.versionsMap = {};
        $scope.versionsCount = 0;
        $scope.changeStatus = [];

        $scope.$watch('app.paintConnections', function(newValue,
                oldValue) {
            $scope.refreshConnections(newValue);
        });

        $scope.$watch('app.hideUnchanged', function(newValue,
                oldValue) {
            if ($scope.app.paintConnections) {
                removeConnections();
                paintConnections();
            }
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

        $scope.addNode = function(node) {
            node['getTargetId'] = function(indexShift) {
                return this.name + "-" + (this.version + indexShift);
            };

            $scope.nodes.push(node);
            $scope.nodesMap[node.id] = node;

            var versionIndex = node.version;
            var version = $scope.versionsMap[versionIndex] || {};
            version[node.name] = node;
            $scope.versionsMap[versionIndex] = version;

            var versionsCount = versionIndex + 1;
            if ($scope.versionsCount < versionsCount) {
                $scope.versionsCount = versionsCount;
            }
        };

        $scope.addChangeStatus = function(params) {
            $scope.changeStatus.push(params);
        };

        var isVisible = function(node) {
            if ($scope.app.hideUnchanged && !node.changed) {
                return false;
            }

            return true;
        }

        var findTarget = function(node) {
            var i = 1;
            while (!target && node.version + i < $scope.versionsCount) {
                var targetId = node.getTargetId(i);
                var target = $scope.nodesMap[targetId];
                if (target && !isVisible(target)) {
                    target = null;
                }

                i++;
            }

            return target;
        }

        var paintConnections = function() {
            for (var i = 0; i < $scope.nodes.length; i++) {
                var node = $scope.nodes[i];
                var target = findTarget(node);

                if (!target) {
                    continue;
                }

                if (!isVisible(node) || !isVisible(target)) {
                    continue;
                }

                jsPlumb.connect({
                    source : node.id,
                    target : target.id,
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
        };

        var removeConnections = function() {
            $('*[class^="_jsPlumb"]').remove();
        };

        $scope.refreshConnections = function(doPrint) {
            if (doPrint) {
                paintConnections();
            } else {
                removeConnections();
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
