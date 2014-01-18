<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 - 2014 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
<%@ page contentType="text/html" pageEncoding="utf-8"
    import="com.day.cq.i18n.I18n,com.day.cq.widget.HtmlLibraryManager,com.day.cq.replication.AgentManager,
 java.util.Collection,com.day.cq.replication.Agent"%>
<%@include file="/libs/foundation/global.jsp"%>
<%
    I18n i18n = new I18n(slingRequest);
    final AgentManager agentManager = sling
            .getService(AgentManager.class);
    Collection<Agent> agents = agentManager.getAgents().values();
%>

<h1><%=i18n.get("Replicate Earlier Versions")%></h1>
<div id="container"
    style="position: relative; left: 100px; width: 600px;">
    <div id="errmsg" style="display: none; background-color: #EFCDC7"></div>


    <label for="multifieldpaths"><%=i18n.get("Root Paths")%>:</label>
    <div id="CQ"></div>

    <input type="hidden" id="cal" name="cal" value=""> <label
        for="datetimecal"><%=i18n.get("Enter the date time")%>:</label>
    <div id="datetimecal">&nbsp;</div>
    <div style="margin-top: 20px">
        <label for="agentId"><%=i18n
                    .get("Replication Agent(you can select more than one)")%>:</label>
    </div>
    <div>
        <select id="cmbAgent" name="cmbAgent" size="<%=agents.size()%>"
            multiple>

            <%
                for (final Agent agent : agents) {
                    if (agent.isEnabled() && agent.isValid()) {
            %>
            <option
                style="width:<%=agent.getConfiguration().getName().length() * 7%>px"
                value="<%=agent.getId()%>"><%=agent.getConfiguration().getName()%></option>

            <%
                }
                }
            %>
        </select>
        <%
            for (final Agent agent : agentManager.getAgents().values()) {
                if (agent.isEnabled() && agent.isValid()) {
        %>
        <input type="hidden" id="<%=agent.getId()%>"
            value="<%=resourceResolver.map(agent.getConfiguration()
                            .getConfigPath())%>" />
        <%
            }
            }
        %>
    </div>

    <div style="margin-top: 20px">
        <input type="button" value="<%=i18n.get("Replicate")%>"
            id="btnReplicate" name="btnReplicate">
    </div>

    <br>
    <div id="replicationqueueMsg"></div>
    <div id="replicationqueueStatus" style="margin-top: 20px"></div>
</div>
<script>
CQ.Ext.onReady(function() {
$("#btnReplicate").click(
function() {
                    var msg = "Process initiated.......";
                    $("#replicationqueueMsg").html(msg);
                    $("#errmsg").html("");
                    $("#errmsg").css("display", "none");
                    var agentList = "<ul>";
                    $("#cmbAgent option:selected").each(
                            function() {

                                agentList += "<h3>" + $(this).text()
                                        + "</h3><li><a href='"
                                        + $("#" + $(this).val()).val()
                                        + ".log.html#end' target='_new'>" + "log"
                                        + "</a></li>";
                                agentList += "<li><a href='"
                                        + $("#" + $(this).val()).val()
                                        + ".html' target='_new'>"
                                        + "replication queue"
                                        + "</a></li><br/>";

                            });
                    $("#replicationqueueStatus").html(agentList);

                    $.post("<%=currentPage.getPath()%>
    .replicateversion.html",
                                                    buildRequestParams(),
                                                    function(resp) {
                                                        if (resp == undefined
                                                                || resp.status == 'error') {
                                                            $("#errmsg").html(
                                                                    resp.error);
                                                            $("#errmsg").css(
                                                                    "display",
                                                                    "block");
                                                            $(
                                                                    "#replicationqueueMsg")
                                                                    .html('');
                                                            $(
                                                                    "#replicationqueueStatus")
                                                                    .html('');
                                                        } else {
                                                            msg = "Replication in progress";
                                                            $(
                                                                    "#replicationqueueMsg")
                                                                    .html(msg);
                                                        }

                                                    });

                                });

                var mmfield = new CQ.form.MultiField({

                    renderTo : "CQ",

                    "fieldLabel" : "Root Paths",
                    width : "400px",
                    name : "rootPaths",
                    "fieldConfig" : {
                        "allowBlank" : "false",
                        predicate : "hierarchy",
                        "xtype" : "pathfield"
                    }
                });

                var cal = new CQ.form.DateTime({
                    "renderTo" : "CQ",
                    "dateWidth" : 100,
                    "hideTime" : false,
                    "name" : "datetimecal",
                    "allowBlank" : false,
                    "hiddenFormat" : "Y-m-d\\TH:i:s",
                    "listeners" : {
                        render : function() {
                            this.wrap.anchorTo("datetimecal", "tl");
                        },
                        change : function(fld, newValue, oldValue) {

                        },
                        dialogselect : function(fld, newValue) {

                        }
                    }
                });
                function buildRequestParams() {
                    var params = "";
                    $("input[name='rootPaths']").each(function(i, value) {
                        params += "rootPaths=" + escape($(this).val()) + "&";
                    });
                    params += "datetimecal="
                            + escape($("input[name='datetimecal']").get(0).value);
                    $('#cmbAgent option:selected').each(function() {

                        params += "&cmbAgent=" + escape($(this).val());
                    });
                    return params;
                }
            });
</script>
