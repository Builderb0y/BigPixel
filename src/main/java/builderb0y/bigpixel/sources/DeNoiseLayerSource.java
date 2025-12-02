package builderb0y.bigpixel.sources;

import java.util.Arrays;
import java.util.stream.IntStream;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.converter.DoubleStringConverter;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.util.FastExp;

public class DeNoiseLayerSource extends MainMaskLayerSource {

	public Spinner<Integer>
		radius = this.parameters.addIntSpinner("Radius", 1, 16, 0, 1, 96.0D),
		iterations = this.parameters.addIntSpinner("iterations",  0, 256, 4, 1, 96.0D);
	public Spinner<Double>
		sensitivity = this.parameters.addDoubleSpinner("sensitivity", new ThresholdValueFactory(), 96.0D);
	public CheckBox
		linear = this.parameters.addCheckbox("linear", "Linear", false);

	public DeNoiseLayerSource(LayerSources sources) {
		super(LayerSourceType.DENOISE, sources);
		int row = this.dependencies.gridPane.getRowCount();
		this.dependencies.gridPane.addRow(row++, new Label("Radius: "), this.radius);
		this.dependencies.gridPane.addRow(row++, new Label("Sensitivity: "), this.sensitivity);
		this.dependencies.gridPane.addRow(row++, new Label("Iterations: "), this.iterations);
		this.dependencies.gridPane.add(this.linear, 1, row);
	}

	@Override
	public void doRedraw(Sampler main, Sampler mask, HDRImage destination, int frame) throws RedrawException {
		//todo: support mask.
		switch (main) {
			case UniformSampler uniform -> {
				FloatVector color = uniform.getColor();
				for (int y = 0; y < destination.height; y++) {
					for (int x = 0; x < destination.width; x++) {
						destination.setColor(x, y, color);
					}
				}
			}
			case VaryingSampler varying -> {
				int radius = this.radius.getValue();
				double sensitivity = this.sensitivity.getValue();
				int iterations = this.iterations.getValue();
				boolean linear = this.linear.isSelected();
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					for (int x = 0; x < destination.width; x++) {
						DoubleVector color = sample(varying, x, y, linear);
						for (int iteration = 0; iteration < iterations; iteration++) {
							color = iterate(color, varying, x, y, radius, sensitivity, linear);
						}
						store(destination, x, y, color, linear);
					}
				});
			}
		}
	}

	public static DoubleVector iterate(DoubleVector start, VaryingSampler input, int x, int y, int radius, double sensitivity, boolean linear) {
		DoubleVector
			globalD1 = DoubleVector.broadcast(DoubleVector.SPECIES_256, 0.0D),
			globalD2 = globalD1;
		int srcWidth = input.getBackingLayer().imageWidth();
		int srcHeight = input.getBackingLayer().imageHeight();
		for (int offsetY = -radius; offsetY <= radius; offsetY++) {
			for (int offsetX = -radius; offsetX <= radius; offsetX++) {
				DoubleVector
					sample = sample(input, Math.floorMod(x + offsetX, srcWidth), Math.floorMod(y + offsetY, srcHeight), linear),
					offset = offset(sample, start),
					sensitiveOffset = sensitiveOffset(offset, sensitivity),
					sensitiveOffsetSquared = sensitiveOffsetSquared(sensitiveOffset);
				double
					bellCurve = bellCurve(sensitiveOffsetSquared);
				DoubleVector
					d1 = d1(bellCurve, sensitiveOffset, sensitivity),
					d2 = d2(bellCurve, sensitiveOffsetSquared, sensitivity);
				globalD1 = globalD1.add(d1);
				globalD2 = globalD2.add(d2);
			}
		}
		return start.add(globalD1.div(absD2(globalD2, sensitivity)));
	}

	public static DoubleVector sample(VaryingSampler sampler, int x, int y, boolean linear) {
		DoubleVector color = (DoubleVector)(sampler.getColor(x, y).convertShape(VectorOperators.F2D, DoubleVector.SPECIES_256, 0));
		if (linear) color = color.mul(color);
		return color;
	}

	public static void store(HDRImage image, int x, int y, DoubleVector color, boolean linear) {
		if (linear) color = color.sqrt();
		image.setColor(x, y, (FloatVector)(color.convertShape(VectorOperators.D2F, FloatVector.SPECIES_128, 0)));
	}

	public static DoubleVector offset(DoubleVector color, DoubleVector start) {
		return start.sub(color);
	}

	public static DoubleVector sensitiveOffset(DoubleVector offset, double sensitivity) {
		return offset.mul(sensitivity);
	}

	public static DoubleVector sensitiveOffsetSquared(DoubleVector sensitiveOffset) {
		return sensitiveOffset.mul(sensitiveOffset);
	}

	public static double bellCurve(DoubleVector sensitiveOffsetSquared) {
		return FastExp.fastExp(-sensitiveOffsetSquared.reduceLanes(VectorOperators.ADD));
	}

	public static DoubleVector d1(double bellCurve, DoubleVector sensitiveOffset, double sensitivity) {
		return sensitiveOffset.mul(-2.0D * sensitivity * bellCurve);
	}

	public static DoubleVector d2(double bellCurve, DoubleVector sensitiveOffsetSquared, double sensitivity) {
		return sensitiveOffsetSquared.mul(4.0D).sub(2.0D).mul(sensitivity * sensitivity * bellCurve);
	}

	public static double absD2(DoubleVector d2, double sensitivity) {
		sensitivity *= sensitivity;
		//sensitivity *= sensitivity;
		return Math.sqrt(d2.mul(d2).reduceLanes(VectorOperators.ADD) + sensitivity);
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

		public void getColors(Sampler input, int x, int y) {
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

	public static class ThresholdValueFactory extends SpinnerValueFactory<Double> {

		public ThresholdValueFactory() {
			this.setValue(4.0D);
			this.setConverter(new DoubleStringConverter());
		}

		@Override
		public void increment(int number) {
			double oldValue = this.getValue();
			double newValue = Double.longBitsToDouble(Double.doubleToRawLongBits(oldValue) + (1L << 50));
			this.setValue(newValue);
		}

		@Override
		public void decrement(int number) {
			double oldValue = this.getValue();
			double newValue = Double.longBitsToDouble(Double.doubleToRawLongBits(oldValue) - (1L << 50));
			this.setValue(newValue);
		}
	}
}