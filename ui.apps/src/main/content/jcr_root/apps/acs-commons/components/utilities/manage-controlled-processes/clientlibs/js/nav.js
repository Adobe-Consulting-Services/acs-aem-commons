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
        var rightContents, rightMenu;
        rightMenu = window.top.document.getElementsByClassName("granite-actionbar-right")[0];
        if (rightMenu) {
            while (rightMenu.firstChild) {
                rightMenu.removeChild(rightMenu.firstChild);
            }
        }
        rightContents = document.getElementById('right-action-bar-contents');
        if (rightContents) {
            while (rightContents.firstChild) {
                rightMenu.appendChild(rightContents.firstChild);
            }
            rightContents.parentNode.removeChild(rightContents);
        }
    }
};

jQuery(document).ready(MCPMenu.init);