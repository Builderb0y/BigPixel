package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.List;

import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.dependencies.inputs.InputBinding;

public class AddLayerSource extends MultiInputLayerSource {

	public CheckBox linear = this.parameters.addCheckbox("linear", "Linear", false);
	public CheckBox alphaWeighting = this.parameters.addCheckbox("alpha_weighting", "Alpha Weighting", false);

	public AddLayerSource(LayerSources sources) {
		super(Type.ADD, sources);
		this.dependencies.getBottomPane().getChildren().addAll(this.linear, this.alphaWeighting);
	}

	@Override
	public MultiInputAccumulator getAccumulator() {
		return new Accumulator(this.linear.isSelected(), this.alphaWeighting.isSelected());
	}

	public static class Accumulator extends MultiInputAccumulator {

		public final boolean linear, alphaWeighting;

		public Accumulator(boolean linear, boolean alphaWeighting) {
			this.linear = linear;
			this.alphaWeighting = alphaWeighting;
		}

		@Override
		public void preprocess(HDRImage image, List<? extends InputBinding> bindings) {
			Arrays.fill(image.pixels, 0.0F);
		}

		@Override
		public FloatVector accumulate(FloatVector existingColor, FloatVector newColor) {
			if (this.linear) newColor = newColor.mul(newColor, Util.RGB_MASK);
			if (this.alphaWeighting) newColor = newColor.mul(newColor.lane(HDRImage.ALPHA_OFFSET));
			return newColor.add(existingColor);
		}

		@Override
		public void postProcess(HDRImage image, List<? extends InputBinding> bindings) {
			if (this.linear) {
				for (int base = 0; base < image.pixels.length; base += 4) {
					FloatVector.fromArray(FloatVector.SPECIES_128, image.pixels, base).lanewise(VectorOperators.SQRT, Util.RGB_MASK).intoArray(image.pixels, base);
				}
			}
			if (this.alphaWeighting) {
				for (int base = HDRImage.ALPHA_OFFSET; base < image.pixels.length; base += 4) {
					image.pixels[base] = 1.0F;
				}
			}
		}
	}
}