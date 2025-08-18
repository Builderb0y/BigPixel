package builderb0y.bigpixel.sources.dependencies.inputs;

import java.util.function.UnaryOperator;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.LayerNode;

public sealed interface LayerSourceInput {

	public abstract FloatVector getColor(int x, int y);

	public abstract LayerSourceInput mapColors(UnaryOperator<FloatVector> operator);

	public static non-sealed interface UniformLayerSourceInput extends LayerSourceInput {

		public abstract FloatVector getColor();

		@Override
		public default FloatVector getColor(int x, int y) {
			return this.getColor();
		}

		@Override
		public abstract UniformLayerSourceInput mapColors(UnaryOperator<FloatVector> operator);
	}

	public static non-sealed interface VaryingLayerSourceInput extends LayerSourceInput {

		public abstract LayerNode getBackingLayer();

		@Override
		public default FloatVector getColor(int x, int y) {
			return this.getBackingLayer().image.getColor(x, y);
		}

		@Override
		public default VaryingLayerSourceInput mapColors(UnaryOperator<FloatVector> operator) {
			return new VaryingLayerSourceInput() {

				@Override
				public LayerNode getBackingLayer() {
					return VaryingLayerSourceInput.this.getBackingLayer();
				}

				@Override
				public FloatVector getColor(int x, int y) {
					return operator.apply(VaryingLayerSourceInput.this.getColor(x, y));
				}

				@Override
				public VaryingLayerSourceInput mapColors(UnaryOperator<FloatVector> operator2) {
					return VaryingLayerSourceInput.this.mapColors((FloatVector color) -> operator2.apply(operator.apply(color)));
				}
			};
		}
	}
}