/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
CQ.Ext.ns("ACS.CQ"); 

/**
 * @class ACS.CQ.MultiFieldPanel
 * @extends CQ.form.Panel
 * <p>The MultiFieldPanel widget is a replacement for the normal multifield widget which
 * supports multiple structures in a single JCR property. It does this by storing a set of
 * key/value pairs serialized as a JSON object. The keys for each pair is defined by setting the
 * 'key' property on the field.</p>
 */

ACS.CQ.MultiFieldPanel = CQ.Ext.extend(CQ.Ext.Panel, {
    panelValue: '',

    /**
     * @constructor
     * Creates a new MultiFieldPanel.
     * @param {Object} config The config object
     */
    constructor: function(config){
        config = config || {};
        if (!config.layout) {
            config.layout = 'form';
            config.padding = '10px';
        }
        ACS.CQ.MultiFieldPanel.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        ACS.CQ.MultiFieldPanel.superclass.initComponent.call(this);

        var multifield = this.findParentByType('multifield'),
            dialog = this.findParentByType('dialog');

        if(ACS.CQ.MultiFieldPanel.xtype === this.xtype){
            this.panelValue = new CQ.Ext.form.Hidden({
                name: this.name
            });

            this.add(this.panelValue);

            dialog.on('beforesubmit', function(){
                var value = this.getValue();

                if (value){
                    this.panelValue.setValue(value);
                }
            }, this);
        }

        dialog.on('loadcontent', function(){
            if(_.isEmpty(multifield.dropTargets)){
                multifield.dropTargets = [];
            }

            multifield.dropTargets = multifield.dropTargets.concat(this.getDropTargets());

            _.each(multifield.dropTargets, function(target){
                if (!target.highlight) {
                    return;
                }

                var dialogZIndex = parseInt(dialog.el.getStyle("z-index"), 10);

                if (!isNaN(dialogZIndex)) {
                    target.highlight.zIndex = dialogZIndex + 1;
                }
            });
        }, this);

        if(dialog.acsInit){
            return;
        }

        var tabPanel = multifield.findParentByType("tabpanel");

        if(tabPanel){
            tabPanel.on("tabchange", function(panel){
                panel.doLayout();
            });
        }

        dialog.on('hide', function(){
            var editable = CQ.utils.WCM.getEditables()[this.path];

            //dialog caching is a real pain when there are multifield items; remove from cache
            delete editable.dialogs[CQ.wcm.EditBase.EDIT];
            delete CQ.WCM.getDialogs()["editdialog-" + this.path];
        }, dialog);

        dialog.acsInit = true;
    },

    afterRender : function(){
        ACS.CQ.MultiFieldPanel.superclass.afterRender.call(this);

        this.items.each(function(){
            if(!this.contentBasedOptionsURL ||
                this.contentBasedOptionsURL.indexOf(CQ.form.Selection.PATH_PLACEHOLDER) < 0){
                return;
            }

            this.processPath(this.findParentByType('dialog').path);
        });
    },

    getValue: function() {
        var pData = {};

        this.items.each(function(i){
            if(i.xtype === "label" || i.xtype === "hidden" || !i.hasOwnProperty("key")){
                return;
            }
            pData[i.key] = i.getValue();
        });

        return $.isEmptyObject(pData) ? "" : JSON.stringify(pData);
    },

    setValue: function(value) {
        this.panelValue.setValue(value);

        var pData = JSON.parse(value);

        this.items.each(function(i){
            if(i.xtype === "label" || i.xtype === "hidden" || !i.hasOwnProperty("key")){
                return;
            }

            i.setValue(pData[i.key]);

            i.fireEvent('loadcontent', this);
        });
    },

    getDropTargets : function() {
        var targets = [], t;

        this.items.each(function(){
            if(!this.getDropTargets){
                return;
            }

            t = this.getDropTargets();

            if(_.isEmpty(t)){
                return;
            }

            targets = targets.concat(t);
        });

        return targets;
    },

    validate: function(){
        var valid = true;

        this.items.each(function(i){
            if(!i.hasOwnProperty("key")){
                return;
            }

            if(!i.isVisible()){
                i.allowBlank = true;
                return;
            }

            if(!i.validate()){
                valid = false;
            }
        });

        return valid;
    },

    getName: function(){
        return this.name;
    }
});

CQ.Ext.reg("multifieldpanel", ACS.CQ.MultiFieldPanel);