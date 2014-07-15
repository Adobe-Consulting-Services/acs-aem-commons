package com.adobe.acs.commons.images.transformers.impl.composites;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Applies a multiply blend to the models.
 *
 * Follows the rules defined here: http://helpx.adobe.com/after-effects/using/blending-modes-layer-styles.html#Multiply
 *
 * Based on http://www.java2s.com/Code/Java/2D-Graphics-GUI/BlendCompositeDemo.htm    
 */
public class MultiplyBlendComposite implements Composite {

    
    private float alpha;
    
    public MultiplyBlendComposite(float alpha) {
        this.alpha = alpha;
    }
    
    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        
        return new MultiplyCompositeContext();
    }

    enum ColorMask {
        
    	ALPHA(24, 0),
        RED(16, 1),
        GREEN(8, 2),
        BLUE(0, 3);
        
        private int mask;
        private int position;
        
        ColorMask(int mask, int position) {
            this.mask = mask;
            this.position = position;
        }

        int getMask() {
            return mask;
        }
        
        int getPosition() {
        	return position;
        }
    }
    
    class MultiplyCompositeContext implements CompositeContext {
        
       
        private static final int MAX_COLOR_DEPTH = 0xFF;
        private static final int BLEND_SHIFT = 8;
        
        @Override
        public void compose(Raster src, Raster destIn, WritableRaster dstOut) {
        	
        	int width = Math.min(src.getWidth(), destIn.getWidth());
            int height = Math.min(src.getHeight(), destIn.getHeight());

            int[] srcPixels = new int[width];
            int[] destPixels = new int[width];

            // Get a row of pixels.
            for (int y = 0; y < height; y++) {
                src.getDataElements(0, y, width, 1, srcPixels);
                destIn.getDataElements(0, y, width, 1, destPixels);
                
                // Each pixel in the row
                for (int x = 0; x < width; x++) {

                    // pixels are stored as INT_ARGB
                    int[] srcPixel = new int[4]; 
                    int[] destPixel = new int[4];

                    int pixel = srcPixels[x];
                    srcPixel[ColorMask.ALPHA.position] = (pixel >> ColorMask.ALPHA.mask) & MAX_COLOR_DEPTH;
                    srcPixel[ColorMask.RED.position] = (pixel >> ColorMask.RED.mask) & MAX_COLOR_DEPTH;
                    srcPixel[ColorMask.GREEN.position] = (pixel >> ColorMask.GREEN.mask) & MAX_COLOR_DEPTH;
                    srcPixel[ColorMask.BLUE.position] = (pixel >> ColorMask.BLUE.mask) & MAX_COLOR_DEPTH;
                    
                    pixel = destPixels[x];
                    // Clearing out the alpha of the destination
                    destPixel[ColorMask.ALPHA.position] = (pixel >> ColorMask.ALPHA.mask) & MAX_COLOR_DEPTH;
                    destPixel[ColorMask.RED.position] = (pixel >> ColorMask.RED.mask) & MAX_COLOR_DEPTH;
                    destPixel[ColorMask.GREEN.position] = (pixel >> ColorMask.GREEN.mask) & MAX_COLOR_DEPTH;
                    destPixel[ColorMask.BLUE.position] = (pixel >> ColorMask.BLUE.mask) & MAX_COLOR_DEPTH;
                    
                    
                    int[] result = blend(srcPixel, destPixel);
                    destPixels[x] = processOpacity(destPixel, result);
                }
                dstOut.setDataElements(0, y, width, 1, destPixels);
            }
            
        }

        private int[] blend(int[] src, int[] dst) {
        	int[] result = new int[4];
            result[ColorMask.ALPHA.position] = 
            		Math.min(255, src[ColorMask.ALPHA.position] + dst[ColorMask.ALPHA.position] - 
            				(src[ColorMask.ALPHA.position] * dst[ColorMask.ALPHA.position]) / 255);
            
            result[ColorMask.RED.position] = (src[ColorMask.RED.position] * dst[ColorMask.RED.position]) >> BLEND_SHIFT;
            result[ColorMask.GREEN.position] = (src[ColorMask.GREEN.position] * dst[ColorMask.GREEN.position]) >> BLEND_SHIFT;
            result[ColorMask.BLUE.position] = (src[ColorMask.BLUE.position] * dst[ColorMask.BLUE.position]) >> BLEND_SHIFT;
            return result;
        }
        
        private int processOpacity(int[] dest, int[] blended) {
        
        	int result = 0;
        	result = result | (((int) (dest[ColorMask.ALPHA.position] + 
        				(blended[ColorMask.ALPHA.position] - dest[ColorMask.ALPHA.position]) * alpha) & 0xFF) << 24);
        	
        	result = result | (((int) (dest[ColorMask.RED.position] + 
        				(blended[ColorMask.RED.position] - dest[ColorMask.RED.position]) * alpha) & 0xFF) <<  16);
        	
        	result = result | (((int) (dest[ColorMask.GREEN.position] + 
        				(blended[ColorMask.GREEN.position] - dest[ColorMask.GREEN.position]) * alpha) & 0xFF) << 8);
        	
        	result = result | ((int) (dest[ColorMask.BLUE.position] + 
        				(blended[ColorMask.BLUE.position] - dest[ColorMask.BLUE.position]) * alpha) & 0xFF);
        	return result;
    	}

        
        @Override
        public void dispose() {
            
        }
    }
}
