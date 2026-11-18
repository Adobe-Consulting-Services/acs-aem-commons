<%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false" import="
        org.apache.sling.api.resource.ValueMap,
        com.adobe.granite.ui.components.Tag,
        com.adobe.granite.ui.components.AttrBuilder,
        com.fasterxml.jackson.databind.ObjectMapper,
        com.fasterxml.jackson.databind.MapperFeature" %><%

    String contextPath = request.getContextPath();
    ValueMap valueMap = resource.getChild("jcr:content").getValueMap();
	String transportUri = valueMap.get("transportUri", "");
	String title = valueMap.get("jcr:title", "");
	String description = valueMap.get("jcr:description", "");
	String smallIcon = "search";
	String path =  resource.getPath();
	String href = "/replicator.html" + path;

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    attrs.addClass("foundation-collection-navigator");
    attrs.addClass("whitecard");

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

    attrs.add("data-properties", mapper.writeValueAsString(valueMap));
    attrs.add("data-foundation-collection-navigator-href", href);
    attrs.add("data-path", path);

%>
<coral-card assetwidth="400" assetheight="380" <%= attrs %>
    colorhint="#FFFFFF">
    <coral-card-asset class="whitecard">
        <img src="/libs/cq/gui/components/workflow/console/default/thumbnail.png">
    </coral-card-asset>
    <coral-card-content>
        <coral-card-title ><%=title%></coral-card-title>
        <coral-card-propertylist class="u-coral-clearFix">
            <coral-card-property title="Description" class="_coral-Card-property coral-Body--small">
                <coral-card-property-content><%=transportUri%></coral-card-property-content>
            </coral-card-property>
        </coral-card-propertylist>
    </coral-card-content>
</coral-card>
<coral-quickactions target="_prev">
    <coral-quickactions-item icon="check" class="foundation-collection-item-activator">Select</coral-quickactions-item>
</coral-quickactions>
