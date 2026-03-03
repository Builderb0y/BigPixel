package builderb0y.bigpixel.sources;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.scripting.types.VectorOperations;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.MainMaskDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;

public abstract class MainMaskLayerSource extends LayerSource {

	public MainMaskDependencies dependencies = this.createDependencies();

	public MainMaskLayerSource(LayerSourceType type, LayerSources sources) {
		super(type, sources);
	}

	public MainMaskDependencies createDependencies() {
		return new MainMaskDependencies(this);
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}

	@Override
	public void resizeIfNecessary() throws RedrawException {
		int selfWidth = this.sources.layer.imageWidth();
		int selfHeight = this.sources.layer.imageHeight();
		boolean resized = false;
		for (InputBinding binding : this.dependencies.allBindings.values()) {
			if (binding.getCurrent() instanceof VaryingSamplerProvider varying) {
				int otherWidth = varying.getBackingLayer().imageWidth();
				int otherHeight = varying.getBackingLayer().imageHeight();
				if (selfWidth != otherWidth || selfHeight != otherHeight) {
					if (resized) {
						throw new RedrawException("Not all input layers are of the same resolution");
					}
					else {
						this.sources.layer.animation.resize(
							selfWidth = otherWidth,
							selfHeight = otherHeight,
							false
						);
					}
				}
				resized = true;
			}
		}
	}

	public void applyMask(Sampler main, Sampler mask, HDRImage destination) {
		switch (mask) {
			case UniformSampler uniformMask -> {
				FloatVector maskColor = uniformMask.getColor();
				VectorMask<Float>  oneLanes = maskColor.compare(VectorOperators.GE, 1.0F);
				VectorMask<Float> zeroLanes = maskColor.compare(VectorOperators.LE, 0.0F);
				if (oneLanes.allTrue()) {
					//no-op: mask behaves as passthrough to computed color.
				}
				else if (zeroLanes.allTrue()) {
					//mask blocks computed color, allowing main color to be visible.
					for (int y = 0; y < destination.height; y++) {
						for (int x = 0; x < destination.width; x++) {
							destination.setColor(x, y, main.getColor(x, y));
						}
					}
				}
				else if (oneLanes.or(zeroLanes).allTrue()) {
					//mask is either completely passthrough or completely blocking for each component.
					for (int y = 0; y < destination.height; y++) {
						for (int x = 0; x < destination.width; x++) {
							FloatVector color = main.getColor(x, y);
							destination.setColor(x, y, color.blend(destination.getColor(x, y), oneLanes));
						}
					}
				}
				else {
					//mask contains at least one component which needs to be interpolated.
					for (int y = 0; y < destination.height; y++) {
						for (int x = 0; x < destination.width; x++) {
							FloatVector color = main.getColor(x, y);
							destination.setColor(x, y, carefulMix(color, destination.getColor(x, y), maskColor, zeroLanes, oneLanes));
						}
					}
				}
			}
			case VaryingSampler varyingMask -> {
				for (int y = 0; y < destination.height; y++) {
					for (int x = 0; x < destination.width; x++) {
						FloatVector maskColor = mask.getColor(x, y);
						VectorMask<Float> zeroLanes = maskColor.compare(VectorOperators.LE, 0.0F);
						VectorMask<Float>  oneLanes = maskColor.compare(VectorOperators.GE, 1.0F);
						if (oneLanes.allTrue()) {
							//no-op.
						}
						else if (zeroLanes.allTrue()) {
							destination.setColor(x, y, main.getColor(x, y));
						}
						else if (oneLanes.or(zeroLanes).allTrue()) {
							FloatVector mainColor = main.getColor(x, y);
							destination.setColor(x, y, mainColor.blend(destination.getColor(x, y), oneLanes));
						}
						else {
							FloatVector mainColor = main.getColor(x, y);
							destination.setColor(x, y, carefulMix(mainColor, destination.getColor(x, y), maskColor, zeroLanes, oneLanes));
						}
					}
				}
			}
		}
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		this.doRedraw(
			this.dependencies.main.getCurrent().createSamplerForFrame(frame),
			this.dependencies.mask.getCurrent().createSamplerForFrame(frame),
			this.sources.layer.getFrame(frame),
			frame
		);
	}

	public abstract void doRedraw(Sampler main, Sampler mask, HDRImage destination, int frame) throws RedrawException;

	//handles NaN's and inf's properly.
	public static FloatVector carefulMix(FloatVector a, FloatVector b, FloatVector f, VectorMask<Float> zeroLanes, VectorMask<Float> oneLanes) {
		FloatVector c = VectorOperations.mix_float4_float4_float4(a, b, f);
		c = c.blend(a, zeroLanes);
		c = c.blend(b, oneLanes);
		return c;
	}

	public static FloatVector carefulMix(FloatVector a, FloatVector b, FloatVector f) {
		return carefulMix(a, b, f, f.compare(VectorOperators.LE, 0.0F), f.compare(VectorOperators.GE, 1.0F));
	}
}