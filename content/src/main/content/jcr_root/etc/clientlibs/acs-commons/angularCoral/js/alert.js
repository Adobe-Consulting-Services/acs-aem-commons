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
angular.module('acsCoral')
.directive("acsCoralAlert", function() {
    
    return {
        restrict: 'A',
        link : function(scope, $element, attrs) {
            var iconName = 'alert',
                message = attrs.alertMessage;
            switch (attrs.alertType) {
            case 'success' :
                iconName = 'checkCircle';
                break;
            case 'help' :
                iconName = 'helpCircle';
            }
            $element.addClass("coral-Alert coral-Alert--" + attrs.alertType);
            if (attrs.alertSize === 'large') {
                $element.addClass("coral-Alert--large");
            }
            if (message === undefined) {
                message = $element.html();
                $element.html("");
            }
            $element.append('<button type="button" class="coral-MinimalButton coral-Alert-closeButton" title="Close" data-dismiss="alert"><i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i></button>');
            $element.append('<i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--' + iconName + '"></i>');
            $element.append('<strong class="coral-Alert-title">' + attrs.alertTitle + '</strong>');
            $element.append('<div class="coral-Alert-message">' + message + '</div>');
        }
    };
});