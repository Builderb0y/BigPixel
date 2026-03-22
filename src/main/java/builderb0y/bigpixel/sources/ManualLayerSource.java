package builderb0y.bigpixel.sources;

import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.History;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.NoDependencies;
import builderb0y.bigpixel.tools.*;
import builderb0y.bigpixel.util.Util;

public class ManualLayerSource extends LayerSource {

	public History history = new History(this);
	public HDRImage toollessImage;
	public FreehandTool   freehandTool = new  FreehandTool(this);
	public LineTool           lineTool = new      LineTool(this);
	public RectangleTool rectangleTool = new RectangleTool(this);
	public MoveTool           moveTool = new      MoveTool(this);
	public BucketTool       bucketTool = new    BucketTool(this);
	public GridPane
		toolSelection = new GridPane();
	public Button
		freehandButton = this.button(this.freehandTool),
		lineButton     = this.button(this.lineTool),
		rectButton     = this.button(this.rectangleTool),
		moveButton     = this.button(this.moveTool),
		bucketButton   = this.button(this.bucketTool);
	public BorderPane
		toolConfig = new BorderPane();
	public SimpleObjectProperty<@Nullable Tool<?>>
		toolWithoutColorPicker = new SimpleObjectProperty<>();

	@Override
	public JsonMap save() {
		return super.save().with("image", this.getToollessImage().save());
	}

	@Override
	public void load(JsonMap map) {
		this.getToollessImage().copyFrom(map.getMap("image"));
	}

	public ManualLayerSource(LayerSources sources) {
		super(LayerSourceType.MANUAL, sources);
		this.toolSelection.add(this.rectButton, 0, 0);
		this.toolSelection.add(this.lineButton, 1, 0);
		this.toolSelection.add(this.freehandButton, 2, 0);
		this.toolSelection.add(this.moveButton, 0, 1);
		this.toolSelection.add(this.bucketButton, 1, 1);
		this.toolSelection.add(this.toolConfig, 0, 2, 3, 1);
		this.toolConfig.centerProperty().bind(this.toolWithoutColorPicker.map(Tool::getConfiguration));
		new com.sun.javafx.scene.TreeShowingProperty(this.toolSelection).addListener(Util.change((Boolean visible) -> {
			if (!visible && this.toolWithoutColorPicker.get() != null) {
				this.toolWithoutColorPicker.set(null);
			}
		}));
		this.toolWithoutColorPicker.addListener(Util.change((Tool<?> oldTool, Tool<?> newTool) -> {
			if (oldTool != null) oldTool.enter();
		}));
	}

	@Override
	public Node getConfigPane() {
		return this.toolSelection;
	}

	public Button button(Tool<?> tool) {
		Button button = new Button();
		button.setGraphic(new ImageView(tool.type.icon()));
		button.setOnAction((ActionEvent _) -> this.toolWithoutColorPicker.set(tool));
		return button;
	}

	public void beginUsingTool() {
		System.arraycopy(this.sources.layer.getOnlyFrame().pixels, 0, this.getToollessImage().pixels, 0, this.getToollessImage().pixels.length);
	}

	public void finishUsingTool() {
		System.arraycopy(this.sources.layer.getOnlyFrame().pixels, 0, this.getToollessImage().pixels, 0, this.getToollessImage().pixels.length);
	}

	public void cancelToolAction() {
		this.redrawLater();
	}

	public HDRImage getToollessImage() {
		if (this.toollessImage == null) {
			this.toollessImage = new HDRImage(this.sources.layer.getOnlyFrame());
		}
		return this.toollessImage;
	}

	@Override
	public void copyFrom(LayerSource source) {
		super.copyFrom(source);
		ManualLayerSource that = (ManualLayerSource)(source);
		System.arraycopy(that.getToollessImage().pixels, 0, this.getToollessImage().pixels, 0, that.getToollessImage().pixels.length);
	}

	@Override
	public LayerDependencies getDependencies() {
		return NoDependencies.INSTANCE;
	}

	@Override
	public int computeMaxProgress(int width, int height) {
		return 0;
	}

	@Override
	public void doRedraw(int frame) throws RedrawException {
		HDRImage source = this.getToollessImage();
		HDRImage destination = this.sources.layer.getOnlyFrame();
		source.checkSize(destination.width, destination.height, true);
		System.arraycopy(source.pixels, 0, destination.pixels, 0, destination.pixels.length);
		Tool<?> tool = this.toolWithoutColorPicker.get();
		if (tool != null) tool.redraw();
		this.clampImage(destination);
	}
}