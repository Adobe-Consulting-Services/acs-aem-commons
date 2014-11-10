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
quickly.factory('Command', function() {

    /* Service Object */

    return  {

        clear: function(cmd) {
            cmd = '';
        },

        getOp: function(cmd) {
            var endIndex;

            if(cmd) {
                cmd = cmd.trim();
                endIndex = cmd.indexOf(' ');

                if (endIndex > -1) {
                    return cmd.substring(0, endIndex);
                } else {
                    return cmd;
                }
            }

            return '';
        },

        getParam: function(cmd, strict) {
            var endIndex;

            if (cmd) {
                cmd = cmd.trim();
                endIndex = cmd.indexOf(' ');

                if (endIndex < 0) {
                    // Is one word .. not sure if op or param
                    if (strict) {
                        return '';
                    } else {
                        // If no Op is present then everything is a param to the default command handler
                        return cmd;
                    }
                } else {
                    return cmd.substring(endIndex + 1, cmd.length);
                }
            }

            return '';
        },

        getParams: function(cmd, strict, max) {
            var param = this.getParam(cmd, strict),
                all = param.trim().split(/\s+|^\n/),
                full = all.slice(0, max), // all options
                params = all.slice(max, all.length).join(' '); // the params

            if(params && params.length > 0) {
                full.push(params);
            }

            return full;
        },

        hasOp: function(cmd) {
            return this.getOp(cmd) !== '';
        },

        hasParam: function(cmd, strict) {
            return this.getParam(cmd, strict) !== '';
        },

        updateParam: function(cmd, param) {
            if(this.hasOp(cmd)) {
                cmd = this.getOp(cmd) + ' ' + param;
            } else {
                cmd = param;
            }

            return cmd;
        }

    };
});