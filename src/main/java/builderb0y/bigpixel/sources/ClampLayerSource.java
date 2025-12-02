package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;

public class ClampLayerSource extends PerPixelLayerSource {

	public ClampLayerSource(LayerSources sources) {
		super(LayerSourceType.CLAMP, sources);
	}

	@Override
	public PerPixelApplicator getApplicator(Sampler main, Sampler mask, int frame) throws RedrawException {
		return Applicator.INSTANCE;
	}

	public static class Applicator extends PerPixelApplicator {

		public static final Applicator INSTANCE = new Applicator();

		@Override
		public FloatVector apply(int x, int y, FloatVector original) {
			return original.max(0.0F).min(1.0F);
		}
	}
}