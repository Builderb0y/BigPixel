package builderb0y.bigpixel.views;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.ZoomableImage.DrawParams;
import builderb0y.bigpixel.util.Util;

public class FlatTilingLayerView extends LayerView2D {

	public final GridPane rootConfigPane = new GridPane();
	public final CheckBox darkenExterior = this.parameters.addCheckbox("darken_exterior", "Darken Exterior", true);
	public final ObjectBinding<DrawParams> drawParams;

	public FlatTilingLayerView(LayerViews views) {
		super(LayerViewType.FLAT_TILING, views);
		this.rootConfigPane.add(this.drawOutline, 0, 0);
		this.rootConfigPane.add(this.darkenExterior, 0, 1);
		this.drawParams = Bindings.createObjectBinding(
			() -> {
				record Params(
					double offsetX,
					double offsetY,
					double zoom,
					boolean darkenExterior,
					boolean drawOutline
				)
				implements DrawParams {}

				return new Params(
					this.offsetX.get(),
					this.offsetY.get(),
					this.zoom.get(),
					this.darkenExterior.isSelected(),
					this.drawOutline.isSelected()
				);
			},
			this.offsetX,
			this.offsetY,
			this.zoom,
			this.darkenExterior.selectedProperty(),
			this.drawOutline.selectedProperty()
		);
	}

	@Override
	public ObservableValue<DrawParams> drawParamsProperty() {
		return this.drawParams;
	}

	@Override
	public @Nullable ProjectionResult handleEdge(double rawX, double rawY, int projectedX, int projectedY, int frameIndex) {
		boolean outside = this.darkenExterior.isSelected() && (projectedX < 0 || projectedX >= this.layerWidth || projectedY < 0 || projectedY >= this.layerHeight);
		rawX = Util.modulus_BP(rawX, this.layerWidth);
		rawY = Util.modulus_BP(rawY, this.layerHeight);
		projectedX = Math.floorMod(projectedX, this.layerWidth);
		projectedY = Math.floorMod(projectedY, this.layerHeight);
		LayerNode layer = this.views.layer;
		HDRImage image = layer.getFrame(frameIndex);
		int base = image.baseIndex(projectedX, projectedY);
		float r = image.pixels[base | HDRImage.  RED_OFFSET];
		float g = image.pixels[base | HDRImage.GREEN_OFFSET];
		float b = image.pixels[base | HDRImage. BLUE_OFFSET];
		float a = image.pixels[base | HDRImage.ALPHA_OFFSET];
		return new ProjectionResult(layer, rawX, rawY, projectedX, projectedY, r, g, b, a, outside ? 0.75F : 1.0F);
	}

	@Override
	public Node getRootConfigPane() {
		return this.rootConfigPane;
	}
}