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

/*global angular: false, moment: false, JSON: false, difflib: false, diffview: false */

angular.module('acs-commons-jcr-compare-app', ['pasvaz.bindonce', 'acsCoral', 'ACS.Commons.notifications'])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', '$q', 'NotificationsService',
        function ($scope, $http, $timeout, $q, NotificationsService) {

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
                    { value: 'dam:AssetContent' },
                    { value: 'cq:Tag'}
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

            var getParams = function(oParams) {
                var params = {},
                    now = new Date();

                angular.copy(oParams, params);

                params.paths = valueObjectsToArray(params.paths);
                params.nodeTypes = valueObjectsToArray(params.nodeTypes);
                params.excludeNodeTypes = valueObjectsToArray(params.excludeNodeTypes);
                params.excludeProperties = valueObjectsToArray(params.excludeProperties);
                params.sortedProperties = valueObjectsToArray(params.sortedProperties);

                // Clean data
                if (!params.queryType || params.queryType.toLowerCase() === 'none') {
                    delete params.queryType;
                    delete params.query;
                }

                // Funky cache buster due to ng issue with digests on ms
                params._ = now.getHours().toString() + now.getMinutes().toString() + now.getSeconds().toString();

                return params;
            };

            var getHeaders = function(host) {
                var headers = {};

                if (host.user && host.user.length > 0) {
                    headers.Authorization = "Basic " + btoa(host.user + ":" + host.password);
                }

                return headers;
            };

            var valueObjectsToArray = function(objs) {
                var arr = [];

                angular.forEach(objs, function(obj) {
                    if(obj.value) {
                        arr.push(obj.value);
                    }
                });

                return arr;
            };

            $scope.compare = function () {
                var promises = [];

                // Clear results
                $scope.diff.left.data = null;
                $scope.diff.right.data = null;

                $scope.app.running = NotificationsService.running(true);

                $q.all([
                    $scope.getHashes($scope.diff.left),
                    $scope.getHashes($scope.diff.right)
                ]).then(function(promises) {

                    // Left
                    if(promises[0].status === 200) {
                        $scope.diff.left.data = promises[0].data;
                    } else {
                        $scope.diff.left.data = 'Unable to collect JCR diff data from ' + $scope.diff.left.name;
                        NotificationsService.add('error', $scope.diff.left.data);
                    }

                    // Right
                    if(promises[1].status === 200) {
                        $scope.diff.right.data = promises[1].data;
                    } else {
                        $scope.diff.right.data = 'Unable to collect JCR diff data from ' + $scope.diff.right.name;
                        NotificationsService.add('error', $scope.diff.right.data);
                    }

                    $scope.$broadcast('hashChanged');

                    $scope.app.running = NotificationsService.running(false);
                });
            };

            $scope.getHashes = function (host) {
                var params,
                    uri = $scope.app.hashesURI;

                params = getParams($scope.config);

                if (host.uri !== 'localhost') {
                    uri = host.uri + uri;
                }

                return $http({
                    method: 'GET',
                    url: encodeURI(uri),
                    params: params,
                    headers: getHeaders(host)
                });
            };

            /* JSON Comparison */

            $scope.compareJSON = function (path) {
                // Clear results
                $scope.jsonData.left = null;
                $scope.jsonData.right = null;
                $scope.jsonData.path = path;

                $scope.app.running = NotificationsService.running(true);

                $q.all([
                        $scope.getJSON($scope.diff.left, path),
                        $scope.getJSON($scope.diff.right, path)
                    ]).then(function(promises) {

                    // Left
                    if(promises[0].status === 200) {
                        $scope.jsonData.left = promises[0].data;
                    } else {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to collect JCR JSON diff data from ' + $scope.diff.left.name);
                    }

                    // Right
                    if(promises[1].status === 200) {
                        $scope.jsonData.right = promises[1].data;
                    } else {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to collect JCR JSON diff data from ' + $scope.diff.right.name);
                    }

                    $scope.$broadcast('jsonChanged');

                    $scope.app.running = NotificationsService.running(false);

                });
            };

            $scope.getJSON = function (host, path) {
                var params,
                    uri = $scope.app.jsonURI;

                params = getParams($scope.config);

                // JSON Specific; Only get the single path and ignore queries

                params.paths = [path];
                delete params.queryType;
                delete params.query;

                if (host.uri !== 'localhost') {
                    uri = host.uri + uri;
                }

                return $http({
                    method: 'GET',
                    url: encodeURI(uri),
                    params: params,
                    headers: getHeaders(host)
                });
            };

            $scope.$watch('jsonData.left', function() {
                if ($scope.jsonData.left && $scope.jsonData.right) {
                    $scope.app.running = NotificationsService.running(false);
                }
            });

            $scope.$watch('jsonData.right', function() {
                if ($scope.jsonData.left && $scope.jsonData.right) {
                    $scope.app.running = NotificationsService.running(false);
                }
            });

            $scope.configAsParams = function(configuration) {
                var params = getParams(configuration),
                    queryParams = $.param(params);

                return  queryParams;
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

            $scope.validHost = function(item) {
                return !(item.name === null || item.name.trim().length === 0);
            };

    }]).directive('contentdiff', ['$timeout', function ($timeout) {
        return {
            restrict: 'A',
            scope: {
                left: '=',
                right: '='
            },
            template:
                '<div ng-show="diffData.length > 0">' +
                '<hr/>' +
                '<ul class="coral-List coral-List--minimal acsCommons-List--legend">' +
                    '<li class="coral-List-item">' +
                    '<span class="legend-box equals"></span>' +
                    'Content the same between AEM instances' +
                    '</li>' +
                    '<li class="coral-List-item">' +
                    '<span class="legend-box unequals"></span>' +
                    'Content is different between AEM instances' +
                    '</li>' +
                    '<li class="coral-List-item">' +
                    '<span class="legend-box leftOnly"></span>' +
                    'Content only exists on left AEM instance' +
                    '</li>' +
                    '<li class="coral-List-item">' +
                    '<span class="legend-box rightOnly"></span>' +
                    'Content only exists on right AEM instance' +
                    '</li>' +
                '</ul>' +

                '<h3>{{ diffData.length }} paths processed</h3>' +
                '<table class="coral-Table">' +
                    '<thead>' +
                        '<tr class="coral-Table-row">' +
                            '<th class="coral-Table-headerCell"></th>' +
                            '<th class="coral-Table-headerCell">Path</th>' +
                        '</tr>' +
                    '</thead>' +
                    '<tbody>' +
                        '<tr    bindonce ' +
                                'ng-repeat="entry in diffData track by $index" ' +
                                'ng-click="getJSON(entry)" ' +
                                'bo-class="\'coral-Table-row acsCommons-Table-row-\' + entry.op">' +
                            '<td class="coral-Table-cell acsCommons-Table-cell-icon" ' +
                                        'bo-show="entry.op === \'equals\'">' +
                                        '<i class="coral-Icon coral-Icon--check"></i></td>' +
                            '<td class="coral-Table-cell acsCommons-Table-cell-icon" ' +
                                        'bo-show="entry.op === \'unequals\'">' +
                                        '<i class="coral-Icon coral-Icon--close"></i></td>' +
                            '<td class="coral-Table-cell acsCommons-Table-cell-icon" ' +
                                        'bo-show="entry.op === \'leftOnly\'">' +
                                    '<i class="coral-Icon coral-Icon--chevronLeft"></i></td>' +
                            '<td class="coral-Table-cell acsCommons-Table-cell-icon" ' +
                                        'bo-show="entry.op === \'rightOnly\'">' +
                                    '<i class="coral-Icon coral-Icon--chevronRight"></i></td>' +

                            '<td class="coral-Table-cell" bo-text="entry.path"></td>' +
                        '</tr>' +
                    '</tbody>' +
                '<table>' +
                '</div>',

            replace: false,
            link: function(scope, element, attrs) {
                var buildHashes,
                    buildDiff;

                scope.getJSON = function(entry) {
                    if (entry.op !== 'equals') {
                        scope.$parent.compareJSON(entry.path);
                    }
                };


                buildHashes = function(data) {
                    var lines,
                        map = {};

                    if (!data) {
                        return map;
                    }

                    lines =  data.split('\n');

                    angular.forEach(lines, function(line) {
                        var entry = line.split('	');

                        if (entry && entry.length === 2) {
                            map[entry[0]] = entry[1];
                        }
                    });

                    return map;
                };


                buildDiff = function(left, right) {
                    var diff = [],
                        paths = [];

                    angular.forEach(left, function(hash, path) {
                        // track the paths that have been processed
                        paths[path] = true;

                        if (right[path])  {
                            // path exists for both

                            if(left[path] === right[path]) {
                                // Equals
                                diff.push({
                                    path: path,
                                    hash: hash,
                                    op: 'equals'
                                });
                            } else {
                                // Unequals
                                diff.push({
                                    path: path,
                                    hash: hash,
                                    op: 'unequals'
                                });
                            }

                        } else {
                            // path only exists in left
                            diff.push({
                                path: path,
                                hash: hash,
                                op: 'leftOnly'
                            });
                        }
                    });

                    // Check for additions to the right that doesnt exist in left
                    angular.forEach(right, function(hash, path) {
                        if (!paths[path]) {
                            // does not exist in the diff, so only exists in right
                            diff.push({
                                path: path,
                                hash: hash,
                                op: 'rightOnly'
                            });
                        }
                    });

                    return diff;
                };


                var computeDiff = function() {
                    scope.diffData = buildDiff(buildHashes(scope.left.data),
                        buildHashes(scope.right.data));
                };

                scope.$on('hashChanged', function() {
                    $timeout(function() {
                        computeDiff();
                    }, 0);

                });
             }
        };
    }]).directive('jsondiff', ['$timeout', function ($timeout) {
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
                var modal = new CUI.Modal({ element:'#jsonDiffModal', visible: false }),
                    centerModal,
                    computeDiff;

                centerModal = function() {
                    // Fix centering
                    $modal = angular.element('#jsonDiffModal');
                    $modal.css('margin-left', -1 * ($modal.outerWidth() / 2));
                };

                computeDiff = function() {
                    var delta = jsondiffpatch.diff(scope.left, scope.right);
                    // Using angular.element since CUI moves the Modal DOM outside of the original context
                    angular.element('#json-diff').html(jsondiffpatch.formatters.html.format(delta));

                    modal.show();
                    centerModal();
                };

                scope.$on('jsonChanged', function() {
                    $timeout(function() {
                        computeDiff();
                    }, 0);
                });
            }
        };
    }]);