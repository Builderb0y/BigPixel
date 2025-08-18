package builderb0y.notgimp;

import java.io.File;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.tools.ColorPickerTool;
import builderb0y.notgimp.tools.ColorPickerTool.ColorPickerCallback;
import builderb0y.notgimp.tools.SourcelessTool;
import builderb0y.notgimp.tools.Tool;

public class OpenImage {

	public MainWindow
		mainWindow;
	public SimpleObjectProperty<File>
		file = new SimpleObjectProperty<>();
	public LayerGraph
		layerGraph = new LayerGraph(this);
	public BorderPane
		layerConfigPane = new BorderPane();
	public SplitPane
		imageAndLayerConfig = new SplitPane();
	public ZoomableImage
		imageDisplay = new ZoomableImage(this);
	public SimpleBooleanProperty
		wrap = new SimpleBooleanProperty();
	public AnimationSource
		animationSource = new AnimationSource(this);
	public ColorPickerTool
		colorPickerTool = new ColorPickerTool(this);
	public SimpleBooleanProperty
		usingColorPickerByControl = new SimpleBooleanProperty(),
		usingColorPickerByButton = new SimpleBooleanProperty();
	public BooleanBinding
		usingColorPicker = this.usingColorPickerByButton.or(this.usingColorPickerByControl);
	public ObjectBinding<@Nullable Tool<?>>
		toolWithoutColorPicker = (ObjectBinding<Tool<?>>)(
			this.layerGraph.selectedLayer.flatMap(
				(LayerNode layer) -> layer.sources.toolWithoutColorPicker
			)
		);
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ObservableObjectValue<@Nullable SourcelessTool<?>>
		toolWithColorPicker = (
		new When(this.usingColorPicker)
		.<SourcelessTool<?>>then(this.colorPickerTool)
		.otherwise((ObjectBinding)(this.toolWithoutColorPicker))
	);
	public ObservableValue<Cursor>
		cursorProperty = this.toolWithColorPicker.map((SourcelessTool<?> tool) -> tool.type.cursor());
	public ObservableValue<String>
		title = this.file.map(File::getName);

	public JsonMap save() {
		JsonMap root = new JsonMap();
		root.add("version", SaveVersions.CURRENT);
		root.put("layer_graph", this.layerGraph.save());
		root.add("animation", this.animationSource.save());
		root.add("wrap", this.wrap.getValue());
		return root;
	}

	public void load(JsonMap map) {
		this.layerGraph.load(map.getMap("layer_graph"));
		this.animationSource.load(map.getMap("animation"));
		this.wrap.set(map.getBoolean("wrap"));
	}

	public OpenImage(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.layerConfigPane.centerProperty().bind(this.layerGraph.selectedLayer.map(LayerNode::getConfigPane));
		this.imageAndLayerConfig.getItems().addAll(this.imageDisplay.displayWithF3, this.layerConfigPane);
		this.imageAndLayerConfig.setDividerPosition(0, 2.0D / 3.0D);
	}

	public void init() {
		this.imageDisplay.init();
	}

	public Parent getMainNode() {
		return this.imageAndLayerConfig;
	}

	public void pickColor(ColorPickerCallback callback) {
		this.colorPickerTool.work = callback;
		this.usingColorPickerByButton.set(true);
	}

	public void stopPickingColor() {
		this.colorPickerTool.work = null;
		this.usingColorPickerByButton.set(false);
	}

	public void controlPressed(ColorPickerCallback callback) {
		this.colorPickerTool.work = callback;
		this.usingColorPickerByControl.set(true);
	}

	public void controlReleased() {
		this.colorPickerTool.work = null;
		this.usingColorPickerByControl.set(false);
	}
}