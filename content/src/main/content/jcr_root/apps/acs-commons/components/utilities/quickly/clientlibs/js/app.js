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

/*global angular: false, document: false, console: false */
var quickly = angular.module('quickly',['ngSanitize', 'ngCookies', 'ngStorage']).config(function($sceProvider) {
    $sceProvider.enabled(true);
});

angular.element(document).ready(function () {
    var quicklyControllerId = '#acs-commons-quickly-ctrl',

        isQuicklyToggleEvent = function(e) {
            if(e.ctrlKey && e.which === 0) {
                // ctrl-space (Chrome/Safari OSX)
                return true;
            } else if(e.ctrlKey && e.which === 32) {
                // ctrl-space (Chrome/Safari Windows)
                return true;
            } else if (e.shiftKey && e.ctrlKey && e.which === 64) {
                // shift-ctrl-space (FireFox OSX)
                return true;
            } else if (e.shiftKey && e.ctrlKey && e.which === 32) {
                // shift-ctrl-space (FireFox Windows)
                return true;

            } else {
                return false;
            }
        },

        isQuicklyDismissEvent = function(e) {
            if(e.keyCode === 27) {
                // Escape
                return true;
            }

            return false;
        };

    $('html').keypress(function(e){
        var controllerEl = angular.element(quicklyControllerId);

        if(!controllerEl || !controllerEl.scope()) {
            return;
        } else if(isQuicklyToggleEvent(e)) {
            controllerEl.scope().app.toggle();
            controllerEl.scope().$apply();
            e.preventDefault();
        } else if(isQuicklyDismissEvent(e)) {
            if(controllerEl.scope().app.visible) {
                controllerEl.scope().app.toggle();
                controllerEl.scope().$apply();
                e.preventDefault();
            }
        }
    });

    $('html').keydown(function(e){
        var controllerEl;
        if(isQuicklyDismissEvent(e)) {
            controllerEl = angular.element(quicklyControllerId);

            if(controllerEl && controllerEl.scope() && controllerEl.scope().app.visible) {
                controllerEl.scope().app.toggle();
                controllerEl.scope().$apply();
                e.preventDefault();
            }
        }
    });


    // Wait for AEM to inject iframes
    setTimeout(function() {
        $('iframe').load(function() {
            var iframe = $('iframe').contents().find('html');

            // Bubble keypress events to parent window html
            iframe.on('keydown keypress', function(e) {

                if(isQuicklyToggleEvent(e) || isQuicklyDismissEvent(e)) {
                    $('html').trigger(e);
                }
            });
        });
    }, 2000);
});