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

    var SELECT_LIST_DEBOUNCE_THRESHOLD = 500;
    var EMPTY_TITLE = Granite.I18n.get('New slide');
    var PARAMS_CONTAINER_CLASS = 'cq-DropArea-params';

    /**
     * A Slide internal reference
     * @typedef {{id: number, name: string, title: string, path: string, fileReference: string, mimeType: string}} Slide
     */

    /**
     * Internal list of slides
     *
     * @type {Array.<Slide>}
     * @private
     */
    var _slides = [];

    /**
     * Currently displayed slide
     *
     * @type {Slide}
     * @private
     */
    var _currentSlide;

    /**
     * Internally used by the id sequence generator
     *
     * @type {number}
     * @private
     */
    var _nextId = 0;

    /**
     * Slideshow content container
     */
    var slideshowContainer;

    /**
     * Container of hidden form params
     *
     * @type {jQuery}
     */
    var paramContainer = $('<span class="' + PARAMS_CONTAINER_CLASS + '"><input type="hidden" name="_charset_" value="utf-8" /></span>');

    /**
     * {@code CUI.Select} widget
     */
    var selectWidget;

    /**
     * Area on which assets are dropped
     */
    var dropArea;

    /**
     * {@code CUI.Textfield}
     */
    var textField;

    /**
     * Generates Sling form parameters based on the given slide
     *
     * @param {Slide} slide - The slide object
     * @param {boolean} [deleteSlide] - Should the slide be deleted from the repository
     */
     function setSlideSlingParams (slide, deleteSlide) {
        if (slide && slide.name && slide.name.length > 0) {
            var regex = new RegExp(/([a-zA-Z0-9_-]+)/);
            var match = regex.exec(slide.name);
            if (match && match.length > 1) {
                var ctnClass = 'cq-DropArea-param-' + match[1];
                var nodeName = './' + slide.name;
                paramContainer.find('.' + ctnClass).remove();
                var slingGroup = $('<span class="' + ctnClass + '"></span>');

                if (deleteSlide) {
                    slingGroup.append('<input type="hidden" name="' + nodeName + '@Delete" value="true">');
                } else {
                    if (slide.title) {
                        slingGroup.append('<input type="hidden" name="' + nodeName + '/jcr:title" value="' + slide.title + '">');
                    } else {
                        slingGroup.append('<input type="hidden" name="' + nodeName + '/jcr:title@Delete" value="delete">');
                    }
                    if (slide.fileReference) {
                        slingGroup.append('<input type="hidden" name="' + nodeName + '/fileReference" value="' + slide.fileReference + '">');
                    } else {
                        slingGroup.append('<input type="hidden" name="' + nodeName + '/fileReference@Delete" value="delete">');
                    }
                }

                paramContainer.append(slingGroup);
            }
        }
    }

    /**
     * Creates one slide from a given HTML element or a jQuery instance
     *
     * @param {jQuery|HTMLElement} element - Source element for the
     * @returns {Slide}
     */
    function createSlide (element) {
        var slide;

        if (element) {
            var $element = element instanceof jQuery ? element : $(element);

            // Get the id of from the name attribute
            var regex = new RegExp(/([0-9]+)$/);
            var name = $element.attr('data-name');
            var match = regex.exec(name);
            var id;

            if (match) {
                id = parseInt(match[1]);
            }

            var fileReference = $element.attr('data-file-reference');

            var text = $element.text();
            if (text === EMPTY_TITLE || text === fileReference) {
                text = '';
            }

            slide = {
                id: id,
                name: name,
                title: text,
                path: $element.attr('data-path'),
                fileReference: fileReference,
                mimeType: $element.attr('data-asset-mimetype')
            };

            setSlideSlingParams(slide);
        }

        return slide;
    }

    /**
     * Returns a new empty slide object
     *
     * @returns {Slide}
     */
    function createNewSlide () {
        var id = getNextId();
        return {
            id: id,
            name: 'image' + id,
            title: '',
            path: '',
            fileReference: '',
            mimeType: ''
        };
    }

    /**
     * Initializes the {@code _nextId} sequence from the list of slides to the biggest available value or {@code 0}
     *
     * @param {Array.<Slide>} slides - List of slides
     */
    function initNextId (slides) {
        var id = -1;

        for (var i = 0; i < slides.length; i++) {
            if (id < slides[i].id) {
                id = slides[i].id;
            }
        }

        _nextId = id + 1;
    }

    /**
     * Returns the next available id
     *
     * @returns {number}
     */
    function getNextId () {
        return _nextId++;
    }

    /**
     * Initializes the list of available slides
     *
     * @param slideList
     */
    function initSlides (slideList) {
        _slides = [];

        var options = slideList.find('option');
        if (options.length > 0) {
            options.each(function () {
                var slide = createSlide(this);
                _slides.push(slide);
            });
        } else  {
            // Init with an empty slide
            var slide = createNewSlide();
            _currentSlide = slide;
            _slides.push(slide);
            addOption(slide);
            selectWidget.setValue(slide.name);
        }

        initNextId(_slides);
    }

    /**
     * Returns the position of the given slide in the list
     *
     * @param {Slide} slide - Actual slide to be found
     * @returns {number} - Position of the slide in the list
     */
    function getSlidePosition (slide) {
        for (var i = 0; i < _slides.length; i++) {
            if (slide.name === _slides[i].name) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the Option from the {@code CUI.Select} Widget
     *
     * @param {string} value - Value of the targeted {@code HTMLelement} option
     * @returns {jQuery} - jQuery Object encapsulating the {@code HTMLelement} option
     */
    function getOption (value) {
        return selectWidget.$element.find('option[value="' + value + '"]').eq(0);
    }

    /**
     * Sets the current slide based on {@code value}'s type and data
     *
     * If {@code value} is null or undefined, current slide is set to the very first option contained in the {@code CUI.Select} Widget
     *
     * @param {Slide|string} [value] - Value of the slide to be set as currently being displayed. Can be either a slide {@code Object}, a string or not set
     */
    function setCurrentSlide (value) {
        var option;

        if (value && value.name) {
            _currentSlide = value;
            setSlideDetail(_currentSlide);
            return;
        } else if (value && value.length > 0) {
            option = getOption(value);
        } else {
            option = selectWidget.$element.find('option').eq(0);
        }

        _currentSlide = createSlide(option);
        setSlideDetail(_currentSlide);
    }

    /**
     * Fills the detail fields with the given slide data
     *
     * @param {Slide} slide - Slide for which data should be displayed
     */
    function setSlideDetail (slide) {
        textField.val(slide && slide.title ? slide.title : '');
        dropArea.trigger($.Event('assetselected', {
            path: slide && slide.fileReference ? slide.fileReference : '',
            silent: true
        }));
    }

    /**
     * Returns the string to be displayed for the given slide
     *
     * @param {Slide} slide - The slide from which to extract the string
     * @returns {string} - The string to be displayed in the {@code CUI.Select#display} Widget for the given slide
     */
    function getOptionDisplayFromSlide (slide) {
        if (slide.title && slide.title.length > 0) {
            return slide.title;
        } else if (slide.fileReference && slide.fileReference > 0) {
            return slide.fileReference;
        }

        return EMPTY_TITLE;
    }

    /**
     * Updates the values of a {@code CUI.SelectList.Option} with the given slide
     *
     * @param {Slide} slide - The slide corresponding to the option to be updated
     * @param {number} [position] - Position of the slide in the list. If no position is provided, tries to determine position from the {@code slide} itself
     */
    function updateOption (slide, position) {
        if (!position || position < 0) {
            position = getSlidePosition(slide);
        }

        if (position === -1) {
            slides.push(slide);
            position = slides.length - 1;
        }

        // No better way found to update an option's display than deleting it and recreating a new option at the very same place
        if (selectWidget.getItems().length > 0 && position >= 0 && position < selectWidget.getItems().length) {
            selectWidget.getOption(position).remove();
        }

        addOption(slide, position);
        selectWidget.setValue(slide.name);
        setSlideSlingParams(slide);
    }

    /**
     * Adds a new {@code CUI.SelectList.Option} to the {@code CUI.Select} based on the given slide's data
     *
     * @param {Slide} slide - The slide from which to extract data from
     * @param {number} [position] - the position where to add the {@code CUI.SelectList.Option}
     */
    function addOption (slide, position) {
        // Add option if it is not yet in the list
        var option = getOption(slide.name);
        if (option.length === 0) {
            selectWidget.addOption(adaptSlideToOption(slide), ($.isNumeric(position) && position < selectWidget.getItems().length) ? position : undefined);
        }

        option = getOption(slide.name);

        // Add Data attributes
        if (option.length > 0) {
            option.attr('data-name', slide.name);
            option.attr('data-path', slide.path);
            option.attr('data-file-reference', slide.fileReference);
            option.attr('data-data-asset-mimetype', slide.mimeType);
        }
    }

    /**
     * Pushes the current Slide to the list of {@code CUI.SelectList.Option} of the {@code CUI.Select},
     * creates a new empty {@code slide} and sets it as the current slide
     */
    function addNewSlideBtnCallback () {
        addOption(_currentSlide);
        var newSlide = createNewSlide();
        _slides.push(newSlide);
        setCurrentSlide(newSlide);
        setSlideSlingParams(_currentSlide);
    }

    /**
     * Deletes the current slide
     */
    function deleteCurrentSlideBtnCallback () {
        var i = getSlidePosition(_currentSlide);
        var removedSlide;
        // Next slide to be displayed after the deletion
        var nextSlide;

        if (i >= 0) {
            removedSlide = _slides.splice(i, 1)[0];

            if (removedSlide && i < selectWidget.getItems().length) {
                selectWidget.getOption(i).remove();

                if (_slides.length > 0) {
                    if (i > 0) {
                        nextSlide = i < _slides.length -1 ? _slides[i] : _slides[i - 1];
                    } else {
                        nextSlide = _slides[0];
                    }
                } else {
                    // If there is no more slide in the list, creates a new empty slide
                    nextSlide = createNewSlide();
                    _slides.push(nextSlide);
                }

                if (nextSlide && nextSlide.path && nextSlide.path.length > 0) {
                    selectWidget.setValue(nextSlide.name);
                }

                setCurrentSlide(nextSlide);
                setSlideSlingParams(removedSlide, true);
            }
        }
    }

    /**
     * Returns the necessary data for the {@code CUI.SelectList.Option} configuration {@code Object} from the given {@code slide}
     *
     * @param {Slide} slide - The slide to extract data from
     * @returns {{display: string, value: string}}
     */
    function adaptSlideToOption (slide) {
        return {
            display: getOptionDisplayFromSlide(slide),
            value: slide.name
        };
    }

    channel.on('foundation-contentloaded', function() {
        slideshowContainer = $('.cq-Slideshow-dialog-content').eq(0);
        if (slideshowContainer.length > 0) {
            var slideList = slideshowContainer.find('.cq-Slideshow-dialog-select').eq(0);
            var addSlideBtn = slideshowContainer.find('.js-Slideshow-add').eq(0);
            var deleteSlideBtn = slideshowContainer.find('.js-Slideshow-delete').eq(0);
            _nextId = 0;
            dropArea = slideshowContainer.find('.cq-DropArea').eq(0);
            textField = slideshowContainer.find('.coral-Textfield').eq(0);
            selectWidget = slideList.data('select');

            if (slideshowContainer.find('.' + PARAMS_CONTAINER_CLASS).length === 0) {
                slideshowContainer.prepend(paramContainer);
            }

            initSlides(selectWidget.$element);
            setCurrentSlide(_slides && _slides.length > 0 ? _slides[0] : createNewSlide());
            setSlideDetail(_currentSlide);

            addSlideBtn.click($.debounce(SELECT_LIST_DEBOUNCE_THRESHOLD, true, addNewSlideBtnCallback));
            deleteSlideBtn.click($.debounce(SELECT_LIST_DEBOUNCE_THRESHOLD, true, deleteCurrentSlideBtnCallback));

            // Listen for Select Widget selection change
            selectWidget.$element.on('selected', function(event) {
                setCurrentSlide(event.selected);
            });

            // listen for drag and drop events on the DropArea
            dropArea.on('assetselected', function (event) {
                _currentSlide.path = '';
                _currentSlide.fileReference = event.path;
                _currentSlide.mimeType = event.mimetype;

                updateOption(_currentSlide);
            });

            // Update the slide title from the title field
            textField.on('blur keyup', function (event) {
                _currentSlide.title = event.target.value;
                var i = getSlidePosition(_currentSlide);

                if (i <= _slides.length) {
                    updateOption(_currentSlide, i);
                }
            });
        }
    });

}(jQuery, Granite.author, jQuery(document), this, document));