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
/*global Granite: false */
(function ($, $document) {
	"use strict";
	

    var foundationReg = $(window).adaptTo('foundation-registry');

    var currentPagePath;

	var duplicateErrorMessage = Granite.I18n.get('This path is already used. Please use another path.');
	var emptyErrorMessage = Granite.I18n.get('This path cannot be empty.');
	var multiFieldErrorMessage = Granite.I18n.get('This path is already used or is empty.');

    var vanityPath = {
        input:{
            selector: 'input[name="./sling:vanityPath"]',
            errorMessage: {
                duplicate: duplicateErrorMessage,
                    empty: emptyErrorMessage
            }
        },
        multifield:{
            errorMessage: multiFieldErrorMessage,
            selector: '[data-cq-msm-lockable="sling:vanityPath"]'
        },

    };

    var checkLoadValidator = function (){
        var hasInput = document.querySelectorAll(vanityPath.multifield.selector).length > 0;
        if (hasInput) {
            loadVanityPathValidator();
            var item = getParameterByName("item");
            if(item){
                currentPagePath = getParameterByName("item") +'/jcr:content';
            }
        }
    };
    

    var getParameterByName = function (name) {
        var match = new RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
        return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
    };
    var findDuplicates = function(arr){
        return arr.filter(function(item, index){ 
		return arr.indexOf(item) != index;
		});
	};

    var validateSelection = function(selection){
        var returnMessage;
        var getData = {
            url: "/bin/acs-commons/vanity-path-validator",
            type: "GET",
            async: false,
            data: {
                _charset_: "UTF-8",
                url: selection,
                excludePath: currentPagePath
            }
        };
        $.ajax(getData).done(function(response) {
             if(response.isUnique !== true){
                 returnMessage = vanityPath.input.errorMessage.duplicate;
             }
        });
        return returnMessage;
    };
    var loadVanityPathValidator = function(){
        var multifieldSelector = vanityPath.multifield.selector;
        var multifield = document.querySelector(multifieldSelector);


   	    var triggerVanityUrlValidator = function(){
            if(typeof timer !== undefined){
                clearTimeout(this.timer);
            }
            this.timer= setTimeout(function validate(){
                var multifieldValidator = $(multifieldSelector).adaptTo('foundation-validation');
                multifieldValidator.checkValidity();
                multifieldValidator.updateUI();
            },1000);
        };

        if(multifield){
            multifield.addEventListener( "keyup", triggerVanityUrlValidator );
        }

        var validateInput = function(input){
            var $input = $(input).adaptTo('foundation-validation');
            $input.setCustomValidity("");
            $input.checkValidity();
            $input.updateUI();
        };
        var setInputAsInvalid = function(input){
            var $input = $(input).adaptTo('foundation-validation');
            $input.setCustomValidity(vanityPath.input.errorMessage.duplicate);
            $input.updateUI();
        };

        foundationReg.register('foundation.validation.validator', {
            selector: multifieldSelector,
            validate: function(el) {
                var inputs = el.querySelectorAll(vanityPath.input.selector);
                if(inputs && inputs.length > 0){
                    if(inputs.length === 1){
                        validateInput(inputs);
                    }else{
                        var inputValues = [];
                        inputs.forEach(function(input){
                            inputValues.push(input.value);
                        });
                        var duplicates = findDuplicates(inputValues);
                        inputs.forEach(function(input){
                            if(duplicates && duplicates.length > 0 && duplicates.indexOf(input.value) > -1){
                                setInputAsInvalid(input);
                            }else{
                                validateInput(input);
                            }
                        });
                    }
                }
            }
        });

        foundationReg.register("foundation.validation.selector", {
            submittable: vanityPath.input.selector,
            candidate: vanityPath.input.selector +':not([disabled]):not([readonly])',
            exclusion: vanityPath.input.selector + ' *'
        });

        foundationReg.register('foundation.validation.validator', {
            selector: vanityPath.input.selector,
            validate: function(el) {
                return (el.value) ? validateSelection(el.value) : vanityPath.input.errorMessage.empty;
            }
        });
    };
	Coral.commons.ready(function(el) {
        checkLoadValidator();
    });
    $(document).on('dialog-ready', function(e) {
        checkLoadValidator();
    });



}(jQuery, jQuery(document)));
