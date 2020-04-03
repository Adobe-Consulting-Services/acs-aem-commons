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
    action="${resourcePath}/users.export.csv" method="get">

    <br/>
    <br/>
  <section class="coral-Form-fieldset">
    <div class="coral-Form-fieldwrapper">
      <button
        class="coral-Button coral-Button--primary"
        onclick="return false;"
        ng-click="download();">Download User CSV Report</button>

      <button
        class="coral-Button coral-Button--secondary"
        onclick="return false;"
        ng-click="save();">Save Configuration</button>
    </div>

    <br/>
    <br/>

    <div class="coral-Form-fieldwrapper">
        <table class="coral-Table acs-table">
            <thead>
                <tr class="coral-Table-row">
                    <th class="coral-Table-headerCell">Custom User Properties <em>(Relative path from the [rep:User] node)</em></th>
                    <th class="coral-Table-headerCell">&nbsp;</th>
                </tr>
            </thead>
            <tbody>
                <tr class="coral-Table-row"
                    ng-repeat="customProperty in form.customProperties track by $index">
                    <td class="coral-Table-cell acs-table-cell">
                        <input type="text"
                               class="coral-Form-field coral-Textfield"
                               placeholder="profile/customProp"
                               ng-model="customProperty.relPropertyPath"/></td>
                    <td class="coral-Table-cell acs-table-cell-action">
                        <i ng-show="form.customProperties.length > 0"
                           ng-click="form.customProperties.splice($index, 1)"
                           class="coral-Icon coral-Icon--minusCircle"></i>
                    </td>
                </tr>
            </tbody>
            <tfoot>
            <tr class="coral-Table-row">
                <td colspan="2" class="coral-Table-cell property-add">
                    <span ng-click="form.customProperties.push({})">
                        <i class="coral-Icon coral-Icon--addCircle withLabel"></i>
                        Add Custom Property
                     </span>
                </td>
            </tr>
            </tfoot>
        </table>
    </div>

    <br/>
    <br/>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Only Include Users by Group Membership</label>
        <select
            ng-model="form.groupFilter">
            <option
                ng-repeat="option in options.groupFilters"
                ng-selected="option.value == form.groupFilter"
                value="{{ option.value }}">{{ option.text }}</option>
        </select>
    </div>

    <br/>
    <br/>

    <div class="coral-Form-fieldwrapper">
      <label class="coral-Form-fieldlabel">Filter by Group <em>(Select none for all groups)</em></label>


        <%-- First Col --%>
       <ul class="coral-List coral-List--minimal acs-column-33-33-33">
            <li class="coral-List-item"
                ng-repeat="group in options.groups.slice(getFromIndex(1), getToIndex(1))">
                <label class="coral-Checkbox">
                    <input class="coral-Checkbox-input"
                           ng-checked="form.groups.indexOf(group) >= 0"
                           ng-click="toggle(form.groups, group)"
                           type="checkbox">
                    <span class="coral-Checkbox-checkmark"></span>
                    <span class="coral-Checkbox-description">{{ group }}</span>
                </label>
       </ul>

        <%-- Second Col --%>
        <ul class="coral-List coral-List--minimal acs-column-33-33-33">
            <li class="coral-List-item"
                ng-repeat="group in options.groups.slice(getFromIndex(2), getToIndex(2))">
                <label class="coral-Checkbox">
                 <input class="coral-Checkbox-input"
                        ng-checked="form.groups.indexOf(group) >= 0"
                        ng-click="toggle(form.groups, group)"
                        type="checkbox">
                 <span class="coral-Checkbox-checkmark"></span>
                 <span class="coral-Checkbox-description">{{ group }}</span>
             </label>
            </li>
        </ul>

        <%-- Third Col --%>
        <ul class="coral-List coral-List--minimal acs-column-33-33-33">
            <li class="coral-List-item"
                ng-repeat="group in options.groups.slice(getFromIndex(3), getToIndex(3))">
                <label class="coral-Checkbox">
                    <input class="coral-Checkbox-input"
                           ng-checked="form.groups.indexOf(group) >= 0"
                           ng-click="toggle(form.groups, group)"
                           type="checkbox">
                    <span class="coral-Checkbox-checkmark"></span>
                    <span class="coral-Checkbox-description">{{ group }}</span>
                </label>
            </li>
        </ul>
        <div style="clear: both;"></div>
    </div>

</form>