/*
 * #%L
 * ACS AEM Commons Package
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
/*global CQ: false, ACS: false */
CQ.Ext.ns("ACS.CQ.GenericListItem");

ACS.CQ.GenericListItem.addTitleFields = function(panel) {
    CQ.HTTP.get("/etc/tags.json", function(options, success, response) {
        var obj, fieldset, allLanguages = CQ.I18n.getLanguages();
        if (success) {
            obj = CQ.Ext.util.JSON.decode(response.responseText);
            if (obj.languages) {
                fieldset = {
                    xtype: 'fieldset',
                    title: 'Localization',
                    defaultType: 'textfield',
                    collapsible: true,
                    defaults: {
                        anchor: '-20'
                    },
                    items:[]
                };
                CQ.Ext.each(obj.languages, function(lang) {
                    var langInfo = allLanguages[lang];
                    if (langInfo) {
                        fieldset.items.push({
                            fieldLabel : langInfo.title,
                            name : './jcr:title.' + lang
                        });
                    }
                });
                if (!CQ.Ext.isEmpty(fieldset.items)) {
                    panel.add(fieldset);
                }
            }
        }
    });
};
