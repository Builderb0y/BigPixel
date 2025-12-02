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
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;

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
	public void doRedraw(int frame) throws RedrawException {
		boolean resized = false;
		HDRImage destination = this.sources.layer.getFrame(frame);
		for (InputBinding binding : this.dependencies.allBindings.values()) {
			if (this.requiresSameSize(binding)) {
				if (binding.getCurrent() instanceof VaryingSampler varying) {
					HDRImage source = varying.getBackingLayer().getFrame(frame);
					if (source.width != destination.width || source.height != destination.height) {
						if (resized) {
							throw new RedrawException("Not all input layers are of the same resolution");
						}
						else {
							destination.resize(source.width, source.height, false);
						}
					}
					resized = true;
				}
			}
		}
		this.doRedraw(
			this.dependencies.main.getCurrent().createSamplerForFrame(frame),
			this.dependencies.mask.getCurrent().createSamplerForFrame(frame),
			destination,
			frame
		);
	}

	public boolean requiresSameSize(InputBinding input) {
		return true;
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