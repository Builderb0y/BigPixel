package builderb0y.bigpixel.sources;

import java.util.Arrays;
import java.util.List;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;

public class MinLayerSource extends MultiInputLayerSource {

	public MinLayerSource(LayerSources sources) {
		super(LayerSourceType.MIN, sources);
	}

	@Override
	public MultiInputAccumulator getAccumulator() {
		return Accumulator.INSTANCE;
	}

	public static class Accumulator extends MultiInputAccumulator {

		public static final Accumulator INSTANCE = new Accumulator();

		@Override
		public void preprocess(HDRImage image, List<? extends InputBinding> bindings) {
			Arrays.fill(image.pixels, Float.POSITIVE_INFINITY);
		}

		@Override
		public FloatVector accumulate(FloatVector existingColor, FloatVector newColor) {
			return existingColor.min(newColor);
		}
	}
}