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
(function() {
    var Original = CQ.wcm.FeedImporter;

    CQ.wcm.FeedImporter = function(config) {
        var selections, selection, additions, parsedAdditions;

        CQ.wcm.FeedImporter.prototype.constructor.call(this, config);

        selections = this.newDialog.findBy(function() {
                return this.xtype === 'selection' && this.name === 'feedType';
            });
        if (selections.length === 1) {
            selection = selections[0];
            additions = CQ.shared.HTTP.get("/bin/acs-commons/custom-importers.json");
            if (additions && additions.body) {
                parsedAdditions = CQ.Ext.util.JSON.decode(additions.body);
                if (parsedAdditions.list) {
                    $.each(parsedAdditions.list, function(idx, addition) {
                        selection.options.push(addition);
                    });
                }
            }
            selection.setOptions(selection.options);
        }
    };
    CQ.wcm.FeedImporter.prototype = Original.prototype;
    CQ.wcm.FeedImporter.superclass = Original.superclass;

    CQ.Ext.reg("feedimporter", CQ.wcm.FeedImporter);
}());
