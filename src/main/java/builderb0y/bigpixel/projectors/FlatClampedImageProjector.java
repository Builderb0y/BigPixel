package builderb0y.bigpixel.projectors;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.ZoomableImage;

public class FlatClampedImageProjector extends ImageProjector2D {

	public FlatClampedImageProjector(ZoomableImage zoomableImage) {
		super(Type.FLAT_CLAMPED, zoomableImage);
	}

	@Override
	public @Nullable Texcoord handleEdge(int projectedX, int projectedY) {
		return projectedX >= 0 && projectedX < this.layerWidth && projectedY >= 0 && projectedY < this.layerHeight ? new Texcoord(projectedX, projectedY) : null;
	}
}