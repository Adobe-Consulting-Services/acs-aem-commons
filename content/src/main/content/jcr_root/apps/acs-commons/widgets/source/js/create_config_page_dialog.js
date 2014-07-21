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
(function(){
    //the original create page dialog fn
    var cqCreatePageDialog = CQ.wcm.Page.getCreatePageDialog;
 
    //override ootb function and add description field
    CQ.wcm.Page.getCreatePageDialog = function(parentPath){
       
        //create dialog by executing the product function
        var dialog = cqCreatePageDialog(parentPath),panel,columns,cmdField;
        if(parentPath.indexOf("/config")!==-1){
            
       
        //make necessary UI changes to the dialog created above
         panel = dialog.findBy(function(comp){
            return comp["jcr:primaryType"] === "cq:Panel";
        }, dialog);
 
        if(panel && panel.length > 0){
             columns = new CQ.form.MultiField({
                "fieldLabel": "columns",
                "name": "columns",
                fieldConfig:{xtype:"textfield"}
            });
 
            panel[0].insert(2,columns);
            panel[0].doLayout();
 
            dialog.params.cmd = "createConfigPage";
 
             cmdField = dialog.formPanel.findBy(function(comp){
                return comp.name === "cmd";
            }, dialog.formPanel);
 
            cmdField[0].setValue("createConfigPage");
        }
        }
        return dialog;
    };
}());