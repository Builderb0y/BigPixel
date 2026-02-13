package builderb0y.bigpixel.sources.dependencies.inputs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.ConfigParameter;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.OrganizedSelection;
import builderb0y.bigpixel.SaveException;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.ColorBox;
import builderb0y.bigpixel.sources.ColorBoxGroup;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.Util;

public class InputBinding {

	public OrganizedSelection.Value<?> owner;
	public ColorBox colorBox;
	public ImageView thumbnail;
	public ChoiceBox<SamplerProvider> selection;
	public ObservableBooleanValue animated;
	public CurveHelper curve;
	public boolean changing;

	public JsonMap save() {
		return switch (this.selection.getValue()) {
			case UniformSamplerProvider uniform -> new JsonMap().with("type", "color").with("color", ConfigParameter.colorToJson(uniform.getColor()));
			case VaryingSamplerProvider varying -> new JsonMap().with("type", "layer").with("layer", varying.getBackingLayer().getDisplayName());
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
		this.selection.valueProperty().addListener(Util.change((SamplerProvider supplier) -> {
			if (supplier == null && !this.changing) new Throwable("stack trace").printStackTrace();
		}));
		this.colorBox.getDisplayPane().visibleProperty().bind(this.selection.valueProperty().isEqualTo(this.colorBox));
		this.thumbnail.visibleProperty().bind(this.selection.valueProperty().map(VaryingSampler.class::isInstance));
		this.thumbnail.imageProperty().bind(this.selection.valueProperty().flatMap((SamplerProvider supplier) -> switch (supplier) {
			case UniformSamplerProvider uniform -> null;
			case VaryingSamplerProvider varying -> varying.getBackingLayer().smallThumbnail.currentFrame;
		}));
		ChangeListener<Object> listener = Util.change(() -> {
			if (!this.changing) owner.redrawLater();
		});
		this.selection.valueProperty().addListener(listener);
		this.colorBox.color.addListener(listener);
		this.curve = new CurveHelper(owner.getLayer(), curveColor);
		this.selection.valueProperty().addListener(Util.change(this.curve::setOtherEnd));
		this.selection.setPrefWidth(128.0D);
		this.animated = Util.toBoolean(
			this
			.selection
			.valueProperty()
			.flatMap((SamplerProvider input) -> switch (input) {
				case UniformSamplerProvider uniform -> null;
				case VaryingSamplerProvider varying -> varying.getBackingLayer().sources.selectedValue;
			})
			.flatMap((LayerSource source) -> source.getDependencies().animatedProperty()),
			false
		);
	}

	public @Nullable LayerNode getSelectedLayer() {
		return this.selection.getValue() instanceof VaryingSamplerProvider varying ? varying.getBackingLayer() : null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void retainAll(List<LayerNode> layers) {
		List inputs = new ArrayList<>(layers.size() + 1);
		inputs.addAll(layers);
		inputs.sort(Comparator.comparing(LayerNode::getDisplayName, String.CASE_INSENSITIVE_ORDER));
		inputs.addFirst(this.colorBox);
		if (!this.selection.getItems().equals(inputs)) {
			SamplerProvider selected = this.selection.getValue();
			boolean oldChanging = this.changing;
			this.changing = true;
			try {
				this.selection.getItems().setAll(inputs);
				this.selection.setValue(
					selected instanceof VaryingSamplerProvider varying
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
	}

	public SamplerProvider getCurrent() {
		return this.selection.getValue();
	}
}