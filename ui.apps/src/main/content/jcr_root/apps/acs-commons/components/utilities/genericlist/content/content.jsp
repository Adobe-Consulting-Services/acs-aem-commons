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
<%@page import="com.adobe.acs.commons.genericlists.GenericList, com.adobe.acs.commons.mcp.form.*, java.lang.annotation.Annotation"%>
<%@include file="/libs/foundation/global.jsp"%>
<ui:includeClientLib categories="coralui3,granite.ui.coral.foundation" />
<%
    GeneratedDialogWrapper dialog = new GeneratedDialogWrapper(GenericList.class, sling);
    String path = request.getParameter("item");
    if (!path.endsWith("jcr:content")) {
        path += "/jcr:content";
    }
    dialog.getForm().setPath(path);
    
    AbstractResourceImpl formResource = (AbstractResourceImpl) dialog.getFormResource();
    // MobileUtil will crash with a NPE if the resource resolver is not defined
    formResource.setResourceResolver(resourceResolver);

%>
<sling:include resource="<%=formResource%>"/>