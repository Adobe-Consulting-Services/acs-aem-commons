/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 by codeflair Gmbh
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
 *
 *
 * implements a keyboard shortcut ctrl-shift-x to switch both the inline and full screen rich text editor to
 * right-to-left mode and back.
 */
(function ($) {

    var isCtrl = false;
    var isShift = false;

    function rtekeyUp(e) {
        // if control or shift key is released, reset the values
        if (e.which === 17) {
            isCtrl = false;
        }
        if (e.which === 16) {
            isShift = false;
        }
    }

    function rtekeyDown(key, target) {
		//check if the user is in an rte editor
        if (!target.hasClass('coral-RichText-editor') && !target.hasClass('is-edited')) {
            return;
        }
        //check if one of the required keys is pressed
		//key event 17 is the ctrl key
        if (key === 17) {
            isCtrl = true;
        }
		//key event 16 is the shift key
        if (key === 16) {
            isShift = true;
        }
		//key event 88 is the 'x' key
        if (key === 88 && isCtrl && isShift) {
            if (target.attr("dir") === "rtl") {
				//current direction is right-to-left, so change direction back to left-to-right
                target.attr("dir", "ltr");
            } else {
				//current direction is left-to-right, so change direction to right-to-left
                target.attr("dir", "rtl");
            }
        }
    }

    function getInlineEditorTarget(e) {
        return $(e.currentTarget).find(".is-edited");
    }

    //register listener on document for full screen rte
    document.addEventListener('keydown', function (e) {
        rtekeyDown(e.which, $(e.currentTarget.activeElement));
    }, false);
    document.addEventListener('keyup', rtekeyUp, false);

    var eventsRegistered = [];
    $(document).on('foundation-contentloaded', function () {
        //register key events on body inside editing-iframe for the inline rte
        //on the early contentloaded events, the body element is a different one than later on.
        var body = $("#ContentFrame").contents().find("body");
        if (body.length === 0) {
            return;
        }
        body = body.get(0);
        try {
            eventsRegistered.indexOf(body);
        } catch (err) {
            //strange bug in IE prevents access to removed dom elements or so
            eventsRegistered = [];
        }
        //ensure that we do not register the events twice on the same body node
        if (eventsRegistered.indexOf(body) === -1) {
            $(body).on('keydown', function (e) {
                rtekeyDown(e.which, getInlineEditorTarget(e));
            });
            $(body).on('keyup', rtekeyUp);
            eventsRegistered.push(body);
        }
    });
})(window.jQuery);