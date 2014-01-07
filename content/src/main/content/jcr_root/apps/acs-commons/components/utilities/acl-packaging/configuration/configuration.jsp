<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2014 Adobe
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
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
        import="org.apache.commons.lang.StringUtils"%><%

    /* Flush Action */
    final String actionType = properties.get("replicationActionType", "");
    boolean hasActionType = StringUtils.isNotBlank(actionType);

    /* Package Definition */
    final String[] principalNames = properties.get("principalNames", new String[]{});
    final String packageName = properties.get("packageName", "ACL Package");
    final String packageGroupName = properties.get("packageGroupName", "ACLs");
    final String packageVersion = properties.get("packageVersion", "1.0.0");

    /* Form */
    final String actionURI = resourceResolver.map(currentPage.getContentResource().getPath() + ".package.json");
%>

<div class="alert">
    Success!
</div>

<h3>Package definition</h3>
<ul>
    <li>Package name: <%= xssAPI.encodeForHTML(packageName) %></li>
    <li>Package group: <%= xssAPI.encodeForHTML(packageGroupName) %></li>
    <li>Package version: <%= xssAPI.encodeForHTML(packageVersion) %></li>
</ul>


<h3>Targeted principals</h3>
<ul>
    <% for(final String principalName : principalNames) { %>
        <li><%= xssAPI.encodeForHTML(principalName) %></li>
    <% } %>
</ul>

<form id="acl-packaging-form" method="post" action="<%= xssAPI.encodeForHTMLAttr(actionURI) %>">
    <input type="hidden" name="packageName" value="<%= xssAPI.encodeForHTMLAttr(packageName) %>"/>
    <input type="hidden" name="packageGroupName" value="<%= xssAPI.encodeForHTMLAttr(packageGroupName) %>"/>
    <input type="hidden" name="packageVersion" value="<%= xssAPI.encodeForHTMLAttr(packageVersion) %>"/>

    <% for(final String principalName : principalNames) { %>
    <input type="hidden" name="principalNames" value="<%= xssAPI.encodeForHTMLAttr(principalName) %>"/>
    <% } %>

    <input type="submit" value="Create package"/>
</form>

<style>
    .alert {
        display: none;
        background-color: red;
        padding: 2rem;
        border: solid 1px maroon;
    }
</style>

<script>

    $(function() {
       $('body').on('submit', '#acl-packaging-form', function() {

           var $this = $(this);
           $.post($this.attr('action'), $this.serialize(), function(data) {

            $('.alert').hide();

           }, 'json');

           e.preventDefault();
       });
    });

</script>