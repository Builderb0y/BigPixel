package builderb0y.notgimp;

import java.util.stream.IntStream;

import javafx.beans.binding.When;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;

import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.sources.LayerSource;
import builderb0y.notgimp.sources.LayerSources;
import builderb0y.notgimp.sources.dependencies.LayerDependencies;
import builderb0y.notgimp.sources.dependencies.inputs.LayerSourceInput.VaryingLayerSourceInput;

import static builderb0y.notgimp.HDRImage.*;

public class LayerNode implements LayerPosition, VaryingLayerSourceInput {

	public static final int
		GRID_WIDTH = 256,
		GRID_HEIGHT = 64,
		PREVIEW_WIDTH = 192,
		PREVIEW_HEIGHT = 48;
	public static final PseudoClass
		SELECTED = PseudoClass.getPseudoClass("selected");

	public LayerGraph
		graph;
	public HDRImage
		image;
	public LayerDragHandler
		dragHandler = new LayerDragHandler(this);
	public LayerSources
		sources = new LayerSources(this);
	public RadioButton
		showing = new RadioButton();
	public ImageView
		thumbnailView = new ImageView();
	public Label
		displayName = new Label();
	public HBox
		innerPreview = new HBox(this.showing, this.displayName);
	public BorderPane
		outerPreview = new BorderPane(this.innerPreview);
	public TextField
		nameEditor = new TextField();
	public HBox
		sourceAndNameEditor = new HBox(this.sources.sourceTypeBox, this.nameEditor);
	public BorderPane
		configPane = new BorderPane();
	public boolean
		redrawRequested = true;
	public SimpleObjectProperty<Throwable>
		redrawException = new SimpleObjectProperty<>(this, "redrawException");

	public JsonMap save() {
		return (
			new JsonMap(8)
			.with("name", this.getDisplayName())
			.with("gridX", this.getGridX())
			.with("gridY", this.getGridY())
			.with("width", this.image.width)
			.with("height", this.image.height)
			.with("sources", this.sources.save())
		);
	}

	public void load(JsonMap saveData) {
		this.sources.load(saveData.getMap("sources"));
	}

	public LayerNode(LayerGraph graph, JsonMap saveData) {
		this(
			graph,
			saveData.getInt("gridX"),
			saveData.getInt("gridY"),
			saveData.getInt("width"),
			saveData.getInt("height"),
			saveData.getString("name")
		);
	}

