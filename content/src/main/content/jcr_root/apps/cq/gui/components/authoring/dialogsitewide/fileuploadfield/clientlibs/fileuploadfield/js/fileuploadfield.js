;(function ($, ns, channel, window, document, undefined) {
    "use strict";

    var _fileUploadClass = "cq-FileUpload",
        _clearButtonClass = "cq-FileUpload-clear",
        _thumbnailClass = "cq-FileUpload-thumbnail",
        _thumbnailImgClass = "cq-FileUpload-thumbnail-img",
        _thumbnailDropHereClass = "cq-FileUpload-thumbnail-dropHere",
        _fileNameClass = "cq-FileUpload-filename",
        _fileReferenceClass = "cq-FileUpload-filereference",
        _fileDeleteClass = "cq-FileUpload-filedelete",
        _fileMoveFromClass = "cq-FileUpload-filemovefrom",

        _cqDropTargetClass = "cq-droptarget",

        _isFilledClass = "is-filled",
        _isHoveredClass = "is-hovered",

         // these are the default parameter, but could be even another property
         // e.g. in video the property for fileReference is called 'asset'
        _fileNameParam = "fileName",
        _fileNameDataAttr = "fileNameParameter",

        _fileReferenceParam = "fileReference",
        _fileReferenceDataAttr = "filereferenceparameter",

        _lastModifiedParam = "jcr:lastModified",
        _lastModifiedByParam = "jcr:lastModifiedBy",

        _fileDeleteParam = "@Delete",
        _fileMoveFromParam = "@MoveFrom",

        // marker class for additional sling servlet fields:
        _additionalSlingParamClass = 'cq-FileUpload-param',

        _clearButtonTpl = "<span class='" + _clearButtonClass + " coral-Button'>" + ns.I18n.get("Clear") + "</span>",
        _thumbnailTpl = "<div class='" + _thumbnailClass + "'>" +
                "<div class='" + _thumbnailDropHereClass + "'>" + ns.I18n.get("Drop asset here") + "</div>" +
                "<div class='" + _thumbnailImgClass + "'></div>" +
            "</div>";

    var TEMP_UPLOAD_SUFFIX = '.sftmp';

    /**
     *
     * @param widget
     * @param resourceURL
     * @constructor
     */
    ns.FileUploadField = function (widget, resourceURL) {
        this.widget = widget;
        this.resourceURL = resourceURL;

        this._computeFieldNames();
        this._createMissingElements();
        this._bindEvents();

        this.widget.$element.addClass(_fileUploadClass);
        this.widget.$element.addClass(_cqDropTargetClass); // to allow interaction with dropController

        // Used when uploading temp file, before form is submitted
        this.widget.options.uploadUrl = resourceURL;
    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._refreshThumbnail = function () {
        var self = this,
            $thumbnail = self.widget.$element.find("." + _thumbnailImgClass),
            thumbnailDom;

        $thumbnail.empty();

        $.ajax({
            url: self.resourceURL + ".json",
            cache: false
        }).done(function (data) {

            var fn = self.fieldNames.fileName.substr(self.fieldNames.fileName.lastIndexOf("/")+1);
            var fr = self.fieldNames.fileReference.substr(self.fieldNames.fileReference.lastIndexOf("/")+1);
            var fileName = data[fn];
            var fileReference = data[fr];
            var displayName = fileReference || fileName;

            if (self._hasImageMimeType()) {
                thumbnailDom = self._createImageThumbnailDom(fileReference || (self.resourceURL + '/file'));
            } else {
                thumbnailDom = $("<p>" + displayName + "</p>");
            }

            if (thumbnailDom) {
                self.widget.$element.addClass(_isFilledClass);
                $thumbnail.append(thumbnailDom);
            }
        });
    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._createImageThumbnailDom = function (imageURL) {
        return $('<img/>', {
            'alt': 'file',
            'class': 'cq-dd-image',
            'src': imageURL + '?:ck=' + (new Date()).getTime()
        });
    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._addElements = function () {
        // add drop here clue, thumbnail container and clear button
        this.widget.$element.append($(_thumbnailTpl));
        this.widget.$element.append($(_clearButtonTpl));
    };


    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._computeFieldNames = function () {
        var fileInput = this.widget.$element.find("input[type=file]"),
            name = fileInput.attr("name") || "",
            path = name.substr(0, name.lastIndexOf("/")) || "",
            pathMatch = path.match(/^\.(\/.+)$/);

        if (name.substr(-TEMP_UPLOAD_SUFFIX.length) === TEMP_UPLOAD_SUFFIX) {
            // File upload is already configured to upload to temp location.
            name = name.substr(0, name.length - TEMP_UPLOAD_SUFFIX.length);
        }

        if (path !== "") {
            path += "/";
        }

        this.fieldNames = {
          fileName       : fileInput.data(_fileNameDataAttr)      || path + _fileNameParam,
          fileReference  : fileInput.data(_fileReferenceDataAttr) || path + _fileReferenceParam,
          lastModified   : path + _lastModifiedParam,
          lastModifiedBy : path + _lastModifiedByParam,
          fileDelete     : name + _fileDeleteParam,
          fileMoveFrom   : name + _fileMoveFromParam,
          tempFileName   : name + TEMP_UPLOAD_SUFFIX,
          tempFileDelete : name + TEMP_UPLOAD_SUFFIX + _fileDeleteParam
        };

        if (pathMatch) {
            // name starts with './'
            this._tempFilePath = this.resourceURL + this.fieldNames.tempFileName.substr(1);

            // Image is stored in sub-path. We need to change resourceURL in order
            // to fetch thumbnail information from the right place
            this.resourceURL += pathMatch[1];
        }
        else {
            this._tempFilePath = this.resourceURL + "/" + this.fieldNames.tempFileName;
        }

        this._tempFilePath = this._tempFilePath
                .replace(new RegExp("^" + ns.HTTP.getContextPath()), "")
                .replace("_jcr_content", "jcr:content");

    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._addParamFields = function(params) {
        var i;

        if (typeof(params) === 'string') {
            try {
                params = JSON.parse(params);
            } catch (e) {
                // ignore erroneous parameters
            }
        }

        if (typeof(params) === 'object') {
            for (i in params) {
                if (params.hasOwnProperty(i)) {
                    this._appendHiddenField(i, _additionalSlingParamClass, params[i]);
                }
            }
        }
    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._addHiddenFields = function () {

        // append hidden sling servlet post parameters
        this._appendHiddenField(this.fieldNames.fileName, _fileNameClass);
        this._appendHiddenField(this.fieldNames.fileReference, _fileReferenceClass, false, true);
        this._appendHiddenField(this.fieldNames.fileDelete, _fileDeleteClass, false, true);
        this._appendHiddenField(this.fieldNames.fileMoveFrom, _fileMoveFromClass, this._tempFilePath, true);

        // avoid caching issues
        this._appendHiddenField(this.fieldNames.lastModified);
        this._appendHiddenField(this.fieldNames.lastModifiedBy);
    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._removeAdditionalSlingParamHiddenFields = function () {
        this.widget.$element.find('input' + _additionalSlingParamClass).remove();
    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._appendHiddenField = function (name, classNames, value, disabled) {
        var $hiddenField = this.widget.$element.closest('form').find("input[name=\"" + name + "\"]"),
            i;

        if ($hiddenField.length === 0) {
            $hiddenField = $("<input type='hidden'/>").attr("name", name);
            this.widget.$element.append($hiddenField);
        } else if ($hiddenField.closest(this.widget.$element).length === 0) {
            return; // Duplicate field elsewhere in the form
        }

        if (classNames !== undefined) {
            if (!$.isArray(classNames)) {
                classNames = [classNames];
            }
            for (i = 0; i < classNames.length; i++) {
                $hiddenField.addClass(classNames[i]);
            }
        }
        if (value !== undefined) {
            $hiddenField.attr("value", value);
        }
        if (disabled !== undefined) {
            $hiddenField.attr("disabled", true);
        }
    };


    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._createMissingElements = function () {


        // add clear button & thumbnail container
        this._addElements();

        // add hidden POST fields
        this._addHiddenFields();

        // add thumbnail if one file has been already uploaded
        this._refreshThumbnail();
    };

    /**
     *
     * @private
     */
    ns.FileUploadField.prototype._bindEvents = function () {
        var self = this,
            lastUploadedFileName = null,
            tempFileUploaded = false;

        // when clear button gets clicked
        self.widget.$element.find("." + _clearButtonClass).on("click tap", function (e) {
            // set hidden delete post parameter
            self.widget.$element.find("." + _fileDeleteClass).removeAttr("disabled").val("true");
            self.widget.$element.find("." + _fileReferenceClass).removeAttr("disabled").val("");
            self.widget.$element.find("." + _fileNameClass).val("");

            self._removeAdditionalSlingParamHiddenFields();

            self.widget.$element.find('.' + _fileMoveFromClass).attr("disabled", true);

            self.widget.$element.removeClass(_isFilledClass);

            self.widget.$element.find("." + _thumbnailImgClass).empty();

            // trigger "change" event (Coral widgets "standard" event)
            self.widget.$element.trigger("change");

        });

        self.widget.$element.on("assetselected", function (event) {

            var assetPath = event.path;
            var assetParams = event.param;
            var assetThumbnail = event.thumbnail;
            var assetMimeType = event.mimetype;

            // check if drop is allowed
            if (!self._isMimeTypeAllowed(assetMimeType)) {
                return;
            }

            self._removeAdditionalSlingParamHiddenFields();

            self.widget.$element.find("." + _fileReferenceClass).removeAttr("disabled").val(assetPath);
            self.widget.$element.find("." + _fileDeleteClass).val("false");
            self.widget.$element.find("." + _fileMoveFromClass).val("false");

            self.widget.$element.find("." + _thumbnailImgClass).empty().append(assetThumbnail);

            self._addParamFields(assetParams);

            self.widget.$element.addClass(_isFilledClass);

            self.widget.$element.trigger("change");
        });

        // when a file from the filesystem gets selected
        self.widget.$element.on("fileselected", function (event) {
            var $form, editablePath, editables, dropTarget, params;

            self._removeAdditionalSlingParamHiddenFields();

            if (self._hasImageMimeType()) {
                $form = self.widget.$element.closest("form");

                if ($form) {
                    editablePath = $form.attr("action").replace("_jcr_content", "jcr:content");

                    if (ns.author && ns.author.store) {
                        editables = ns.author.store.find(editablePath);
                        if (editables.length !== 0) {
                            dropTarget = editables[0].getDropTarget("image");
                            if (dropTarget) {
                                params = dropTarget.params;
                                self._addParamFields(params);
                            }
                        }
                    }
                }
            }

            // Widget handles filename not the way, we need it.
            delete self.widget.fileNameElement;
            delete self.widget.options.fileNameParameter;

            // Upload to temp location
            if (self.widget.options.useHTML5) {
              event.item.fileName = self.fieldNames.tempFileName;

              self.widget.uploadFile(event.item);
            }
            else {
              var fileInput = self.widget.$element.find("input[type=file]"),
                  oldName = fileInput.attr("name");

              lastUploadedFileName = event.item.fileName;

              fileInput.attr("name", self.fieldNames.tempFileName);

              self.widget.uploadFile(event.item);

              fileInput.attr("name", oldName);
            }

            self.widget.$element.trigger("change");
        });

        self.widget.$element.on("dropzonedragover", function (event) {
            self.widget.$element.addClass(_isHoveredClass);
        });

        self.widget.$element.on("dropzonedragleave", function (event) {
            self.widget.$element.removeClass(_isHoveredClass);
        });

        self.widget.$element.on("dropzonedrop", function (event) {
            self.widget.$element.removeClass(_isHoveredClass);
        });

        // handle dnd events only in authoring environment
        // handle asset drop on IE9 (drop zone not listening to DnD events but this is a special case)
        if (Granite.author && !self.widget.options.useHTML5) {
            self.widget.$element.on("drop", function (event) {
                self.widget.$element.trigger("dropzonedrop");
            });
           // FIXME dropzone blinking
           self.widget.$element.on("dragover", function (event) {
               event.stopPropagation();
               event.preventDefault();
               self.widget.$element.trigger("dropzonedragover");
           });
           self.widget.$element.on("dragleave", function (event) {
               event.stopPropagation();
               event.preventDefault();
               self.widget.$element.trigger("dropzonedragleave");
           });
        }


        // Trigger success handler on non-HTML5 browsers
        self.widget.$element.on("fileuploadload", function (event) {
            var status = $(event.content).find("#Status").text(),
                item = event.item || {};

            item = event.item || {};
            item.file = item.file || {};
            item.file.type = item.file.type || "";
            item.file.name = item.file.name || lastUploadedFileName;

            lastUploadedFileName = null;

            self.widget._internalOnUploadLoad(event, item, status, event.content);
        });
        // when a file has been uploaded with success
        self.widget.$element.on("fileuploadsuccess", function (event) {
            var $thumbnail = self.widget.$element.find("." + _thumbnailImgClass),
                file = event.item.file,
                thumbnailDom;

            $thumbnail.empty();

            if (file.type.indexOf("image") !== -1) {

                self.widget.$element.addClass(_isFilledClass);

                (function ($thumbnail, reader) {
                    reader.onload = function(e) {
                        $thumbnail.append($("<img/>").attr("src", e.target.result));
                    };

                    reader.readAsDataURL(file);
                }($thumbnail, new FileReader()));


            } else {
                thumbnailDom = $("<p>" + file.name  + "</p>");
                self.widget.$element.addClass(_isFilledClass);
                $thumbnail.append(thumbnailDom);
            }

            // set hidden post parameters
            self.widget.$element.find("." + _fileReferenceClass).removeAttr("disabled").val("");
            self.widget.$element.find("." + _fileDeleteClass).attr("disabled", "true");
            self.widget.$element.find("." + _fileMoveFromClass).removeAttr("disabled").val(self._tempFilePath);
            self.widget.$element.find("." + _fileNameClass).prop("value", file.name);

            tempFileUploaded = true;
        });

        channel
            .one("dialog-closed", function (e) {
                if (tempFileUploaded) {
                  self._deleteTempUpload();
                }
            });
    };

    /**
     * Sends a delete operation to remove the temporary file upload
     * @param {String} url Path to the temporary upload
     * @returns $.Deferred
     * @private
     */
    ns.FileUploadField.prototype._deleteTempUpload = function () {
        var data = {};
        data[this.fieldNames.tempFileDelete] = true;

        $.post(this.widget.options.uploadUrl, data);
    };

    ns.FileUploadField.prototype._isMimeTypeAllowed = function (mimeType)  {
        var isAllowed = false;

        this.widget.options.mimeTypes.some(function (allowedMimeType) {
            if (allowedMimeType === "*" || (new RegExp(allowedMimeType)).test(mimeType)) {
                isAllowed = true;
                return true;
            }
        });

        return isAllowed;
    };

    ns.FileUploadField.prototype._hasImageMimeType = function() {
        return this.widget.options.mimeTypes.indexOf("image") !== -1;
    };

    // when a dialog shows up, initialize the fileupload field
    $(document).on("foundation-contentloaded", function (event) {
        var $container = $(event.target);

        if ($container.hasClass("cq-dialog")) {
            $container.find(".coral-FileUpload").each(function () {
                var $element = $(this);
                var widget = $element.data("fileUpload");
                var resourceURL = $element.parents("form.cq-dialog").attr("action");

                if (widget) {
                    new ns.FileUploadField(widget, resourceURL);
                }
            });
        }
    });


}(jQuery, Granite, jQuery(document), this, document));
