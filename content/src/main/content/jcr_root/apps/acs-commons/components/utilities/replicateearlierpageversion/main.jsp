<%@ page contentType="text/html"
             pageEncoding="utf-8"
			 import="com.day.cq.i18n.I18n,com.day.cq.widget.HtmlLibraryManager,com.adobe.acs.commons.replicatepageversion.ReplicatePageVersionService,com.day.cq.replication.Agent" %>
			 <%@include file="/libs/foundation/global.jsp"%>
<%
     I18n i18n = new I18n(slingRequest);
final ReplicatePageVersionService rpvs=sling.getService(ReplicatePageVersionService.class);
%>

	<h1><%= i18n.get("Replicate Earlier Versions") %></h1>
        <div id="errmsg" style="display:none;background-color: #EFCDC7"></div>
	<form target="treeProgress"  method="POST" id="activateearlierversion" name="activateearlierversion">
    <input type="hidden" id="pathPage" name="pathPage" value="/content">
	<input type="hidden" id="pathAsset" name="pathAsset" value="/content/dam">
        <input type="hidden" id="cal" name="cal" value="">
    <table class="form">
        <tr>
            <td><label for="fakePathFieldPages"><%= i18n.get("Pages root Path") %>:</label></td>
            <td><div id="fakePathPages">&nbsp;</div><br>
                <small><%= i18n.get("Select root path of the site") %></small>
            </td>
        </tr>
        <tr>
            <td><label for="fakePathFieldAssets"><%= i18n.get("Assets root Path") %>:</label></td>
            <td><div id="fakePathAssets">&nbsp;</div><br>
                <small><%= i18n.get("Select root path of the site assets") %></small>
            </td>
        </tr>
        <tr>
            <td><label for="datetimecal"><%= i18n.get("Enter the date time") %>:</label></td>
            <td><div id="datetimecal">&nbsp;</div><br>

            </td>
        </tr>
        <tr>
            <td><label for="agentId"><%= i18n.get("Replication Agent") %>:</label></td>
            <td><select id="cmbAgent" name="cmbAgent">
                <option value="">Select Agent</option>
                <%
        for (final Agent agent : rpvs.getAgents()) {
        %>
                <option value="<%=agent.getId()%>"><%=agent.getConfiguration().getName()%></option>
                <%
    }
        %>
                </select><br>

            </td>
        </tr>
        <tr>
            <td></td>
            <td>
                
                <input type="button" value="<%= i18n.get("Replicate") %>" id="btnReplicate" name="btnReplicate">
            </td>
        </tr>
    </table>
        <div id="CQ"></div>
</form><br>
<div id="replicationqueue"></div>
        <script>
            // provide a path selector field with a repository browse dialog
            CQ.Ext.onReady(function() {
                $("#btnReplicate").click(function(){
                    var msg="Please Wait.......";
                    $("#replicationqueue").html(msg);
                    $.post("/bin/replicatepageversion",$("#activateearlierversion").serialize(),function(data){

                        var resp=data;

					if(resp.status=='error'){
                        $("#replicationqueue").html('');
                        alert(resp.error);
                        $("#errmsg").html(resp.error);
                        $("#errmsg").css("display","block");
                        $("#replicationqueue").html('');                        
                    }else{
                        $("#errmsg").html("");
                        $("#errmsg").css("display","none");
                        var lnk="<a href='"+resp.agentPath+".html' target='_new' >View Replication Queue</a><br/><a href='"+resp.agentPath+".log.html' target='_new' >View Replication Log</a>";
                        $("#replicationqueue").html(lnk);
                    }
                    });
                });
                 var pathPage = new CQ.form.PathField({
                    renderTo: "CQ",
//                    "content": "/content",
                    rootPath: "/content",
                    predicate: "hierarchy",
                    hideTrigger: false,
                    showTitlesInTree: false,
                    name: "fakePathFieldPages",
                    value: "/content",
                    width: 400,
                    listeners: {
                        render: function() {
                            this.wrap.anchorTo("fakePathPages", "tl");
                        },
                        change: function (fld, newValue, oldValue) {
                            document.getElementById("pathPage").value = newValue;
                        },
                        dialogselect: function(fld, newValue) {
                            document.getElementById("pathPage").value = newValue;
                        }
                    }
                });
              var pathAsset = new CQ.form.PathField({
                    //"applyTo": "path",
                    renderTo: "CQ",
                  //                    "content": "/content/dam",
                    rootPath: "/content/dam",
                    predicate: "hierarchy",
                    hideTrigger: false,
                    showTitlesInTree: false,
                    name: "fakePathFieldAssets",
                  value: "/content/dam",
                    width: 400,
                    listeners: {
                        render: function() {
                            this.wrap.anchorTo("fakePathAssets", "tl");
                        },
                        change: function (fld, newValue, oldValue) {
                            document.getElementById("pathAsset").value = newValue;
                        },
                        dialogselect: function(fld, newValue) {
                            document.getElementById("pathAsset").value = newValue;
                        }
                    }
                });	

                       var cal= new CQ.form.DateTime({
                    "renderTo":"CQ",
            "dateWidth": 100,
            "hideTime": false,
            "name": "datetimecal",
            "allowBlank": false,
            "hiddenFormat": "Y-m-d,H:i:s",
            "listeners": {
render: function() {
                            this.wrap.anchorTo("datetimecal", "tl");
                        },
                   change: function (fld, newValue, oldValue) {


                        },
                        dialogselect: function(fld, newValue) {

                           
                        }
            }
        });
            });
        </script>
