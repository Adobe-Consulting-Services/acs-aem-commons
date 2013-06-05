/*global CQ: false, ACS: false */

/**
 * Search predicate form widget which uses a selection to select a property
 * value.
 */
ACS.CQ.wcm.SelectionPredicate = CQ.Ext.extend(CQ.form.CompositeField, {

    /**
     * @cfg {String} propertyName Name of the property.
     */
    propertyName : null,

    /**
     * @cfg {String} predicateName Name of the predicate. Defaults to
     *      'property'.
     */
    predicateName : null,

    /**
     * @cfg {String} options
     */
    options : null,

    constructor : function(config) {
        config = config || {};
        var defaults = {
            "border" : false,
            "predicateName" : "property"
        };
        config = CQ.Util.applyDefaults(config, defaults);
        ACS.CQ.wcm.SelectionPredicate.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        ACS.CQ.wcm.SelectionPredicate.superclass.initComponent.call(this);

        var id = CQ.wcm.PredicateBase.createId(this.predicateName);

        this.add(new CQ.Ext.form.Hidden({
            "name" : id,
            "value" : this.propertyName
        }));

        this.add(new CQ.form.Selection({
            "type" : "select",
            "name" : id + ".value",
            "options" : this.options,
            "triggerClass" : "",
            "hideLabel" : true,
            "anchor" : "100%"
        }));
    }

});
CQ.Ext.reg("selectionpredicate", ACS.CQ.wcm.SelectionPredicate);
