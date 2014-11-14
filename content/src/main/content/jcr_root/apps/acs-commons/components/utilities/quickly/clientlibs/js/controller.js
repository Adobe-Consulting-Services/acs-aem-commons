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

/*global quickly: false, angular: false, _: false */

quickly.controller('QuicklyCtrl',
        ['$scope', '$http', '$timeout', 'Command', 'Init', 'Operations', 'Results',
        function($scope, $http, $timeout, Command, Init, Operations, Results){

    $scope.app = {
        visible: false,
        timeout: 0,
        timeoutThrottle: 500
    };

    // initialized via $scope.app.reset()
    $scope.cmd = '';
    $scope.action = {};
    $scope.result = {};
    $scope.results = [];

    /* Watchers */
    $scope.$watch('cmd', function(newValue, oldValue) {

        if($scope.app.visible) {
            // Cancel any existing promises of result retrieval
            $timeout.cancel($scope.app.timeout);

            $scope.app.timeout = $timeout(function() {
                    $scope.app.getResults($scope.cmd);
                },
                $scope.app.timeoutThrottle
            );
        }
    });

    $scope.app.getResults = function(cmd) {
        Results.getResults(cmd).then(function(results) {
            $scope.results = results || [];
            $scope.result = Results.getSelected($scope.results);
        });
    };

    $scope.app.toggle = function(force) {
        $scope.app.reset();

        if(_.isUndefined(force)) {
            $scope.app.visible = !$scope.app.visible;
        } else {
            $scope.app.visible = force;
        }
    };

    $scope.app.reset = function() {
        $scope.cmd = '';
        $scope.result = null;
        $scope.results = [];
        $scope.action = {
            empty: true,
            params: {}
        };
    };

    // Called after a quickly operation is complete; reset and hides quickly
    $scope.app.complete = function() {
        $scope.app.toggle(false);
        $scope.app.reset();
    };

    /* Navigation */

    $scope.app.up = function() {
        Results.up($scope.results);
        $scope.result = Results.getSelected($scope.results);
    };

    $scope.app.down = function() {
        Results.down($scope.results);
        $scope.result = Results.getSelected($scope.results);
    };

    $scope.app.over = function(result) {
        Results.select(result, $scope.results);
        $scope.result = Results.getSelected($scope.results);
    };

    /* Working Actions */

    $scope.app.right = function() {
       //$scope.cmd = Action.right($scope.cmd, Results.getSelected($scope.results));
    };

    $scope.app.select = function() {
        if($scope.result) {
            if(Operations.process($scope.cmd, $scope.result)) {
                // Processed via JS Operations
                $scope.app.complete();
            } else {
                // Processed via Form Operation
                $scope.action = $scope.result.action;
            }
        }
    };

    /* Initialization */

    var init = function() {
        $scope.app.reset();

        // Initialize the app
        Init.init().then(function() {
          // Init all operations after app initializes
          Operations.init();
        });
    };

    init();

}]);
