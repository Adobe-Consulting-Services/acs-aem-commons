<%--
  #%L
  ACS AEM Tools Package
  %%
  Copyright (C) 2014 Adobe
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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %>

<div id="blocklyArea" style="height:90%; min-height: 500px; width:100%">
    <div id="blocklyDiv" style="height:100%; width:100%"></div>
</div>
<xml id="toolbox" style="display: none">
    <category id="variables" name="Variables">
        <block type="variables_set"></block>
        <block type="variables_get"></block>
        <block type="math_change"></block>
    </category>
    <category name="Flow">
        <block type="controls_repeat"></block>
        <block type="controls_repeat_ext"></block>
        <block type="controls_whileUntil"></block>
        <block type="controls_for"></block>
        <block type="controls_forEach"></block>
        <block type="controls_flow_statements"></block>
    </category>
    <category name="Math">
        <block type="math_number"></block>
        <block type="math_arithmetic"></block>
        <block type="math_modulo"></block>
        <block type="math_random_int"></block>
        <block type="math_constrain"></block>
        <block type="math_number_property"></block>
    </category>
    <category name="Logic">
        <block type="controls_if"></block>
        <block type="logic_compare"></block>
        <block type="logic_operation"></block>
        <block type="logic_negate"></block>
        <block type="logic_boolean"></block>
        <block type="logic_ternary"></block>
    </category>
    <category name="Text">
        <block type="text"></block>
        <block type="text_print"></block>
        <block type="text_print">
            <value name="VALUE">
                <block type="text">
                    <field name="TEXT">text</field>
                </block>
            </value>                
        </block>
        <block type="text_reverse"></block>
        <block type="text_length"></block>
        <block type="text_isEmpty"></block>
        <block type="text_join"></block>
        <block type="text_changeCase"></block>
        <block type="text_trim"></block>
        <block type="text_charAt"></block>
        <block type="text_replace"></block>
        <block type="text_indexOf"></block>
        <block type="text_getSubstring"></block>
    </category>
</xml>