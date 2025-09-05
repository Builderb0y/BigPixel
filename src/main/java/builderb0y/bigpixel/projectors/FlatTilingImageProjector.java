package builderb0y.bigpixel.projectors;

import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.ZoomableImage;

public class FlatTilingImageProjector extends ImageProjector2D {

	public FlatTilingImageProjector(ZoomableImage zoomableImage) {
		super(Type.FLAT_TILING, zoomableImage);
	}

	@Override
	public @Nullable Texcoord handleEdge(int projectedX, int projectedY) {
		boolean outside = projectedX < 0 || projectedX >= this.layerWidth || projectedY < 0 || projectedY >= this.layerHeight;
		return new Texcoord(
			Math.floorMod(projectedX, this.layerWidth),
			Math.floorMod(projectedY, this.layerHeight)
		) {

			@Override
			public FloatVector sample(HDRImage image) {
				FloatVector sample = super.sample(image);
				if (outside) sample = sample.mul(0.75F, Util.RGB_MASK);
				return sample;
			}
		};
	}
}