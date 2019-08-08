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
/*global CQ: false, ACS: false */
ACS.CQ.Util = ACS.CQ.Util || {};
ACS.CQ.Util.isVanityPathUnique = function(vanityPath, pagePath) {
    var response = CQ.HTTP.get("/bin/wcm/duplicateVanityCheck?vanityPath=" + vanityPath + "&pagePath=" + pagePath),
        paths = JSON.parse(response.responseText);

    return paths.length === 0;
};

CQ.Ext.apply(CQ.Ext.form.VTypes, {
    uniqueVanityPath: function(value, field) {
        var dialog = field.findParentByType("dialog");
        return ACS.CQ.Util.isVanityPathUnique(value, dialog.path);
    },
    uniqueVanityPathText: "The vanity path must be unique."
    
});

