/** Source code from http://experience-aem.blogspot.in/2013/09/aem-cq-56-multifield-panel.html writen by Sreekanth Choudry Nalabotu
This is the exact implementation by Sreekanth

 **/
 /*global CQ: false, ACS: false */
CQ.Ext.ns("ACS.CQ");
/**
 * @class ACS.CQ.MultiFieldPanel
 * @extends CQ.form.Panel
 * <p>The MultiFieldPanel widget is a drop-in replacement for the normal multi fieldwidget with the additional functionality of being able to configure multiple fields 
 * </p>
 * @constructor
 * Creates a new MultiFieldPanel.
 * @param {Object} config The config object
 */
ACS.CQ.MultiFieldPanel = CQ.Ext.extend(CQ.Ext.Panel, {
    panelValue: '',

    constructor: function(config){
        config = config || {};
        ACS.CQ.MultiFieldPanel.superclass.constructor.call(this, config);
    },

    initComponent: function () {
        ACS.CQ.MultiFieldPanel.superclass.initComponent.call(this);

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
            if(i.xtype === "label" || i.xtype === "hidden" || !i.hasOwnProperty("dName")){
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
            if(i.xtype === "label" || i.xtype === "hidden" || !i.hasOwnProperty("dName")){
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

CQ.Ext.reg("multifieldpanel", ACS.CQ.MultiFieldPanel);	