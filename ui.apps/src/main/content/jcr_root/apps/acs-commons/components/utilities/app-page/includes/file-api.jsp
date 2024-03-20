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
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>

<%@include file="/libs/foundation/global.jsp" %><%

    pageContext.setAttribute("fileApiJS",
            resourceResolver.map(slingRequest, "/etc/clientlibs/acs-commons/vendor/FileAPI.min.js"));
    pageContext.setAttribute("fileApiSWF",
            resourceResolver.map(slingRequest, "/etc/clientlibs/acs-commons/vendor/FileAPI.min.js/FileAPI.flash.swf"));

%><script>
    // Need to be loaded before angular-file-upload-shim(.min).js
    FileAPI = {
        jsUrl: '${fileApiJS}',
        flashUrl: '${fileApiSWF}'
    }
</script>

