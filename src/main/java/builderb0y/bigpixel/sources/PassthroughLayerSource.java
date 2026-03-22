package builderb0y.bigpixel.sources;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.MainDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;

public class PassthroughLayerSource extends LayerSource {

	public MainDependencies dependencies = new MainDependencies(this);

	public PassthroughLayerSource(LayerSources sources) {
		super(LayerSourceType.PASSTHROUGH, sources);
	}

	@Override
	public void resizeIfNecessary() throws RedrawException {
		if (this.dependencies.main.getCurrent() instanceof VaryingSamplerProvider varying) {
			this.sources.layer.animation.checkSize(
				varying.getBackingLayer().imageWidth(),
				varying.getBackingLayer().imageHeight(),
				false
			);
		}
	}

	@Override
	public int computeMaxProgress(int width, int height) {
		return 0;
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		Sampler source = this.dependencies.main.getCurrent().createSamplerForFrame(frame);
		HDRImage destination = this.sources.layer.getFrame(frame);
		boolean clampRGB = this.clampRGB.isSelected();
		boolean clampA = this.clampAlpha.isSelected();
		for (int y = 0; y < destination.height; y++) {
			for (int x = 0; x < destination.width; x++) {
				destination.setColor(x, y, clamp(source.getColor(x, y), clampRGB, clampA));
			}
		}
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}
}