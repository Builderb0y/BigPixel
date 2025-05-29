package builderb0y.notgimp.sources;

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

	public        ManualLayerSource        manualSource = new        ManualLayerSource(this);

	public         AlphaLayerSource         alphaSource = new         AlphaLayerSource(this);
	public           AddLayerSource           addSource = new           AddLayerSource(this);
	public       AverageLayerSource       averageSource = new       AverageLayerSource(this);
	public      MultiplyLayerSource      multiplySource = new      MultiplyLayerSource(this);
	public        ScreenLayerSource        screenSource = new        ScreenLayerSource(this);
	public           MinLayerSource           minSource = new           MinLayerSource(this);
	public           MaxLayerSource           maxSource = new           MaxLayerSource(this);

	public        InvertLayerSource        invertSource = new        InvertLayerSource(this);
	public     NormalizeLayerSource     normalizeSource = new     NormalizeLayerSource(this);
	public         ClampLayerSource         clampSource = new         ClampLayerSource(this);
	public GradientRemapLayerSource gradientRemapSource = new GradientRemapLayerSource(this);
	public   ColorMatrixLayerSource   colorMatrixSource = new   ColorMatrixLayerSource(this);
	public    CliffCurveLayerSource    cliffCurveSource = new    CliffCurveLayerSource(this);
	public      ConvolveLayerSource      convolveSource = new      ConvolveLayerSource(this);
	public        KMeansLayerSource        kMeansSource = new        KMeansLayerSource(this);
	public           WFCLayerSource           wfcSource = new           WFCLayerSource(this);

	public       DerivedLayerSource       derivedSource = new       DerivedLayerSource(this);
	public    ProceduralLayerSource    proceduralSource = new    ProceduralLayerSource(this);

	public ReadOnlyObjectProperty<LayerSource>
		currentSourceProperty = this.choiceBox.getSelectionModel().selectedItemProperty();
	public ObservableObjectValue<@Nullable Tool<?>>
		toolWithoutColorPicker = (
			new When(this.currentSourceProperty.isEqualTo(this.manualSource))
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
			case "manual"         -> this.       manualSource;

			case "alpha"          -> this.        alphaSource;
			case "add"            -> this.          addSource;
			case "avg"            -> this.      averageSource;
			case "mul"            -> this.     multiplySource;
			case "screen"         -> this.       screenSource;
			case "min"            -> this.          minSource;
			case "max"            -> this.          maxSource;

			case "invert"         -> this.       invertSource;
			case "normalize"      -> this.    normalizeSource;
			case "clamp"          -> this.        clampSource;
			case "gradient_remap" -> this.gradientRemapSource;
			case "color_matrix"   -> this.  colorMatrixSource;
			case "cliff"          -> this.   cliffCurveSource;
			case "convolve"       -> this.     convolveSource;
			case "kmeans"         -> this.       kMeansSource;
			case "wfc"            -> this.          wfcSource;

			case "derived"        -> this.      derivedSource;
			case "procedural"     -> this.   proceduralSource;

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

			this.alphaSource,
			this.addSource,
			this.averageSource,
			this.multiplySource,
			this.screenSource,
			this.minSource,
			this.maxSource,

			this.invertSource,
			this.normalizeSource,
			this.clampSource,
			this.gradientRemapSource,
			this.colorMatrixSource,
			this.cliffCurveSource,
			this.convolveSource,
			this.kMeansSource,
			this.wfcSource,

			this.derivedSource,
			this.proceduralSource
		);
		this.choiceBox.getSelectionModel().select(this.manualSource);
	}

	public LayerSources(Layer newLayer, LayerSources from) {
		this(newLayer);
		this.       manualSource.copyFrom(from.       manualSource);

		this.        alphaSource.copyFrom(from.        alphaSource);
		this.          addSource.copyFrom(from.          addSource);
		this.      averageSource.copyFrom(from.      averageSource);
		this.     multiplySource.copyFrom(from.     multiplySource);
		this.       screenSource.copyFrom(from.       screenSource);
		this.          minSource.copyFrom(from.          minSource);
		this.          maxSource.copyFrom(from.          maxSource);

		this.       invertSource.copyFrom(from.       invertSource);
		this.    normalizeSource.copyFrom(from.    normalizeSource);
		this.        clampSource.copyFrom(from.        clampSource);
		this.gradientRemapSource.copyFrom(from.gradientRemapSource);
		this.  colorMatrixSource.copyFrom(from.  colorMatrixSource);
		this.   cliffCurveSource.copyFrom(from.   cliffCurveSource);
		this.     convolveSource.copyFrom(from.     convolveSource);
		this.       kMeansSource.copyFrom(from.       kMeansSource);
		this.          wfcSource.copyFrom(from.          wfcSource);

		this.      derivedSource.copyFrom(from.      derivedSource);
		this.   proceduralSource.copyFrom(from.   proceduralSource);
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
		this.       manualSource.init(fromSave);

		this.        alphaSource.init(fromSave);
		this.          addSource.init(fromSave);
		this.      averageSource.init(fromSave);
		this.     multiplySource.init(fromSave);
		this.       screenSource.init(fromSave);
		this.          minSource.init(fromSave);
		this.          maxSource.init(fromSave);

		this.       invertSource.init(fromSave);
		this.    normalizeSource.init(fromSave);
		this.        clampSource.init(fromSave);
		this.gradientRemapSource.init(fromSave);
		this.  colorMatrixSource.init(fromSave);
		this.   cliffCurveSource.init(fromSave);
		this.     convolveSource.init(fromSave);
		this.       kMeansSource.init(fromSave);
		this.          wfcSource.init(fromSave);

		this.      derivedSource.init(fromSave);
		this.   proceduralSource.init(fromSave);
	}

	public void invalidateStructure() {
		this.getCurrentSource().invalidateStructure();
	}

	public LayerSource getCurrentSource() {
		return this.currentSourceProperty.getValue();
	}
}