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
CQ.Ext.ns("ACS.CQ.ColumnControl");

ACS.CQ.ColumnControl.validateDialog = function(dialog) {
    var fields = dialog.findByType("numberfield"),
        expectedTotal = 100,
        width = 0;

    CQ.Ext.each(fields, function(field) {
        width += field.getValue();
    });

    if (width !== expectedTotal) {
        CQ.Ext.Msg.show({
            title:'Validation Error',
            msg:'Total width of all columns needs to be exactly 100 percent!',
            buttons: CQ.Ext.MessageBox.OK,
            icon:CQ.Ext.MessageBox.ERROR
            });
        return false;
    }
    return true;
};
