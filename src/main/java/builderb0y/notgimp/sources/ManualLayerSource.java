package builderb0y.notgimp.sources;

import java.util.Collection;
import java.util.Collections;

import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.tools.*;

public class ManualLayerSource extends LayerSource {

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
		JsonMap map = new JsonMap();
		map.add("type", "manual");
		map.add("image", this.toollessImage.save());
		return map;
	}

	@Override
	public void load(JsonMap map) {
		this.toollessImage = new HDRImage(map.getMap("image"));
	}

	public ManualLayerSource(LayerSources sources) {
		super(sources, "Manual");
		this.toolSelection.add(this.rectButton, 0, 0);
		this.toolSelection.add(this.lineButton, 1, 0);
		this.toolSelection.add(this.freehandButton, 2, 0);
		this.toolSelection.add(this.moveButton, 0, 1);
		this.toolSelection.add(this.bucketButton, 1, 1);
		this.toolSelection.add(this.toolConfig, 0, 2, 3, 1);
		this.toolConfig.centerProperty().bind(this.toolWithoutColorPicker.map(Tool::getConfiguration));
	}

	public void init(boolean fromSave) {
		if (this.toollessImage == null) {
			this.toollessImage = new HDRImage(this.sources.layer.image);
		}
	}

	public Button button(Tool<?> tool) {
		Button button = new Button();
		button.setGraphic(new ImageView(tool.type.icon()));
		button.setOnAction((ActionEvent event) -> this.toolWithoutColorPicker.set(tool));
		return button;
	}

	public void beginUsingTool() {
		System.arraycopy(this.sources.layer.image.pixels, 0, this.toollessImage.pixels, 0, this.toollessImage.pixels.length);
	}

	public void finishUsingTool() {
		System.arraycopy(this.sources.layer.image.pixels, 0, this.toollessImage.pixels, 0, this.toollessImage.pixels.length);
	}

	public void cancelToolAction() {
		this.requestRedraw();
	}

	public void copyFrom(ManualLayerSource that) {
		if (this.toollessImage == null) {
			this.toollessImage = new HDRImage(this.sources.layer.image);
		}
		System.arraycopy(that.toollessImage.pixels, 0, this.toollessImage.pixels, 0, that.toollessImage.pixels.length);
	}

	@Override
	public void onDeselected() {}

	@Override
	public void onSelected() {}

	@Override
	public void invalidateStructure() {}

	@Override
	public Collection<Layer> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public boolean isAnimated() {
		return false;
	}

	@Override
	public Node getRootNode() {
		return this.toolSelection;
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage source = this.toollessImage;
		HDRImage destination = this.sources.layer.image;
		if (source.width != destination.width || source.height != destination.height) {
			source.resize(destination.width, destination.height, true);
		}
		System.arraycopy(source.pixels, 0, destination.pixels, 0, destination.pixels.length);
		Tool<?> tool = this.toolWithoutColorPicker.get();
		if (tool != null) tool.redraw();
	}
}