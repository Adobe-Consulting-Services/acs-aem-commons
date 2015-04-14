/*global CQ: false, ACS: false, JSON: false, console: false */

CQ.Ext.ns('ACS.CQ');

ACS.CQ.WCMViews = {
    SK_TAB_PANEL: 'cq-sk-tabpanel',
    WCM_VIEWS: 'WCM_VIEWS',

    addTagsPanel: function (sidekick) {
        var CONTEXTS = CQ.wcm.Sidekick.CONTEXTS,
            tabPanel,
            getWCMViews;

        if (!sidekick) {
            return;
        } else if (($.inArray(this.WCM_VIEWS, CONTEXTS) !== -1)
            || sidekick.panels[this.WCM_VIEWS]) {
            return;
        }

        CONTEXTS.push(this.WCM_VIEWS);

        tabPanel = sidekick.findById(this.SK_TAB_PANEL);

        getWCMViews = function () {
            CQ.shared.HTTP.get(sidekick.path + '.wcm-views.json',
                function (options, success, response) {
                    var json,
                        buttons = [];

                    console.log(response);

                    json = JSON.parse(response.responseText);
                    
                    $.each(json, function (index, view) {

                        var button = new CQ.Ext.Button({
                            name: 'WCM_VIEWS_' + view.value.toUpperCase(),
                            text: view.title,
                            context: CQ.wcm.Sidekick.WCM_VIEWS,
                            handler: function () {
                                CQ.Util.reload(CQ.WCM.getContentWindow(),
                                    CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML + '?wcm-views=' + view.value));
                            }
                        });

                        buttons.push(button);
                    });

                    console.log('adding panels');

                    sidekick.panels[this.WCM_VIEWS] = new CQ.Ext.Panel({
                        "border": false,
                        "autoScroll": true,
                        "layout": 'column',
                        items: buttons,
                        "id": 'cq-sk-tab-' + this.WCM_VIEWS
                    });
                    
                    tabPanel.add({
                        "tabTip": 'WCM Views',
                        "iconCls": 'cq-sidekick-tab cq-cft-tab-icon full',
                        "items": sidekick.panels[this.WCM_VIEWS],
                        "layout": 'fit'
                    });

                    sidekick.doLayout();

                }
            );
        };

        getWCMViews();
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