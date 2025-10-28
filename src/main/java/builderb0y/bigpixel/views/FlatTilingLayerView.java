package builderb0y.bigpixel.views;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.Util;

public class FlatTilingLayerView extends LayerView2D {

	public GridPane rootConfigPane = new GridPane();
	public CheckBox darkenExterior = this.parameters.addCheckbox("darken_exterior", "Darken Exterior", true);

	public FlatTilingLayerView(LayerViews views) {
		super(Type.FLAT_TILING, views);
		this.rootConfigPane.add(this.drawOutline, 0, 0);
		this.rootConfigPane.add(this.darkenExterior, 0, 1);
	}

	@Override
	public @Nullable ProjectionResult handleEdge(int projectedX, int projectedY) {
		boolean outside = this.darkenExterior.isSelected() && (projectedX < 0 || projectedX >= this.layerWidth || projectedY < 0 || projectedY >= this.layerHeight);
		return new ProjectionResult(
			this.views.layer,
			Math.floorMod(projectedX, this.layerWidth),
			Math.floorMod(projectedY, this.layerHeight)
		) {

			@Override
			public FloatVector sample() {
				FloatVector sample = super.sample();
				if (outside) sample = sample.mul(0.75F, Util.RGB_MASK);
				return sample;
			}
		};
	}

	@Override
	public Node getRootConfigPane() {
		return this.rootConfigPane;
	}
}