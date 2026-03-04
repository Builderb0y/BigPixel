package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;

public enum BoundsHandling {
	IGNORE,
	CLAMP,
	WRAP;

	public static final BoundsHandling[] VALUES = values();

	public static record DualBoundsHandling(BoundsHandling horizontal, BoundsHandling vertical) {

		public @Nullable FloatVector sample(Sampler sampler, int x, int y, int width, int height) {
			switch (this.horizontal) {
				case IGNORE -> {
					if (Integer.compareUnsigned(x, width) >= 0) return null;
				}
				case CLAMP -> {
					x = Math.min(Math.max(x, 0), width - 1);
				}
				case WRAP -> {
					x = Math.floorMod(x, width);
				}
			}
			switch (this.vertical) {
				case IGNORE -> {
					if (Integer.compareUnsigned(y, height) >= 0) return null;
				}
				case CLAMP -> {
					y = Math.min(Math.max(y, 0), height - 1);
				}
				case WRAP -> {
					y = Math.floorMod(y, height);
				}
			}
			return sampler.getColor(x, y);
		}
	}
}