/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2019 Adobe
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
 * Enables use of the templated textfield dialog field. Listens to the author's input and generates
 * the Coral UI dropdown with the available options for replacement.
 * Add a field to the dialog with the resource type of
 * acs-commons/components/authoring/templatedtextfield to enable the dropdown.
 */
(function ($, $document) {
  "use strict";

  Coral.commons.ready('.templated-textfield', function() {
    var lastKey = 0;
    var skipUpdate = false;
    var bracketKeyNum = 219;

    function insertAt(original, add, position) {
      return original.substring(0, position) + add + original.substring(position);
    }

    $(document).on('templated-textfield:ready', function() {
      $('.templated-textfield').on('keyup', function(e) {
        var $this = $(this);
        var isDown = e.keyCode == 40;
        var emptyPlaceholder = $this.val().indexOf('{{}}') !== -1;

        // Check if the current key and the last key pressed (skipping the Shift key) was the bracket character "{"
        // Also check if the user has an empty placeholder and is pressing down to show the dropdown again
        if ((e.keyCode == bracketKeyNum && lastKey == bracketKeyNum) || (isDown && emptyPlaceholder)) {
          lastKey = 0;
          skipUpdate = true;

          // Set the current cursor location
          var currentLoc;
          if (isDown) {
            currentLoc = $this.val().indexOf('{{}}') + 2;
          } else {
            currentLoc = this.selectionStart;
            $this.val(insertAt($this.val(), '}}', this.selectionStart));
          }
          this.selectionStart = currentLoc;
          this.selectionEnd = currentLoc;

          // Start popup
          var properties = $('#property-datasource').data('properties');
          var dropdown = new Coral.SelectList();
          dropdown.hidden = true;

          // Create the groups by splitting the 'prefix.propertyName' strings
          var grouped = {};
          $.each(properties, function(key, value) {
            var parts = key.split('.');
            var group = parts[0];
            var propertyName = parts[1];
            if (!grouped.hasOwnProperty(group)) {
              grouped[group] = [];
            }
            grouped[group].push(propertyName);
          });

          // Iterating over the groups and creating the select items/groups with the values
          $.each(grouped, function(key, value) {
            var selectGroup = new Coral.SelectList.Group();
            selectGroup.label = key;
            value.sort();
            $.each(value, function(index) {
              var item = new Coral.SelectList.Item();
              var propertyName = value[index];
              item.content.innerHTML = propertyName;
              item.content.value = key + '.' + propertyName;
              selectGroup.items.add(item);
            });
            dropdown.groups.add(selectGroup);
          });

          // Setup the Coral UI Overlay to house all the items in the dropdown list
          var overlay = new Coral.Overlay();
          overlay.target = '.templated-textfield';
          dropdown.style.maxHeight = '200px';
          dropdown.show();
          overlay.innerHTML = dropdown.outerHTML;
          overlay.style.minWidth = this.offsetWidth + 'px';
          overlay.interaction = 'on';
          overlay.alignMy = Coral.Overlay.align.LEFT_TOP;
          overlay.alignAt = Coral.Overlay.align.LEFT_BOTTOM;
          $this.after(overlay);
          overlay.show();

          // Listener the SelectList change event to take the value from the item selected and add it
          // into the user's authored text
          var $selectList = $this.next('coral-overlay').find('coral-selectlist');
          $selectList.on('coral-selectlist:change', function(event) {
            var value = event.target.selectedItem.content.value;
            $this.val(insertAt($this.val(), value, currentLoc));
            overlay.remove();
            $this.focus();
            var cursorLoc = currentLoc + value.length + 2;
            $this.get(0).selectionStart = cursorLoc;
            $this.get(0).selectionEnd = cursorLoc;
          });

          // Click event to remove the overlay
          $(document).on('click', function(e) {
            var target = $(e.target);
            var targetOverlay = target.closest('coral-overlay');
            if (targetOverlay.length === 0 && !overlay.hidden) {
              overlay.remove();
            }
          });
        }
        if (e.keyCode !== 16 && !skipUpdate) {
          lastKey = e.keyCode;
        }
        skipUpdate = false;
      });
    });

    // Listener to focus the dropdown list of elements when the overlay is opened
    $(document).on('coral-overlay:open', function(e) {
      var overlay = $('.templated-textfield').next('coral-overlay');
      if (overlay.length > 0 && overlay.hasClass('is-open')) {
        overlay.find('coral-selectlist').focus();
      }
    });
  });
}(jQuery, jQuery(document)));
