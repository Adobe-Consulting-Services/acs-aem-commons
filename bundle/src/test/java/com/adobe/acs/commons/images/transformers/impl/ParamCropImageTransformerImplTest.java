package com.adobe.acs.commons.images.transformers.impl;

import com.adobe.acs.commons.images.CropConstants;
import com.day.image.Layer;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParamCropImageTransformerImplTest {

	private static final String PROPERTY_COORDINATES_UNIT = "xyCoordinatesUnit";

	private static final String PIXELS_UNIT = "px";


	private static final String PROPERTY_SIZE_UNIT = "sizeUnit";

	private ParamCropImageTransformerImpl transformer;

	@Mock
	Layer layer;

	Map<String, Object> map = null;

	@Before
	public void setUp() throws Exception {
		map = new HashMap<String, Object>();
		transformer = new ParamCropImageTransformerImpl();

		when(layer.getWidth()).thenReturn(1600);
		when(layer.getHeight()).thenReturn(900);
	}

	@After
	public void tearDown() throws Exception {
		reset(layer);
		map = null;
	}

	@Test
	public void testTransform() throws Exception {
		Rectangle expected = new Rectangle();
		expected.setBounds(160, 180, 480, 360);

		map.put(CropConstants.CROP_START_X_PARAM, "1000");
		map.put(CropConstants.CROP_START_Y_PARAM, "2000");
		map.put(CropConstants.CROP_WIDTH_PARAM, "3000");
		map.put(CropConstants.CROP_HEIGHT_PARAM, "4000");

		ValueMap properties = new ValueMapDecorator(map);

		transformer.transform(layer, properties);

		verify(layer, times(1)).crop(expected);
	}

	@Test
	public void testTransform_no_crop() throws Exception {
		Rectangle expected = new Rectangle();
		expected.setBounds(0, 0, 1600, 900);

		ValueMap properties = new ValueMapDecorator(map);

		Layer result = transformer.transform(layer, properties);

		assertThat(result, is(layer));
	}

	@Test
	public void testTransformInPixels() throws Exception {
		Rectangle expected = new Rectangle();
		expected.setBounds(100, 100, 300, 400);

		map.put(CropConstants.CROP_START_X_PARAM, "100");
		map.put(CropConstants.CROP_START_Y_PARAM, "100");
		map.put(CropConstants.CROP_WIDTH_PARAM, "300");
		map.put(CropConstants.CROP_HEIGHT_PARAM, "400");
		map.put(PROPERTY_COORDINATES_UNIT, PIXELS_UNIT);
		map.put(PROPERTY_SIZE_UNIT, PIXELS_UNIT);
		ValueMap properties = new ValueMapDecorator(map);

		transformer.transform(layer, properties);

		verify(layer, times(1)).crop(expected);
	}

	@Test
	public void testTransform_smartBoundsWidth() throws Exception {
		// 1600 x 900
		Rectangle expected = new Rectangle();
		expected.setBounds(0, 0, 1600, 400);

		map.put(CropConstants.CROP_START_X_PARAM, "0");
		map.put(CropConstants.CROP_START_Y_PARAM, "0");
		map.put(CropConstants.CROP_WIDTH_PARAM, "12500");
		map.put(CropConstants.CROP_HEIGHT_PARAM, "5550");
		ValueMap properties = new ValueMapDecorator(map);

		transformer.transform(layer, properties);

		verify(layer, times(1)).crop(expected);
	}

	@Test
	public void testTransform_smartBoundsHeight() throws Exception {
		// 1600 x 900
		Rectangle expected = new Rectangle();
		expected.setBounds(0, 0, 400, 900);

		map.put(CropConstants.CROP_START_X_PARAM, "0");
		map.put(CropConstants.CROP_START_Y_PARAM, "0");
		map.put(CropConstants.CROP_WIDTH_PARAM, "5000");
		map.put(CropConstants.CROP_HEIGHT_PARAM, "20000");
		ValueMap properties = new ValueMapDecorator(map);

		transformer.transform(layer, properties);

		verify(layer, times(1)).crop(expected);
	}

	@Test
	public void testTransform_smartBoundsBoth_width() throws Exception {
		// 1600 x 900
		Rectangle expected = new Rectangle();
		expected.setBounds(0, 0, 1600, 400);

		map.put(CropConstants.CROP_START_X_PARAM, "0");
		map.put(CropConstants.CROP_START_Y_PARAM, "0");
		map.put(CropConstants.CROP_WIDTH_PARAM, "125000");
		map.put(CropConstants.CROP_HEIGHT_PARAM, "55550");
		ValueMap properties = new ValueMapDecorator(map);

		transformer.transform(layer, properties);

		verify(layer, times(1)).crop(expected);
	}

	@Test
	public void testTransform_smartBoundsBoth_height() throws Exception {
		// 1600 x 900
		Rectangle expected = new Rectangle();
		expected.setBounds(0, 0, 400, 900);

		map.put(CropConstants.CROP_START_X_PARAM, "0");
		map.put(CropConstants.CROP_START_Y_PARAM, "0");
		map.put(CropConstants.CROP_WIDTH_PARAM, "50000");
		map.put(CropConstants.CROP_HEIGHT_PARAM, "200000");
		ValueMap properties = new ValueMapDecorator(map);

		transformer.transform(layer, properties);

		verify(layer, times(1)).crop(expected);
	}

	@Test
	public void testTransform_emptyParams() throws Exception {
		ValueMap properties = new ValueMapDecorator(map);

		transformer.transform(layer, properties);

		verifyZeroInteractions(layer);
	}

	@Test
	public void testTransform_nullParams() throws Exception {
		transformer.transform(layer, null);

		verifyZeroInteractions(layer);
	}
}
