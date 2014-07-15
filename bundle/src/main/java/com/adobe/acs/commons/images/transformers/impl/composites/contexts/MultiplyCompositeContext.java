package com.adobe.acs.commons.images.transformers.impl.composites.contexts;

import java.awt.CompositeContext;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;


/**
 * Applies a multiply blend to the models.
 *
 * Follows the rules defined here: http://helpx.adobe.com/after-effects/using/blending-modes-layer-styles.html#Multiply
 *
 * Based on http://www.java2s.com/Code/Java/2D-Graphics-GUI/BlendCompositeDemo.htm    
 */
public class MultiplyCompositeContext implements CompositeContext {
    
    
    private static final int MAX_COLOR_DEPTH = 0xFF;
    private static final int BLEND_SHIFT = 8;
    
    private final float alpha;
    
    
    public MultiplyCompositeContext(float alpha) {
    	this.alpha = alpha;
    }
    
    @Override
    public void compose(Raster src, Raster destIn, WritableRaster dstOut) {

    	//    	int width = Math.min(src.getWidth(), destIn.getWidth());
//        int height = Math.min(src.getHeight(), destIn.getHeight());
//
//        int[] srcPixels = new int[width];
//        int[] destPixels = new int[width];
//
//        // Get a row of pixels.
//        for (int y = 0; y < height; y++) {
//            src.getDataElements(0, y, width, 1, srcPixels);
//            destIn.getDataElements(0, y, width, 1, destPixels);
//            
//            // Each pixel in the row
//            for (int x = 0; x < width; x++) {
//
//                // pixels are stored as INT_ARGB
//                int[] srcPixel = new int[4]; 
//                int[] destPixel = new int[4];
//
//                int pixel = srcPixels[x];
//                
//                srcPixel[ARGBMask.ALPHA.position] = (pixel >> ARGBMask.ALPHA.mask) & MAX_COLOR_DEPTH;
//                srcPixel[ARGBMask.RED.position] = (pixel >> ARGBMask.RED.mask) & MAX_COLOR_DEPTH;
//                srcPixel[ARGBMask.GREEN.position] = (pixel >> ARGBMask.GREEN.mask) & MAX_COLOR_DEPTH;
//                srcPixel[ARGBMask.BLUE.position] = (pixel >> ARGBMask.BLUE.mask) & MAX_COLOR_DEPTH;
//                
//                pixel = destPixels[x];
//                // Clearing out the alpha of the destination
//                destPixel[ARGBMask.ALPHA.position] = (pixel >> ARGBMask.ALPHA.mask) & MAX_COLOR_DEPTH;
//                destPixel[ARGBMask.RED.position] = (pixel >> ARGBMask.RED.mask) & MAX_COLOR_DEPTH;
//                destPixel[ARGBMask.GREEN.position] = (pixel >> ARGBMask.GREEN.mask) & MAX_COLOR_DEPTH;
//                destPixel[ARGBMask.BLUE.position] = (pixel >> ARGBMask.BLUE.mask) & MAX_COLOR_DEPTH;
//                
//                
//                int[] result = blend(srcPixel, destPixel);
//                destPixels[x] = processOpacity(destPixel, result);
//            }
//            dstOut.setDataElements(0, y, width, 1, destPixels);
//        }
        
    }

//    private int[] blend(int[] src, int[] dst) {
//    	int[] result = new int[4];
//        result[ARGBMask.ALPHA.position] = 
//        		Math.min(255, src[ARGBMask.ALPHA.position] + dst[ARGBMask.ALPHA.position] - 
//        				(src[ARGBMask.ALPHA.position] * dst[ARGBMask.ALPHA.position]) / 255);
//        
//        result[ARGBMask.RED.position] = (src[ARGBMask.RED.position] * dst[ARGBMask.RED.position]) >> BLEND_SHIFT;
//        result[ARGBMask.GREEN.position] = (src[ARGBMask.GREEN.position] * dst[ARGBMask.GREEN.position]) >> BLEND_SHIFT;
//        result[ARGBMask.BLUE.position] = (src[ARGBMask.BLUE.position] * dst[ARGBMask.BLUE.position]) >> BLEND_SHIFT;
//        return result;
//    }
//    
//    private int processOpacity(int[] dest, int[] blended) {
//    
//    	int result = 0;
//    	result = result | (((int) (dest[ARGBMask.ALPHA.position] + 
//    				(blended[ARGBMask.ALPHA.position] - dest[ARGBMask.ALPHA.position]) * alpha) & 0xFF) << 24);
//    	
//    	result = result | (((int) (dest[ARGBMask.RED.position] + 
//    				(blended[ARGBMask.RED.position] - dest[ARGBMask.RED.position]) * alpha) & 0xFF) <<  16);
//    	
//    	result = result | (((int) (dest[ARGBMask.GREEN.position] + 
//    				(blended[ARGBMask.GREEN.position] - dest[ARGBMask.GREEN.position]) * alpha) & 0xFF) << 8);
//    	
//    	result = result | ((int) (dest[ARGBMask.BLUE.position] + 
//    				(blended[ARGBMask.BLUE.position] - dest[ARGBMask.BLUE.position]) * alpha) & 0xFF);
//    	return result;
//	}
//
    
    @Override
    public void dispose() {
        
    }
    
    public float getAlpha() {
    	return alpha;
    }
}

