package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.MainMaskDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class ColorMatrixLayerSource extends PerPixelLayerSource {

	public static class Dependencies extends MainMaskDependencies {

		public UnmovableInputBinding
			toRed   = this.addBinding("to_red",   "Red -> "),
			toGreen = this.addBinding("to_green", "Green -> "),
			toBlue  = this.addBinding("to_blue",  "Blue -> ");

		public Dependencies(LayerSource source) {
			super(source);
		}
	}

	@Override
	public MainMaskDependencies createDependencies() {
		return new Dependencies(this);
	}

	public Dependencies dependencies() {
		return (Dependencies)(this.dependencies);
	}

	public ColorMatrixLayerSource(LayerSources sources) {
		super(Type.COLOR_MATRIX, sources);
	}

	@Override
	public PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException {
		return new Applicator(
			this.dependencies().toRed.getCurrent(),
			this.dependencies().toGreen.getCurrent(),
			this.dependencies().toBlue.getCurrent()
		);
	}

	public static class Applicator extends PerPixelApplicator {

		public final LayerSourceInput red, green, blue;

		public Applicator(LayerSourceInput red, LayerSourceInput green, LayerSourceInput blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return (
				this.red.getColor(x, y).mul(original.lane(HDRImage.RED_OFFSET))
				.add(this.green.getColor(x, y).mul(original.lane(HDRImage.GREEN_OFFSET)))
				.add(this.blue.getColor(x, y).mul(original.lane(HDRImage.BLUE_OFFSET)))
				.withLane(HDRImage.ALPHA_OFFSET, original.lane(HDRImage.ALPHA_OFFSET))
			);
		}
	}
}