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

/*global angular: false, moment: false, JSON: false, difflib: false, diffview: false, console: false */

angular.module('acs-commons-jcr-checksum-compare-app', ['acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', 'NotificationsService',
        function ($scope, $http, $timeout, NotificationsService) {

            $scope.app = {
                hashesURI: '/bin/acs-commons/jcr-compare.hashes.txt',
                jsonURI: '/bin/acs-commons/jcr-compare.dump.json',
                hostNames: [],
                running: false
            };

            $scope.config = {
                optionsName: 'REQUEST',
                paths: [
                    { value: '/content' }
                ],
                queryType: 'None',
                nodeTypes: [
                    { value: 'cq:PageContent' },
                    { value: 'dam:AssetContent' }
                ],
                excludeNodeTypes: [
                    { value: 'rep:ACL' },
                    { value: 'cq:meta' } //added for AEM6.1 compatibility
                ],
                excludeProperties: [
                    { value: 'jcr:mixinTypes'}, //added as author instances have cq:ReplicationStatus and pubs don't
                    { value: 'jcr:created' },
                    { value: 'jcr:createdBy'},
                    { value: 'jcr:uuid' },
                    { value: 'jcr:lastModified' },
                    { value: 'jcr:lastModifiedBy' },
                    { value: 'cq:lastModified' },
                    { value: 'cq:lastModifiedBy' },
                    { value: 'cq:lastReplicated' },
                    { value: 'cq:lastReplicatedBy' },
                    { value: 'cq:lastReplicationAction' },
                    { value: 'jcr:versionHistory' },
                    { value: 'jcr:predecessors' },
                    { value: 'jcr:baseVersion' }
                ],
                sortedProperties: [
                    { value: 'cq:tags' }
                ]
            };

            $scope.hosts = [{
                name: 'localhost',
                uri: '',
                data: '',
                active: true
            }];

            $scope.diff = {
                left: null,
                right: null
            };

            $scope.jsonData = {
                left: null,
                right: null
            };

            /* Methods */

            $scope.getParams = function(params) {

                // Add cache-buster
                params._ = new Date().getTime();

                params.paths = $scope.valueObjectsToArray(params.paths);
                params.nodeTypes = $scope.valueObjectsToArray(params.nodeTypes);
                params.excludeNodeTypes = $scope.valueObjectsToArray(params.excludeNodeTypes);
                params.excludeProperties = $scope.valueObjectsToArray(params.excludeProperties);
                params.sortedProperties = $scope.valueObjectsToArray(params.sortedProperties);

                // Clean data
                if (!params.queryType || params.queryType.toLowerCase() === 'none') {
                    delete params.queryType;
                    delete params.query;
                }

                return params;
            };

            $scope.compare = function () {
                // Clear results
                $scope.diff.left.data = null;
                $scope.diff.right.data = null;

                $scope.getHashes($scope.diff.left);
                $scope.getHashes($scope.diff.right);
            };

            $scope.getHashes = function (host) {
                var params;

                $scope.app.running = NotificationsService.running(true);
                params  = $scope.getParams(angular.copy($scope.config));

                $http({
                    method: 'GET',
                    url: encodeURI(host.uri + $scope.app.hashesURI),
                    params: params,
                    headers: {'Authorization': ((host.user && host.user.length > 0)?"Basic " + btoa(host.user + ":" + host.password):undefined)}
                }).
                    success(function (data, status, headers, config) {
                        host.data = data;
                    }).
                    error(function (data, status, headers, config) {
                        host.data = 'ERROR: Unable to collect JCR diff data from ' + host.name;

                        NotificationsService.add('error',
                            'ERROR', 'Unable to collect JCR diff data from ' + host.name);

                    }).
                    finally(function() {
                        $scope.app.running = NotificationsService.running(false);
                    });
            };


            /* JSON Comparison */

            $scope.compareJSON = function (path) {
                // Clear results
                $scope.jsonData.left = null;
                $scope.jsonData.right = null;
                $scope.jsonData.path = path;

                $scope.getJSON($scope.diff.left, true, path);
                $scope.getJSON($scope.diff.right, false,  path);
            };

            $scope.getJSON = function (host, isLeft, path) {
                var params;

                $scope.app.running = NotificationsService.running(true);

                params = $scope.getParams(angular.copy($scope.config));
                params.paths = [ path ];

                $http({
                    method: 'GET',
                    url: encodeURI(host.uri + $scope.app.jsonURI),
                    params: params,
                    headers: {'Authorization': ((host.user && host.user.length > 0)?"Basic " + btoa(host.user + ":" + host.password):undefined)}
                }).
                    success(function (data, status, headers, config) {
                        if (isLeft) {
                            $scope.jsonData.left = data;
                        } else {
                            $scope.jsonData.right = data;
                        }
                    }).
                    error(function (data, status, headers, config) {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to collect JCR JSON diff data from ' + host.name);
                    }).
                    finally(function() {
                        $scope.app.running = NotificationsService.running(false);
                    });
            };


            $scope.configAsParams = function() {
                var params = $scope.getParams(angular.copy($scope.config));

                return $.param(params);
            };

            $scope.init = function(hostNames) {
                angular.forEach(hostNames, function(hostName) {
                    this.push({
                        name: hostName,
                        uri: hostName,
                        data: '',
                        active: false
                    });
                }, $scope.hosts);
            };


            $scope.valueObjectsToArray = function(objs) {
                var arr = [];

                angular.forEach(objs, function(obj) {
                    if(obj.value) {
                        arr.push(obj.value);
                    }
                });

                return arr;
            };

    }]).directive('contentdiff', function ($compile) {
        return {
            restrict: 'A',
            scope: {
                left: '=',
                right: '=',
                inline: '@',
            },
            replace: false,
            link: function(scope, element, attrs) {

                var computeDiff = function() {
                    var sequenceMatcher, opCodes, diffData, baseAsLines, newAsLines, html;

                    if (scope.left && scope.left.data && scope.right && scope.right.data) {

                        baseAsLines = difflib.stringAsLines(scope.left.data);
                        newAsLines = difflib.stringAsLines(scope.right.data);

                        sequenceMatcher = new difflib.SequenceMatcher(baseAsLines, newAsLines);
                        opCodes = sequenceMatcher.get_opcodes();

                        // build the diff view and add it to the current DOM
                        diffData = diffview.buildView({
                            baseTextLines: baseAsLines,
                            newTextLines: newAsLines,
                            opcodes: opCodes,
                            baseTextName: scope.left.name,
                            newTextName: scope.right.name,
                            contextSize: 100,
                            viewType: scope.inline ? 1 : 0
                        });


                        html = angular.element(diffData);

                        html.find('td.delete, td.insert').each(function() {
                            var $this = angular.element(this),
                                t = $this.text(),
                                path = t.substr(0, t.indexOf('    '));
                            $this.attr('ng-click', 'compareJSON(\'' + encodeURI(path) + '\')');
                        });

                        // Bind to the Controllers scope so compareJSON(..) resolves
                        element.html($compile(html)(scope.$parent));

                    } else {
                        element.html('');
                    }
                };

                scope.$watch('left.data', function() {
                    if (scope.left && scope.right && scope.left.data && scope.right.data) {
                        computeDiff();
                    }
                });

                scope.$watch('right.data', function() {
                    if (scope.left && scope.right && scope.left.data && scope.right.data) {
                        computeDiff();
                    }
                });
             }
        };
    }).directive('jsondiff', function () {
        return {
            restrict: 'A',
            scope: {
                left: '=',
                right: '=',
                path: '@'
            },
            template: '<div id="jsonDiffModal" class="coral-Modal">' +
                        '<div class="coral-Modal-header">' +
                            '<i class="coral-Modal-typeIcon coral-Icon coral-Icon--sizeS"></i>' +
                            '<h2 class="coral-Modal-title coral-Heading coral-Heading--2">{{ path }}</h2>' +
                            '<button type="button" class="coral-MinimalButton coral-Modal-closeButton" title="Close" data-dismiss="modal">' +
                                '<i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon "></i>' +
                            '</button>' +
                        '</div>' +
                        '<div class="coral-Modal-body acsCommons-Modal-body" id="json-diff">' +
                            '<p id="json-diff"></p>' +

                        '</div>' +
                        '<div class="coral-Modal-footer">' +
                            '<button type="button" class="coral-Button" data-dismiss="modal">Close</button>' +
                        '</div>' +
                    '</div>',
            replace: false,
            link: function(scope, element, attrs) {
                var modal = new CUI.Modal({ element:'#jsonDiffModal', visible: false });

                var computeDiff = function() {
                    var $modal,
                        delta = jsondiffpatch.diff(scope.left, scope.right);
                    element.find('#json-diff').html(jsondiffpatch.formatters.html.format(delta));

                    modal.show();

                    // Fix centering
                    $modal = angular.element('#jsonDiffModal');
                    $modal.css('margin-left', -1 * ($modal.outerWidth() / 2));

                };

                scope.$watch('left', function() {
                    if (scope.left && scope.right) {
                        computeDiff();
                    }
                });

                scope.$watch('right', function() {
                    if (scope.left && scope.right) {
                        computeDiff();
                    }
                });
            }
        };
    });