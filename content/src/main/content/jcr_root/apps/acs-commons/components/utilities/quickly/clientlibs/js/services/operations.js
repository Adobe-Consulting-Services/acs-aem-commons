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

/*global angular: false, quickly: false, typeof: false */
quickly.factory('Operations', ['Command', 'Result', 'BackOperation', 'FavoritesOperation',
    function(Command, Result, BackOperation, FavoritesOperation) {

    /* Service Object */

    return {

        operations: [
            BackOperation,
            FavoritesOperation
        ],

        getResults: function(cmd) {
            var i = 0,
                cmdOp,
                operation;
            for (; i < this.operations.length; i++) {
                operation = this.operations[i];

                cmdOp = Command.getOp(cmd);
                if (operation && operation.accepts(cmdOp) && (typeof operation.getResults === 'function')) {
                    return operation.getResults(cmd);
                }
            }

            return null;
        },

        process: function(cmd, result) {
            var i = 0,
                cmdOp,
                operation;
            for (; i < this.operations.length; i++) {
                operation = this.operations[i];

                cmdOp = Command.getOp(cmd);
                if (operation && operation.accepts(cmdOp) && (typeof operation.process === 'function')) {
                    return operation.process(cmd, result);
                }
            }

            return null;
        },

        init: function () {
            var i = 0,
                operation;
            for (; i < this.operations.length; i++) {
                operation = this.operations[i];

                if (operation && (typeof operation.init === 'function')) {
                    operation.init();
                }
            }
        }
    };
}]);