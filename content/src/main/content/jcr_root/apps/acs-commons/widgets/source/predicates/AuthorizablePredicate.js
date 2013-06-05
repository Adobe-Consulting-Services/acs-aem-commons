/*global CQ: false, ACS: false */

/**
 * Search predicate form widget which looks up an authorizable.
 */
ACS.CQ.wcm.AuthorizablePredicate = CQ.Ext.extend(CQ.form.CompositeField, {

    /**
     * @cfg {String} propertyName Name of the property. Defaults to
     *      'jcr:content/jcr:lastModifiedBy'.
     */
    propertyName : null,

    /**
     * @cfg {String} predicateName Name of the predicate. Defaults to
     *      'property'.
     */
    predicateName : null,
    
    /**
     * @cfg {String} filter can have either the value "groups" or "users".
     * If set to "groups" only, Groups are searched, "users" only
     * searches for Users. (defaults to users)
     */
    filter : null,

    constructor : function(config) {
        config = config || {};
        var defaults = {
            "border" : false,
            "predicateName" : "property",
            "propertyName" : "jcr:content/jcr:lastModifiedBy",
            "fieldLabel" : CQ.I18n.getMessage("Last Modified By"),
            "filter" : "users"
        };
        config = CQ.Util.applyDefaults(config, defaults);
        ACS.CQ.wcm.AuthorizablePredicate.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        ACS.CQ.wcm.AuthorizablePredicate.superclass.initComponent.call(this);

        var id = CQ.wcm.PredicateBase.createId(this.predicateName);

        this.add(new CQ.Ext.form.Hidden({
            "name" : id,
            "value" : this.propertyName
        }));

        this.add(new CQ.security.AuthorizableSelection({
            "name" : id + ".value",
            "hideLabel" : true,
            "anchor" : "100%",
            "valueField" : "id",
            "displayField" : "name",
            "filter" : this.filter
        }));
    }

});
CQ.Ext.reg("authorizablepredicate", ACS.CQ.wcm.AuthorizablePredicate);
