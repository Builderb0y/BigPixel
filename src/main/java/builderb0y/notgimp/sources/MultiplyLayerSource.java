package builderb0y.notgimp.sources;

import java.util.Arrays;
import java.util.List;

import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.dependencies.inputs.InputBinding;

public class MultiplyLayerSource extends MultiInputLayerSource {

	public CheckBox alphaWeighting = this.parameters.addCheckbox("alpha_weighting", "Alpha Weighting", false);

	public MultiplyLayerSource(LayerSources sources) {
		super(Type.MULTIPLY, sources);
		this.dependencies.getBottomPane().getChildren().add(this.alphaWeighting);
	}

	@Override
	public MultiInputAccumulator getAccumulator() {
		return new Accumulator(this.alphaWeighting.isSelected());
	}

	public static class Accumulator extends MultiInputAccumulator {

		public final boolean alphaWeighting;

		public Accumulator(boolean alphaWeighting) {
			this.alphaWeighting = alphaWeighting;
		}

		@Override
		public void preprocess(HDRImage image, List<? extends InputBinding> bindings) {
			Arrays.fill(image.pixels, 1.0F);
		}

		@Override
		public FloatVector accumulate(FloatVector existingColor, FloatVector newColor) {
			if (this.alphaWeighting) {
				newColor = Util.WHITE.sub(Util.WHITE.sub(newColor).mul(newColor.lane(HDRImage.ALPHA_OFFSET)));
			}
			return existingColor.mul(newColor);
		}

		@Override
		public void postProcess(HDRImage image, List<? extends InputBinding> bindings) {
			if (this.alphaWeighting) {
				for (int base = HDRImage.ALPHA_OFFSET; base < image.pixels.length; base += 4) {
					image.pixels[base] = 1.0F;
				}
			}
		}
	}
}