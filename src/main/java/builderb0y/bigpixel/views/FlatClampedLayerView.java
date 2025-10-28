package builderb0y.bigpixel.views;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

public class FlatClampedLayerView extends LayerView2D {

	public GridPane rootConfigPane = new GridPane();

	public FlatClampedLayerView(LayerViews views) {
		super(Type.FLAT_CLAMPED, views);
		this.rootConfigPane.add(this.drawOutline, 0, 0);
	}

	@Override
	public @Nullable ProjectionResult handleEdge(int projectedX, int projectedY) {
		return (
			projectedX >= 0 &&
			projectedX < this.layerWidth &&
			projectedY >= 0 &&
			projectedY < this.layerHeight
			? new ProjectionResult(
				this.views.layer,
				projectedX,
				projectedY
			)
			: null
		);
	}

	@Override
	public Node getRootConfigPane() {
		return this.rootConfigPane;
	}
}