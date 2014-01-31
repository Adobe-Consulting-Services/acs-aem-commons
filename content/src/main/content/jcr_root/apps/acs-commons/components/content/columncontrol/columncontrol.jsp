<%@ page import="org.apache.sling.commons.json.JSONObject" %>
<%@ page import="java.io.PrintWriter,com.day.cq.wcm.api.WCMMode" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@page session="false" %>

<div class="row acs-commons">



    <%
   
        try {
            Property property = null;

            if(currentNode.hasProperty("columns")){
                property = currentNode.getProperty("columns");
            }

            if (property != null) {
                JSONObject obj = null;
                Value[] values = null;

                if(property.isMultiple()){
                    values = property.getValues();
                }else{
                    values = new Value[1];
                    values[0] = property.getValue();
                }
int index=1;
                for (Value val : values) {
                    obj = new JSONObject(val.getString());
                    String path=obj.getString("pathName");
                    if(path==null ||"".equals(path)){
                        path="path"+index;
                    }
                    request.setAttribute("path",path);
    %>
    <div class="colctrl_<%= obj.get("cols") %>" >
        <cq:include path="${path}" resourceType="foundation/components/parsys" />
    </div>



    <%
        index++;
                }
            } else {
    %>
               Please edit to add columns
                <br><br>
    <%
            }
        } catch (Exception e) {
          %>   Please edit to add columns
                <br><br><%
        }
    %>

</div>
