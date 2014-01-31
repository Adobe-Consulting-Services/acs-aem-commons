/** from http://experience-aem.blogspot.in/2013/09/aem-cq-56-multifield-panel.html writen by Sreekanth Choudry Nalabotu
Todo - This has to be separated out from column control component and saved in the location specific to cq widgets.
 **/
var AEMCommonsClientLib = AEMCommonsClientLib || {};

AEMCommonsClientLib.MultiFieldPanel = CQ.Ext.extend(CQ.Ext.Panel, {
    panelValue: '',

    constructor: function(config){
        config = config || {};
        AEMCommonsClientLib.MultiFieldPanel.superclass.constructor.call(this, config);
    },

    initComponent: function () {
        AEMCommonsClientLib.MultiFieldPanel.superclass.initComponent.call(this);

        this.panelValue = new CQ.Ext.form.Hidden({
            name: this.name
        });

        this.add(this.panelValue);

        var dialog = this.findParentByType('dialog');

        dialog.on('beforesubmit', function(){
            var value = this.getValue();

            if(value){
                this.panelValue.setValue(value);
            }
        },this);
    },

    getValue: function () {
        var pData = {};

        this.items.each(function(i){
            if(i.xtype == "label" || i.xtype == "hidden" || !i.hasOwnProperty("dName")){
                return;
            }

            pData[i.dName] = i.getValue();
        });

        return $.isEmptyObject(pData) ? "" : JSON.stringify(pData);
    },

    setValue: function (value) {
        this.panelValue.setValue(value);

        var pData = JSON.parse(value);

        this.items.each(function(i){
            if(i.xtype == "label" || i.xtype == "hidden" || !i.hasOwnProperty("dName")){
                return;
            }

            if(!pData[i.dName]){
                return;
            }

            i.setValue(pData[i.dName]);
        });
    },

    validate: function(){
        return true;
    },

    getName: function(){
        return this.name;
    }
});

CQ.Ext.reg("multifieldpanel", AEMCommonsClientLib.MultiFieldPanel);