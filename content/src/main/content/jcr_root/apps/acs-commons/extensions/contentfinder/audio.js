{
    "tabTip": CQ.I18n.getMessage("Audio"),
    "id": "cfTab-Audio",
    "xtype": "contentfindertab",
    "iconCls": "cq-cft-tab-icon audio",
    "ranking": 25,
    "allowedPaths": [
        "/content/*",
        "/etc/scaffolding/*",
        "/etc/workflow/packages/*"
    ],
    "items": [
        CQ.wcm.ContentFinderTab.getQueryBoxConfig({
            "id": "cfTab-Audio-QueryBox",
            "items": [
                CQ.wcm.ContentFinderTab.getSuggestFieldConfig({"url": "/bin/wcm/contentfinder/suggestions.json/content/dam"})
            ]
        }),
        CQ.wcm.ContentFinderTab.getResultsBoxConfig({
            "itemsDDGroups": [CQ.wcm.EditBase.DD_GROUP_ASSET],
            "itemsDDNewParagraph": {
                "path": "acs-commons/components/content/audio",
                "propertyName": "./asset"
            },
            "tbar": [
                CQ.wcm.ContentFinderTab.REFRESH_BUTTON,
                "->",
                {
                    "toggleGroup": "cfTab-Audio-TG",
                    "enableToggle": true,
                    "toggleHandler": function(button, pressed) {
                        var tab = CQ.Ext.getCmp("cfTab-Audio");
                        if (pressed) {
                            tab.dataView.tpl = new CQ.Ext.XTemplate(CQ.wcm.ContentFinderTab.THUMBS_TEMPLATE);
                            tab.dataView.itemSelector = CQ.wcm.ContentFinderTab.THUMBS_ITEMSELECTOR;
                        }
                        if (tab.dataView.store != null) {
                            tab.dataView.refresh();
                        }
                    },
                    "pressed": true,
                    "allowDepress": false,
                    "cls": "cq-btn-thumbs cq-cft-dataview-btn",
                    "iconCls":"cq-cft-dataview-mosaic",
                    "tooltip": {
                        "text": CQ.I18n.getMessage("Mosaic View"),
                        "autoHide":true
                    }
                },
                {
                    "toggleGroup": "cfTab-Audio-TG",
                    "enableToggle": true,
                    "toggleHandler": function(button, pressed) {
                        var tab = CQ.Ext.getCmp("cfTab-Audio");
                        if (pressed) {
                            tab.dataView.tpl = new CQ.Ext.XTemplate(CQ.wcm.ContentFinderTab.DETAILS_TEMPLATE);
                            tab.dataView.itemSelector = CQ.wcm.ContentFinderTab.DETAILS_ITEMSELECTOR;
                        }
                        if (tab.dataView.store != null) {
                            tab.dataView.refresh();
                        }
                    },
                    "pressed": false,
                    "allowDepress": false,
                    "cls": "cq-btn-details",
                    "cls": "cq-btn-details cq-cft-dataview-btn",
                    "iconCls":"cq-cft-dataview-list",
                    "tooltip": {
                        "text": CQ.I18n.getMessage("List View"),
                        "autoHide": true
                    }
                }
            ]
        },{
            "url": "/bin/wcm/contentfinder/asset/view.json/content/dam"
        }, {
            "baseParams": {
                "mimeType": "audio"
            }
        })
    ]
}