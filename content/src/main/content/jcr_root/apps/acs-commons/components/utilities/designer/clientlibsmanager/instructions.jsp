<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"%>


<p>
    Leverage the DesignHtmlLibraryManager OSGi Service to get comma delimited lists of the Clientlibs that can be passed to &lt;cq:includeClientLibs/&gt; tag lib.
</p>

<hr/>

<p>Class name</p>

<code>com.adobe.acs.commons.designer.DesignHtmlLibraryManager</code>

<hr/>

<p>OSGi Service retrieval</p>

<code>
    DesignHtmlLibraryManager designHtmlLibraryManager =
        sling.getService(DesignHtmlLibraryManager.class);
</code>

<hr/>

<p>Available methods</p>

<ul>
    <li>designHtmlLibraryManager.getHeadLibs(currentDesign)</li>
    <li>designHtmlLibraryManager.getCssHeadLibs(currentDesign)</li>
    <li>designHtmlLibraryManager.getJsHeadLibs(currentDesign)</li>

    <li>designHtmlLibraryManager.getBodyStartLibs(currentDesign)</li>
    <li>designHtmlLibraryManager.getCssBodyStartLibs(currentDesign)</li>
    <li>designHtmlLibraryManager.getJsBodyStartLibs(currentDesign)</li>


    <li>designHtmlLibraryManager.getBodyEndLibs(currentDesign)</li>
    <li>designHtmlLibraryManager.getCssBodyEndLibs(currentDesign)</li>
    <li>designHtmlLibraryManager.getJsBodyEndLibs(currentDesign)</li>
</ul>

<hr/>

<p>Example:</p>

<code>
    &lt;cq:includeClientLibs
        categories="&lt;%= designHtmlLibraryManager.getJsBodyEndLibs(currentDesign) %&gt;"/&gt;
</code>