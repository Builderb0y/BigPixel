package builderb0y.bigpixel;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
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
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import org.controlsfx.control.PopOver;

import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.LayerSource;
import builderb0y.bigpixel.sources.LayerSource.RedrawException;
import builderb0y.bigpixel.sources.LayerSources;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.Sampler.VaryingSampler;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.RateLimiter;
import builderb0y.bigpixel.util.RateLimiter.AsyncPeriodicRateLimiter;
import builderb0y.bigpixel.util.Util;
import builderb0y.bigpixel.views.LayerView;
import builderb0y.bigpixel.views.LayerViews;

public class LayerNode implements LayerPosition, VaryingSamplerProvider {

	public static final int
		GRID_WIDTH = 192,
		GRID_HEIGHT = 160,
		PREVIEW_WIDTH = 128,
		PREVIEW_HEIGHT = 128;
	public static final PseudoClass
		SELECTED = PseudoClass.getPseudoClass("selected");

	public LayerGraph
		graph;
	public HDRAnimation
		animation;
	public Thumbnail
		smallThumbnail,
		bigThumbnail;
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
	public Rectangle
		progressIndicator = new Rectangle();
	public StackPane
		progressAndLabel = new StackPane(this.progressIndicator, this.showing);
	public RateLimiter
		progressUpdater = new AsyncPeriodicRateLimiter(50L, () -> {
			double progress = this.sources.currentSource().getProgress();
			this.progressIndicator.setWidth(progress * this.progressAndLabel.getWidth());
		});
	public ObservableValue<InputBinding.SaveData>
		infoProperty = this.showing.textProperty().map(InputBinding.VaryingSaveData::new);
	public ImageView
		smallThumbnailView,
		bigThumbnailView;
	public BorderPane
		innerPreview = new BorderPane(),
		outerPreview = new BorderPane(this.innerPreview);
	public TextField
		nameEditor = new TextField();
	public Button
		extraSettingsButton = new Button(" ⋮  ");
	{
		this.extraSettingsButton.setFont(new Font(20.0D));
	}
	public PopOver
		extraSettingsPopOver = new PopOver();
	public BorderPane
		sourceAndNameEditor = new BorderPane();
	{
		this.sourceAndNameEditor.setLeft(new HBox(this.sources.rootButton, this.nameEditor));
		this.sourceAndNameEditor.setRight(this.extraSettingsButton);
	}
	public boolean
		redrawRequested = true;
	public SimpleObjectProperty<String>
		redrawException = new SimpleObjectProperty<>(this, "redrawException");

