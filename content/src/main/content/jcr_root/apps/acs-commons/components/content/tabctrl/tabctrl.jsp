<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2014 Adobe
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
  --%><%--
  ==============================================================================

  Tabs component(SEO Friendly URL)

  ==============================================================================

--%>

<%@page import="java.util.ArrayList"%>
<%@ include file="/libs/foundation/global.jsp" %>
<%@ page contentType="text/html; charset=utf-8" import="
    com.day.cq.i18n.I18n,
    org.apache.commons.lang.StringUtils,
    com.day.cq.wcm.api.WCMMode,
    com.adobe.acs.commons.tabs.beans.TabBean,
    com.day.cq.commons.jcr.JcrUtil,
    org.apache.sling.api.resource.ResourceResolver,
    javax.jcr.Session,
    org.apache.jackrabbit.commons.JcrUtils,
    javax.jcr.Node,
    org.apache.sling.settings.SlingSettingsService,
    java.util.Set,
    java.util.Iterator,
    com.adobe.acs.commons.tabs.TabsConfig"
%>
<%
    ResourceResolver resolver = slingRequest.getResourceResolver();
   Session session = resolver.adaptTo(Session.class);
    TabsConfig tabsConfig = new TabsConfig(slingRequest.getResourceResolver());
    boolean bPublish = false;
    SlingSettingsService settingsService = sling.getService(org.apache.sling.settings.SlingSettingsService.class);
    Set<String> runModes = settingsService.getRunModes();
    Iterator iter = runModes.iterator();
    while (iter.hasNext()) {
    String run_mode = iter.next().toString().trim();
        if(run_mode.startsWith("publish"))
        {
            bPublish = true;
        }
    }
%>
    <c:set var="mode" value="<%=WCMMode.fromRequest(slingRequest) %>"/>
    <c:set var="edit_mode" value="<%=WCMMode.EDIT %>"/>
    <cq:includeClientLib categories="acs-commons.tabs"/>
    <c:set var="tabList" value="<%=tabsConfig.getTabs(currentNode, request, session, bPublish) %>"/>
    <c:set var="selectedSelector" value="<%=tabsConfig.getSelectedSelector() %>"/>
    <c:set var="selectedStyle" value="<%=tabsConfig.getSelectedStyle(currentNode) %>"/>

    <div id="main" class="tabs">
        <div class="wrapper">
<div class="tabContainer ${selectedStyle}">
    <div class="tabLabels">
        <c:choose>
            <c:when test="${not empty tabList}">
                <c:forEach items="${tabList}" var="tab" varStatus="count">
                    <c:if test="${not empty tab.selector}">
                        <c:if test="${tab.selector == selectedSelector}">
                            <div class="tabLabelItems tabOn">
                        </c:if>
                        <c:if test="${tab.selector != selectedSelector}">
                            <div class="tabLabelItems">
                        </c:if>            
                                <div class="tabLabelItem">
                                    <a href="<%= currentPage.getPath() %>/<c:out value='${tab.selector}'></c:out>.html"><c:out value='${tab.name}'></c:out></a>
                                </div>
                             </div>
                    </c:if>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <c:if test="${mode == edit_mode}">
                    <c:out value='Configure the tabs'></c:out>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="clears"></div>    
    <c:choose>
        <c:when test="${not empty selectedSelector}">
            <div class="tabBodies withsidenav">
                <div class="tabBodyContainer">
                    <div class="tabLeft">
                           <cq:include path="<%= "tab-content-"+ tabsConfig.getSelectedSelector() %>" resourceType="foundation/components/parsys"/> 
                    </div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <c:if test="${mode == edit_mode}">
                <div class="tabBodies withsidenav">
                    <div class="tabBodyContainer">
                        <div class="tabLeft">
                            <c:out value='Select the tab above to get the respective content'></c:out>
                        </div>
                    </div>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>
               
        </div>
    </div>