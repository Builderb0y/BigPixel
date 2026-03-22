package builderb0y.bigpixel.sources;

import java.util.stream.IntStream;

import javafx.scene.control.CheckBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.MainDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.Util;

public class RescaleLayerSource extends LayerSource {

	public MainDependencies dependencies = new MainDependencies(this);
	public CheckBox linear = new CheckBox("Linear");

	public RescaleLayerSource(LayerSources sources) {
		super(LayerSourceType.RESCALE, sources);
		this.extraSettingsPane.getChildren().add(this.linear);
		this.linear.setSelected(true);
		this.linear.selectedProperty().addListener(Util.change(this::redrawLater));
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
				boolean linear = this.linear.isSelected();
				int toWidth = destination.width;
				int toHeight = destination.height;
				int fromWidth = varying.getBackingLayer().imageWidth();
				int fromHeight = varying.getBackingLayer().imageHeight();
				IntStream.range(0, toHeight).parallel().forEach((int toY) -> {
					for (int toX = 0; toX < toWidth; toX++) {
						double fromMinX = ((double)(toX    )) * ((double)(fromWidth )) / ((double)(toWidth ));
						double fromMinY = ((double)(toY    )) * ((double)(fromHeight)) / ((double)(toHeight));
						double fromMaxX = ((double)(toX + 1)) * ((double)(fromWidth )) / ((double)(toWidth ));
						double fromMaxY = ((double)(toY + 1)) * ((double)(fromHeight)) / ((double)(toHeight));
						FloatVector sum = Util.INVISIBLACK;
						float samples = 0.0F;
						int iterMinX = (int)(Math.floor(fromMinX));
						int iterMinY = (int)(Math.floor(fromMinY));
						int iterMaxX = (int)(Math.ceil (fromMaxX));
						int iterMaxY = (int)(Math.ceil (fromMaxY));
						for (int fromY = iterMinY; fromY < iterMaxY; fromY++) {
							for (int fromX = iterMinX; fromX < iterMaxX; fromX++) {
								FloatVector color = varying.getColor(fromX, fromY);
								color = color.max(0.0F).min(1.0F);
								if (linear) color = color.mul(color, Util.RGB_MASK);
								double regionMinX = Math.max(fromX,     fromMinX);
								double regionMinY = Math.max(fromY,     fromMinY);
								double regionMaxX = Math.min(fromX + 1, fromMaxX);
								double regionMaxY = Math.min(fromY + 1, fromMaxY);
								float weight = color.lane(HDRImage.ALPHA_OFFSET) * (float)((regionMaxX - regionMinX) * (regionMaxY - regionMinY));
								sum = sum.add(color.mul(weight));
								samples += weight;
							}
						}
						sum = samples == 0.0F ? Util.INVISIBLACK : sum.div(samples);
						if (linear) sum = sum.lanewise(VectorOperators.SQRT, Util.RGB_MASK);
						destination.setColor(toX, toY,  clamp(sum, clampRGB, clampA));
					}
					this.incrementProgress();
				});
			}
		};
	}
}