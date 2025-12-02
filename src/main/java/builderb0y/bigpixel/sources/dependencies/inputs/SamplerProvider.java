package builderb0y.bigpixel.sources.dependencies.inputs;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.UniformSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;

public sealed interface SamplerProvider {

	public abstract Sampler createSamplerForCurrentFrame();

	public abstract Sampler createSamplerForFrame(int frame);

	@Override
	public String toString();

	public static non-sealed interface UniformSamplerProvider extends SamplerProvider {

		public abstract FloatVector getColor();

		@Override
		public default UniformSampler createSamplerForCurrentFrame() {
			return UniformSampler.of(this.getColor());
		}

		@Override
		public default UniformSampler createSamplerForFrame(int frame) {
			return UniformSampler.of(this.getColor());
		}
	}

	public static non-sealed interface VaryingSamplerProvider extends SamplerProvider {

		public abstract LayerNode getBackingLayer();

		@Override
		public default VaryingSampler createSamplerForCurrentFrame() {
			return VaryingSampler.of(this.getBackingLayer(), this.getBackingLayer().getFrame());
		}

		@Override
		public default VaryingSampler createSamplerForFrame(int frame) {
			return VaryingSampler.of(this.getBackingLayer(), this.getBackingLayer().getFrame(frame));
		}
	}
}