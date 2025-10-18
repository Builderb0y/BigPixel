package builderb0y.bigpixel.sources;

import java.util.Arrays;
import java.util.stream.IntStream;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.converter.FloatStringConverter;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public class DeNoiseLayerSource extends MainMaskLayerSource {

	public Spinner<Integer>
		radius = this.parameters.addIntSpinner("Radius", 1, 16, 0, 1, 96.0D),
		iterations = this.parameters.addIntSpinner("iterations",  0, 256, 4, 1, 96.0D);
	public Spinner<Float>
		threshold = this.parameters.addFloatSpinner("threshold", new ThresholdValueFactory(), 96.0D);
	public CheckBox
		linear = this.parameters.addCheckbox("linear", "Linear", false);

	public DeNoiseLayerSource(LayerSources sources) {
		super(Type.DENOISE, sources);
		int row = this.dependencies.gridPane.getRowCount();
		this.dependencies.gridPane.addRow(row++, new Label("Radius: "), this.radius);
		this.dependencies.gridPane.addRow(row++, new Label("Threshold: "), this.threshold);
		this.dependencies.gridPane.addRow(row++, new Label("Iterations: "), this.iterations);
		this.dependencies.gridPane.add(this.linear, 1, row);
	}

	@Override
	public void doRedraw(LayerSourceInput main, LayerSourceInput mask, HDRImage destination) throws RedrawException {
		switch (main) {
			case UniformLayerSourceInput uniform -> {
				FloatVector color = uniform.getColor();
				for (int y = 0; y < destination.height; y++) {
					for (int x = 0; x < destination.width; x++) {
						destination.setColor(x, y, color);
					}
				}
			}
			case VaryingLayerSourceInput varying -> {
				int radius = this.radius.getValue();
				float threshold = this.threshold.getValue();
				int iterations = this.iterations.getValue();
				boolean linear = this.linear.isSelected();
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					ColorCounter counter = new ColorCounter(
						radius,
						threshold,
						iterations,
						linear,
						destination
					);
					for (int x = 0; x < destination.width; x++) {
						counter.getColors(varying, x, y);
						destination.setColor(x, y, counter.chooseColor());
					}
				});
			}
		}
	}

	public static class ColorCounter {

		public float[] closestColors = new float[64];
		public float[] swap = new float[64];
		public int[] counts = new int[16];
		public int radius, iterations, colorCount;
		public float thresholdSquared;
		public boolean linear;
		public HDRImage destination;

		public ColorCounter(
			int radius,
			float threshold,
			int iterations,
			boolean linear,
			HDRImage destination
		) {
			this.radius = radius;
			this.thresholdSquared = threshold * threshold;
			this.iterations = iterations;
			this.linear = linear;
			this.destination = destination;
		}

		public void getColors(LayerSourceInput input, int x, int y) {
			this.colorCount = 0;
			Arrays.fill(this.closestColors, 0.0F);
			for (int iteration = 0; iteration < this.iterations; iteration++) {
				Arrays.fill(this.counts, 0);
				for (int offsetY = -this.radius; offsetY <= this.radius; offsetY++) {
					for (int offsetX = -this.radius; offsetX <= this.radius; offsetX++) {
						FloatVector pixel = input.getColor(
							Math.floorMod(x + offsetX, this.destination.width),
							Math.floorMod(y + offsetY, this.destination.height)
						);
						if (this.linear) pixel = pixel.mul(pixel);
						int closestIndex = KMeansLayerSource.getClosestIndex(this.colorCount, this.closestColors, pixel);
						if (closestIndex < 0) {
							closestIndex = this.colorCount++;
							pixel.intoArray(this.closestColors, 0);
						}
						else {
							FloatVector closestColor = FloatVector.fromArray(FloatVector.SPECIES_128, this.closestColors, closestIndex << 2);
							FloatVector difference = pixel.sub(closestColor);
							float distance = difference.mul(difference).reduceLanes(VectorOperators.ADD);
							if (distance > this.thresholdSquared) {
								closestIndex = this.colorCount++;
								pixel.intoArray(this.closestColors, closestIndex << 2);
							}
						}
						FloatVector.fromArray(FloatVector.SPECIES_128, this.swap, closestIndex << 2).add(pixel).intoArray(this.swap, closestIndex << 2);
						this.counts[closestIndex]++;
					}
				}
				for (int index = 0; index < this.colorCount; index++) {
					if (this.counts[index] != 0) {
						FloatVector.fromArray(FloatVector.SPECIES_128, this.swap, index << 2).div(this.counts[index]).intoArray(this.swap, index << 2);
					}
				}
				float[] tmp = this.closestColors;
				this.closestColors = this.swap;
				this.swap = tmp;
				Arrays.fill(this.swap, 0.0F);
			}
		}

		public FloatVector chooseColor() {
			int bestIndex = 0;
			for (int index = 1; index < this.colorCount; index++) {
				if (this.counts[index] > this.counts[bestIndex]) bestIndex = index;
			}
			FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, this.closestColors, bestIndex << 2);
			if (this.linear) color = color.sqrt();
			return color;
		}
	}

	public static class ThresholdValueFactory extends SpinnerValueFactory<Float> {

		public ThresholdValueFactory() {
			this.setValue(0.5F);
			this.setConverter(new FloatStringConverter());
		}

		@Override
		public void increment(int number) {
			float oldValue = this.getValue();
			float newValue = Math.min(oldValue + 0.03125F, 2.0F);
			if (oldValue != newValue) this.setValue(newValue);
		}

		@Override
		public void decrement(int number) {
			float oldValue = this.getValue();
			float newValue = Math.max(oldValue - 0.03125F, 0.0F);
			if (oldValue != newValue) this.setValue(newValue);
		}
	}
}