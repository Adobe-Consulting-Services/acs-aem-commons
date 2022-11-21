/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
(function() {
    function getCSRFToken() {
        return fetch('/libs/granite/csrf/token.json').then(function(r) { return r.json().then(function(j) { return j.token; }); });
    }

    function execute(event) {
        event.preventDefault();

        var el = event.target;
        var action = el.getAttribute('name');
        var actionUrl = el.dataset.url;

        getCSRFToken().then(function(token) {
            fetch(actionUrl, {
              method: 'POST',
              headers: { 'CSRF-Token': token, 'Content-Type': 'application/x-www-form-urlencoded' },
              body: new URLSearchParams( { 'preview': action === 'preview' }),
            })
            .then(function(response) { return response.json(); })
            .then(function(data) {
                if (data.status === 'success') {
                    displayCreateSuccess(data);
                } else if (data.status === 'preview') {
                    displayPreview(data);
                } else {
                    displayError(data.msg);
                }
            })
            .catch(function(error) {
            console.log(error);
                displayError(error);
            });
        });
    }

    function displayPreview(data) {
       var results = document.getElementById('results');
       var template = document.getElementById('preview-results').content.cloneNode(true);

       var filters = '';
       if (data.filterSets && data.filterSets.length > 0) {
           data.filterSets.forEach(function(filterSet) {
                filters += '<tr class="spectrum-Table-row"><td class="spectrum-Table-cell">' + filterSet.rootPath + '</td><td class="spectrum-Table-cell">' + filterSet.importMode + '</td></tr>';
            });
        } else {
                filters += '<tr class="spectrum-Table-row"><td class="spectrum-Table-cell">No paths found</td><td class="spectrum-Table-cell"></td></tr>';
        }

        template.querySelector('[slot="filters"]').innerHTML = filters;
        removeAllChildNodes(results);
        results.appendChild(template);
        document.querySelector('[data-tab="results"]').click();
    }

    function displayCreateSuccess(data) {
       var results = document.getElementById('results');

       var template = document.getElementById('success-results').content.cloneNode(true);

       var filters = '';
       if (data.filterSets && data.filterSets.length > 0) {
           data.filterSets.forEach(function(filterSet) {
                filters += '<tr class="spectrum-Table-row"><td class="spectrum-Table-cell">' + filterSet.rootPath + '</td><td class="spectrum-Table-cell">' + filterSet.importMode + '</td></tr>';
           });
        } else {
                filters += '<tr class="spectrum-Table-row"><td class="spectrum-Table-cell">No paths found</td><td class="spectrum-Table-cell"></td></tr>';
        }

        template.querySelector('[slot="filters"]').innerHTML = filters;
        template.querySelector('[slot="package-manager-link"]').innerHTML = data.path;
        template.querySelector('[slot="package-manager-link"]').setAttribute('href', '/crx/packmgr/index.jsp#' + data.path);

        removeAllChildNodes(results);
        results.appendChild(template);
        document.querySelector('[data-tab="results"]').click();
    }

    function displayError(message) {
       var results = document.getElementById('results');
       var template = document.getElementById('error-results').content.cloneNode(true);

        template.querySelector('[slot="message"]').innerHTML = message;
        removeAllChildNodes(results);
        results.appendChild(template);
        document.querySelector('[data-tab="results"]').click();
    }

    /* Handle action button clicks */
    document.getElementById('preview').addEventListener('click', execute);
    document.getElementById('create').addEventListener('click', execute);

    /* Handle tabs clicks */
    document.querySelectorAll('[data-tab]').forEach(function(tab) {
        tab.addEventListener('click', function(e) {
            var activeTab = e.target;
            document.querySelectorAll('[data-tab]').forEach(function(tabToDeselect) { tabToDeselect.parentElement.classList.remove('is-selected'); });
            document.querySelectorAll('[data-tab-content]').forEach(function(tabContentToHide) { tabContentToHide.style.display = 'none'; });

            activeTab.parentElement.classList.add('is-selected');
            document.querySelector('[data-tab-content="' + activeTab.dataset.tab + '"]').style.display = 'block';

            document.querySelector('[data-tab-selection-indicator]').style.left = activeTab.dataset.tabIndicator;
        });
    });


    function removeAllChildNodes(parent) {
        while (parent.firstChild) {
            parent.removeChild(parent.firstChild);
        }
    }
})();






