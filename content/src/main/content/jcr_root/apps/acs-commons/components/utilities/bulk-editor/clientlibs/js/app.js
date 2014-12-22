/*global angular: false */

angular.module('bulkEditor', [])
    .config( ['$provide', function ($provide){
        //Disabled browser URL change events to not disturb the $.bbq usage
        $provide.decorator('$browser', ['$delegate', function ($delegate) {
            $delegate.onUrlChange = function () {};
            $delegate.url = function () { return "";};
            return $delegate;
        }]);
    }])
    .controller('MainCtrl', ['$scope', '$http', '$timeout', '$location',
    function ($scope, $http, $timeout, $location) {

        var DEFAULT_QUERY = {
            'path': '/content',
            'type': 'cq:Page',
            'p.limit': '10',
            'p.hits': 'simple'
        };

        $scope.app = {
            'running': false,
            'selectAll': false
        };

        $scope.query = $.extend({}, DEFAULT_QUERY);
        
        $scope.update = {
            params: {},
            toUpdate: [],            
            successes: [],
            errors: []
        };
        
        $scope.data = {};
        $scope.results = [];
        $scope.notifications = [];
        $scope.isDefault = true;
        
        $scope.$watch('query', function (newValue, oldValue) {
            $scope.cleanQuery();
            $scope.queryString = JSON.stringify(newValue);
            $scope.isDefault = $scope.query['p.hits'] === 'simple' ? true : false;
            $scope.data = {};        
		}, true);

        $scope.addNotification = function (type, title, message) {
            var timeout = 10000;

            if (type === 'success') {
                timeout = timeout / 2;
            }

            $scope.notifications.push({
                type: type,
                title: title,
                message: message
            });

            $timeout(function () {
                $scope.notifications.shift();
            }, timeout);
        };
        
        $scope.selectAll = function() {
            angular.forEach($scope.results, function (index, key) {
                index.selected = $scope.app.selectAll;
            });
        };
        
        $scope.search = function() {
            $scope.app.running = true;
			$http({
                method: 'GET',
                url: encodeURI('/bin/querybuilder.json'),
                params: $scope.query,
                headers: {'Content-Type': 'application/json'}
            }).success(function (data, status, headers, config) {
                angular.forEach(data.hits, function (index, key) {
                    if(index.hasOwnProperty('jcr:path')){
                        index.path = index['jcr:path'];
                    }
                });
            
                $scope.data = data;
                $scope.results = data.hits;
                $scope.pushQueryToUrl();
                $scope.app.running = false;
                $scope.clear();     
            }).error(function (data, status, headers, config) {
                $scope.addNotification('error', 'ERROR', 'Could not retrieve search results.');
                $scope.app.running = false;
                $scope.clear();
            });
        };
        
        $scope.doUpdate = function() {
            $scope.app.running = true;
            $scope.clear();
            
            if($scope.update.params.property === undefined || $scope.update.params.property.length === 0){
                $scope.addNotification('error', 'ERROR', 'Could not proceed with update on property when no property is defined.');
                $scope.app.running = false;
                return;
            }
            
            angular.forEach($scope.results, function (index, key) {
                if(index.selected){
                    $scope.update.toUpdate.push(index);
                }
            });
            
            if($scope.update.toUpdate.length > 0){
                $scope.doPost(0);
            } else {
                $scope.addNotification('error', 'ERROR', 'No items selected. There is nothing to update.');
                $scope.app.running = false;
                return;
            }
        };
        
        $scope.doReset = function(){
            $scope.app.running = false;
            $scope.clear();
            $scope.query = $.extend({}, DEFAULT_QUERY);
        };
        
        $scope.doPost = function(id) {
            var params = {}, item;
            item = $scope.update.toUpdate[id];
            params._charset_ = 'utf-8';
            if($scope.update.params.remove){
                params[$scope.update.params.property + "@Delete"] = "Delete";
            } else {
                params[$scope.update.params.property] = $scope.update.params['property.value'];
                if($scope.update.params.patch) {
                    params[$scope.update.params.property] = $scope.update.params.patchop + $scope.update.params['property.value'];
                    params[$scope.update.params.property + "@Patch"] = "true";
                }
                if($scope.update.params.datatype !== undefined && $scope.update.params.datatype.length > 0){
                    params[$scope.update.params.property + "@TypeHint"] = $scope.update.params.datatype;
                }
            }

            $http({
                url: item.path,
                cache: false,
                method: 'POST',
                params: params
            }).success(function(data, status, headers, config){
                $scope.update.successes.push(data.path);
                id++;
                if(id < $scope.update.toUpdate.length){
                    $scope.doPost(id);
                } else {
                    $scope.app.running = false;
                }
            }).error(function(data, status, headers, config){
                $scope.update.errors.push(data.path);
                id++;
                if(id < $scope.update.toUpdate.length){
                    $scope.doPost(id);
                } else {
                    $scope.app.running = false;    
                }
            });
        };
        
        $scope.customColumns = function(){
            if($scope.update.params['p.properties'] !== undefined && $scope.update.params['p.properties'].length > 0){
                return $scope.update.params['p.properties'].split(" ");
            }
            return [];
        };
        
        $scope.loadQueryFromUrl = function() {
            $.extend($scope.query, $.bbq.getState());
        };
        
        $scope.pushQueryToUrl = function() {
			$.bbq.pushState($scope.query, 2);
        };
        
        $scope.cleanQuery = function() {
            var property, value;
            for (property in $scope.query) {
                if ($scope.query.hasOwnProperty(property)) {
                    value = $scope.query[property];
                    if(value === undefined || value.length === 0) {
                        delete $scope.query[property];
                    }
                }
            }
        };
        
        $scope.fromJson = function(obj, path){
            var i, part, parts = path.split("/");
            for(i = 0; i < parts.length; i++){
                part = parts[i];
                if(obj.hasOwnProperty(part)){
                    obj = obj[part];
                }else{
                    return "(null)";
                }
            }
            return obj;
        };

        $scope.clear = function() {
            $scope.app.selectAll = false;
            $scope.update.toUpdate = [];
            $scope.update.errors = [];
            $scope.update.successes = [];            
        };

        $scope.init = function () {
            $scope.loadQueryFromUrl();
        };
    }
]);
