<%@ page import="com.adobe.granite.ui.components.Config" %>
<%@include file="/libs/granite/ui/global.jsp" %>

<%
    Config mCfg = cmp.getConfig();

    String SEARCH_PATHBROWSER_WRAPPER_ID = "acs-search-pathbrowser-wrapper-" + mCfg.get("name", String.class).substring(2);
    String ACS_PREFIX = "acs.granite.ui.search.pathBrowser";
%>

<div id="<%=SEARCH_PATHBROWSER_WRAPPER_ID%>">
    <%--include ootb pathbrowser--%>
    <sling:include resourceType="/libs/granite/ui/components/foundation/form/pathbrowser"/>
</div>

<script>
    (function($){
        var wrapper = $("#<%=SEARCH_PATHBROWSER_WRAPPER_ID%>"),
            pathBrowser = wrapper.find("[data-init='pathbrowser']");

        if(_.isEmpty(pathBrowser)){
            console.log("ACS Commons - search path browser wrapper not found");
            return;
        }

        //set the search based pathbrowser loaders and renderers defined in search-based-pathbrowser.js
        pathBrowser.attr("data-autocomplete-callback", "<%=ACS_PREFIX%>" + ".autocompletecallback");
        pathBrowser.attr("data-option-loader", "<%=ACS_PREFIX%>" + ".optionLoader");
        pathBrowser.attr("data-option-renderer", "<%=ACS_PREFIX%>" + ".optionRenderer");
    }(jQuery));
</script>