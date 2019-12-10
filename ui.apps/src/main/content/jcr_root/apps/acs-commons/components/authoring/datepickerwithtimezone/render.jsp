<%@taglib prefix="ui" uri="http://www.adobe.com/taglibs/granite/ui/1.0" %>
<ui:includeClientLib categories="acs.commons.datepickerwithtimezone" />
<%--
  ADOBE CONFIDENTIAL
  ___________________

  Copyright 2015 Adobe
  All Rights Reserved.

  NOTICE: All information contained herein is, and remains
  the property of Adobe and its suppliers, if any. The intellectual
  and technical concepts contained herein are proprietary to Adobe
  and its suppliers and are protected by all applicable intellectual
  property laws, including trade secret and copyright laws.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe.
--%><%
%>
<%@ include file="/libs/granite/ui/global.jsp" %>
<%
%>
<%@ page import="org.apache.sling.api.request.RequestPathInfo,
                 org.apache.sling.api.resource.Resource,
                 org.apache.sling.api.resource.ResourceUtil,
                 org.apache.commons.lang3.StringUtils,
                 com.adobe.granite.ui.components.AttrBuilder,
                 com.adobe.granite.ui.components.Config,
                 com.adobe.granite.ui.components.Field,
                 com.adobe.granite.ui.components.Tag,
                 com.day.cq.i18n.I18n" %><%--###
DatePicker
==========

.. granite:servercomponent:: /libs/granite/ui/components/coral/foundation/form/datepicker
   :supertype: /libs/granite/ui/components/coral/foundation/form/field

   A field that allows user to enter date.

   It extends :granite:servercomponent:`Field </libs/granite/ui/components/coral/foundation/form/field>` component.

   It has the following content structure:

   .. gnd:gnd::

      [granite:FormDatePicker] > granite:FormField

      /**
       * The name that identifies the field when submitting the form.
       *
       * The `SlingPostServlet @TypeHint <http://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#typehint>`_ hidden input with value ``Date`` is also generated based on the name.
       */
      - name (String)

      /**
       * The value of `SlingPostServlet @TypeHint <http://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#typehint>`_.
       */
      - typeHint (String) = 'Date'

      /**
       * The value of the field.
       */
      - value (StringEL)

      /**
       * A hint to the user of what can be entered in the field.
       */
      - emptyText (String) i18n

      /**
       * Indicates if the field is in disabled state.
       */
      - disabled (Boolean)

      /**
       * Indicates if the field is mandatory to be filled.
       */
      - required (Boolean)

      /**
       * The name of the validator to be applied. E.g. ``foundation.jcr.name``.
       * See :doc:`validation </jcr_root/libs/granite/ui/components/coral/foundation/clientlibs/foundation/js/validation/index>` in Granite UI.
       */
      - validation (String) multiple

      /**
       * The type of the picker.
       */
      - type (String) = 'date' < 'date', 'datetime', 'time'

      /**
       * The date format for display.
       */
      - displayedFormat (String) i18n

      /**
       * The date format of the actual value, and for form submission.
       */
      - valueFormat = 'YYYY-MM-DD[T]HH:mm:ss.000[Z]'

      /**
       * The minimum boundary of the date.
       */
      - minDate (String)

      /**
       * The maximum boundary of the date.
       */
      - maxDate (String)

      /**
       * Indicates if a informative message should be displayed regarding timezone prevalence
       */
      - displayTimezoneMessage (Boolean)

      /**
       * Specifies a CSS selector targeting another datepickers that are before this datepicker.
       * If those datepickers are not before this datepicker, it will be invalid.
       */
      - beforeSelector (String)

      /**
       * Specifies a CSS selector targeting another datepickers that are after this datepicker.
       * If those datepickers are not after this datepicker, it will be invalid.
       */
      - afterSelector (String)
