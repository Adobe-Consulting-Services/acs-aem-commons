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
     ng-init="app.uri = '${resourcePath}.json';">

    <p>The ACS Commons QR Code exposes a QR Code for the AEM Publish equivalent of the page being authored.
        This allows for quick review of published pages on the AEM Publish server via a physical mobile/tablet device.</p>

    <ul>
        <li>Once enabled, the QR Icon will be available in the Touch UI Page Editor's top action base.</li>
        <li>The AEM Author  Publish Host URL mapping defined below determines for which host the QR Code will be generated for.
            <br/>
            For example:
            <ul>
                <li>Assume AEM Author host is '<i>aem-author.example.com</i>' and AEM Publish host is '<i>www.example.com</i>'.</li>
                <li>If the page being authored is <b>aem-author.project.com/content/example/en/home.html</b>, then generated QR code will have URL as <b>www.example.com/content/project/en/home.html</b></li>
            </ul>
        </li>
        <li>Do <strong>not</strong> add <i>http</i> or <i>https</i> to the url mappings; ONLY specify the host names.</li>
    </ul>

    <hr/>

    <cq:include script="includes/form.jsp"/>
</div>
