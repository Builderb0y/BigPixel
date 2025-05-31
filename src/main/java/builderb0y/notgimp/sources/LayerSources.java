package builderb0y.notgimp.sources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.binding.When;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.SaveException;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.tools.Tool;

public class LayerSources {

	public List<LayerSource> orderedSources = new ArrayList<>(32);
	public Map<String, LayerSource> namedSources = new HashMap<>(32);

	public Layer layer;
	public BorderPane rootPane = new BorderPane();
	public ChoiceBox<LayerSource> choiceBox = new ChoiceBox<>();

	public        ManualLayerSource        manualSource = this.add(new        ManualLayerSource(this));

	public         AlphaLayerSource         alphaSource = this.add(new         AlphaLayerSource(this));
	public           AddLayerSource           addSource = this.add(new           AddLayerSource(this));
	public       AverageLayerSource       averageSource = this.add(new       AverageLayerSource(this));
	public      MultiplyLayerSource      multiplySource = this.add(new      MultiplyLayerSource(this));
	public        ScreenLayerSource        screenSource = this.add(new        ScreenLayerSource(this));
	public           MinLayerSource           minSource = this.add(new           MinLayerSource(this));
	public           MaxLayerSource           maxSource = this.add(new           MaxLayerSource(this));

	public        InvertLayerSource        invertSource = this.add(new        InvertLayerSource(this));
	public     NormalizeLayerSource     normalizeSource = this.add(new     NormalizeLayerSource(this));
	public         ClampLayerSource         clampSource = this.add(new         ClampLayerSource(this));
	public GradientRemapLayerSource gradientRemapSource = this.add(new GradientRemapLayerSource(this));
	public   ColorMatrixLayerSource   colorMatrixSource = this.add(new   ColorMatrixLayerSource(this));
	public    CliffCurveLayerSource    cliffCurveSource = this.add(new    CliffCurveLayerSource(this));
	public      ConvolveLayerSource      convolveSource = this.add(new      ConvolveLayerSource(this));
	public        KMeansLayerSource        kMeansSource = this.add(new        KMeansLayerSource(this));
	public           WFCLayerSource           wfcSource = this.add(new           WFCLayerSource(this));

	public       DerivedLayerSource       derivedSource = this.add(new       DerivedLayerSource(this));
	public    ProceduralLayerSource    proceduralSource = this.add(new    ProceduralLayerSource(this));

	public ReadOnlyObjectProperty<LayerSource>
		currentSourceProperty = this.choiceBox.getSelectionModel().selectedItemProperty();
	public ObservableObjectValue<@Nullable Tool<?>>
		toolWithoutColorPicker = (
			new When(this.currentSourceProperty.isEqualTo(this.manualSource))
			.then(this.manualSource.toolWithoutColorPicker)
			.otherwise((Tool<?>)(null))
		);

	public <S extends LayerSource> S add(S source) {
		LayerSource old = this.namedSources.putIfAbsent(source.saveName, source);
		if (old != null) {
			throw new IllegalArgumentException(old + " and " + source + " both have the same save name! (" + source.saveName + ')');
		}
		this.orderedSources.add(source);
		return source;
	}

	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("tab", this.getCurrentSource().save());
		return map;
	}

	public void load(JsonMap saveData) {
		JsonMap tab = saveData.getMap("tab");
		String type = tab.getString("type");
		LayerSource source = this.namedSources.get(type);
		if (source == null) throw new SaveException("Unknown current layer source: " + type);
		source.load(tab);
		this.choiceBox.getSelectionModel().select(source);
	}

	public LayerSources(Layer layer) {
		this.layer = layer;
		this.rootPane.setTop(this.choiceBox);
		this.rootPane.centerProperty().bind(this.currentSourceProperty.map(LayerSource::getRootNode));
		this.choiceBox.getItems().addAll(this.orderedSources);
		this.choiceBox.getSelectionModel().select(this.manualSource);
	}

	public LayerSources(Layer newLayer, LayerSources from) {
		this(newLayer);
		assert this.orderedSources.size() == from.orderedSources.size();
		for (int index = 0; index < this.orderedSources.size(); index++) {
			LayerSource ourSource = this.orderedSources.get(index);
			LayerSource fromSource = from.orderedSources.get(index);
			ourSource.copyFrom(fromSource);
		}
	}

	public void init(boolean fromSave) {
		this.currentSourceProperty.addListener(
			Util.change(
				(LayerSource oldSource, LayerSource newSource) -> {
					oldSource.onDeselected();
					newSource.onSelected();
				}
			)
		);
		this.orderedSources.forEach((LayerSource source) -> source.init(fromSave));
	}

	public void invalidateStructure() {
		this.getCurrentSource().invalidateStructure();
	}

	public LayerSource getCurrentSource() {
		return this.currentSourceProperty.getValue();
	}
}