###--%>
<%

    Config cfg = cmp.getConfig();
    ValueMap vm = (ValueMap) request.getAttribute(Field.class.getName());
    Field field = new Field(cfg);

    boolean isMixed = field.isMixed(cmp.getValue());

    String name = cfg.get("name", String.class);
    String typeHint = cfg.get("typeHint", "Date");

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();
    cmp.populateCommonAttrs(attrs);

    if (isMixed) {
        attrs.addClass("foundation-field-mixed");
    }

    attrs.add("type", cfg.get("type", String.class));
    attrs.add("name", name);
    attrs.add("min", cfg.get("minDate", String.class));
    attrs.add("max", cfg.get("maxDate", String.class));
    attrs.addDisabled(cfg.get("disabled", false));
    attrs.add("displayformat", cfg.get("displayedFormat", "YYYY-MM-DD HH:mm"));

    attrs.add("valueformat", cfg.get("valueFormat", "YYYY-MM-DD[T]HH:mm:ss.000[Z]"));
    attrs.add("headerformat", i18n.get("MMMM YYYY", "Datepicker headline, see moment.js for allowed formats"));

    String fieldLabel = cfg.get("fieldLabel", String.class);
    String fieldDesc = cfg.get("fieldDescription", String.class);
    String labelledBy = null;

    if (fieldLabel != null && fieldDesc != null) {
        labelledBy = vm.get("labelId", String.class) + " " + vm.get("descriptionId", String.class);
    } else if (fieldLabel != null) {
        labelledBy = vm.get("labelId", String.class);
    } else if (fieldDesc != null) {
        labelledBy = vm.get("descriptionId", String.class);
    }

    if (StringUtils.isNotBlank(labelledBy)) {
        attrs.add("labelledby", labelledBy);
    }

    String beforeSelector = cfg.get("beforeSelector");
    if (!StringUtils.isEmpty(beforeSelector)) {
        attrs.add("data-granite-datepicker-before", beforeSelector);
    }

    String afterSelector = cfg.get("afterSelector");
    if (!StringUtils.isEmpty(afterSelector)) {
        attrs.add("data-granite-datepicker-after", afterSelector);
    }

    if (isMixed) {
        attrs.add("placeholder", i18n.get("<Mixed Entries>")); // TODO Maybe define this String somewhere
    } else {
        attrs.add("value", vm.get("value", String.class));
        attrs.add("placeholder", i18n.getVar(cfg.get("emptyText", String.class)));
    }

    attrs.addBoolean("required", cfg.get("required", false));

    String validation = StringUtils.join(cfg.get("validation", new String[0]), " ");
    attrs.add("data-foundation-validation", validation);
    attrs.add("data-validation", validation); // Compatibility

%>
<coral-datepicker <%= attrs %>></coral-datepicker>
<%

    if (!StringUtils.isBlank(name)) {
        AttrBuilder typeAttrs = new AttrBuilder(request, xssAPI);
        typeAttrs.addClass("foundation-field-related");
        typeAttrs.add("type", "hidden");
        typeAttrs.add("value", typeHint);
        typeAttrs.add("name", name + "@TypeHint");

%><input <%= typeAttrs %>><%
    }
if(!cfg.get("disabled", false)) {
        String timeZoneFieldName = name + "tz";
        String selectedTz = cmp.getValue().get(timeZoneFieldName, "");

        RequestPathInfo requestPathInfo = slingRequest.getRequestPathInfo();
        String resourcePath = requestPathInfo.getSuffix();
        Resource cfResource = resourceResolver.resolve(resourcePath + "/jcr:content/data/master");

        if(!ResourceUtil.isNonExistingResource(cfResource)) {
            selectedTz = cfResource.getValueMap().get(timeZoneFieldName, "UTC+00:00");
        }

%>

<coral-select class="datepickertz" name="<%=name%>tz" placeholder="Choose Timezone" style="width:100%">
    <coral-select-item value="UTC+00:00" <%out.print("UTC+00:00".equals(selectedTz) ? "selected" : "");%>>
        (UTC&plusmn;00:00)
    </coral-select-item>
    <coral-select-item value="EST" <%out.print("EST".equals(selectedTz) ? "selected" : "");%>>
        Eastern Time - New York
    </coral-select-item>
</coral-select>
<%}%>
