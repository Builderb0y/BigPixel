package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.MainMaskDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.UnmovableInputBinding;

public class ColorMatrixLayerSource extends PerPixelLayerSource {

	public static class Dependencies extends MainMaskDependencies {

		public UnmovableInputBinding
			toRed   = this.addBinding("to_red",   "Red -> ",   CurveHelper.PARAM),
			toGreen = this.addBinding("to_green", "Green -> ", CurveHelper.PARAM),
			toBlue  = this.addBinding("to_blue",  "Blue -> ",  CurveHelper.PARAM);

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
		super(LayerSourceType.COLOR_MATRIX, sources);
	}

	@Override
	public PerPixelApplicator getApplicator(Sampler main, Sampler mask, int frame) throws RedrawException {
		return new Applicator(
			this.dependencies().toRed.getCurrent().createSamplerForFrame(frame),
			this.dependencies().toGreen.getCurrent().createSamplerForFrame(frame),
			this.dependencies().toBlue.getCurrent().createSamplerForFrame(frame)
		);
	}

	public static class Applicator extends PerPixelApplicator {

		public final Sampler red, green, blue;

		public Applicator(Sampler red, Sampler green, Sampler blue) {
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