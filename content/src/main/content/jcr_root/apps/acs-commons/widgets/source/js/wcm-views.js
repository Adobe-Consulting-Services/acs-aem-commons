/*global CQ: false, ACS: false */
CQ.Ext.ns("ACS.CQ");

ACS.CQ.WCMViews = {
    SK_TAB_PANEL: "cq-sk-tabpanel",
    WCM_VIEWS: "WCM_VIEWS",

    addTagsPanel: function (sidekick) {
        var CONTEXTS = CQ.wcm.Sidekick.CONTEXTS,
            tabPanel,
            buttons;

        if (!sidekick) {
            return;
        } else if (($.inArray(this.WCM_VIEWS, CONTEXTS) !== -1)
                || sidekick.panels[this.WCM_VIEWS]) {
            return;
        }

        CONTEXTS.push(this.WCM_VIEWS);

        tabPanel = sidekick.findById(this.SK_TAB_PANEL);

        buttons = [
            new CQ.Ext.Button({
                name: "ONE",
                text: "One",
                context: CQ.wcm.Sidekick.WCM_VIEWS/*,
                handler: function () {
                    CQ.Util.reload(CQ.WCM.getContentWindow(),
                        CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML + "?wcm-views=foo"));
                }                                   */

            }),
            new CQ.Ext.Button({
                name: "TWO",
                text: "Two",
                context: CQ.wcm.Sidekick.WCM_VIEWS

            }),
            new CQ.Ext.Button({
                name: "THREE",
                text: "Three",
                context: CQ.wcm.Sidekick.WCM_VIEWS

            })
        ];

        sidekick.panels[this.WCM_VIEWS] = new CQ.Ext.Panel({
            "border": false,
            "autoScroll": true,
            "layout": "column",
            items: buttons,
            "id": "cq-sk-tab-" + this.WCM_VIEWS
        });

        tabPanel.add({
            "tabTip": "Tags",
            "iconCls": "cq-sidekick-tab cq-cft-tab-icon full",
            "items": sidekick.panels[this.WCM_VIEWS],
            "layout": "fit"
        });

        sidekick.doLayout();
    }
};

(function () {
    var sidekick,
        SK_INTERVAL,
        uri = window.location.pathname;

    if (uri.indexOf("/cf") === 0 || uri.indexOf("/content") === 0) {
        sidekick = ACS.CQ.WCMViews;

        SK_INTERVAL = setInterval(function () {
            var sk = CQ.WCM.getSidekick();

            if (sk && sk.findById(sidekick.SK_TAB_PANEL)) {
                clearInterval(SK_INTERVAL);
                sidekick.addTagsPanel(sk);
            }
        }, 250);
    }
}());