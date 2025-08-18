package builderb0y.bigpixel.sources;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.NamedLayerDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public class PassthroughLayerSource extends LayerSource {

	public static class Dependencies extends NamedLayerDependencies {

		public InputBinding main = this.addBinding("main", "Input: ");

		public Dependencies(LayerSource source) {
			super(source);
		}
	}

	public Dependencies dependencies = new Dependencies(this);

	public PassthroughLayerSource(LayerSources sources) {
		super(Type.PASSTHROUGH, sources);
	}

	@Override
	public void doRedraw() throws RedrawException {
		LayerSourceInput source = this.dependencies.main.getCurrent();
		HDRImage destination = this.sources.layer.image;
		if (source instanceof VaryingLayerSourceInput varying) {
			HDRImage image = varying.getBackingLayer().image;
			destination.checkSize(image.width, image.height, false);
		}
		for (int y = 0; y < destination.height; y++) {
			for (int x = 0; x < destination.width; x++) {
				destination.setColor(x, y, source.getColor(x, y));
			}
		}
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}
}