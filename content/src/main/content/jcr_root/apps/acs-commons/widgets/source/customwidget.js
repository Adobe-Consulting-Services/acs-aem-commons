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
var Ejst = {};

Ejst.toggleProperties = function(id, expand) {
    var box = CQ.Ext.get(id),
        arrow = CQ.Ext.get(id + '-arrow');
    if (expand || !box.hasClass('open')) {
        box.addClass('open');
        arrow.update('&laquo;');
    } else {
        box.removeClass('open');
        arrow.update('&raquo;');
    }
};

Ejst.expandProperties = function(comp) {
    comp.refresh();
    var id = comp.path.substring(comp.path.lastIndexOf('/')+1); 
    Ejst.toggleProperties(id, true);
};

Ejst.x2 = {};

/**
 * Manages the tabs of the specified tab panel. The tab with
 * the specified ID will be shown, the others are hidden.
 * @param {CQ.Ext.TabPanel} tabPanel The tab panel
 * @param {String} tab the ID of the tab to show
 */
Ejst.x2.manageTabs = function(tabPanel, tab) {
    var tabs=['selection','tab1','tab2','tab3','tab4','tab5','tab6','tab7'],
        index = tab ? tabs.indexOf(tab) : -1,
        i = 1;
//    if (index == -1) return;
    for (i; i !== tabs.length; i++) {
        if (index === i) {
            tabPanel.unhideTabStripItem(i);
        } else {
            tabPanel.hideTabStripItem(i);
        }
    }
    tabPanel.doLayout();
};

/**
 * Hides the specified tab.
 * @param {CQ.Ext.Panel} tab The panel
 */
Ejst.x2.hideTab = function(tab) {
    var tabPanel = tab.findParentByType('tabpanel'),
        index = tabPanel.items.indexOf(tab);
    tabPanel.hideTabStripItem(index);
};

/**
 * Shows the tab which ID matches the value of the specified field.
 * @param {CQ.Ext.form.Field} field The field
 */
Ejst.x2.showTab = function(field) {
    Ejst.x2.manageTabs(field.findParentByType('tabpanel'), field.getValue());
};

/**
 * Toggles the field set on the same tab as the check box.
 * @param {CQ.Ext.form.Checkbox} box The check box
 */
Ejst.x2.toggleFieldSet = function(box) {
    var panel = box.findParentByType('panel'),
        fieldSet = panel.findByType('fieldset')[0],
        show = box.getValue()[0];
    if (show) {
        fieldSet.show();
        
        panel.doLayout();
    } else {
        fieldSet.hide();
        fieldSet.items.each(function(field) {
            try {
                field.setValue();
            } catch (e) {
            }
        });
    }
};
