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
 *
 * A sample component dialog using the Touch UI Multi Field
 * Note the usage of empty valued acs-commons-nested property
 */

(function ($, $document) {
    "use strict";

    ACS.TouchUI.NodeCompositeMultiField = new Class({
        toString: 'ACS TouchUI Composite Multifield Store as Nodes',
        extend: ACS.TouchUI.CompositeMultiField,

        addDataInFields: function () {
        }
    });
}(jQuery, jQuery(document)));
