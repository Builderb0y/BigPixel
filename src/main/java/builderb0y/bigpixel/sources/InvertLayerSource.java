package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.util.Util;

public class InvertLayerSource extends PerPixelLayerSource {

	public InvertLayerSource(LayerSources sources) {
		super(LayerSourceType.INVERT, sources);
	}

	@Override
	public PerPixelApplicator getApplicator(Sampler main, Sampler mask, int frame) throws RedrawException {
		return Applicator.INSTANCE;
	}

	public static class Applicator extends PerPixelApplicator {

		public static final Applicator INSTANCE = new Applicator();

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return Util.WHITE.sub(original);
		}
	}
}