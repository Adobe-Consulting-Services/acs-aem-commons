<%--
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
--%><%@page session="false" %><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page import="java.util.HashMap,
                  com.adobe.granite.ui.components.ds.DataSource,
                  com.adobe.granite.ui.components.ds.SimpleDataSource,
                  com.adobe.granite.ui.components.ds.ValueMapResource,
                  com.adobe.granite.ui.components.ExpressionHelper,
                  com.adobe.granite.ui.components.Config,
                  com.day.cq.dam.api.Asset,
                  org.apache.commons.collections.iterators.TransformIterator,
                  org.apache.commons.collections.Transformer,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ResourceMetadata,
                  org.apache.sling.api.resource.ResourceResolver,
                  org.apache.sling.api.resource.ValueMap,
                  org.apache.sling.api.wrappers.ValueMapDecorator,
                  org.apache.commons.lang.StringUtils" %><%

    /**
     A datasource returning child nodes and add data-attributes to them.
     A title and the path to a referenced asset is also added to the data-attributes

     @property {StringEL} path - The path of the parent resource
     */

    ExpressionHelper ex = cmp.getExpressionHelper();
    Config dsCfg = new Config(resource.getChild(Config.DATASOURCE));

    String contentPath = ex.getString(dsCfg.get("path", String.class));

    final Resource content = contentPath != null ? resourceResolver.getResource(contentPath) : null;

    if (content != null) {
        final ResourceResolver resolver = resourceResolver;
        final String emptySlideStr = i18n.get("New Slide");

        @SuppressWarnings("unchecked")
        DataSource ds = new SimpleDataSource(new TransformIterator(content.listChildren(), new Transformer() {
            public Object transform(Object o) {
                Resource child = (Resource) o;
                ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
                final Config childCfg = new Config(child);
                String title = childCfg.get("jcr:title");
                String name = child.getName();
                name = !StringUtils.isEmpty(name) ? name : "";
                String path = child.getPath();
                String fileReference = childCfg.get("fileReference");
                fileReference = !StringUtils.isEmpty(fileReference) ? fileReference : "";

                String text;
                if (!StringUtils.isEmpty(title)) {
                    text = title;
                } else if (!StringUtils.isEmpty(fileReference)) {
                    text = fileReference;
                } else {
                    text = emptySlideStr;
                }

                vm.put("value", name);
                vm.put("path", !StringUtils.isEmpty(path) ? path : "");
                vm.put("name", name);
                vm.put("text", text);
                vm.put("file-reference", fileReference);

                if (!StringUtils.isEmpty(fileReference)) {
                    Resource assetRow = resolver.resolve(childCfg.get("fileReference"));

                    if (assetRow != null) {
                        Asset asset = assetRow.adaptTo(Asset.class);
                        vm.put("asset-mimetype", asset.getMetadataValue("dam:MIMEtype"));
                    }
                }

                return new ValueMapResource(resolver, new ResourceMetadata(), "nt:unstructured", vm);
            }
        }));

        request.setAttribute(DataSource.class.getName(), ds);
    }
%>
