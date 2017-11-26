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
  --%>
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%

%><div ng-controller="MainCtrl"
       ng-cloak
       ng-init="init('${resourcePath}');">

    <cq:include script="includes/form.jsp"/>

    <br/>
    <hr/>

    <p>The ACS Commons QR Code feature exposes a QR Code for the AEM Publish equivalent of the page being authored.
        This allows for quick review of published pages on the AEM Publish server via a physical mobile/tablet device.</p>

    <ul>
        <li>Once enabled, the QR Icon will be available in the Touch UI Page Editor's top action base.</li>
        <li>AEM's Externalizer will be used to generate the Publish URL.</li>
    </ul>

    <center>
        <img src="./qr-code/_jcr_content/qr-code-image.png" title="QR Code"/>
    </center>
</div>