	public LayerNode(LayerGraph graph, int x, int y, int width, int height, String name) {
		this.graph = graph;
		this.image = new HDRImage(width, height);
		super();
		this.dragHandler.init();
		this.displayName.setText(name);
		this.nameEditor.setText(name);
		this.showing.setFont(new Font(16));
		this.showing.setGraphic(this.thumbnailView);
		this.showing.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		this.innerPreview.setAlignment(Pos.CENTER_LEFT);
		this.innerPreview.setSpacing(8.0D);
		this.innerPreview.getStyleClass().add("layer-preview");
		this.outerPreview.setMinWidth(PREVIEW_WIDTH);
		this.outerPreview.setMaxWidth(PREVIEW_WIDTH);
		this.outerPreview.setMinHeight(PREVIEW_HEIGHT);
		this.outerPreview.setMaxHeight(PREVIEW_HEIGHT);
		this.outerPreview.getStyleClass().add("popout-borders");
		this.showing.setToggleGroup(graph.visibleLayer);
		this.showing.setUserData(this);
		this.dragHandler.gridX.set(x);
		this.dragHandler.gridY.set(y);
		this.thumbnailView.setPreserveRatio(true);
		this.thumbnailView.setImage(new WritableImage(width, height));
		this.onThumbnailSizeChanged();
		this.displayName.styleProperty().bind(
			this.redrawException.map((Throwable _) -> "-fx-text-fill: #FF3F3F;")
		);
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(
			this.redrawException.map(Throwable::getLocalizedMessage)
		);
		this.displayName.tooltipProperty().bind(
			new When(tooltip.textProperty().isNotEmpty())
			.then(tooltip)
			.otherwise((Tooltip)(null))
		);
		this.outerPreview.setOnMouseClicked((MouseEvent _) -> {
			this.graph.selectedLayer.set(this);
		});
		this.nameEditor.setOnAction((ActionEvent _) -> {
			this.setDisplayName(this.nameEditor.getText());
			this.nameEditor.setText(this.displayName.getText());
		});
		this.nameEditor.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				this.nameEditor.setText(this.getDisplayName());
			}
		});
		this.configPane.setTop(this.sourceAndNameEditor);
		this.configPane.centerProperty().bind(this.sources.currentSourceProperty.map(LayerSource::getConfigNode));
	}

	public FloatVector getPixelWrapped(IntVector coord) {
		return this.getPixelWrapped(coord.lane(0), coord.lane(1));
	}

	public FloatVector getPixelWrapped(int x, int y) {
		return this.getColor(
			Math.floorMod(x, this.image.width),
			this.image.height + ~Math.floorMod(y, this.image.height)
		);
	}

	@Override
	public LayerNode getBackingLayer() {
		return this;
	}

	public void requestRedraw() {
		this.redrawRequested = true;
		this.graph.requestRedraw();
	}

	public void redrawImmediately() {
		try {
			this.sources.getCurrentSource().redrawImmediately();
			this.redrawThumbnail();
			this.redrawException.set(null);
		}
		catch (Throwable throwable) {
			while (throwable.getCause() != null) {
				throwable = throwable.getCause();
			}
			this.redrawException.set(throwable);
		}
	}

	public LayerDependencies getDependencies() {
		return this.sources.getCurrentSource().getDependencies();
	}

	public String getDisplayName() {
		return this.displayName.getText();
	}

	public void setDisplayName(String displayName) {
		this.graph.layersByName.remove(this.getDisplayName());
		displayName = this.graph.adjustName(displayName);
		this.setDisplayNameDirectly(displayName);
		this.graph.layersByName.put(displayName, this);
	}

	public void setDisplayNameDirectly(String displayName) {
		this.displayName.setText(displayName);
		this.nameEditor.setText(displayName);
	}

	public ImageView duplicateThumbnail() {
		ImageView view = new ImageView();
		view.setPreserveRatio(true);
		view.imageProperty().bind(this.thumbnailView.imageProperty());
		view.fitWidthProperty().bind(this.thumbnailView.fitWidthProperty());
		view.fitHeightProperty().bind(this.thumbnailView.fitHeightProperty());
		return view;
	}

	public void onThumbnailSizeChanged() {
		if (this.image.width > this.image.height) {
			this.thumbnailView.setFitWidth(32.0D);
			this.thumbnailView.setFitHeight(0.0D);
		}
		else {
			this.thumbnailView.setFitWidth(0.0D);
			this.thumbnailView.setFitHeight(32.0D);
		}
	}

	public void redrawThumbnail() {
		WritableImage fxImage = (WritableImage)(this.thumbnailView.getImage());
		if (fxImage.getWidth() != this.image.width || fxImage.getHeight() != this.image.height) {
			this.thumbnailView.setImage(fxImage = new WritableImage(this.image.width, this.image.height));
			this.onThumbnailSizeChanged();
		}
		PixelWriter writer = fxImage.getPixelWriter();
		if (writer.getPixelFormat() != PixelFormat.getByteBgraPreInstance()) {
			throw new IllegalStateException("Pixel format changed");
		}
		int width = this.image.width;
		int height = this.image.height;
		byte[] pixels = new byte[width * height * 4];
		IntStream.range(0, height).parallel().forEach((int y) -> {
			for (int x = 0; x < width; x++) {
				int baseIndex = this.image.baseIndex(x, y);
				float red     = this.image.pixels[baseIndex |   RED_OFFSET];
				float green   = this.image.pixels[baseIndex | GREEN_OFFSET];
				float blue    = this.image.pixels[baseIndex |  BLUE_OFFSET];
				float alpha   = this.image.pixels[baseIndex | ALPHA_OFFSET];
				float clampedAlpha = Util.clampF(alpha);
				pixels[baseIndex    ] = Util.clampB(blue  * clampedAlpha);
				pixels[baseIndex | 1] = Util.clampB(green * clampedAlpha);
				pixels[baseIndex | 2] = Util.clampB(red   * clampedAlpha);
				pixels[baseIndex | 3] = Util.clampB(               alpha);
			}
		});
		writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), pixels, 0, width << 2);
	}

	public Pane getPreviewNode() {
		return this.outerPreview;
	}

	public Parent getColorNode() {
		return this.innerPreview;
	}

	public Pane getConfigPane() {
		return this.configPane;
	}

	@Override
	public String toString() {
		return this.getDisplayName();
	}

	@Override
	public int getGridX() {
		return this.dragHandler.gridX.get();
	}

	@Override
	public int getGridY() {
		return this.dragHandler.gridY.get();
	}

	public void setGridX(int gridX) {
		this.dragHandler.gridX.set(gridX);
	}

	public void setGridY(int gridY) {
		this.dragHandler.gridY.set(gridY);
	}
}