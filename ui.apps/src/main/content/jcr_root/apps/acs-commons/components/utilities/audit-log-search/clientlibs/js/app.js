/*
 * #%L
 * ACS AEM Commons Package
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
angular
  .module("acs-commons-audit-log-search-app", [
    "acsCoral",
    "ACS.Commons.notifications"
  ])
  .controller("MainCtrl", [
    "$scope",
    "$http",
    "$timeout",
    "NotificationsService",
    function ($scope, $http, $timeout, NotificationsService) {
      $scope.app = {
        uri: ""
      };

      $scope.form = {
        contentRoot: "",
        includeChildren: "true",
        type: "",
        user: "",
        startDate: "",
        endDate: "",
        order: "",
        limit: 500
      };

      $scope.result = {};

      $scope.search = function () {
        var start = new Date().getTime();
        NotificationsService.running(true);
        $scope.result = {};

        $http({
          method: "GET",
          url: $scope.app.uri + "?" + $("#audit-log-search-form").serialize()
        }).then(
          function (response) {
            var data = response.data;
            var status = response.status;
            var headers = response.headers;
            var config = response.config;

            var time = new Date().getTime() - start;
            data.time = time;
            $scope.result = data || {};
            NotificationsService.running(false);
            if (data.succeeded) {
              NotificationsService.add(
                "success",
                "SUCCESS",
                "Found " + data.count + " audit events in " + time + "ms!"
              );
            } else if (
              data.indexOf("The query read or traversed more than ") !== -1
            ) {
              NotificationsService.add(
                "error",
                "ERROR",
                "Unable to search audit logs due to traversal limits, please ensure you have Oak Indexes installed!"
              );
            } else {
              NotificationsService.add(
                "error",
                "ERROR",
                "Unable to search audit logs, please consult logs for further information!"
              );
            }
          },
          function (error) {
            var data = error.data;
            var status = error.status;
            NotificationsService.running(false);
            NotificationsService.add(
              "error",
              "ERROR",
              "Unable to search audit logs!"
            );
          }
        );
      };

      $scope.init = function () {};
    }
  ]);
