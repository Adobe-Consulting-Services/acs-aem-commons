/*global CQ: false, ACS: false, JSON: false, console: false; self: false, top: false */

CQ.Ext.ns('ACS.CQ');

ACS.CQ.WCMViews = {
    SK_TAB_PANEL: 'cq-sk-tabpanel',
    WCM_VIEWS: 'WCM_VIEWS',

    addWCMViewsPanel: function (sidekick) {
        var tabPanel;

        if (!sidekick) {
            return;
        } else if (($.inArray(this.WCM_VIEWS, CQ.wcm.Sidekick.CONTEXTS) !== -1)
            || sidekick.panels[this.WCM_VIEWS]) {
            return;
        }

        CQ.wcm.Sidekick.CONTEXTS.push(this.WCM_VIEWS);

        tabPanel = sidekick.findById(this.SK_TAB_PANEL);

        (function () {
            CQ.shared.HTTP.get(sidekick.path + '.wcm-views.json',
                function (options, success, response) {
                    var json,
                        buttons = [];

                    json = JSON.parse(response.responseText);

                    /* Disabled */
                    buttons.push(new CQ.Ext.Button({
                        name: 'WCM_VIEWS_DISABLED',
                        text: 'Disabled',
                        context: CQ.wcm.Sidekick.WCM_VIEWS,
                        handler: function () {
                            CQ.Util.reload(CQ.WCM.getContentWindow(),
                                CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML + '?wcm-views=disabled'));
                        }
                    }));

                    /* Default */
                    buttons.push(new CQ.Ext.Button({
                        name: 'WCM_VIEWS_DEFAULT',
                        text: 'Default',
                        context: CQ.wcm.Sidekick.WCM_VIEWS,
                        handler: function () {
                            CQ.Util.reload(CQ.WCM.getContentWindow(),
                                CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML));
                        }
                    }));

                    $.each(json, function (index, view) {

                        buttons.push(new CQ.Ext.Button({
                            name: 'WCM_VIEWS_' + view.value.toUpperCase(),
                            text: view.title,
                            context: CQ.wcm.Sidekick.WCM_VIEWS,
                            handler: function () {
                                CQ.Util.reload(CQ.WCM.getContentWindow(),
                                    CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML + '?wcm-views=' + view.value));
                            }
                        }));

                    });

                    sidekick.panels[this.WCM_VIEWS] = new CQ.Ext.Panel({
                        border: false,
                        autoScroll: true,
                        layout: 'column',
                        items: buttons,
                        id: 'cq-sk-tab-WCM_VIEWS',
                        cls: 'cq-sidekick-buttons'
                    });

                    tabPanel.add({
                        tabTip: 'WCM Views',
                        iconCls: 'cq-sidekick-tab cq-sidekick-tab-icon-wcm-views full',
                        items: sidekick.panels[this.WCM_VIEWS],
                        layout: 'fit'
                    });

                    sidekick.doLayout();

                }
            );
        }());
    }
};

(function () {
    var sidekick,
        SK_INTERVAL,
        uri = window.location.pathname;

    if (uri.indexOf("/cf") === 0
        || (self === top && uri.indexOf("/content") === 0)) {
        sidekick = ACS.CQ.WCMViews;

        SK_INTERVAL = setInterval(function () {
            var sk = CQ.WCM.getSidekick();

            if (sk && sk.findById(sidekick.SK_TAB_PANEL)) {
                clearInterval(SK_INTERVAL);
                sidekick.addWCMViewsPanel(sk);
            }
        }, 250);
    }
}());