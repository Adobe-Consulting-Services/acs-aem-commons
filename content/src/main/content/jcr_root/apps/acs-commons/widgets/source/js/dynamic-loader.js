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
        var data, i, el,
            head = document.getElementsByTagName("head")[0];
        if (success) {
            data = JSON.parse(response.responseText);
            if (data.js) {
                for (i = 0; i < data.js.length; i++) {
                    el = document.createElement("script");
                    el.type = "text/javascript";
                    el.src = data.js[i];
                    head.appendChild(el);
                }
            }
            if (data.css) {
                for (i = 0; i < data.css.length; i++) {
                    el = document.createElement("link");
                    el.rel = "stylesheet";
                    el.type = "text/css";
                    el.href = data.css[i];
                    head.appendChild(el);
                }
            }
        }
    });
})();