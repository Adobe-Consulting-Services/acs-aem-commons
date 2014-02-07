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
<%@include file="/libs/foundation/global.jsp"%>

<%@ page import="java.util.Iterator" %>
<%@ page import="com.day.cq.wcm.foundation.Image" %>
<%@ page import="org.apache.sling.commons.json.JSONArray" %>
<%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %>

<%
    Iterator<Resource> children = resource.listChildren();

    if(!children.hasNext()){
%>
        <wcmmode:edit>
            Double-Click to add Images
        </wcmmode:edit>
<%
    }else{
        Resource imagesResource = children.next();
        ValueMap map = imagesResource.adaptTo(ValueMap.class);
        String order = map.get("order", String.class);

        Image img = null; String src = null;
        JSONArray array = new JSONArray(order);
%>
        <!--
        Logic from this fantastic slide show example http://jonraasch.com/blog/a-simple-jquery-slideshow
        -->

        <style>
            #imagemultifieldslideshow {
                position:relative;
                height:350px;
            }

            #imagemultifieldslideshow IMG {
                position:absolute;
                top:0;
                left:0;
                z-index:8;
            }

            #imagemultifieldslideshow IMG.active {
                z-index:10;
            }

            #imagemultifieldslideshow IMG.last-active {
                z-index:9;
            }
        </style>

        <script>
            function slideSwitch() {
                var $active = $('#imagemultifieldslideshow IMG.active');

                if ( $active.length == 0 ){
                    $active = $('#imagemultifieldslideshow IMG:last');
                }

                var $next =  $active.next().length ? $active.next() : $('#imagemultifieldslideshow IMG:first');

                $active.addClass('last-active');

                $next.css({ opacity: 0.0 } ).addClass('active')
                        .animate({opacity: 1.0}, 1000, function() {
                            $active.removeClass('active last-active');
                        });
            }

            $(function() {
                setInterval( "slideSwitch()", 2000 );
            });
        </script>

        <div id="imagemultifieldslideshow">
<%
        for(int i = 0; i < array.length(); i++){
            img = new Image(imagesResource.getChild(String.valueOf(array.get(i))));
            img.setItemName(Image.PN_REFERENCE, "imageReference");
            img.setSelector("img");

            src = img.getSrc();
%>
            <img src='<%=src%>' <%= ( i == 0) ? "class='active'" : ""%> />
<%
        }
%>
        </div>
<%
    }
%>