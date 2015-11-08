/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

    addWCMViewsCookie: function(name) {
        CQ.HTTP.setCookie('acs-commons.wcm-views', name, '/', 100 * 365);
    },

    clearWCMViewsCookie: function() {
        CQ.HTTP.clearCookie('acs-commons.wcm-views', '/');
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
                            ACS.CQ.WCMViews.clearWCMViewsCookie();
                            
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
                                ACS.CQ.WCMViews.addWCMViewsCookie(view.value);

                                CQ.Util.reload(CQ.WCM.getContentWindow(),
                                    CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML));
                            }
                        }));
                    });

                    /* Disabled */
                    buttons.push(new CQ.Ext.Button({
                        name: 'WCM_VIEWS_DISABLED',
                        text: 'Disable WCM Views',
                        context: CQ.wcm.Sidekick.WCM_VIEWS,
                        handler: function () {
                            ACS.CQ.WCMViews.addWCMViewsCookie('disabled');

                            CQ.Util.reload(CQ.WCM.getContentWindow(),
                                CQ.HTTP.externalize(sidekick.path + CQ.HTTP.EXTENSION_HTML));
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
    
    if (uri.indexOf('/cf') === 0 ||
             (uri.indexOf('/content/') === 0 && self === top)) {

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