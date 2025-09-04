package builderb0y.bigpixel.sources.dependencies.inputs;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.LayerNode;

public sealed interface LayerSourceInput {

	public abstract FloatVector getColor(int x, int y);

	public static non-sealed interface UniformLayerSourceInput extends LayerSourceInput {

		public abstract FloatVector getColor();

		@Override
		public default FloatVector getColor(int x, int y) {
			return this.getColor();
		}
	}

	public static non-sealed interface VaryingLayerSourceInput extends LayerSourceInput {

		public abstract LayerNode getBackingLayer();

		@Override
		public default FloatVector getColor(int x, int y) {
			return this.getBackingLayer().image.getColor(x, y);
		}
	}
}