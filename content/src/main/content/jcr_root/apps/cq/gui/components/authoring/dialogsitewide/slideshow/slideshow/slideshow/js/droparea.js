/*
 * ADOBE CONFIDENTIAL
 *
 * Copyright 2015 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
;(function ($, ns, channel, window, document, undefined) {

    var DROPAREA_CLASS = 'cq-DropArea';
    var DROPAREA_IMAGE_CLASS = 'cq-DropArea--image';
    var THUMBNAIL_CLASS = 'cq-DropArea-thumbnail';
    var PLACEHOLDER_CLASS = 'cq-DropArea-placeholder';

    /**
     * Updates the DropArea content based on the given event's data
     *
     * @param {jQuery} placeholder - Text to be displayed as an invite when no content is available
     * @param {jQuery} thumbnail - Thumbnail to be potentially displayed
     * @param {string} [path] - Thumbnail source path
     */
    var update = function (placeholder, thumbnail, path) {
        if (placeholder && placeholder.length > 0) {
            placeholder.toggle(!(path && path.length > 0));
        }

        path = path || '';
        if (thumbnail && thumbnail.length > 0) {
            thumbnail.attr("src", path);
        }
    };

    channel.on('foundation-contentloaded', function() {
        channel.find('.' + DROPAREA_CLASS).each(function () {
            var dropArea = $(this);
            var placeholderTxt;

            if (dropArea.hasClass(DROPAREA_IMAGE_CLASS)) {
                placeholderTxt = Granite.I18n.get('Drop an image');
            } else {
                placeholderTxt = Granite.I18n.get('Drop an asset');
            }

            var placeholder = $('<span class=".' + PLACEHOLDER_CLASS + '">' + placeholderTxt + '</span>');
            var thumbnail = $('<img class="' + THUMBNAIL_CLASS + '"/>');

            dropArea.empty();
            dropArea.append(placeholder);
            dropArea.append(thumbnail);

            dropArea.on("assetselected", function (event) {
                if (event.silent) {
                    event.stopImmediatePropagation();
                }

                update(placeholder, thumbnail, event.path);
            });
        });
    });

}(jQuery, Granite.author, jQuery(document), this, document));