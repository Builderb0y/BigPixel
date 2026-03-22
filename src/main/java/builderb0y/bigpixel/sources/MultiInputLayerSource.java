package builderb0y.bigpixel.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.MultiLayerDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.MoveableInputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;

public abstract class MultiInputLayerSource extends LayerSource {

	public MultiLayerDependencies dependencies = new MultiLayerDependencies(this);

	public MultiInputLayerSource(LayerSourceType type, LayerSources sources) {
		super(type, sources);
	}

	@Override
	public void resizeIfNecessary() throws RedrawException {
		List<MoveableInputBinding> inputs = this.dependencies.inputs;
		if (inputs.isEmpty()) {
			throw new RedrawException("No inputs");
		}
		int width = -1, height = -1;
		for (MoveableInputBinding input : inputs) {
			if (input.getCurrent() instanceof VaryingSamplerProvider varying) {
				int imageWidth = varying.getBackingLayer().imageWidth();
				int imageHeight = varying.getBackingLayer().imageHeight();
				if (width < 0) {
					width = imageWidth;
					height = imageHeight;
				}
				else if (width != imageWidth || height != imageHeight) {
					throw new RedrawException("All layer-based dependencies must have the same resolution");
				}
			}
		}
		if (width >= 0) {
			this.sources.layer.animation.checkSize(width, height, false);
		}
	}

	public List<InputBinding> filterInputs() throws RedrawException {
		List<MoveableInputBinding> inputs = this.dependencies.inputs;
		if (inputs.isEmpty()) {
			throw new RedrawException("No inputs");
		}
		List<InputBinding> filtered = new ArrayList<>(inputs.size());
		for (MoveableInputBinding input : inputs) {
			if (input.enabled.isSelected()) {
				filtered.add(input);
			}
		}
		if (filtered.isEmpty()) {
			throw new RedrawException("No enabled inputs");
		}
		return filtered;
	}

	public int countInputs() {
		int count = 0;
		List<MoveableInputBinding> inputs = this.dependencies.inputs;
		if (!inputs.isEmpty()) {
			for (MoveableInputBinding input : inputs) {
				if (input.enabled.isSelected()) {
					count++;
				}
			}
		}
		return count;
	}

	public abstract MultiInputAccumulator getAccumulator();

	@Override
	public int computeMaxProgress(int width, int height) {
		return height * this.countInputs();
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		List<? extends InputBinding> bindings = this.filterInputs();
		MultiInputAccumulator accumulator = this.getAccumulator();
		HDRImage destination = this.sources.layer.getFrame(frame);
		accumulator.preprocess(destination, bindings);
		for (int index = bindings.size(); --index >= 0;) {
			Sampler next = bindings.get(index).getCurrent().createSamplerForFrame(frame);
			IntStream.range(0, destination.height).parallel().forEach((int y) -> {
				for (int x = 0; x < destination.width; x++) {
					destination.setColor(
						x,
						y,
						accumulator.accumulate(
							destination.getColor(x, y),
							next.getColor(x, y)
						)
					);
				}
				this.incrementProgress();
			});
		}
		accumulator.postProcess(destination, bindings);
		this.clampImage(destination);
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}

	public static abstract class MultiInputAccumulator {

		public abstract void preprocess(HDRImage image, List<? extends InputBinding> bindings);

		public abstract FloatVector accumulate(FloatVector existingColor, FloatVector newColor);

		public void postProcess(HDRImage image, List<? extends InputBinding> bindings) {}
	}
}