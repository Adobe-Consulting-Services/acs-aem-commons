/*global CQ: false , ACS: false */
CQ.Ext.ns("ACS.CQ.activation");
ACS.CQ.activation.AssetReferenceSearchDialog = CQ.Ext.extend(CQ.wcm.AssetReferenceSearchDialog,{
    constructor: function(config){
        config = config || {};
        this.dataPath = config.dataPath;
        if (!this.dataPath) {
            this.dataPath = "/libs/wcm/core/content/reference.json";
        }
        this.path = config.path;

        var colActivate = new CQ.wcm.AssetReferenceSearchDialog.CheckColumn({
            id: 'publish',
            header: CQ.I18n.getMessage("(Re)Publish"),
            dataIndex: 'publish',
            width: 80
        }),cm = new CQ.Ext.grid.ColumnModel([
            colActivate,
            {
                id:'name',
                header: CQ.I18n.getMessage("Name"),
                dataIndex: 'name',
                renderer: function(v, params, rec) {
                    var p = rec.get("path");

                    return '<span ext:qtip="' + p + '">' + v + '</span>';

                    // nice tag label display variant
                    //var isTag = rec.get("type") === "tag";
                    //if (isTag) {
                    //    return '<div class="taglabel" ext:qtip="' + p + '">' + CQ.tagging.TagLabel.createLabelHtml(v, true) + '</div>';
                    //} else {
                    //    return '<span ext:qtip="' + p + '">' + v + '</span>';
                    //}
                }
            }, {
                id:'status',
                header: CQ.I18n.getMessage("Status on Publish"),
                dataIndex: 'status',
                width: 120,
                renderer: function(v, params, rec) {
                    return v === "outdated" ? CQ.I18n.getMessage("Outdated") :
                           CQ.I18n.getMessage("Not available");
                }
            }, {
                header: CQ.I18n.getMessage("Type"),
                dataIndex: 'type',
                hidden: true,
                groupRenderer: function(v) {
                    switch (v) {
                        case "asset":   return CQ.I18n.getMessage("Assets");
                        case "tag":     return CQ.I18n.getMessage("Tags");
                        case "config":  return CQ.I18n.getMessage("Configurations");
                        case "campaign":return CQ.I18n.getMessage("Campaigns (will activate experiences and teasers as well)");
                        case "product": return CQ.I18n.getMessage("Products");
                        case "designpage": return CQ.I18n.getMessage("Designs");
                        default:        return CQ.I18n.getMessage("Unknown");
                    }
                }
            }
        ]),url,hmd,grid;

        // by default columns are sortable
        cm.defaultSortable = true;
         url = CQ.HTTP.addParameter(this.dataPath, "path", this.path);
        url = CQ.HTTP.addParameter(url, "_charset_", "UTF-8");

         hmd = this;
        this.store = new CQ.Ext.data.GroupingStore({
            reader: new CQ.Ext.data.JsonReader({
                "root": "assets",
                "fields": [{
                    name: "type"
                },{
                    name: "name"
                },{
                    name: "path"
                },{
                    name: "published"
                },{
                    name: "outdated"
                },{
                    name: "status"
                },{
                    name: "publish",
                    defaultValue: true
                },{
                    name: "disabled"
                }]
            }),
            "url": url,
            "groupField": "type",
            "sortInfo": {field: "type", direction: "ASC"}, // required for groupField...
            "listeners":{
                "beforeload":function() {
                    hmd.loadingRefs = true;
                },
                "load":function() {
                    hmd.loadingRefs = false;
                    var dlg = this;
                    if (this.getTotalCount() === 0) {
                        hmd.callback(hmd.path);
                    } else {
                        hmd.show();
                    }
                }
            }
        });

         grid = new CQ.Ext.grid.GridPanel({
            store: this.store,
            stateful: false,
            cm: cm,
            flex: 1,
            disableSelection: true,
            enableHdMenu: false,
            autoExpandColumn: 'name',
            clicksToEdit:1,
            loadMask:{
                msg:CQ.I18n.getMessage("Loading references...")
            },
            view: new CQ.Ext.grid.GroupingView({
                groupTextTpl: "{group}" // uses the groupRenderer func in the type Column
            })
        });

        colActivate.init(grid);

        // overwrite config
        config = CQ.Util.applyDefaults(config, {
            "xtype": "dialog",
            "title":CQ.I18n.getMessage("Activate"),
            "formUrl":"/bin/replicate.json",
            "items": {
                "xtype": "panel",
                "layout": "vbox",
                "layoutConfig": {
                    "padding": "0 15 0 0"
                },
                "items": [{
                    "xtype": "static",
                    "height": 30,
                    "cls": "x-form-item-description",
                    "text": CQ.I18n.getMessage('The following items need to be published along with the selected page(s):')
                },
                grid]
            },
            "listeners": {
                "render":function() {
                    grid.getStore().load();
                }
            },
            "callback": function(paths) {
                CQ.Log.WARN("No callback defined");
            }
        });
     
        ACS.CQ.activation.AssetReferenceSearchDialog.superclass.constructor.call(this,config);
        
       /* var dialog = new CQ.wcm.AssetReferenceSearchDialog(config);
        var gridpanel =  dialog.findBy(function(comp){
            return comp["jcr:primaryType"] === "cq:Panel";
        }, dialog);
        */
    }
}
);
(function(global) {
var activatePage = CQ.wcm.SiteAdmin.activatePage;
CQ.wcm.SiteAdmin.activatePage = function(){
    var paths = [],selections = this.getSelectedPages(),data,i,dialog;
    for ( i = 0; i < selections.length; i++) {
        paths.push(selections[i].id);
    }
     data = {
            id: CQ.Util.createId("cq-asset-reference-search-dialog"),
            path: paths,
            callback: function(p) {
                
            }
        };
   
        dialog = new ACS.CQ.activation.AssetReferenceSearchDialog(data);
       
};
}(window));
