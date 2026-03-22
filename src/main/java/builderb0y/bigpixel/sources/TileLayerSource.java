package builderb0y.bigpixel.sources;

import java.util.stream.IntStream;

import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.MainDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;

public class TileLayerSource extends LayerSource {

	public MainDependencies
		dependencies = new MainDependencies(this);
	public Spinner<Integer>
		offsetX = this.parameters.addIntSpinner("offsetX", Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 80),
		offsetY = this.parameters.addIntSpinner("offsetY", Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 80);

	public TileLayerSource(LayerSources sources) {
		super(LayerSourceType.TILE, sources);
		this.dependencies.addExtraNodeRow(new HBox(new Label("Offset X: "), this.offsetX));
		this.dependencies.addExtraNodeRow(new HBox(new Label("Offset Y: "), this.offsetY));
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}

	@Override
	public int computeMaxProgress(int width, int height) {
		return switch (this.dependencies.main.getCurrent()) {
			case UniformSamplerProvider _ -> 0;
			case VaryingSamplerProvider _ -> height;
		};
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		HDRImage destination = this.sources.layer.getFrame(frame);
		boolean clampRGB = this.clampRGB.isSelected();
		boolean clampA = this.clampAlpha.isSelected();
		switch (this.dependencies.main.getCurrent().createSamplerForFrame(frame)) {
			case UniformSampler uniform -> {
				FloatVector color = clamp(uniform.getColor(), clampRGB, clampA);
				for (int index = 0; index < destination.pixels.length; index += 4) {
					color.intoArray(destination.pixels, index);
				}
			}
			case VaryingSampler varying -> {
				int offsetX = this.offsetX.getValue();
				int offsetY = this.offsetY.getValue();
				int srcWidth = varying.getBackingLayer().imageWidth();
				int srcHeight = varying.getBackingLayer().imageHeight();
				IntStream.range(0, destination.height).parallel().forEach((int y) -> {
					int modY = Math.floorMod(y - offsetY, srcHeight);
					for (int x = 0; x < destination.width; x++) {
						int modX = Math.floorMod(x - offsetX, srcWidth);
						destination.setColor(x, y, clamp(varying.getColor(modX, modY), clampRGB, clampA));
					}
					this.incrementProgress();
				});
			}
		}
	}
}