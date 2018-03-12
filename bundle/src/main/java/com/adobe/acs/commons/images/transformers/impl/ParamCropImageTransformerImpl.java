package com.adobe.acs.commons.images.transformers.impl;

import java.awt.*;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.images.CropConstants;
import com.adobe.acs.commons.images.ImageTransformer;
import com.day.image.Layer;

@Component(label = "ACS AEM Commons - Image Transformer - Crop with URL Parameters")
@Properties({@Property(name = ImageTransformer.PROP_TYPE, value = ParamCropImageTransformerImpl.TYPE, propertyPrivate = true)})
@Service
public class ParamCropImageTransformerImpl extends CropImageTransformerImpl {
	private static final Logger log = LoggerFactory.getLogger(ParamCropImageTransformerImpl.class);

	static final String TYPE = "paramcrop";

	private static final String PROPERTY_COORDINATES_UNIT = "xyCoordinatesUnit";

	private static final String PROPERTY_SIZE_UNIT = "sizeUnit";

	private static final Integer BASE_POINT_UNIT = 10000;

	private static final String PIXELS_UNIT = "px";

	/**
	 * Returns transformed Image. Requires parameters: * X coordinate for crop start * Y coordinate for crop start *
	 * Crop Width * Crop Height Parameters values by default are divided by 10000. To disable this functionality and put
	 * values in pixels two more parameters are required: * xyCoordinatesUnit set to "px" (causes the XY Coordinates
	 * will be in Pixels). * sizeUnit set to "px" (causes the Width and Height will be in Pixels)
	 *
	 * @param layer      the Image layer
	 * @param properties the ValueMap with crop parameters
	 * @return transformed image
	 */
	@Override
	public final Layer transform(final Layer layer, final ValueMap properties) {
		if (properties == null || properties.isEmpty()) {
			log.warn("Transform [ {} ] requires parameters.", TYPE);
			return layer;
		}

		log.debug("Transforming with [ {} ]", TYPE);

		if (checkPropertiesMap(properties)) {
			final boolean smartBounding = properties.get(KEY_SMART_BOUNDING, true);
			boolean xyCoordinatesInPx = PIXELS_UNIT.equalsIgnoreCase(properties.get(PROPERTY_COORDINATES_UNIT,
					String.class));
			boolean sizeInPx = PIXELS_UNIT.equalsIgnoreCase(properties.get(PROPERTY_SIZE_UNIT, String.class));
			Integer propX = properties.get(CropConstants.CROP_START_X_PARAM, Integer.class);
			Integer propY = properties.get(CropConstants.CROP_START_Y_PARAM, Integer.class);
			Integer propWidth = properties.get(CropConstants.CROP_WIDTH_PARAM, Integer.class);
			Integer propHeight = properties.get(CropConstants.CROP_HEIGHT_PARAM, Integer.class);

			if (!(isStartCropInTheLeftTopCorner(propX, propY) && isWidthAndHeightAsOriginal(propWidth, propHeight))) {


				int x = xyCoordinatesInPx ? propX
						: calculateSize(propX, layer.getWidth());
				int y = xyCoordinatesInPx ? propY
						: calculateSize(propY, layer.getHeight());
				int width = sizeInPx ? propWidth : calculateSize(
						propWidth, layer.getWidth());
				int height = sizeInPx ? propHeight : calculateSize(
						propHeight, layer.getHeight());

				Rectangle rectangle = new Rectangle();

				if (smartBounding) {
					rectangle = this.getSmartBounds(x, y, width, height, layer.getWidth(), layer.getHeight());
				} else {
					rectangle.setBounds(x, y, width, height);
				}

				layer.crop(rectangle);

				if ((smartBounding && layer.getWidth() != width) || (layer.getHeight() != height)) {
					log.debug("SmartBounding resulted in an image of an incorrect size (based on crop params). "
							+ "resizing to: [ width: {}, height: {} ]", width, height);
					layer.resize(width, height);
				}
			} else {
				log.debug("With given crop parameters, original image will be returned as crop result. Cropping has been aborted.");
			}
		} else {
			log.warn("Required parameters could not be found. Cropping has been aborted.");
		}

		return layer;
	}

	private boolean isWidthAndHeightAsOriginal(Integer propWidth, Integer propHeight) {
		return propWidth.equals(BASE_POINT_UNIT) && propHeight.equals(BASE_POINT_UNIT);
	}

	private boolean isStartCropInTheLeftTopCorner(Integer propX, Integer propY) {
		return propX == 0 && propY == 0;
	}

	private boolean checkPropertiesMap(ValueMap properties) {
		Set<String> propertiesNames = properties.keySet();
		return propertiesNames.contains(CropConstants.CROP_START_X_PARAM)
				&& propertiesNames.contains(CropConstants.CROP_START_Y_PARAM)
				&& propertiesNames.contains(CropConstants.CROP_WIDTH_PARAM)
				&& propertiesNames.contains(CropConstants.CROP_HEIGHT_PARAM);
	}

	private int calculateSize(Integer value, int imageSize) {
		float val = (float) value / BASE_POINT_UNIT * imageSize;
		return Math.round(val);
	}
}
