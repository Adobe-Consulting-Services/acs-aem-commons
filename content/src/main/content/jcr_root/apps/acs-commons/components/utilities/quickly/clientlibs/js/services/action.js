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

/*global angular: false, quickly: false */
quickly.factory('Action', ['Command', 'Operations', 'BaseResult', 'Results', 'UI', function(Command, Operations, BaseResult, Results, UI) {
        var buildForm = function(action) {
            var params = action.params || [],
                form;

            if(action.method === 'js') {
                // Hacks to get around JSLint's distaste for eval-ish things
                form = angular.element('<form>')
                    .attr('onsubmit', action.script + ';return false;' || 'return false;')
                    .attr('method', 'get');
            } else {
                form = angular.element('<form>')
                    .attr('action', action.uri || '#')
                    .attr('method', action.method || 'get')
                    .attr('target', action.target || '_top');

                angular.forEach(params, function(value, key) {
                    form.append(angular.element('<input>')
                        .attr('type', 'hidden')
                        .attr('name', key)
                        .attr('value', value));
                }, this);
            }

            // UI add form

            return form;
        },

    process = function(cmd, result) {
        var form;

        if(BaseResult.isNoopAction(result)) {
            // Noop means do nothing!
            return false;
        } else if(BaseResult.isJsOperationAction(result)) {
            return Operations.process(cmd, result);
        } else {
            form = buildForm(result.action);
            UI.injectForm(form);

            return form.submit();
        }
    };

    /* Service Object */

    return  {

        right: function(cmd, result) {
            if(result.action.method === 'cmd') {
                cmd = result.action.autoComplete || '';
            } else if(result.action.autoComplete) {
                cmd = Command.updateParam(cmd, result.action.autoComplete);

                UI.focusCommand();
                UI.scrollCommandInputToEnd();
            }

            return cmd;
        },

        select: function(cmd, result, results) {
            Results.select(result, results);
            process(cmd, result);
        }

    };
}]);