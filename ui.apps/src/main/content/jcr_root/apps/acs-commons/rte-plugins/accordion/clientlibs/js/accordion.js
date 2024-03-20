/*
 * #%L
 * ACS AEM Commons Package
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
$(function() {
    // Trigger the show/hide on click of the accordion headline
    $( document ).on('click', '.acs-commons-rte-accordion .accordion-header', function(){
        var selected = $(this),
            parent = selected.parents('.acs-commons-rte-accordion');

        //Expand or collapse this panel
        selected.next().slideToggle('fast');
        selected.parent().toggleClass('active');

        //Hide the other panels
        $( 'li', parent ).not(selected.parent()).removeClass('active');
        $( '.accordion-content', parent ).not(selected.next()).slideUp('fast');
    });
});