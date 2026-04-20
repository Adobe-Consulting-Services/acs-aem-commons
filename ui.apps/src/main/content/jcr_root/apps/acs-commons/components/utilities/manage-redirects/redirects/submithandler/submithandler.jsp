<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2015 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%><%
%><%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
          import="org.apache.sling.commons.json.io.JSONStringer,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Tag" %><%--###

 Update table of redirects when users adds/edits a rule
======
   Example::

      + myform
        - sling:resourceType = "granite/ui/components/coral/foundation/form"
        - method = "post"
        - foundationForm = true
        + successresponse
          - sling:resourceType = "acs-commons/components/utilities/manage-redirects/redirects/submithandler"
        + items
          + field1
          + field2
###--%><%

Config cfg = cmp.getConfig();

Tag tag = cmp.consumeTag();

AttrBuilder attrs = tag.getAttrs();
cmp.populateCommonAttrs(attrs);

JSONStringer json = new JSONStringer();
json.object();
json.key("name").value("acs.redirects.update");
json.endObject();

attrs.addClass("foundation-form-response-ui-success");
attrs.add("data-foundation-form-response-ui-success", json.toString());

%><meta <%= attrs %>>