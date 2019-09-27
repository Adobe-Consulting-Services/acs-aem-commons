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
  --%><%--
  
  This is obviously heavily based on the AEM Foundation Video component so as
  to maximize reuse of client code.
  
  --%><%@ include file="/libs/foundation/global.jsp" %><%
%><%@ page import="org.apache.sling.xss.XSSAPI" %><%
%><%@taglib prefix="audio" uri="http://www.adobe.com/consulting/acs-aem-commons/audio" %><%
%><%@taglib prefix="dam" uri="http://www.adobe.com/consulting/acs-aem-commons/dam" %><%
%><%@taglib prefix="xss" uri="http://www.adobe.com/consulting/acs-aem-commons/xss/2.0" %><%
    XSSAPI slingXssAPI = slingRequest.adaptTo(XSSAPI.class);
    pageContext.setAttribute("slingXssAPI", slingXssAPI);
%>
<c:set var="resourcePath">${xss:encodeForJSString(slingXssAPI, resource.resourceType)}</c:set>
<script type="text/javascript">
(function() {

    //get audio file name,fileName and path
    var mediaName = '${xss:encodeForJSString(slingXssAPI, dam:getTitleOrName(audio_asset))}';
    var mediaFile = '${xss:encodeForJSString(slingXssAPI, audio_asset.name)}';
    var mediaPath = '${xss:encodeForJSString(slingXssAPI, audio_asset.path)}';

    var audio = document.getElementById("${id}");
    var audioOpen = false;
    // delay (in ms) due to buggy player implementation
    // when seeking, audio.currentTime is not updated correctly so we need to delay
    // retreiving currentTime by an offset
    var delay = 250;
    //mouse up flag
    var isMouseUp = true;
    //store currentTime for 1 second
    var pauseTime = 0;
    // clickstream cloud data to be send based on context mapping
     var Analytics_data = new Object();

    if (audio && audio.addEventListener) {
        audio.addEventListener("playing", play, false);
    }

    function open() {
        audio.addEventListener("pause", pause, false);
        audio.addEventListener("ended", ended, false);
        audio.addEventListener("seeking", pause, false);
        audio.addEventListener("seeked", play, false);
         
        //store flag for mouse events in order to play only if the mouse is up
        audio.addEventListener("mousedown", mouseDown, false);
        audio.addEventListener("mouseup", mouseUp, false);
        function mouseDown(){ 
            isMouseUp=false;
        } 
        function mouseUp(){ 
            isMouseUp = true;
        }

        Analytics_data = new Object();
        Analytics_data["length"] = Math.floor(audio.duration);
        Analytics_data["playerType"] = "HTML5 audio";
        Analytics_data["source"] = mediaName;
        Analytics_data["playhead"] = Math.floor(audio.currentTime);
        
        Analytics_data["audioName"] = mediaName;
        Analytics_data["audioFileName"] = mediaFile;
        Analytics_data["audioFilePath"] = mediaPath;

        CQ_Analytics.record({event: 'audioinitialize', values: Analytics_data, componentPath: '${resourcePath}' });

        storeAudioCurrentTime();
    }

    function play() {
        if (CQ_Analytics && CQ_Analytics.record) {
            // open audio call
            if (!audioOpen) {
                open();
                audioOpen = true; 
            } else {
                //send pause event before play for scrub events
                pause();
                // register play
                setTimeout(playDelayed, delay);
            }
        }
    }

    function playDelayed() {
        if (isMouseUp){
            Analytics_data = new Object(); 
            Analytics_data["playhead"] = Math.floor(audio.currentTime-delay/1000);
            Analytics_data["source"] = mediaName;
            CQ_Analytics.record({event: 'audioplay', values: Analytics_data, componentPath: '${resourcePath}' }); 
        }
    }

    function pause() {
        Analytics_data = new Object(); 
        Analytics_data["playhead"] = pauseTime;
        Analytics_data["source"] = mediaName;
        CQ_Analytics.record({event: 'audiopause', values: Analytics_data, componentPath: '${resourcePath}' }); 
    }

    function ended() {
        Analytics_data = new Object(); 
        Analytics_data["playhead"] = Math.floor(audio.currentTime);
        Analytics_data["source"] = mediaName;
        CQ_Analytics.record({event: 'audioend', values: Analytics_data, componentPath: '${resourcePath}' }); 
        //reset temp variables
        audioOpen = false;
        pauseTime = 0;
    }
    
    //store current time for one second that will be use for pause
    function storeAudioCurrentTime() {
        var timer = window.setInterval(function() {
            if (audio.ended != true) {
                pauseTime = Math.floor(audio.currentTime); 
            } else { 
                window.clearInterval(timer);
            }
        },1000);
    }
})();
</script>
