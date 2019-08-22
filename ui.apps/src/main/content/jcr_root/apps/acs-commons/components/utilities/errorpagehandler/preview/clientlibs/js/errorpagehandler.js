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
 /*global CQ: false */
$(function() {
    $('#error-page-handler .toggle').click(function() {
        var $this = $(this),
        $section = $this.closest('.section');

        if($section.hasClass('collapsed')) {
            $section.removeClass('collapsed');
            $section.addClass('expanded');
            $this.text($this.data('collapse-text'));
        } else {
            $section.removeClass('expanded');
            $section.addClass('collapsed');
            $this.text($this.data('expand-text'));
        }
    });

    $('#error-page-handler .edit-mode').click(function () {
        CQ.WCM.setMode(CQ.WCM.MODE_EDIT);
        return true;
    });

    $('#error-page-handler .edit-error-page').click(function () {
        CQ.WCM.setMode(CQ.WCM.MODE_EDIT);
        return true;
    });
});
