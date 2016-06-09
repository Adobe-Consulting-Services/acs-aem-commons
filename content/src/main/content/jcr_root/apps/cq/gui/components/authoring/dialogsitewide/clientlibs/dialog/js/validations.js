/*
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
*/
(function($, Granite) {
    "use strict";
    
    var unitPattern = /^([0-9]+(\.[0-9]+)?(px|em|ex|%|in|cm|mm|pt|pc))$/;
    
    /**
     * A validation for a valid CSS unit.
     */
    $.validator.register({
        selector: '[data-validation="cq.cssunit"]',
        validate: function(el) {
            var valid = el.val().length === 0 || unitPattern.test(el.val());
            
            if (!valid) {
                return Granite.I18n.get("Must be a valid CSS length. ex: 10px or 2.3em");
            }
        }
    });

    var formNamePattern = /^[a-zA-Z0-9_\\.\\/:\\-]+$/;

    /**
     * A validation for form field names.
     */
    $.validator.register({
        selector: '[data-validation="cq.formfieldname"]',
        validate: function(el) {
            var valid = el.val().length === 0 || formNamePattern.test(el.val());

            if (!valid) {
                return Granite.I18n.get("Element name should only contain characters, numbers or _./:-");
            }
        }
    });

})(Granite.$, Granite);
