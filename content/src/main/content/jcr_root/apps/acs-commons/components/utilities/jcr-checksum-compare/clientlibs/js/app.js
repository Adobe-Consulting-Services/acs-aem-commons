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
                servlet: '/bin/acs-commons/jcr-compare.hashes.txt',
                hostNames: [],
                running: false
            };

            $scope.form = {
                path: '/content/geometrixx/en/products',
            };

            $scope.hosts = [{
                name: 'Self',
                uri: null,
                data: '',
                active: true
            }];

            $scope.diff = {
                baseData: {
                    name: 'base label',
                    data: ''
                },
                newData: {
                    name: 'new label',
                    data: ''
                }
            };

            /* Methods */

            $scope.compare = function () {
                // Clear results
                $scope.results = [];

                $scope.diffSelf();
                $scope.diffRemote();
            };

            $scope.diffSelf  = function () {

                $http({
                    method: 'GET',
                    url: encodeURI($scope.app.servlet + '?path=' + $scope.form.path)
                }).
                    success(function (data, status, headers, config) {
                        $scope.hosts[0].data = data || 'Empty Response';
                    }).
                    error(function (data, status, headers, config) {
                        NotificationsService.add('error',
                            'ERROR', 'Unable to collect JCR diff data from ' + $scope.app.servlet);
                    });
            };

            
            $scope.diffRemote  = function () {

                // Get external hosts
                angular.forEach($scope.hosts, function (host, index) {

                    if (host.active && host.uri && host.uri != '') {

                        $http({
                            method: 'GET',
                            url: encodeURI(host.uri + $scope.app.servlet + '?path=' + $scope.form.path)
                        }).
                            success(function (data, status, headers, config) {
                                host.data = data;
                            }).
                            error(function (data, status, headers, config) {
                                host.data = 'Unable to collect JCR diff data';

                                NotificationsService.add('error',
                                    'ERROR', 'Unable to collect JCR diff data from ' + host.name);
                            });
                    }
                });
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


    }]).directive('diff', function () {
        return {
            restrict: 'A',
            scope: {
                baseData: '=',
                newData: '=',
                inline: '@',
            },
            replace: false,
            link: function(scope, element, attrs) {

                var computeDiff = function() {
                    var sequenceMatcher, opCodes, diffData, baseAsLines, newAsLines;

                    if (scope.baseData.data && scope.newData.data) {

                        baseAsLines = difflib.stringAsLines(scope.baseData.data);
                        newAsLines = difflib.stringAsLines(scope.newData.data);

                        sequenceMatcher = new difflib.SequenceMatcher(baseAsLines, newAsLines);
                        opCodes = sequenceMatcher.get_opcodes();

                        // build the diff view and add it to the current DOM
                        diffData = diffview.buildView({
                            baseTextLines: baseAsLines,
                            newTextLines: newAsLines,
                            opcodes: opCodes,
                            baseTextName: scope.baseData.name,
                            newTextName: scope.newData.name,
                            contextSize: 100,
                            viewType: scope.inline ? 1 : 0
                        });

                        element.html(diffData);

                    } else {
                        element.html('<h3>Nothing to diff<h3>');

                    }
                };

                scope.$watch('baseData.data', function() {
                   computeDiff();
                });

                scope.$watch('newData.data', function() {
                    computeDiff();
                });
             }
        };
    });


