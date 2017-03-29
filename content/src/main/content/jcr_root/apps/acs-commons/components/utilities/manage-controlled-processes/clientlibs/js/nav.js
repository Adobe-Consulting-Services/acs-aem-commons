/* 
 * Copyright 2017 Adobe.
 *
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
 */
/* global jQuery, Granite */
var MCPMenu = {
    init: function () {
        var menu = document.getElementById('mcp-main-menu');
        if (menu) {
            menu.on('coral-columnview:activeitemchange', MCPMenu.menuSelected);
        }
    },
    menuSelected: function (evt) {
        var detail = evt.detail.activeItem.attributes['data-src'].value;
        var url = Granite.HTTP.getPath() + "." + detail + ".html";
        document.getElementById('mcp-workspace').innerHTML="<iframe src='"+url+"' style='display:block; width: 100%; height:100%; border:none'></iframe>";
    }
};

jQuery(document).ready(MCPMenu.init);