	public JsonMap save() {
		return (
			new JsonMap(8)
			.with("name",    this.getDisplayName())
			.with("gridX",   this.getGridX())
			.with("gridY",   this.getGridY())
			.with("width",   this.imageWidth())
			.with("height",  this.imageHeight())
			.with("sources", this.sources.save())
			.with("views",   this.views.save())
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
		StackPane.setAlignment(this.showing, Pos.CENTER_LEFT);
		StackPane.setAlignment(this.progressIndicator, Pos.TOP_LEFT);
		this.progressIndicator.getStyleClass().add("layer-progress");
		this.progressIndicator.heightProperty().bind(this.progressAndLabel.heightProperty());
		this.animation          = new HDRAnimation(this, width, height);
		this.smallThumbnail     = new Thumbnail(this, 32);
		this.bigThumbnail       = new Thumbnail(this, 80);
		this.smallThumbnailView = this.smallThumbnail.createView();
		this.bigThumbnailView   = this.bigThumbnail.createView();
		this.innerPreview.setCenter(this.bigThumbnailView);
		this.innerPreview.setTop(this.progressAndLabel);
		this.sources.init();
		this.views.init();
		this.dragHandler.init();
		this.configPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		this.showing.setText(name);
		this.nameEditor.setText(name);
		this.innerPreview.getStyleClass().add("selectable");
		this.outerPreview.setMinWidth(PREVIEW_WIDTH);
		this.outerPreview.setMaxWidth(PREVIEW_WIDTH);
		this.outerPreview.setMinHeight(PREVIEW_HEIGHT);
		this.outerPreview.setMaxHeight(PREVIEW_HEIGHT);
		this.outerPreview.getStyleClass().add("popout-borders");
		this.showing.setToggleGroup(graph.visibleLayer);
		this.showing.setUserData(this);
		this.setGridPos(gridX, gridY, false);
		this.showing.styleProperty().bind(
			this.redrawException.map((String _) -> "-fx-text-fill: #FF3F3F;")
		);
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(this.redrawException);
		this.showing.tooltipProperty().bind(
			new When(tooltip.textProperty().isNotEmpty())
			.then(tooltip)
			.otherwise((Tooltip)(null))
		);
		this.outerPreview.setOnMouseClicked((MouseEvent _) -> {
			this.graph.selectedLayer.set(this);
		});
		this.nameEditor.setOnAction((ActionEvent _) -> {
			this.setDisplayName(this.nameEditor.getText());
			this.nameEditor.setText(this.showing.getText());
		});
		this.nameEditor.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				this.nameEditor.setText(this.getDisplayName());
			}
		});
		this.extraSettingsPopOver.getRoot().getStylesheets().clear();
		this.extraSettingsPopOver.getRoot().setPadding(new Insets(10.0D));
		this.extraSettingsPopOver.setDetachable(false);
		this.extraSettingsButton.getStyleClass().remove("button");
		this.extraSettingsButton.getStyleClass().add("phantom-button");
		this.extraSettingsButton.setOnAction((ActionEvent _) -> {
			if (!this.extraSettingsPopOver.isShowing()) {
				this.extraSettingsPopOver.setContentNode(this.sources.currentSource().extraSettings);
				this.extraSettingsPopOver.show(this.extraSettingsButton);
			}
		});
		this.sourceConfigPane.setTop(this.sourceAndNameEditor);
		this.sourceConfigPane.centerProperty().bind(this.sources.selectedValue.map(LayerSource::getConfigPane));
		this.viewConfigPane.setTop(this.views.rootButton);
		this.viewConfigPane.centerProperty().bind(this.views.selectedValue.map(LayerView::getRootConfigPane));
	}

	public void progressChangedAsync() {
		this.progressUpdater.run();
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

	@Override
	public ObservableValue<InputBinding.SaveData> serializedForm() {
		return this.infoProperty;
	}

	public void requestRedraw() {
		this.redrawRequested = true;
		this.graph.redrawThread.wakeup();
	}

	public void redrawOffThread() {
		try {
			LayerSource source = this.sources.currentSource();
			int frameCount = this.animation.getFrameCount();
			Set<String> messages = new HashSet<>(2);
			if (frameCount == 1) {
				try {
					source.doRedraw(0);
				}
				catch (RedrawException exception) {
					messages.add(exception.getLocalizedMessage());
				}
			}
			else {
				IntStream.range(0, frameCount).parallel().forEach((int frame) -> {
					try {
						source.doRedraw(frame);
					}
					catch (RedrawException exception) {
						messages.add(exception.getLocalizedMessage());
					}
				});
			}
			Platform.runLater(() -> this.redrawException.set(messages.isEmpty() ? null : messages.stream().collect(Collectors.joining("; "))));
		}
		catch (Throwable throwable) {
			throwable.printStackTrace();
			while (throwable.getCause() != null) {
				throwable = throwable.getCause();
			}
			String message = throwable.getLocalizedMessage();
			Platform.runLater(() -> this.redrawException.set(message));
		}
	}

	public void afterRedraw() {
		if (this.graph.getVisibleLayer() == this) {
			this.graph.openImage.imageDisplay.displayRenderer.invalidateAll();
		}
		this.bigThumbnail.invalidateAll();
		this.smallThumbnail.invalidateAll();
	}

	public LayerDependencies getDependencies() {
		return this.sources.selectedValue.get().getDependencies();
	}

	public String getDisplayName() {
		return this.showing.getText();
	}

	public void setDisplayName(String displayName) {
		this.graph.layersByName.remove(this.getDisplayName());
		displayName = this.graph.adjustName(displayName);
		this.setDisplayNameDirectly(displayName);
		this.graph.layersByName.put(displayName, this);
	}

	public void setDisplayNameDirectly(String displayName) {
		this.showing.setText(displayName);
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