package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.List;

import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.sources.dependencies.inputs.InputBinding;

public class MaxLayerSource extends MultiInputLayerSource {

	public MaxLayerSource(LayerSources sources) {
		super(Type.MAX, sources);
	}

	@Override
	public MultiInputAccumulator getAccumulator() {
		return Accumulator.INSTANCE;
	}

	public static class Accumulator extends MultiInputAccumulator {

		public static final Accumulator INSTANCE = new Accumulator();

		@Override
		public void preprocess(HDRImage image, List<? extends InputBinding> bindings) {
			Arrays.fill(image.pixels, Float.NEGATIVE_INFINITY);
		}

		@Override
		public FloatVector accumulate(FloatVector existingColor, FloatVector newColor) {
			return existingColor.max(newColor);
		}
	}
}