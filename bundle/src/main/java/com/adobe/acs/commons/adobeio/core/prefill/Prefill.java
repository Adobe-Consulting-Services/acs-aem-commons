package com.adobe.acs.commons.adobeio.core.prefill;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.core.service.EndpointService;
import com.adobe.forms.common.service.DataXMLOptions;
import com.adobe.forms.common.service.DataXMLProvider;
import com.adobe.forms.common.service.FormsException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Example on how the Adobe I/O connection can be used for pre-filling a form in AEM-forms
 *
 */
@Component(service = DataXMLProvider.class)
public class Prefill implements DataXMLProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(Prefill.class);

	@Reference(target = "(getId=getPrefillData)")
	private EndpointService acsEndpointService;

	@Override
	public String getServiceDescription() {
		return "Prefill from ACS";
	}

	@Override
	public String getServiceName() {
		return "acsprefill";
	}

	@Override
	public InputStream getDataXMLForDataRef(DataXMLOptions options) throws FormsException {
		LOGGER.info("in getDataXMLForDataRef");

		// Getting data from ACS via Adobe I/O
		JsonObject result = acsEndpointService.performIOAction();

		if (result.has("content")) {
			// when we have results, we get the last one
			JsonArray arr = result.getAsJsonArray("content");
			if (arr != null && arr.size() > 0) {
				JsonElement profile = arr.get(arr.size() - 1);

				// building the xml output for the prefill service
				String prefillData = "<afData><afUnboundData><dataRoot>";
				prefillData += "<firstName>" + profile.getAsJsonObject().get("firstName").getAsString() + "</firstName>";
				prefillData += "<lastName>" + profile.getAsJsonObject().get("lastName").getAsString() + "</lastName>";
				prefillData += "</dataRoot></afUnboundData></afData>";
				return new ByteArrayInputStream(prefillData.getBytes());
			}
		}
		return null;

	}

}
