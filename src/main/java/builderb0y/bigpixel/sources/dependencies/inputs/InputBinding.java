package builderb0y.bigpixel.sources.dependencies.inputs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.*;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBox;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.UniformLayerSourceInput;
import builderb0y.bigpixel.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

public class InputBinding {

	public OrganizedSelection.Value<?> owner;
	public ColorBox colorBox;
	public ImageView thumbnail;
	public ChoiceBox<LayerSourceInput> selection;
	public CurveHelper curve;
	public boolean changing;

	public JsonMap save() {
		return switch (this.selection.getValue()) {
			case UniformLayerSourceInput uniform -> new JsonMap().with("type", "color").with("color", ConfigParameter.colorToJson(uniform.getColor()));
			case VaryingLayerSourceInput varying -> new JsonMap().with("type", "layer").with("layer", varying.getBackingLayer().getDisplayName());
		};
	}

	public void load(JsonMap map) {
		String type = map.getString("type");
		switch (type) {
			case "color" -> {
				this.colorBox.color.set(ConfigParameter.colorFromJson(map.getArray("color")));
				this.selection.setValue(this.colorBox);
			}
			case "layer" -> {
				String layerName = map.getString("layer");
				LayerNode self = this.owner.getLayer();
				LayerNode layer = self.graph.getLayerByName(layerName);
				if (layer == null) {
					throw new SaveException("Unknown layer dependency: " + layerName + " for dependent layer " + self.getDisplayName());
				}
				this.selection.setValue(layer);
			}
			default -> {
				throw new SaveException("Unknown dependency type: " + type);
			}
		}
	}

	public InputBinding(OrganizedSelection.Value<?> owner, ColorBoxGroup group, Color curveColor) {
		this.owner = owner;
		this.colorBox  = group.addBox(new ColorBox(Util.WHITE));
		this.thumbnail = new ImageView();
		this.selection = new ChoiceBox<>(FXCollections.observableArrayList(this.colorBox));
		this.selection.setValue(this.colorBox);
		this.selection.valueProperty().addListener(Util.change((LayerSourceInput input) -> {
			if (input == null && !this.changing) new Throwable("stack trace").printStackTrace();
		}));
		this.colorBox.getDisplayPane().visibleProperty().bind(this.selection.valueProperty().isEqualTo(this.colorBox));
		this.thumbnail.visibleProperty().bind(this.selection.valueProperty().map(VaryingLayerSourceInput.class::isInstance));
		ObservableValue<ImageView> thumbnailCopying = this.selection.valueProperty().map((LayerSourceInput input) -> {
			return input instanceof VaryingLayerSourceInput varying ? varying.getBackingLayer().thumbnailView : null;
		});
		this.thumbnail.imageProperty().bind(thumbnailCopying.flatMap(ImageView::imageProperty));
		this.thumbnail.fitWidthProperty().bind(thumbnailCopying.flatMap(ImageView::fitWidthProperty));
		this.thumbnail.fitHeightProperty().bind(thumbnailCopying.flatMap(ImageView::fitHeightProperty));
		this.thumbnail.setPreserveRatio(true);
		ChangeListener<Object> listener = Util.change(() -> {
			if (!this.changing) owner.redrawLater();
		});
		this.selection.valueProperty().addListener(listener);
		this.colorBox.color.addListener(listener);
		this.curve = new CurveHelper(owner.getLayer(), curveColor);
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
		boolean oldChanging = this.changing;
		this.changing = true;
		try {
			this.selection.getItems().setAll(inputs);
			this.selection.setValue(
				selected instanceof VaryingLayerSourceInput varying
				&& layers.contains(varying.getBackingLayer())
				? selected
				: this.colorBox
			);
		}
		finally {
			this.changing = oldChanging;
		}
		if (this.selection.getValue() != selected) {
			this.owner.redrawLater();
		}
	}

	public LayerSourceInput getCurrent() {
		return this.selection.getValue();
	}
}