/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

/**
 * Classic UI Listeners.
 */
var componentCloner = {};

/**
 * Function to build URI and send a GET request to the servlet to clone the specified component by its path.
 * See https://helpx.adobe.com/experience-manager/6-3/sites/developing/using/reference-materials/widgets-api/index.html?class=CQ.Dialog
 *     - Public Events
 *          - beforesubmit
 * @param dialog CQ.Dialog
 * @returns {boolean}
 */
componentCloner.clone = function (dialog) {
    var copyPath = dialog.find('name','./path')[0].value,
        uri = dialog.path + '.clone' + CQ.shared.HTTP.EXTENSION_JSON,
        url = CQ.shared.HTTP.addParameter(uri, 'path', copyPath);

    CQ.shared.HTTP.get(url, componentCloner.callback);
    dialog.close();

    return false;
};

/**
 * 'componentCloner.clone' method's callback function.
 * Callback function to handle the response JSON from the component cloner servlet.
 * See https://helpx.adobe.com/experience-manager/6-3/sites/developing/using/reference-materials/widgets-api/index.html?class=CQ.shared.HTTP
 *     - Public Methods
 *          - get method's callback: Function parameter
 * @param options Object
 * @param success Boolean
 * @param response Object
 */
componentCloner.callback = function (options, success, response) {
    var href = window.location.href,
        data = JSON.parse(response.body);

    // remove any existing error query params
    href = href.replace('&componentClonerError=true', '');
    href = href.replace('?componentClonerError=true', '');

    if (!success || data.componentClonerError) {
        href += (href.indexOf('?') > 1 ? '&' : '?') + 'componentClonerError=true';
    }

    window.location.replace(href);
};
