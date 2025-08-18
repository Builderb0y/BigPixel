package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;

public class InvertLayerSource extends PerPixelLayerSource {

	public InvertLayerSource(LayerSources sources) {
		super(Type.INVERT, sources);
	}

	@Override
	public PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException {
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