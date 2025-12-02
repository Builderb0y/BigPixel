package builderb0y.bigpixel.views;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.AnimationView.DrawParams;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;

public class FlatClampedLayerView extends LayerView2D {

	public final GridPane rootConfigPane = new GridPane();
	public final ObservableValue<DrawParams> drawParams;

	public FlatClampedLayerView(LayerViews views) {
		super(LayerViewType.FLAT_CLAMPED, views);
		this.rootConfigPane.add(this.drawOutline, 0, 0);
		this.drawParams = Bindings.createObjectBinding(
			() -> {
				record Params(
					double offsetX,
					double offsetY,
					double zoom,
					boolean drawOutline
				)
				implements DrawParams {}
				return new Params(
					this.offsetX.get(),
					this.offsetY.get(),
					this.zoom.get(),
					this.drawOutline.isSelected()
				);
			},
			this.offsetX,
			this.offsetY,
			this.zoom,
			this.drawOutline.selectedProperty()
		);
	}

	@Override
	public ObservableValue<DrawParams> drawParamsProperty() {
		return this.drawParams;
	}

	@Override
	public @Nullable ProjectionResult handleEdge(int projectedX, int projectedY) {
		if (projectedX >= 0 && projectedX < this.layerWidth && projectedY >= 0 && projectedY < this.layerHeight) {
			LayerNode layer = this.views.layer;
			HDRImage image = layer.getFrame();
			int base = image.baseIndex(projectedX, projectedY);
			return new ProjectionResult(
				layer,
				projectedX,
				projectedY,
				image.pixels[base | HDRImage.  RED_OFFSET],
				image.pixels[base | HDRImage.GREEN_OFFSET],
				image.pixels[base | HDRImage. BLUE_OFFSET],
				image.pixels[base | HDRImage.ALPHA_OFFSET]
			);
		}
		return null;
	}

	@Override
	public Node getRootConfigPane() {
		return this.rootConfigPane;
	}
}