package builderb0y.notgimp.sources;

import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput;

public class ClampLayerSource extends PerPixelLayerSource {

	public ClampLayerSource(LayerSources sources) {
		super(Type.CLAMP, sources);
	}

	@Override
	public PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException {
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