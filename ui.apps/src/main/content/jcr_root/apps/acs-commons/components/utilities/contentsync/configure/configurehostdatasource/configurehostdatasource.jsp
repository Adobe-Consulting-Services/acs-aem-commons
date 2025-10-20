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
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" import="
        com.adobe.granite.ui.components.PagingIterator,
        com.adobe.granite.ui.components.ds.DataSource,
        com.adobe.granite.ui.components.ds.AbstractDataSource,
        java.util.Iterator,
        com.adobe.acs.commons.contentsync.ConfigurationUtils
            " %><%

    Resource hostsResource = resourceResolver.getResource(ConfigurationUtils.HOSTS_PATH);
    DataSource ds = new AbstractDataSource() {
        public Iterator<Resource> iterator() {
            return new PagingIterator<Resource>(hostsResource.listChildren(), null, null);
        }
    };

    request.setAttribute(DataSource.class.getName(), ds);
%>
