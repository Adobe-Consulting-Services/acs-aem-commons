/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2017 Adobe
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
/*global CQ: false */
(function() {
    CQ.HTTP.get("/bin/acs-commons/dynamic-classicui-clientlibs.json", function(options, success, response) {
        var paths, i;
        if (success) {
            paths = JSON.parse(response.responseText);
            for (i = 0; i < paths.length; i++) {
                $.getScript(paths[i]);
            }
        }
    });
})();