package builderb0y.bigpixel;

import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.LayerSource.RedrawException;
import builderb0y.bigpixel.sources.LayerSources;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView;
import builderb0y.bigpixel.views.LayerViews;

public class LayerNode implements LayerPosition, VaryingSamplerProvider {

	public static final int
		GRID_WIDTH = 256,
		GRID_HEIGHT = 64,
		PREVIEW_WIDTH = 192,
		PREVIEW_HEIGHT = 48;
	public static final PseudoClass
		SELECTED = PseudoClass.getPseudoClass("selected");

	public LayerGraph
		graph;
	public HDRAnimation
		animation;
	public Thumbnail
		thumbnail;
	public LayerDragHandler
		dragHandler = new LayerDragHandler(this);
	public LayerSources
		sources = new LayerSources(this);
	public LayerViews
		views = new LayerViews(this);
	public BorderPane
		sourceConfigPane = new BorderPane(),
		viewConfigPane   = new BorderPane();
	public TabPane
		configPane = new TabPane(
		new Tab("Source", this.sourceConfigPane),
		new Tab("View", this.viewConfigPane)
	);
	public RadioButton
		showing = new RadioButton();
	public ImageView
		thumbnailView;
	public Label
		displayName = new Label();
	public HBox
		innerPreview = new HBox(this.showing, this.displayName);
	public BorderPane
		outerPreview = new BorderPane(this.innerPreview);
	public TextField
		nameEditor = new TextField();
	public HBox
		sourceAndNameEditor = new HBox(this.sources.rootButton, this.nameEditor);
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
			.with("width", this.imageWidth())
			.with("height", this.imageHeight())
			.with("sources", this.sources.save())
			.with("views", this.views.save())
		);
	}

	public void load(JsonMap saveData) {
		this.sources.load(saveData.getMap("sources"));
		this.views.load(saveData.getMap("views"));
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

	public LayerNode(LayerGraph graph, int gridX, int gridY, int width, int height, String name) {
		this.graph = graph;
		super();
		this.animation = new HDRAnimation(this, width, height);
		this.thumbnail = new Thumbnail(graph.openImage.animationSource, this.animation, 32);
		this.thumbnailView = this.thumbnail.createView();
		this.sources.init();
		this.views.init();
		this.dragHandler.init();
		this.configPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
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
		this.setGridPos(gridX, gridY, false);
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
		this.sourceConfigPane.setTop(this.sourceAndNameEditor);
		this.sourceConfigPane.centerProperty().bind(this.sources.selectedValue.map(LayerSource::getConfigNode));
		this.viewConfigPane.setTop(this.views.rootButton);
		this.viewConfigPane.centerProperty().bind(this.views.selectedValue.map(LayerView::getRootConfigPane));
	}

	public HDRImage getFrame() {
		return this.animation.getFrame();
	}

	public HDRImage getOnlyFrame() {
		if (this.animation.isAnimated()) {
			throw new IllegalStateException("Animated");
		}
		else {
			return this.getFrame();
		}
	}

	public HDRImage getFrame(int index) {
		return this.animation.getFrame(index);
	}

	public int getFrameCount() {
		return this.animation.getFrameCount();
	}

	public int imageWidth() {
		return this.animation.width();
	}

	public int imageHeight() {
		return this.animation.height();
	}

	@Override
	public LayerNode getBackingLayer() {
		return this;
	}

	public VaryingSampler createInvertedInput(int frame) {
		return VaryingSampler.inverted(this, this.getFrame(frame));
	}

	public void requestRedraw() {
		this.redrawRequested = true;
		this.graph.redrawThread.wakeup();
	}

	public void redrawOffThread() {
		try {
			LayerSource source = this.sources.currentSource();
			int frameCount = this.animation.getFrameCount();
			if (frameCount == 1) {
				source.doRedraw(0);
			}
			else {
				IntStream.range(0, frameCount).parallel().forEach((int frame) -> {
					try {
						source.doRedraw(frame);
					}
					catch (RedrawException exception) {
						throw Util.rethrow(exception);
					}
				});
			}
			Platform.runLater(() -> this.redrawException.set(null));
		}
		catch (RedrawException exception) {
			Platform.runLater(() -> this.redrawException.set(exception));
		}
		catch (Throwable throwable) {
			while (throwable.getCause() != null) {
				throwable = throwable.getCause();
			}
			Throwable throwable_ = throwable;
			Platform.runLater(() -> this.redrawException.set(throwable_));
		}
	}

	public LayerDependencies getDependencies() {
		return this.sources.selectedValue.get().getDependencies();
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

	public Pane getPreviewNode() {
		return this.outerPreview;
	}

	public Parent getColorNode() {
		return this.innerPreview;
	}

	public TabPane getConfigPane() {
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

	public void setGridPos(int gridX, int gridY, boolean animate) {
		this.dragHandler.setGridPos(gridX, gridY, animate);
	}

	public void setGridX(int gridX, boolean animate) {
		this.dragHandler.setGridX(gridX, animate);
	}

	public void setGridY(int gridY, boolean animate) {
		this.dragHandler.setGridY(gridY, animate);
	}
}