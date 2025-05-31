package builderb0y.notgimp.sources;

import java.util.Arrays;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.FastRandom;
import builderb0y.notgimp.HDRImage;

public class KMeansLayerSource extends SingleInputEffectLayerSource {

	public Spinner<Integer>
		seed = this.addIntSpinner("seed", Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 80.0D),
		colors = this.addIntSpinner("colors",  1, 1 << 24, 2, 1, 80.0D),
		iterations = this.addIntSpinner("iterations",  0, 256, 4, 1, 80.0D);
	public CheckBox
		linear = this.addCheckbox("linear", "Linear", false);
	public GridPane
		gridPane = new GridPane();

	public KMeansLayerSource(LayerSources sources) {
		super(sources, "kmeans", "K-Means");
		this.gridPane.add(new Label("Seed: "), 0, 0);
		this.gridPane.add(this.seed, 1, 0);
		this.gridPane.add(new Label("Colors: "), 0, 1);
		this.gridPane.add(this.colors, 1, 1);
		this.gridPane.add(new Label("Iterations: "), 0, 2);
		this.gridPane.add(this.iterations, 1, 2);
		this.gridPane.add(this.linear, 1, 3);
		this.rootNode.setCenter(this.gridPane);
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage source = this.getSingleInput(true).image;
		int colorCount = this.colors.getValue();
		int iterations = this.iterations.getValue();
		boolean linear = this.linear.isSelected();
		FastRandom random = new FastRandom(this.seed.getValue().longValue());
		float[] closestColors = new float[colorCount << 2];
		for (int index = 0; index < colorCount; index++) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, random.nextInt(source.pixels.length) & ~3);
			if (linear) pixel = pixel.mul(pixel);
			pixel.intoArray(closestColors, index << 2);
		}
		float[] swap = new float[colorCount << 2];
		int[] counts = new int[colorCount];
		for (int iteration = 0; iteration < iterations; iteration++) {
			for (int base = 0, length = source.pixels.length; base < length; base += 4) {
				FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, base);
				if (linear) pixel = pixel.mul(pixel);
				int closestIndex = getClosestIndex(colorCount, closestColors, pixel);
				FloatVector.fromArray(FloatVector.SPECIES_128, swap, closestIndex << 2).add(pixel).intoArray(swap, closestIndex << 2);
				counts[closestIndex]++;
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
		HDRImage destination = this.sources.layer.image;
		for (int base = 0, length = source.pixels.length; base < length; base += 4) {
			FloatVector pixel = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, base);
			if (linear) pixel = pixel.mul(pixel);
			int closestIndex = getClosestIndex(colorCount, closestColors, pixel);
			FloatVector result = FloatVector.fromArray(FloatVector.SPECIES_128, closestColors, closestIndex << 2);
			if (linear) result = result.sqrt();
			result.intoArray(destination.pixels, base);
		}
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
}