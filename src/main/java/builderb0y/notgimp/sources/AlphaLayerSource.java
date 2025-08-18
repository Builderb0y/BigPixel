package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.List;

import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.dependencies.inputs.InputBinding;

public class AlphaLayerSource extends MultiInputLayerSource {

	public CheckBox linear = this.parameters.addCheckbox("linear", "Linear", false);

	public AlphaLayerSource(LayerSources sources) {
		super(Type.ALPHA, sources);
		this.dependencies.getBottomPane().getChildren().add(this.linear);
	}

	@Override
	public MultiInputAccumulator getAccumulator() {
		return new Accumulator(this.linear.isSelected());
	}

	public static class Accumulator extends MultiInputAccumulator {

		public final boolean linear;

		public Accumulator(boolean linear) {
			this.linear = linear;
		}

		@Override
		public void preprocess(HDRImage image, List<? extends InputBinding> bindings) {
			Arrays.fill(image.pixels, 0.0F);
		}

		@Override
		public FloatVector accumulate(FloatVector existingColor, FloatVector newColor) {
			if (this.linear) newColor = newColor.mul(newColor, Util.RGB_MASK);
			float oldAlpha = existingColor.lane(HDRImage.ALPHA_OFFSET);
			float newAlpha = newColor.lane(HDRImage.ALPHA_OFFSET);
			float finalAlpha = 1.0F - (1.0F - oldAlpha) * (1.0F - newAlpha);
			FloatVector result = existingColor.mul(oldAlpha - oldAlpha * newAlpha /* oldAlpha * (1 - newAlpha) */).add(newColor.mul(newAlpha));
			if (finalAlpha != 0.0F) result = result.div(finalAlpha);
			result = result.withLane(HDRImage.ALPHA_OFFSET, finalAlpha);
			return result;
		}

		@Override
		public void postProcess(HDRImage image, List<? extends InputBinding> bindings) {
			if (this.linear) {
				for (int base = 0; base < image.pixels.length; base += 4) {
					FloatVector.fromArray(FloatVector.SPECIES_128, image.pixels, base).lanewise(VectorOperators.SQRT, Util.RGB_MASK).intoArray(image.pixels, base);
				}
			}
		}
	}
}