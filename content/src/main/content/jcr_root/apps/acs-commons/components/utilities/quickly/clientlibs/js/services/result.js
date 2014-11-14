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

/*global angular: false, quickly: false, _: false */
quickly.factory('Result', function() {
    return {
        ACTION_METHODS: {
            JS_OPERATION_ACTION: 'js-operation-action'
        },

        isNoopAction: function(result) {
            return !result || !result.action || !result.action.method || result.action.method === 'noop';
        },

        isJsOperationAction: function(result) {
            return result && result.action && result.action.method === this.ACTION_METHODS.JS_OPERATION_ACTION;
        },

        build: function(overlay) {
            var result = {
                title: '',
                path: '',
                description: '',
                selected: false,
                action: {
                    uri: '#',
                    script: null,
                    method: 'get',
                    target: '_self',
                    params: {}
                },
                secondaryAction: {
                    uri: '#',
                    script: null,
                    method: 'get',
                    target: '_self',
                    params: {}
                }
            };

            return _.merge(result, overlay || {});
        }
    };
});
