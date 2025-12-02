package builderb0y.bigpixel.sources.dependencies.inputs;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;

public sealed interface Sampler {

	public abstract FloatVector getColor(int x, int y);

	public default FloatVector getColor(IntVector uv) {
		return this.getColor(uv.lane(0), uv.lane(1));
	}

	public static non-sealed interface UniformSampler extends Sampler {

		public abstract FloatVector getColor();

		@Override
		public default FloatVector getColor(int x, int y) {
			return this.getColor();
		}

		public static UniformSampler of(FloatVector color) {
			return () -> color;
		}
	}

	public static non-sealed interface VaryingSampler extends Sampler {

		public abstract LayerNode getBackingLayer();

		@Override
		public abstract FloatVector getColor(int x, int y);

		public static VaryingSampler of(LayerNode layer, HDRImage frame) {
			return new VaryingSampler() {

				@Override
				public LayerNode getBackingLayer() {
					return layer;
				}

				@Override
				public FloatVector getColor(int x, int y) {
					return frame.getColor(x, y);
				}
			};
		}

		public static VaryingSampler inverted(LayerNode layer, HDRImage frame) {
			return new VaryingSampler() {

				@Override
				public LayerNode getBackingLayer() {
					return layer;
				}

				@Override
				public FloatVector getColor(int x, int y) {
					return frame.getColor(x, frame.height + ~y);
				}
			};
		}
	}
}