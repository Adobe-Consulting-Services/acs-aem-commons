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

angular.module('ACS.Commons.notifications', []).factory('NotificationsService', ['$timeout', function ($timeout) {
    return {
        data: {
            timeout: 20 * 1000,
            notifications: [],
            running: {
                visible: false,
                title: 'The process is executing',
                message: 'Please be patient while the process executes.'
            }
        },

        init: function (title, message) {
            this.data.running.title = title;
            this.data.running.message = message;
        },

        running: function (visible) {
            if (visible !== undefined) {
                this.data.running.visible = visible;
            } else {
                this.data.running.visible = !this.data.running.visible;
            }

            return this.data.running.visible;
        },

        isRunning: function() {
            return this.data.running.visible;
        },

        add: function (type, title, message) {
            var self = this,
                notification = {
                type: type || 'info',
                title: title,
                message: message,
                timestamp: (new Date().getTime() / 1000),
                /* CoralUI2 Icon - Does not follow usual naming conventions */
                icon:  function () {
                    if (this.type === 'success') {
                        return 'coral-Icon--checkCircle';
                    } else if (this.type === 'info') {
                        return 'coral-Icon--infoCircle';
                    } else if (this.type === 'help') {
                        return 'coral-Icon--helpCircle';
                    } else {
                        return 'coral-Icon--alert';
                    }
                }
            };

            if (notification.title) {
                // Add notification
                this.data.notifications.unshift(notification);

                // Remove notification when the time is right
                $timeout(function () {
                    var index = self.data.notifications.indexOf(notification);

                    if (index > -1) {
                        self.data.notifications.splice(index, 1);
                    }
                }, this.data.timeout);
            }
        }
    };
}]).directive('notifications', ['NotificationsService', function (NotificationsService) {
    return {
        restrict: 'E',
        scope: {
            size: '@size',
            dismissible: '@dismissible'
        },
        template: '<div ng-show="data.notifications.length > 0 || data.running.visible"' +
        ' class="notifications">' +
        '<div ng-show="data.running.visible" class="coral-Alert coral-Alert--notice coral-Alert--large">' +
        '<i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>' +
        '<strong class="coral-Alert-title">{{ data.running.title }}</strong>' +
        '<div class="coral-Alert-message">{{ data.running.message }}</div>' +
        '</div>' +

        '<div ng-repeat="notification in data.notifications">' +
        '<div class="coral-Alert coral-Alert--{{ notification.type }} coral-Alert--{{ size }}">' +
        '<button ng-hide="dismissible === \'false\'" ' +
            'type="button" class="coral-MinimalButton coral-Alert-closeButton" title="Close" data-dismiss="alert">' +
        '<i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i></button>' +
        '<i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS {{ notification.icon() }}"></i>' +
        ' <strong class="coral-Alert-title">{{ notification.title }}</strong>' +
        ' <div class="coral-Alert-message">{{ notification.message }}</div>' +
        '</div>' +
        '</div>' +
        '</div>',
        replace: true,
        link: function(scope, element, attrs) {
            scope.data = NotificationsService.data;
        }
    };
}]);
