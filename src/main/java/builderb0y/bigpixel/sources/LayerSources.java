package builderb0y.bigpixel.sources;

import java.util.EnumMap;
import java.util.List;

import javafx.beans.binding.When;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ChoiceBox;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.SaveException;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.tools.Tool;

public class LayerSources {

	public EnumMap<LayerSource.Type, LayerSource> sourcesByType = new EnumMap<>(LayerSource.Type.class);

	public LayerNode layer;
	public ChoiceBox<LayerSource.Type> sourceTypeBox = new ChoiceBox<>();

	public ReadOnlyObjectProperty<LayerSource.Type>
		currentTypeProperty = this.sourceTypeBox.getSelectionModel().selectedItemProperty();
	public ObservableValue<LayerSource>
		currentSourceProperty = this.currentTypeProperty.map(this::getOrCreateSource);
	public ObservableObjectValue<@Nullable Tool<?>>
		toolWithoutColorPicker = (
			new When(this.currentTypeProperty.isEqualTo(LayerSource.Type.MANUAL))
			.then(this.manualSource().toolWithoutColorPicker)
			.otherwise((Tool<?>)(null))
		);

	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("tab", this.getCurrentSource().save());
		return map;
	}

	public void load(JsonMap saveData) {
		JsonMap tab = saveData.getMap("tab");
		String typeName = tab.getString("type");
		LayerSource.Type type = LayerSource.Type.BY_SAVE_NAME.get(typeName);
		if (type == null) throw new SaveException("Unknown current layer source: " + typeName);
		this.getOrCreateSource(type).load(tab);
		this.sourceTypeBox.getSelectionModel().select(type);
	}

	public LayerSources(LayerNode layer) {
		this.layer = layer;
		super();
		this.sourceTypeBox.getItems().addAll(LayerSource.Type.VALUES);
		this.sourceTypeBox.getSelectionModel().select(LayerSource.Type.MANUAL);
		this.sourceTypeBox.valueProperty().addListener(Util.change(
			(LayerSource.Type oldType, LayerSource.Type newType) -> {
				LayerSource oldSource = this.sourcesByType.get(oldType);
				if (oldSource != null) {
					oldSource.getDependencies().getCurves().forEach((CurveHelper helper) -> helper.selfSourceIsSelected.set(false));
				}
				LayerSource newSource = this.sourcesByType.get(newType);
				if (newSource != null) {
					newSource.getDependencies().getCurves().forEach((CurveHelper helper) -> helper.selfSourceIsSelected.set(true));
				}
			}
		));
	}

	public LayerSources(LayerNode newLayer, LayerSources from) {
		this(newLayer);
		LayerSource fromSource = from.getCurrentSource();
		this.getOrCreateSource(fromSource.type).copyFrom(fromSource);
	}

	public LayerSource getOrCreateSource(LayerSource.Type type) {
		return this.sourcesByType.computeIfAbsent(type, (LayerSource.Type type_) -> {
			LayerSource source = type_.factory.apply(this);
			source.getDependencies().retainAll(this.layer.graph.getPossibleDependencies(this.layer));
			return source;
		});
	}

	public ManualLayerSource manualSource() {
		return (ManualLayerSource)(this.getOrCreateSource(LayerSource.Type.MANUAL));
	}

	public LayerSource getCurrentSource() {
		return this.getOrCreateSource(this.currentTypeProperty.getValue());
	}

	public void setPotentialInputs(List<LayerNode> layers) {
		for (LayerSource source : this.sourcesByType.values()) {
			source.getDependencies().retainAll(layers);
		}
	}
}