package builderb0y.bigpixel.sources;

import java.util.List;

import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;

public class AverageLayerSource extends MultiInputLayerSource {

	public CheckBox linear = this.parameters.addCheckbox("linear", "Linear", false);
	public CheckBox alphaWeighting = this.parameters.addCheckbox("alpha_weighting", "Alpha Weighting", false);

	public AverageLayerSource(LayerSources sources) {
		super(Type.AVERAGE, sources);
		this.dependencies.getBottomPane().getChildren().addAll(this.linear, this.alphaWeighting);
	}

	@Override
	public MultiInputAccumulator getAccumulator() {
		return new Accumulator(this.linear.isSelected(), this.alphaWeighting.isSelected());
	}

	public static class Accumulator extends AddLayerSource.Accumulator {

		public Accumulator(boolean linear, boolean alphaWeighting) {
			super(linear, alphaWeighting);
		}

		@Override
		public void postProcess(HDRImage image, List<? extends InputBinding> bindings) {
			if (bindings.size() > 1) {
				float rcpCount = 1.0F / bindings.size();
				for (int base = 0; base < image.pixels.length; base += 4) {
					FloatVector.fromArray(FloatVector.SPECIES_128, image.pixels, base).mul(rcpCount).intoArray(image.pixels, base);
				}
			}
			super.postProcess(image, bindings);
		}
	}
}