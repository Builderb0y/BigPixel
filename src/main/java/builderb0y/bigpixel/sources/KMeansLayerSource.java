package builderb0y.bigpixel.sources;

import java.util.Arrays;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.FastRandom;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public class KMeansLayerSource extends PerPixelLayerSource {

	public Spinner<Integer>
		seed = this.parameters.addIntSpinner("seed", Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 80.0D),
		colors = this.parameters.addIntSpinner("colors",  1, 1 << 24, 2, 1, 80.0D),
		iterations = this.parameters.addIntSpinner("iterations",  0, 256, 4, 1, 80.0D);
	public CheckBox
		linear = this.parameters.addCheckbox("linear", "Linear", false);

	public KMeansLayerSource(LayerSources sources) {
		super(Type.K_MEANS, sources);
		int row = this.dependencies.gridPane.getRowCount();
		this.dependencies.gridPane.add(new Label("Seed: "), 0, row);
		this.dependencies.gridPane.add(this.seed, 1, row);
		this.dependencies.gridPane.add(new Label("Colors: "), 0, ++row);
		this.dependencies.gridPane.add(this.colors, 1, row);
		this.dependencies.gridPane.add(new Label("Iterations: "), 0, ++row);
		this.dependencies.gridPane.add(this.iterations, 1, row);
		this.dependencies.gridPane.add(this.linear, 1, ++row);
	}

	@Override
	public PerPixelApplicator getApplicator(LayerSourceInput main, LayerSourceInput mask) throws RedrawException {
		HDRImage destination = ((VaryingLayerSourceInput)(main)).getBackingLayer().image;
		int colorCount = this.colors.getValue();
		int iterations = this.iterations.getValue();
		boolean linear = this.linear.isSelected();
		FastRandom random = new FastRandom(this.seed.getValue().longValue());
		float[] closestColors = new float[colorCount << 2];
		for (int index = 0; index < colorCount; index++) {
			FloatVector pixel = main.getColor(random.nextInt(destination.width), random.nextInt(destination.height));
			if (linear) pixel = pixel.mul(pixel);
			pixel.intoArray(closestColors, index << 2);
		}
		float[] swap = new float[colorCount << 2];
		int[] counts = new int[colorCount];
		for (int iteration = 0; iteration < iterations; iteration++) {
			for (int y = 0; y < destination.height; y++) {
				for (int x = 0; x < destination.width; x++) {
					FloatVector pixel = main.getColor(x, y);
					if (linear) pixel = pixel.mul(pixel);
					int closestIndex = getClosestIndex(colorCount, closestColors, pixel);
					FloatVector.fromArray(FloatVector.SPECIES_128, swap, closestIndex << 2).add(pixel).intoArray(swap, closestIndex << 2);
					counts[closestIndex]++;
				}
			}
			for (int index = 0; index < colorCount; index++) {
				if (counts[index] != 0) {
					FloatVector.fromArray(FloatVector.SPECIES_128, swap, index << 2).div(counts[index]).intoArray(swap, index << 2);
				}
			}
			float[] tmp = closestColors;
			closestColors = swap;
			swap = tmp;
			Arrays.fill(swap, 0.0F);
			Arrays.fill(counts, 0);
		}
		return new Applicator(closestColors, colorCount, linear);
	}

	public static int getClosestIndex(int colorCount, float[] closestColors, FloatVector pixel) {
		int closestIndex = -1;
		float closestDistanceSquared = Float.POSITIVE_INFINITY;
		for (int newIndex = 0; newIndex < colorCount; newIndex++) {
			FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, closestColors, newIndex << 2);
			FloatVector difference = pixel.sub(color);
			float newDistance = difference.mul(difference).reduceLanes(VectorOperators.ADD);
			if (newDistance < closestDistanceSquared) {
				closestIndex = newIndex;
				closestDistanceSquared = newDistance;
			}
		}
		return closestIndex;
	}

	public static class Applicator extends PerPixelApplicator {

		public final float[] closestColors;
		public final int colorCount;
		public final boolean linear;

		public Applicator(float[] closestColors, int colorCount, boolean linear) {
			this.closestColors = closestColors;
			this.colorCount = colorCount;
			this.linear = linear;
		}

		@Override
		public FloatVector apply(int x, int y, FloatVector color) {
			if (this.linear) color = color.mul(color);
			int closestIndex = getClosestIndex(this.colorCount, this.closestColors, color);
			color = FloatVector.fromArray(FloatVector.SPECIES_128, this.closestColors, closestIndex << 2);
			if (this.linear) color = color.sqrt();
			return color;
		}
	}
}