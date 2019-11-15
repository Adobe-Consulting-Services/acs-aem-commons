<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2016 Adobe
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
  --%><%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"%><%
%><form
    ng-controller="MainCtrl"
    ng-init="init('${resourcePath}');"
    class="coral-Form coral-Form--vertical"
    action="${resourcePath}/tags.localized.csv" method="get">

    <br/>
    <section class="coral-Form-fieldset">
        <div class="coral-Form-fieldwrapper">
            <button
            class="coral-Button coral-Button--primary"
            onclick="return false;"
            ng-click="download();">Download Tags CSV</button>
        </div>

        <br/>

        <h3>Tags root</h3>

        <p>Type tags path, which will be used as tag tree root. Examples: (AEM 6.3)
            <i>/etc/tags/my_brand</i> or (AEM 6.4 or higher) <i>/content/cq:tags/my_brand</i></p>

        <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
                ng-model="tagPath"
                type="text"
                placeholder="/etc/tags/... or /content:/cq:tags/..."/>

        <h3>Type</h3>
        <p>Export tags can have localized or non-localized structure, according to Tag Maker parsers.</p>
        <input class="acsCommons-Form-tagexport-localization" type="radio" name="type" ng-model="localized" value="true" checked> Localized<br>
        <input class="acsCommons-Form-tagexport-localization" type="radio" name="type" ng-model="localized" value="false"> Non localized<br>

        <h3>Default localization</h3>
        <p>Default tag language, applied only if tag doesn't have localization value.</p>
        <input class="coral-Form-field coral-Textfield acsCommons-Form-tagexport-default-localization"
               ng-model="defaultLocalization"
               type="text"
               placeholder="en"/>
    </section>

</form>