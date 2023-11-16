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
    /*global angular: false */
    angular
      .module("acs-commons-redirectmappage-app", [
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

          $scope.entries = [];
          $scope.entriesToRemove = [];
          $scope.filteredEntries = [];
          $scope.invalidEntries = [];
          $scope.redirectMap = "";
          $scope.currentEntry = null;

          $scope.addEntry = function () {
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.entries = [];
            $scope.invalidEntries = [];
            $http({
              method: "POST",
              url:
                $scope.app.uri + ".addentry.json?" + $("#entry-form").serialize()
            }).then(
              function (response) {
                var data = response.data;
                var status = response.status;
                var headers = response.headers;
                var config = response.config;

                var time = new Date().getTime() - start;
                data.time = time;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                $scope.filterEntries();
                NotificationsService.running(false);
                NotificationsService.add("success", "SUCCESS", "Entry added!");
                $scope.loadRedirectMap();
              },
              function (error) {
                var data = error.data;
                var status = error.status;
                NotificationsService.running(false);
                NotificationsService.add("error", "ERROR", "Unable to add entry!");
              }
            );
          };



        $scope.page = 1; // Current page
        $scope.itemsPerPage = 100; // Number of items to load per page
        $scope.hasMoreData = true; // Whether there's more data to load

    $scope.loadMoreData = function () {
      if ($scope.hasMoreData && $scope.itemsPerPage < $scope.entries.length) {
        $scope.itemsPerPage = $scope.itemsPerPage +100;
        $scope.filterEntries();
      }
    };

    $scope.filterEntries = function () {
      $scope.filteredEntries = [];
      var term = $("#filter-form")
        .find("input[name=filter]")
        .val()
        .toLowerCase();

      if (term.trim() !== "" && term.trim() !== "*") {
        $scope.page = 1; // Reset the page to 1
        $scope.hasMoreData = true; // Reset to allow loading more data
        filterData(term);
      } else {
        $scope.filteredEntries = $scope.entries.slice(
          0,
          $scope.page * $scope.itemsPerPage
        );
        $scope.hasMoreData = $scope.filteredEntries.length < $scope.entries.length;
      }
    };


    function filterData(term) {
      $scope.filteredEntries = $scope.entries.filter(function (el) {
        var found = term.trim() === "*";
            Object.values(el).forEach(function (val) {
              if (val.toString().toLowerCase().indexOf(term) !== -1) {
                found = true;
              }
            });

        return found;
      });

      $scope.hasMoreData = $scope.filteredEntries.length < $scope.entries.length;
      NotificationsService.add(
        "success",
        "SUCCESS",
        "Found " + $scope.filteredEntries.length + " entries for " + term + "!"
      );
    }

    function loadMoreData() {
      if ($scope.hasMoreData) {
        // Simulate a delay for the example
        // In a real application, you would make an AJAX request to fetch data
        // based on the current page and itemsPerPage
        simulateDataLoading();
      }
    }

    function simulateDataLoading() {
      // Simulate fetching more data with a delay
      setTimeout(function () {
        var newData = $scope.entries.slice(
          $scope.filteredEntries.length,
          $scope.filteredEntries.length + $scope.itemsPerPage
        );

        // Check if there's more data
        $scope.hasMoreData = $scope.filteredEntries.length < $scope.entries.length;

        $scope.filteredEntries = $scope.filteredEntries.concat(newData);

        NotificationsService.add(
          "success",
          "SUCCESS",
          "Loaded " + newData.length + " more entries!"
        );

        $scope.$apply(); // Notify AngularJS that the data has changed
      }, 1000); // Simulated delay in milliseconds
    }






    // Example: Reset the filter and load the first page of data
    $scope.resetFilter = function () {
      $scope.page = 1;
      $scope.filteredEntries = [];
      $scope.hasMoreData = true;
      $scope.filterEntries();
    };



          $scope.init = function () {
            $('.endor-Crumbs-item[href="/miscadmin"]')
              .html("Redirects")
              .attr("href", "/miscadmin#/etc/acs-commons/redirect-maps");
            $scope.load();
          };

          $scope.load = function () {
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.filteredEntries = [];
            $scope.entries = [];
            $scope.invalidEntries = [];
            $http({
              method: "GET",
              url: $scope.app.uri + ".redirectentries.json"
            }).then(
              function (response) {
                var data = response.data;
                var status = response.status;
                var headers = response.headers;
                var config = response.config;
                var time = new Date().getTime() - start;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                NotificationsService.running(false);
                NotificationsService.add(
                  "success",
                  "SUCCESS",
                  "Found " + data.length + " entries in " + time + "ms!"
                );
                $scope.loadRedirectMap();
                $scope.filterEntries();
              },
              function (error) {
                var data = error.data;
                var status = error.status;
                NotificationsService.running(false);
                NotificationsService.add(
                  "error",
                  "ERROR",
                  "Unable load redirect entries!"
                );
              }
            );
          };

          $scope.loadRedirectMap = function () {
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.redirectMap = "";
            $http({
              method: "GET",
              url: $scope.app.uri + ".redirectmap.txt"
            }).then(
              function (response) {
                var data = response.data;
                var status = response.status;
                var headers = response.headers;
                var config = response.config;
                var time = new Date().getTime() - start;
                $scope.redirectMap = data || "";
                NotificationsService.running(false);
                NotificationsService.add(
                  "success",
                  "SUCCESS",
                  "Loaded redirect map in " + time + "ms!"
                );
              },
              function (error) {
                var data = error.data;
                var status = error.status;
                NotificationsService.running(false);
                NotificationsService.add(
                  "error",
                  "ERROR",
                  "Unable load redirect map!"
                );
              }
            );
          };

          $scope.openEditor = function (path) {
            if (path.indexOf("/content/dam") === -1) {
              window.open("/editor.html" + path + ".html", "_blank");
            } else {
              window.open(
                "/mnt/overlay/dam/gui/content/assets/metadataeditor.external.html?_charset_=utf-8&item=" +
                  path,
                "_blank"
              );
            }
          };

          $scope.editItem = function (id) {
            $scope.entries.forEach(function (el) {
              if (el.id === id) {
                console.log("Editing entry: " + id);
                document.querySelector("input[name=edit-source]").value = el.source;
                document.querySelector("input[name=edit-target]").value = el.target;
                document.querySelector("input[name=edit-target-base]").value = el.target;
                console.log("Target Base " +document.querySelector("input[name=edit-target-base]").value);
                document.querySelector("input[name=edit-id]").value = id;
              }
            });

            var dialog = document.querySelector("#edit-entry");
            dialog.show();
          };

          $scope.postValues = function (e, id) {
            e.preventDefault();

            NotificationsService.running(true);

            var $form = $("#" + id);

            $.post($form.attr("action"), $form.serialize(), function () {
              location.reload(true);
            });
            return false;
          };

          $scope.removeAlert = function (id) {
            $scope.currentEntry = id;

            var dialog = document.querySelector("#remove-confirm");
            dialog.show();
          };
          $scope.removeAlertMulti = function () {
            var dialog = document.querySelector("#removemulti-confirm");
            dialog.show();
          };
          $scope.selectEntry = function (id) {
            $scope.currentEntry = id;
            if($scope.entriesToRemove.includes(id) === true ){
               //$scope.entriesToRemove = $scope.entriesToRemove.filter(item => item !== id);
            }else{
                $scope.entriesToRemove.push(id);
            }
            console.log("entriesToRemove : "+ $scope.entriesToRemove.toString());
          };

          $scope.removeLine = function () {
            var idx = $scope.currentEntry;
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.entries = [];
            $scope.filteredEntries = [];
            $scope.invalidEntries = [];
            var multi = false;
            if($scope.entriesToRemove.length > 0){
                multi = true;
                idx = $scope.entriesToRemove;
            }
            $http({
              method: "POST",
              url: $scope.app.uri + ".removeentry.json?idx=" + idx+"&multi="+multi
            }).then(
              function (response) {
                var data = response.data;
                var status = response.status;
                var headers = response.headers;
                var config = response.config;
                var time = new Date().getTime() - start;
                data.time = time;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                NotificationsService.running(false);
                NotificationsService.add(
                  "success",
                  "SUCCESS",
                  "Redirect map updated! BY REMOVING "+ $scope.entriesToRemove
                );
                $scope.loadRedirectMap();
                $scope.filterEntries();
                $scope.entriesToRemove = [];
              },
              function (error) {
                var data = error.data;
                var status = error.status;
                NotificationsService.running(false);
                NotificationsService.add(
                  "error",
                  "ERROR",
                  "Unable remove entry " + idx + "!"
                );
              }
            );
          };

          $scope.saveLine = function () {
            var dialog = document.querySelector("#edit-entry");
            var start = new Date().getTime();
            NotificationsService.running(true);
            $scope.entries = [];
            $scope.invalidEntries = [];
            $http({
              method: "POST",
              url:
                $scope.app.uri +
                ".updateentry.json?" +
                $("#update-form").serialize()
            }).then(
              function (response) {
                var data = response.data;
                var status = response.status;
                var headers = response.headers;
                var config = response.config;

                var time = new Date().getTime() - start;
                data.time = time;
                $scope.entries = data.entries || [];
                $scope.invalidEntries = data.invalidEntries || [];
                $scope.filterEntries();
                NotificationsService.running(false);
                NotificationsService.add("success", "SUCCESS", "Entry updated!");
                $scope.loadRedirectMap();
                dialog.hide();
                // Reload the current page after a successful update
               // Reload the table data
               location.reload(true);
              },
              function (error) {
                var data = error.data;
                var status = error.status;
                NotificationsService.running(false);
                NotificationsService.add(
                  "error",
                  "ERROR",
                  "Unable to update entry!"
                );
                dialog.hide();
              }
            );
          };

          $scope.updateRedirectMap = function (e) {
            e.preventDefault();

            NotificationsService.running(true);

            var $form = $("#fn-acsCommons-update-redirect");

            $.ajax({
              url: $form.attr("action"),
              data: new FormData($form[0]),
              cache: false,
              contentType: false,
              processData: false,
              type: "POST",
              success: function (data) {
                location.reload(true);
              }
            });
            return false;
          };
        }
      ]);
