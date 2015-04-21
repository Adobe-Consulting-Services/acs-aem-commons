/*global CQ: false, ACS: false, JSON: false, self: false, top: false */

CQ.Ext.ns('ACS.CQ');

ACS.CQ.WCMViews = {
    INTERVAL: 0,
    SK_TAB_PANEL: 'cq-sk-tabpanel',
    WCM_VIEWS: 'WCM_VIEWS',

    updateSidekick: function () {
        var sidekick = CQ.WCM.getSidekick();

        function check(scope) {
            var sk = CQ.WCM.getSidekick();

            if (!sk) {
                // Wait for the CQ_Sidekick to get loaded in 1/4 second increments
                scope.INTERVAL = setTimeout(function() { check(scope); }, 250);
            } else if (!sk.panelsLoaded || !CQ.Ext.getCmp(ACS.CQ.WCMViews.SK_TAB_PANEL)) {
                // Wait for the CQ_Sidekick's panels to be loaded
                scope.INTERVAL = setTimeout(function() { check(scope); }, 100);
            } else {
                clearTimeout(scope.INTERVAL);
                ACS.CQ.WCMViews.addWCMViewsPanel(sk);
            }
        }
        
        check(this);
    },
    
    addWCMViewsPanel: function (sidekick) {
        var tabPanel,
            wcmViewsTab = CQ.Ext.getCmp('cq-sk-tab-WCM_VIEWS');
        
        if (!sidekick) {
            return;
        } else if (wcmViewsTab && wcmViewsTab.items && wcmViewsTab.length > 0) {
           // Nothing to do here
            return;
        }

        CQ.wcm.Sidekick.CONTEXTS.push(this.WCM_VIEWS);

        tabPanel = CQ.Ext.getCmp(this.SK_TAB_PANEL);

        (function () {
            CQ.shared.HTTP.get(sidekick.path + '.wcm-views.json',
                function (options, success, response) {
                    var json,
                        buttons = [];

                    json = JSON.parse(response.responseText);

                    /* Default */
                    buttons.push(new CQ.Ext.Button({
                        name: 'WCM_VIEWS_DEFAULT',
                        text: 'Default View',
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

                    /* Disabled */
                    buttons.push(new CQ.Ext.Button({
                        name: 'WCM_VIEWS_DISABLED',
                        text: 'Disable WCM Views',
                        context: CQ.wcm.Sidekick.WCM_VIEWS,
                        handler: function () {
                            CQ.Util.reload(CQ.WCM.getContentWindow(),
                                CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML + '?wcm-views=disabled'));
                        }
                    }));

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
    var uri = window.location.pathname,
        INTERVAL;
    
    if (uri.indexOf('/cf') === 0 
            || (uri.indexOf('/content/') === 0 && self === top)) {

        INTERVAL = setInterval(function () {
            var sidekick = CQ.WCM.getSidekick();
            
            if(sidekick) {
                clearInterval(INTERVAL);

                sidekick.addListener('loadcontent', function () {
                    ACS.CQ.WCMViews.updateSidekick();
                });
            }
        }, 25);
    }
}());