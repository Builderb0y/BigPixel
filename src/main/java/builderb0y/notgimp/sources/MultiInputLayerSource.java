package builderb0y.notgimp.sources;

import java.util.List;

import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.sources.dependencies.LayerDependencies;
import builderb0y.notgimp.sources.dependencies.MultiLayerDependencies;
import builderb0y.notgimp.sources.dependencies.inputs.InputBinding;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public abstract class MultiInputLayerSource extends LayerSource {

	public MultiLayerDependencies dependencies = new MultiLayerDependencies(this);

	public MultiInputLayerSource(Type type, LayerSources sources) {
		super(type, sources);
	}

	public List<? extends InputBinding> ensureSameSize() throws RedrawException {
		List<? extends InputBinding> inputs = this.dependencies.listView.getItems();
		if (inputs.isEmpty()) {
			throw new RedrawException("No inputs");
		}
		HDRImage first = null;
		for (InputBinding input : inputs) {
			if (input.selection.getValue() instanceof VaryingLayerSourceInput varying) {
				HDRImage image = varying.getBackingLayer().image;
				if (first == null) {
					first = image;
				}
				else if (first.width != image.width || first.height != image.height) {
					throw new RedrawException("All layer-based dependencies must have the same resolution");
				}
			}
		}
		HDRImage destination = this.sources.layer.image;
		if (first != null) {
			destination.checkSize(first.width, first.height, false);
		}
		return inputs;
	}

	public abstract MultiInputAccumulator getAccumulator();

	@Override
	public void doRedraw() throws RedrawException {
		List<? extends InputBinding> bindings = this.ensureSameSize();
		MultiInputAccumulator accumulator = this.getAccumulator();
		HDRImage destination = this.sources.layer.image;
		accumulator.preprocess(destination, bindings);
		for (int index = bindings.size(); --index >= 0;) {
			LayerSourceInput next = bindings.get(index).getCurrent();
			for (int y = 0; y < destination.height; y++) {
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
			}
		}
		accumulator.postProcess(destination, bindings);
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