/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

/*global CQ: false, window: false */

CQ.Ext.onReady(function () {

    var resultTemplate,
        statusListItemTemplate,
        rootPathsInputField,
        dateTimeInputField,
        serializeMultiValues,
        hideAll,
        buildAgentInfoHTML,
        showReplicationMsg,
        showReplicationAgentsInfo,
        showErrorMsg;

    /* Template */

    resultTemplate = new CQ.Ext.XTemplate(
        '<h4>Version replication initiated with the following items</h4>',
        '<ul>',
            '<tpl for="result">',
                '<li>{path} [ <strong>{status}</strong>',
                '{[version=""]}',
                '<tpl if="version !== \'\'">',
                        ' @ version <strong>{version}</strong>',
                '</tpl>',
                ' ]</li>',
            '</tpl>',
        '</ul>'
    );

    statusListItemTemplate = new CQ.Ext.XTemplate(
        '<li>',
            '<h3>{title}</h3>',
            '<ul>',
                '<li>',
                    '<a href="{logHref}" target="_blank">Replication Log</a>',
                '</li>',
                '<li>',
                    '<a href="{agentHref}" target="_blank">Replication Agent Config</a>',
                '</li>',
            '</ul>',
        '</li>'
    );

    /* ExtJS Input Fields */

    rootPathsInputField = new CQ.form.MultiField({
        renderTo: "cq-inject-rootpaths",
        fieldLabel: "Root Paths",
        addItemLabel: "Add a root path",
        name: "rootPaths",
        fieldConfig: {
            allowBlank: "false",
            predicate: "hierarchy",
            xtype: "pathfield"
        }
    });

    dateTimeInputField = new CQ.form.DateTime({
        renderTo: "cq-inject-datetime",
        hideTime: false,
        name: "datetimecal",
        allowBlank: false,
        hiddenFormat: "Y-m-d\\TH:i:s a",
        listeners: {
            render: function () {
                this.wrap.anchorTo("cq-inject-datetime", "tl");
            }
        }
    });

    /* Event Handlers */

    $('body').on('click', '#submit-button', function(e) {
        var $form = $('.cq-inline-form'),
            $agents = $form.find('select[name="cmbAgent"] option:selected'),
            requestTimeout,
            formData;

        // Don't submit the form
        e.preventDefault();

        // Clear all messaging HTML elements
        hideAll();

        formData = {
            rootPaths: serializeMultiValues($form.find('input[name="rootPaths"]')),
            datetimecal: $form.find('input[name="datetimecal"]').val(),
            cmbAgent:  serializeMultiValues($agents)
        };

        // Show agent list in result notifications/
        // Set a 1 second timeout to allow invalid input params to be caught and return.
        requestTimeout = setTimeout(function() {
            showReplicationAgentsInfo(buildAgentInfoHTML($agents));
        }, 1000);

        // Submit HTTP POST
        $.post($form.data('action'), formData, function(data) {

            showReplicationMsg(resultTemplate.apply(data));

        }).fail(function(data) {
            var json = $.parseJSON(data.responseText);

            clearTimeout(requestTimeout);

            showErrorMsg(json.error);
        }).always(function() {
            window.scrollTo(0,0);
        }, 'json');
    });

    /* Utility functions */

    buildAgentInfoHTML = function(agents) {
        var i,
            data,
            $agent,
            $html = $('<ul>');

        agents = agents || [];

        for(i = 0; i < agents.length; i++) {
            $agent = $(agents[i]);

            data = {
                title: $.trim($agent.text()),
                logHref: $agent.data('agent-path') + '.log.html#end',
                agentHref: $agent.data('agent-path') + '.html'
            };

            $html.append(statusListItemTemplate.apply(data));
        }

        return $html.html();
    };

    serializeMultiValues = function(el) {
        var array = [];

        el.each(function() {
            array.push($(this).val());
        });

        return array;
    };

    hideAll = function() {
        $('#error-message').hide();
        $('#results').hide();
    };

    showReplicationMsg = function(msg) {
        var $el = $('#replication-queue-message');

        $el.html(msg);

        $('#results').show();
    };

    showReplicationAgentsInfo = function(agentInfo) {
        var $el = $('#replication-agents-info');

        $el.find('.message').html(agentInfo);

        $('#results').show();
    };

    showErrorMsg = function(msg) {
        var $el = $('#error-message');

        hideAll();

        if(typeof msg === undefined || !msg) {
            msg = "A system error occurred.";
        }

        $el.find('.message').html(msg);
        $el.show();
    };
});