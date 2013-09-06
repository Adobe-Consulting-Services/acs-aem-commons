<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="com.adobe.acs.commons.designer.DesignHtmlLibraryManager"%><%

    String[] cssHeadLibs = properties.get("css/" + DesignHtmlLibraryManager.PROP_HEAD_LIBS, new String[]{});
    String[] cssBodyStartLibs = properties.get("css/" + DesignHtmlLibraryManager.PROP_BODY_START_LIBS, new String[]{});
    String[] cssBodyEndLibs = properties.get("css/" + DesignHtmlLibraryManager.PROP_BODY_END_LIBS, new String[]{});

    String[] jsHeadLibs = properties.get("js/" + DesignHtmlLibraryManager.PROP_HEAD_LIBS, new String[]{});
    String[] jsBodyStartLibs = properties.get("js/" + DesignHtmlLibraryManager.PROP_BODY_START_LIBS, new String[]{});
    String[] jsBodyEndLibs = properties.get("js/" + DesignHtmlLibraryManager.PROP_BODY_END_LIBS, new String[]{});
%>

<div class="clientlibs">
    <h1><em>ClientLibs Manager</em></h1>


    <h2>Head ClientLibs</h2>
    <p>Use in the &lt;head&gt; tag</p>

    <h3>CSS ClientLibs</h3>
    <% if(cssHeadLibs.length == 0) { %><em>CSS ClientLibs not set</em><% } %>
    <ul>
        <% for(String lib : cssHeadLibs) { %>
        <li><%= lib %></li>
        <% } %>
    </ul>

    <h3>JavaScript Clientlibs</h3>
    <% if(jsHeadLibs.length == 0) { %><em>JS ClientLibs not set</em><% } %>
    <ul>
        <% for(String lib : jsHeadLibs) { %>
        <li><%= lib %></li>
        <% } %>
    </ul>



    <h2>Body Start ClientLibs</h2>
    <p>Use immediately after &lt;body&gt;</p>

    <h3>CSS ClientLibs</h3>
    <% if(cssBodyStartLibs.length == 0) { %><em>CSS ClientLibs not set</em><% } %>
    <ul>
        <% for(String lib : cssBodyStartLibs) { %>
        <li><%= lib %></li>
        <% } %>
    </ul>

    <h3>JavaScript ClientLibs </h3>
    <% if(jsBodyStartLibs.length == 0) { %><em>JS ClientLibs not set</em><% } %>
    <ul>
        <% for(String lib : jsBodyStartLibs) { %>
        <li><%= lib %></li>
        <% } %>
    </ul>



    <h2>Body End ClientLibs</h2>
    <p>Use immediately before &lt;/body&gt;</p>

    <h3>CSS ClientLibs</h3>
    <% if(cssBodyEndLibs.length == 0) { %><em>CSS ClientLibs not set</em><% } %>
    <ul>
        <% for(String lib : cssBodyEndLibs) { %>
        <li><%= lib %></li>
        <% } %>
    </ul>

    <h3>JavaScript ClientLibs</h3>
    <% if(jsBodyEndLibs.length == 0) { %><em>JS ClientLibs not set</em><% } %>
    <ul>
        <% for(String lib : jsBodyEndLibs) { %>
        <li><%= lib %></li>
        <% } %>
    </ul>
</div>

<div class="instructions">
    <cq:include script="instructions.jsp"/>
</div>