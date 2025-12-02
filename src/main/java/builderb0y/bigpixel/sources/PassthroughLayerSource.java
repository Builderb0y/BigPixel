package builderb0y.bigpixel.sources;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.MainDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;

public class PassthroughLayerSource extends LayerSource {

	public MainDependencies dependencies = new MainDependencies(this);

	public PassthroughLayerSource(LayerSources sources) {
		super(LayerSourceType.PASSTHROUGH, sources);
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		Sampler source = this.dependencies.main.getCurrent().createSamplerForFrame(frame);
		HDRImage destination = this.sources.layer.getFrame(frame);
		if (source instanceof VaryingSampler varying) {
			int width = varying.getBackingLayer().imageWidth();
			int height = varying.getBackingLayer().imageHeight();
			destination.checkSize(width, height, false);
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