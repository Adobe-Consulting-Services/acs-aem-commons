<%--
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
  --%>
<%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false" import="
        org.apache.sling.api.resource.ValueMap,
        com.adobe.granite.ui.components.Tag,
        com.adobe.granite.ui.components.AttrBuilder" %><%

    String contextPath = request.getContextPath();
    ValueMap valueMap = resource.adaptTo(ValueMap.class);
	String username = valueMap.get("username", "");
	String password = valueMap.get("password", "");
	String url = valueMap.get("host", "");
	String icon = "viewList";
	String smallIcon = "search";
	String path =  resource.getPath();
	String href = "/apps/acs-commons/content/contentsync.html" + path;

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    attrs.addClass("foundation-collection-navigator");
    attrs.addClass("whitecard");
%>
<coral-card assetwidth="400" assetheight="380" <%= attrs %>
    data-foundation-collection-navigator-href="<%= href %>"
	data-host="<%=url%>"
	data-username="<%=username%>"
	data-password="<%=password%>"
	data-path="<%=path%>"
    colorhint="#FFFFFF">
    <coral-card-asset class="whitecard">
        <coral-icon icon="<%= icon %>" class="largeIcon centerText">
        </coral-icon>
    </coral-card-asset>
    <coral-card-content class="customCardContent">
        <coral-card-title class="customCardTitle">
            <table>
                <tr>
                    <td>
                        <div class="smallIcon">
                            <coral-icon icon="<%= smallIcon %>">
                            </coral-icon>
                        </div>
                    </td>
                    <td>
                        <%= url %>
                    </td>
                </tr>
            </table>
        </coral-card-title>
    </coral-card-content>
</coral-card>
<coral-quickactions target="_prev">
    <coral-quickactions-item icon="check" class="foundation-collection-item-activator">Select</coral-quickactions-item>
    <coral-quickactions-item icon="gears" class="configureQuickAction"
    data-editpath="<%=path%>">Configure</coral-quickactions-item>
</coral-quickactions>
