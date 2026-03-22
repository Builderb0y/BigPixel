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
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.BoundsHandling.DualBoundsHandling;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.FastExp;

public class DeNoiseLayerSource extends MainMaskLayerSource {

	public final BoundsHandlingChooser
		bounds = this.parameters.addDualBoundsHandling("bounds");
	public final Spinner<Integer>
		radius = this.parameters.addIntSpinner("Radius", 1, 16, 0, 1, 96.0D),
		iterations = this.parameters.addIntSpinner("iterations",  0, 256, 4, 1, 96.0D);
	public final Spinner<Double>
		sensitivity = this.parameters.addDoubleSpinner("sensitivity", new ThresholdValueFactory(), 96.0D);
	public final CheckBox
		linear = this.parameters.addCheckbox("linear", "Linear", false);

	public DeNoiseLayerSource(LayerSources sources) {
		super(LayerSourceType.DENOISE, sources);
		this.dependencies.addBoundsHandlingButton(this.bounds.showButton);
		int row = this.dependencies.gridPane.getRowCount();
		this.dependencies.gridPane.addRow(row++, new Label("Radius: "), this.radius);
		this.dependencies.gridPane.addRow(row++, new Label("Sensitivity: "), this.sensitivity);
		this.dependencies.gridPane.addRow(row++, new Label("Iterations: "), this.iterations);
		this.dependencies.gridPane.add(this.linear, 1, row);
	}

	@Override
	public int computeMaxProgress(SamplerProvider main, SamplerProvider mask, int width, int height) {
		return switch (main) {
			case UniformSamplerProvider _ -> 0;
			case VaryingSamplerProvider _ -> height;
		};
	}

	@Override
	public void doRedraw(Sampler main, Sampler mask, HDRImage destination, int frame) throws RedrawException {
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
				boolean clampRGB = this.clampRGB.isSelected();
				boolean clampA = this.clampAlpha.isSelected();
				int radius = this.radius.getValue();
				double sensitivity = this.sensitivity.getValue();
				int iterations = this.iterations.getValue();
				boolean linear = this.linear.isSelected();
				int width = varying.getBackingLayer().imageWidth();
				int height = varying.getBackingLayer().imageHeight();
				DualBoundsHandling boundsHandling = this.bounds.dualHandling.get();
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					if (this.getLayer().redrawRequested) return;
					for (int x = 0; x < destination.width; x++) {
						DoubleVector color = sample(varying, boundsHandling, x, y, destination.width, destination.height, linear);
						for (int iteration = 0; iteration < iterations; iteration++) {
							color = iterate(color, varying, boundsHandling, x, y, width, height, radius, sensitivity, linear);
						}
						store(destination, x, y, color, varying.getColor(x, y), mask.getColor(x, y), linear, clampRGB, clampA);
					}
					this.incrementProgress();
				});
			}
		}
	}

	public static DoubleVector iterate(DoubleVector start, VaryingSampler input, DualBoundsHandling boundsHandling, int x, int y, int width, int height, int radius, double sensitivity, boolean linear) {
		DoubleVector
			globalD1 = DoubleVector.broadcast(DoubleVector.SPECIES_256, 0.0D),
			globalD2 = globalD1;
		double[] gaussianWeights = gaussian(radius);
		double center = gaussianWeights[radius];
		double compensation = 1.0D / (center * center);
		for (int offsetY = -radius; offsetY <= radius; offsetY++) {
			for (int offsetX = -radius; offsetX <= radius; offsetX++) {
				DoubleVector sample = sample(input, boundsHandling, x + offsetX, y + offsetY, width, height, linear);
				if (sample == null) continue;
				DoubleVector
					offset = offset(sample, start),
					sensitiveOffset = sensitiveOffset(offset, sensitivity),
					sensitiveOffsetSquared = sensitiveOffsetSquared(sensitiveOffset);
				double
					bellCurve = bellCurve(sensitiveOffsetSquared) * gaussianWeights[offsetX + radius] * gaussianWeights[offsetY + radius] * compensation;
				DoubleVector
					d1 = d1(bellCurve, sensitiveOffset, sensitivity),
					d2 = d2(bellCurve, sensitiveOffsetSquared, sensitivity);
				globalD1 = globalD1.add(d1);
				globalD2 = globalD2.add(d2);
			}
		}
		return start.add(globalD1.div(absD2(globalD2, sensitivity)));
	}

	public static @Nullable DoubleVector sample(VaryingSampler sampler, DualBoundsHandling boundsHandling, int x, int y, int width, int height, boolean linear) {
		FloatVector floatColor = boundsHandling.sample(sampler, x, y, width, height);
		if (floatColor == null) return null;
		DoubleVector doubleColor = (DoubleVector)(floatColor.convertShape(VectorOperators.F2D, DoubleVector.SPECIES_256, 0));
		if (linear) doubleColor = doubleColor.mul(doubleColor);
		return doubleColor;
	}

	public static void store(HDRImage image, int x, int y, DoubleVector color, FloatVector originalColor, FloatVector mask, boolean linear, boolean clampRGB, boolean clampA) {
		if (linear) color = color.sqrt();
		image.setColor(x, y, clamp(carefulMix(originalColor, (FloatVector)(color.convertShape(VectorOperators.D2F, FloatVector.SPECIES_128, 0)), mask), clampRGB, clampA));
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

	public static double[] gaussian(int radius) {
		class Cache {

			public static final double[][] CACHE = new double[17][];
			static {
				CACHE[0] = new double[] { 1.0D };
			}
		}
		double[] result = Cache.CACHE[radius];
		if (result == null) {
			int diameter = radius * 2 + 1;
			result = Arrays.copyOf(gaussian(radius - 1), diameter);
			for (int size = diameter - 2; size < diameter; size++) {
				for (int index = size; index > 0;) {
					result[index] = (result[index] + result[--index]) * 0.5D;
				}
				result[0] *= 0.5D;
			}
			Cache.CACHE[radius] = result;
		}
		return result;
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