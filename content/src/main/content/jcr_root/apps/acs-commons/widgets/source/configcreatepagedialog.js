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