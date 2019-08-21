<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 Adobe
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
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="com.adobe.acs.commons.designer.DesignHtmlLibraryManager,
          com.adobe.acs.commons.designer.PageRegion"%><%

    /* Head */
    final String[] cssHeadLibs = properties.get(PageRegion.HEAD + "/" + DesignHtmlLibraryManager.PROPERTY_CSS, new String[]{});
    final String[] jsHeadLibs = properties.get(PageRegion.HEAD + "/" + DesignHtmlLibraryManager.PROPERTY_JS, new String[]{});

    /* Body */
    final String[] jsBodyLibs = properties.get(PageRegion.BODY + "/" + DesignHtmlLibraryManager.PROPERTY_JS, new String[]{});
%>

<div class="clientlibs">
    <h1><em>ClientLibs Manager</em></h1>

    <div class="head-libs">
        <h2>Head ClientLibs</h2>
        <p class="instructions">Use in the &lt;head&gt; tag</p>

        <h3>CSS ClientLibs</h3>
        <ul>
            <% if(cssHeadLibs.length == 0) { %><li class="not-set">CSS ClientLibs not set</li><% } %>
            <% for(String lib : cssHeadLibs) { %>
            <li><%= lib %></li>
            <% } %>
        </ul>

        <h3>JavaScript Clientlibs</h3>
        <ul>
            <% if(jsHeadLibs.length == 0) { %><li class="not-set">JS ClientLibs not set</li><% } %>
            <% for(String lib : jsHeadLibs) { %>
            <li><%= lib %></li>
            <% } %>
        </ul>
    </div>

    <div class="body-libs">
        <h2>Body ClientLibs</h2>
        <p class="instructions">Typically used immediately before &lt;/body&gt;</p>

        <h3>JavaScript ClientLibs</h3>
        <ul>
            <% if(jsBodyLibs.length == 0) { %><li class="not-set">JS ClientLibs not set</li><% } %>
            <% for(String lib : jsBodyLibs) { %>
            <li><%= lib %></li>
            <% } %>
        </ul>
    </div>
</div>
