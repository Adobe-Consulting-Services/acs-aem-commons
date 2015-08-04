<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2013 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false"
        import="com.adobe.acs.commons.images.NamedImageTransformer,
                com.adobe.acs.commons.wcm.datasources.DataSourceBuilder,
                com.adobe.acs.commons.wcm.datasources.DataSourceOption,
                java.util.ArrayList,
                java.util.Arrays,
                java.util.Comparator,
                java.util.List" %><%

    final DataSourceBuilder dataSourceBuilder = sling.getService(DataSourceBuilder.class);
    final NamedImageTransformer[] namedImageTransforms = sling.getServices(NamedImageTransformer.class, null);
    final List<DataSourceOption> options = new ArrayList<DataSourceOption>();

    Arrays.sort(namedImageTransforms, new Comparator<NamedImageTransformer>() {
        public int compare(NamedImageTransformer a, NamedImageTransformer b) {
            String nameA = a.getTransformName().toLowerCase();
            String nameB = b.getTransformName().toLowerCase();

            return nameA.compareTo(nameB);
        }
    });

    for (final NamedImageTransformer transform : namedImageTransforms) {
        options.add(new DataSourceOption(transform.getTransformName(),
                                         transform.getTransformName()));
    }

    dataSourceBuilder.addDataSource(slingRequest, options);
%>
