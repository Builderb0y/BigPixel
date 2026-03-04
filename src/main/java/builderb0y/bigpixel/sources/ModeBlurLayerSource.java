package builderb0y.bigpixel.sources;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.BoundsHandling.DualBoundsHandling;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;

public class ModeBlurLayerSource extends MainMaskLayerSource {

	public final BoundsHandlingChooser bounds = this.parameters.addDualBoundsHandling("bounds");
	public final Spinner<Integer> iterations = this.parameters.addIntSpinner("iterations", 1, 64, 1, 1, 64.0D);

	public ModeBlurLayerSource(LayerSources sources) {
		super(LayerSourceType.MODE_BLUR, sources);
		this.dependencies.addBoundsHandlingButton(this.bounds.showButton);
		this.dependencies.addExtraNodeRow(new HBox(new Label("Iterations: "), this.iterations));
	}

	@Override
	public void doRedraw(Sampler main, Sampler mask, HDRImage destination, int frame) throws RedrawException {
		switch (main) {
			case UniformSampler uniform -> {
				FloatVector color = this.clampColor(uniform.getColor());
				for (int index = 0; index < destination.pixels.length; index += 4) {
					color.intoArray(destination.pixels, index);
				}
			}
			case VaryingSampler varying -> {
				int iterations = this.iterations.getValue();
				HDRImage tmp = iterations != 1 ? new HDRImage(destination.width, destination.height) : null;
				this.startProgressing(destination.height * iterations);
				this.apply(main, (iterations & 1) != 0 ? destination : tmp);
				for (int iteration = 1; iteration < iterations; iteration++) {
					boolean flip = (iteration & 1) == (iterations & 1);
					this.apply(VaryingSampler.of(varying.getBackingLayer(), flip ? destination : tmp), flip ? tmp : destination);
				}
				this.applyMask(main, mask, destination);
				this.clampImage(destination);
			}
		}
	}

	public void apply(Sampler from, HDRImage to) throws RedrawException {
		DualBoundsHandling boundsHandling = this.bounds.dualHandling.get();
		IntStream.range(0, to.height).parallel().forEach((int y) -> {
			Map<FloatVector, Integer> counts = new HashMap<>();
			for (int x = 0; x < to.width; x++) {
				for (int offsetY = -1; offsetY <= 1; offsetY++) {
					for (int offsetX = -1; offsetX <= 1; offsetX++) {
						FloatVector color = boundsHandling.sample(from, x + offsetX, y + offsetY, to.width, to.height);
						if (color != null) {
							counts.merge(color, (2 - Math.abs(offsetX)) * (2 - Math.abs(offsetY)), Integer::sum);
						}
					}
				}
				Iterator<Map.Entry<FloatVector, Integer>> iterator = counts.entrySet().iterator();
				Map.Entry<FloatVector, Integer> best = iterator.next();
				while (iterator.hasNext()) {
					Map.Entry<FloatVector, Integer> competitor = iterator.next();
					if (competitor.getValue() > best.getValue()) best = competitor;
				}
				to.setColor(x, y, best.getKey());
				counts.clear();
			}
			this.incrementProgress();
		});
	}
}