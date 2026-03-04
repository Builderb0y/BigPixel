package builderb0y.bigpixel.views;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.ZoomableImage.DrawParams;

public class FlatClampedLayerView extends LayerView2D {

	public final GridPane rootConfigPane = new GridPane();
	public final ObservableValue<DrawParams> drawParams;

	public FlatClampedLayerView(LayerViews views) {
		super(LayerViewType.FLAT_CLAMPED, views);
		this.rootConfigPane.add(this.drawOutline, 0, 0);
		this.rootConfigPane.add(this.showAlpha, 0, 1);
		this.drawParams = Bindings.createObjectBinding(
			() -> {
				record Params(
					double offsetX,
					double offsetY,
					double zoom,
					boolean drawOutline,
					boolean showAlpha
				)
				implements DrawParams {}
				return new Params(
					this.offsetX.get(),
					this.offsetY.get(),
					this.zoom.get(),
					this.drawOutline.isSelected(),
					this.showAlpha.isSelected()
				);
			},
			this.offsetX,
			this.offsetY,
			this.zoom,
			this.drawOutline.selectedProperty(),
			this.showAlpha.selectedProperty()
		);
	}

	@Override
	public ObservableValue<DrawParams> drawParamsProperty() {
		return this.drawParams;
	}

	@Override
	public @Nullable ProjectionResult handleEdge(double rawX, double rawY, int projectedX, int projectedY, int frameIndex) {
		if (projectedX >= 0 && projectedX < this.layerWidth && projectedY >= 0 && projectedY < this.layerHeight) {
			LayerNode layer = this.views.layer;
			HDRImage image = layer.getFrame(frameIndex);
			int base = image.baseIndex(projectedX, projectedY);
			float
				r = image.pixels[base | HDRImage.  RED_OFFSET],
				g = image.pixels[base | HDRImage.GREEN_OFFSET],
				b = image.pixels[base | HDRImage. BLUE_OFFSET],
				a = image.pixels[base | HDRImage.ALPHA_OFFSET];
			return new ProjectionResult(
				layer,
				rawX,
				rawY,
				projectedX,
				projectedY,
				r, g, b, a,
				r, g, b, a
			);
		}
		else {
			return null;
		}
	}

	@Override
	public Node getRootConfigPane() {
		return this.rootConfigPane;
	}
}