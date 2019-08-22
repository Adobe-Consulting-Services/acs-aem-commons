/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/*global top: false */

//keymapping for edit layer
jQuery(document).bind('keydown', 'ctrl+shift+e', function() {  
      top.Granite.author.layerManager.activateLayer("Edit");
});

//keymapping for preview layer
jQuery(document).bind('keydown', 'ctrl+shift+p', function() {  
      top.Granite.author.layerManager.activateLayer("Preview");
});

//keymapping for annotate layer
jQuery(document).bind('keydown', 'ctrl+shift+a', function() {  
      top.Granite.author.layerManager.activateLayer("Annotate");
});

//keymapping for developer layer
jQuery(document).bind('keydown', 'ctrl+shift+d', function() {  
      top.Granite.author.layerManager.activateLayer("Developer");
});

//keymapping for targeting layer
jQuery(document).bind('keydown', 'ctrl+shift+t', function() {  
      top.Granite.author.layerManager.activateLayer("Targeting");
});


jQuery(document).bind('keydown', 'ctrl+shift+s', function() { 
    if ($.cookie("cq-editor-sidepanel") === "open") {
        top.Granite.author.SidePanel.close();
    } else {
        top.Granite.author.SidePanel.open();
    }
});
