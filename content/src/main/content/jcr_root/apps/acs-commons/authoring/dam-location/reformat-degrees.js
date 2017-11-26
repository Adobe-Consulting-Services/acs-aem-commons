/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2016 Adobe
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
(function($) {
    var REGEX = /(\d+),(\d+)\.(\d+)([NEWS])/;
    $(function() {
        $(".acs-dam-location-degrees input[disabled]").each(function(i, el) {
            var $el = $(el),
                match = REGEX.exec($el.val());

            if (match) {
                $el.val(match[1] + "° " + match[2] + "' " + match[3] + '" ' + match[4]);
            }
        });
    });
}(jQuery));