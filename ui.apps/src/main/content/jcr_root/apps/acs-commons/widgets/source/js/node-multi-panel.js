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
 * @class ACS.CQ.NodeMultiFieldPanel
 * @extends ACS.CQ.MultiFieldPanel
 * <p>The NodeMultiFieldPanel widget is a extension for multifield widget which
 * supports multiple structures. It does this by storing the items as nodes in JCR
 */

ACS.CQ.NodeMultiFieldPanel = CQ.Ext.extend(ACS.CQ.MultiFieldPanel, {
    setValuesInChildNode: function (items, prefix, counter){
        items.each(function(i){
            if(!i.hasOwnProperty("key")){
                return;
            }

            i.name = prefix + "/" + (counter) + "/" + i.key;

            if(i.hiddenField){
                i.hiddenField.name = prefix + "/" + (counter) + "/" + i.key;
            }

            if(i.el && i.el.dom){ //form serialization workaround
                i.el.dom.name = prefix + "/" + (counter) + "/" + i.key;
            }
        },this);
    },

    initComponent: function () {
        ACS.CQ.NodeMultiFieldPanel.superclass.initComponent.call(this);

        var multi = this.findParentByType("multifield"),
            dialog = this.findParentByType('dialog'),
            multiPanels = multi.findByType("nodemultifieldpanel");

        dialog.removeListener("beforesubmit", this.setValuesAsJson, dialog);

        this.setValuesInChildNode(this.items, this.name, multiPanels.length + 1);

        multi.on("removeditem", function(){
            multiPanels = multi.findByType("nodemultifieldpanel");

            for(var x = 1; x <= multiPanels.length; x++){
                this.setValuesInChildNode(multiPanels[x-1].items, multiPanels[x-1].name, x);
            }
        }, this);
    },

    getValue: function () {
        var pData = {};

        this.items.each(function(i){
            if(!i.hasOwnProperty("key")){
                return;
            }

            pData[i.key] = i.getValue();
        });

        return pData;
    },

    setValue: function (value) {
        var counter = 1, item,
            multi = this.findParentByType("multifield"),
            multiPanels = multi.findByType("nodemultifieldpanel");

        if(multiPanels.length === 1){
            item = value[counter];
        }else{
            item = value;
        }

        this.items.each(function(i){
            if(!i.hasOwnProperty("key")){
                return;
            }

            i.setValue(item[i.key]);

            i.fireEvent('loadcontent', this);
        });

        if(multiPanels.length === 1){
            while(true){
                item = value[++counter];

                if(!item){
                    break;
                }

                multi.addItem(item);
            }
        }
    }
});

CQ.Ext.reg("nodemultifieldpanel", ACS.CQ.NodeMultiFieldPanel);
