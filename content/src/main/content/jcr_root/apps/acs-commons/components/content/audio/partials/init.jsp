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
  --%><%@ include file="/libs/foundation/global.jsp" %><%
%><%@ page import="com.day.cq.dam.video.VideoProfile,
                   com.day.cq.dam.api.Asset,
                   com.day.cq.dam.api.Rendition,
                   com.day.cq.dam.commons.util.PrefixRenditionPicker,
                   java.util.Map,
                   java.util.LinkedHashMap" %><%

    // try find referenced asset
    Asset asset = null;
    Resource assetRes = resourceResolver.getResource(properties.get("asset", ""));
    if (assetRes != null) {
        asset = assetRes.adaptTo(Asset.class);
    }
    if (asset != null) {
        request.setAttribute("audio_asset", asset);
        // render each profiles as a <source> element
        Map<VideoProfile, Rendition> renditions = new LinkedHashMap<VideoProfile, Rendition>();
        String[] profiles = currentStyle.get("profiles", new String[] { "mp3hq", "ogghq" });
        
        for (String profile : profiles) {
            VideoProfile videoProfile = VideoProfile.get(resourceResolver, profile);
            
            if (videoProfile != null) {
                String prefix = "cq5dam.audio." + videoProfile.getName();
                Rendition rendition = asset.getRendition(new PrefixRenditionPicker(prefix));
                if (rendition != null) {
                    renditions.put(videoProfile, rendition);
                }
            }
        }
        request.setAttribute("renditions", renditions);
        request.setAttribute("profiles", profiles);
        
        StringBuilder attributes = new StringBuilder();

        String audioClass = currentStyle.get("audioClass", "");
        if (audioClass.length() > 0) {
            attributes.append(" class=\"").append(audioClass).append("\"");
        }
        if (!currentStyle.get("noControls", false)) {
            attributes.append(" controls=\"controls\"");
        }
        if (currentStyle.get("autoplay", false)) {
            attributes.append(" autoplay=\"autoplay\"");
        }
        if (currentStyle.get("loop", false)) {
            attributes.append(" loop=\"loop\"");
        }
        String preload = currentStyle.get("preload", "");
        if (preload.length() > 0) {
            attributes.append(" preload=\"").append(preload).append("\"");
        }
        String id = "cq-audio-html5-" + System.currentTimeMillis();
        
        request.setAttribute("attributes", attributes);
        request.setAttribute("id", id);
    }
%>