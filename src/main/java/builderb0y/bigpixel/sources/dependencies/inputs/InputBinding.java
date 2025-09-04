package builderb0y.bigpixel.sources.dependencies.inputs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.SaveException;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBox;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.SourceParameter;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public class InputBinding {

	public LayerSource source;
	public ColorBox colorBox;
	public ImageView thumbnail;
	public ChoiceBox<LayerSourceInput> selection;
	public ObservableValue<LayerSourceInput> delegate;
	public CurveHelper curve;

	public JsonMap save() {
		return switch (this.selection.getValue()) {
			case UniformLayerSourceInput uniform -> new JsonMap().with("type", "color").with("color", SourceParameter.colorToJson(uniform.getColor()));
			case VaryingLayerSourceInput varying -> new JsonMap().with("type", "layer").with("layer", varying.getBackingLayer().getDisplayName());
		};
	}

	public void load(JsonMap map) {
		String type = map.getString("type");
		switch (type) {
			case "color" -> {
				this.colorBox.color.set(SourceParameter.colorFromJson(map.getArray("color")));
				this.selection.setValue(this.colorBox);
			}
			case "layer" -> {
				String layerName = map.getString("layer");
				LayerNode self = this.source.sources.layer;
				LayerNode layer = self.graph.getLayerByName(layerName);
				if (layer == null) {
					throw new SaveException("Unknown layer dependency: " + layerName + " for dependent layer " + self.getDisplayName());
				}
				if (layer.getGridX() >= self.getGridX()) {
					layer = null;
					//throw new SaveException("Invalid position for layer dependency " + self.getDisplayName() + " for dependent layer " + layer.getDisplayName());
				}
				this.selection.setValue(layer);
			}
			default -> {
				throw new SaveException("Unknown dependency type: " + type);
			}
		}
	}

	public InputBinding(LayerSource source, ColorBoxGroup group) {
		this.source    = source;
		this.colorBox  = group.addBox(new ColorBox(Util.WHITE));
		this.thumbnail = new ImageView();
		this.selection = new ChoiceBox<>();
		this.delegate  = this.selection.valueProperty();
		this.colorBox.getDisplayPane().visibleProperty().bind(this.selection.valueProperty().isEqualTo(this.colorBox));
		this.thumbnail.visibleProperty().bind(this.selection.valueProperty().map(VaryingLayerSourceInput.class::isInstance));
		ObservableValue<ImageView> thumbnailCopying = this.selection.valueProperty().map((LayerSourceInput input) -> {
			return input instanceof VaryingLayerSourceInput varying ? varying.getBackingLayer().thumbnailView : null;
		});
		this.thumbnail.imageProperty().bind(thumbnailCopying.flatMap(ImageView::imageProperty));
		this.thumbnail.fitWidthProperty().bind(thumbnailCopying.flatMap(ImageView::fitWidthProperty));
		this.thumbnail.fitHeightProperty().bind(thumbnailCopying.flatMap(ImageView::fitHeightProperty));
		this.thumbnail.setPreserveRatio(true);
		ChangeListener<Object> listener = Util.change(source::requestRedraw);
		this.selection.valueProperty().addListener(listener);
		this.colorBox.color.addListener(listener);
		this.curve = new CurveHelper(this.source.sources.layer);
		this.selection.valueProperty().addListener(Util.change(this.curve::setOtherEnd));
		this.selection.setPrefWidth(128.0D);
	}

	public @Nullable LayerNode getSelectedLayer() {
		return this.selection.getValue() instanceof VaryingLayerSourceInput varying ? varying.getBackingLayer() : null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void retainAll(List<LayerNode> layers) {
		List inputs = new ArrayList<>(layers.size() + 1);
		inputs.addAll(layers);
		inputs.sort(Comparator.comparing(LayerNode::getDisplayName, String.CASE_INSENSITIVE_ORDER));
		inputs.addFirst(this.colorBox);
		LayerSourceInput selected = this.selection.getValue();
		this.selection.getItems().setAll(inputs);
		this.selection.setValue(
			selected instanceof VaryingLayerSourceInput varying
			&& layers.contains(varying.getBackingLayer())
			? selected
			: this.colorBox
		);
	}

	public LayerSourceInput getCurrent() {
		return this.delegate.getValue();
	}
}