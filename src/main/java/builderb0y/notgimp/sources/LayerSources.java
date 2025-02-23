package builderb0y.notgimp.sources;

import javafx.beans.binding.Bindings;
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

	public Layer layer;
	public BorderPane rootPane = new BorderPane();
	public ChoiceBox<LayerSource> choiceBox = new ChoiceBox<>();

	public ManualLayerSource manualSource = new ManualLayerSource(this);
	public InvertLayerSource invertSource = new InvertLayerSource(this);
	public CliffCurveLayerSource cliffCurveSource = new CliffCurveLayerSource(this);
	public ConvolveLayerSource convolveSource = new ConvolveLayerSource(this);
	public NormalizeLayerSource normalizeSource = new NormalizeLayerSource(this);
	public DerivedLayerSource derivedSource = new DerivedLayerSource(this);
	public ProceduralLayerSource proceduralSource = new ProceduralLayerSource(this);

	public ReadOnlyObjectProperty<LayerSource>
		currentSourceProperty = this.choiceBox.getSelectionModel().selectedItemProperty();
	public ObservableObjectValue<@Nullable Tool<?>>
		toolWithoutColorPicker = (
			new When(Bindings.equal(this.currentSourceProperty, this.manualSource))
			.then(this.manualSource.toolWithoutColorPicker)
			.otherwise((Tool<?>)(null))
		);

	public JsonMap save() {
		JsonMap map = new JsonMap();
		map.add("tab", this.getCurrentSource().save());
		return map;
	}

	public void load(JsonMap saveData) {
		JsonMap tab = saveData.getMap("tab");
		String type = tab.getString("type");
		LayerSource source = switch (type) {
			case "manual" -> this.manualSource;
			case "invert" -> this.invertSource;
			case "cliff" -> this.cliffCurveSource;
			case "convolve" -> this.convolveSource;
			case "normalize" -> this.normalizeSource;
			case "derived" -> this.derivedSource;
			case "procedural" -> this.proceduralSource;
			default -> throw new SaveException("Unknown current layer source: " + type);
		};
		source.load(tab);
		this.choiceBox.getSelectionModel().select(source);
	}

	public LayerSources(Layer layer) {
		this.layer = layer;
		this.rootPane.setTop(this.choiceBox);
		this.rootPane.centerProperty().bind(this.currentSourceProperty.map(LayerSource::getRootNode));
		this.choiceBox.getItems().addAll(
			this.manualSource,
			this.invertSource,
			this.cliffCurveSource,
			this.convolveSource,
			this.normalizeSource,
			this.derivedSource,
			this.proceduralSource
		);
		this.choiceBox.getSelectionModel().select(this.manualSource);
	}

	public LayerSources(Layer newLayer, LayerSources from) {
		this(newLayer);
		this.    manualSource.copyFrom(from.    manualSource);
		this.    invertSource.copyFrom(from.    invertSource);
		this.cliffCurveSource.copyFrom(from.cliffCurveSource);
		this.  convolveSource.copyFrom(from.  convolveSource);
		this. normalizeSource.copyFrom(from. normalizeSource);
		this.   derivedSource.copyFrom(from.   derivedSource);
		this.proceduralSource.copyFrom(from.proceduralSource);
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
		this.    manualSource.init(fromSave);
		this.    invertSource.init(fromSave);
		this.cliffCurveSource.init(fromSave);
		this.  convolveSource.init(fromSave);
		this. normalizeSource.init(fromSave);
		this.   derivedSource.init(fromSave);
		this.proceduralSource.init(fromSave);
		this.getCurrentSource().redraw(false);
	}

	public void invalidateStructure() {
		this.getCurrentSource().invalidateStructure();
	}

	public LayerSource getCurrentSource() {
		return this.currentSourceProperty.getValue();
	}
}