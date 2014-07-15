
package com.adobe.acs.commons.images.transformers.impl.composites.contexts;

public enum ARGBMask {

	ALPHA(24),
	RED(16),
	GREEN(8),
	BLUE(0);

	private int	mask;

	ARGBMask(int mask) {
		this.mask = mask;
	}

	int getMask() {
		return mask;
	}

}
