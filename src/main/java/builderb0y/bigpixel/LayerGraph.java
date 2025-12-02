package builderb0y.bigpixel;

import java.util.*;
import java.util.stream.Stream;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.json.JsonArray;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.json.JsonString;
import builderb0y.bigpixel.json.JsonValue;
import builderb0y.bigpixel.sources.dependencies.CurveHelper;
import builderb0y.bigpixel.util.Util;

public class LayerGraph {

	public OpenImage
		openImage;
	public List<LayerNode>
		layerList = new ArrayList<>();
	public Map<String, LayerNode>
		layersByName = new HashMap<>();
	public Rectangle
		dragRectangle = new Rectangle(224.0D, 48.0D, new Color(0.25D, 1.0D, 0.25D, 0.5D));
	public Pane
		underlay = new Pane(),
		mainGrid = new Pane(),
		overlay  = new Pane(this.dragRectangle);
	public StackPane
		stackPane = new StackPane(this.underlay, this.mainGrid, this.overlay);
	public ScrollPane
		mainScroll = new ScrollPane(this.stackPane);
	public MenuItem
		addLayerAboveItem = new MenuItem("Add layer above"),
		addLayerBelowItem = new MenuItem("Add layer below"),
		addLayerLeftItem  = new MenuItem("Add layer left" ),
		addLayerRightItem = new MenuItem("Add layer right");
	public ButtonBase
		addLayerButton = new MenuButton(
			"🗋",
			null,
			this.addLayerAboveItem,
			this.addLayerBelowItem,
			this.addLayerLeftItem,
			this.addLayerRightItem
		),
		addFirstLayerButton = new Button("🗋"),
		duplicateLayerButton = new Button("🗐"),
		removeLayerButton = new Button("🗑"),
		resizeLayerButton = new Button("◰"),
		centerLayerButton = new Button("⊡");
	{
		this.addLayerButton.setTooltip(new Tooltip("Add layer..."));
		this.addFirstLayerButton.setTooltip(new Tooltip("Add layer..."));
		this.duplicateLayerButton.setTooltip(new Tooltip("Duplicate selected layer"));
		this.removeLayerButton.setTooltip(new Tooltip("Remove selected layer"));
		this.resizeLayerButton.setTooltip(new Tooltip("Resize selected layer"));
		this.centerLayerButton.setTooltip(new Tooltip("Reset view"));
	}
	public VBox
		buttons = new VBox(
			this.addLayerButton,
			this.duplicateLayerButton,
			this.removeLayerButton,
			this.resizeLayerButton,
			this.centerLayerButton
		);
	public BorderPane
		withButtons = new BorderPane();
	public SimpleObjectProperty<LayerNode>
		selectedLayer = new SimpleObjectProperty<>(this, "selectedLayer");
	public ToggleGroup
		visibleLayer = new ToggleGroup();
	public ObservableValue<LayerNode>
		visibleLayerProperty = this.visibleLayer.selectedToggleProperty().map((Toggle toggle) -> (LayerNode)(toggle.getUserData()));
	public SimpleObjectProperty<Point2D>
		lastMouseLocation = new SimpleObjectProperty<>(this, "lastMouseLocation");
	public RedrawThread
		redrawThread = new RedrawThread(this);

	public JsonMap save() {
		JsonMap saveData = new JsonMap(4);
		Toggle toggle = this.visibleLayer.getSelectedToggle();
		if (toggle != null) saveData.put("showing_layer", ((LayerNode)(toggle.getUserData())).getDisplayName());
		LayerNode selected = this.selectedLayer.get();
		if (selected != null) saveData.put("selected_layer", selected.getDisplayName());
		JsonArray layers = new JsonArray(this.layerList.size());
		for (LayerNode layer : this.layerList) {
			layers.add(layer.save());
		}
		saveData.put("layers", layers);
		return saveData;
	}

	public void load(JsonMap saveData) {
		record QueuedLoad(LayerNode layer, JsonMap saveData) {}
		JsonArray layersData = saveData.getArray("layers");
		List<QueuedLoad> toLoad = new ArrayList<>(layersData.size());
		for (JsonValue value : layersData) {
			LayerNode layer = new LayerNode(this, value.asMap());
			try {
				this.addLayer(layer, false);
			}
			catch (RuntimeException exception) {
				throw new SaveException(exception.getLocalizedMessage(), exception);
			}
			toLoad.add(new QueuedLoad(layer, value.asMap()));
		}
		for (QueuedLoad queuedLoad : toLoad) {
			queuedLoad.layer.load(queuedLoad.saveData);
		}
		if (saveData.get("showing_layer") instanceof JsonString string) {
			LayerNode showing = this.getLayerByName(string.value);
			if (showing != null) this.visibleLayer.selectToggle(showing.showing);
		}
		if (saveData.get("selected_layer") instanceof JsonString string) {
			LayerNode selected = this.getLayerByName(string.value);
			if (selected != null) this.selectedLayer.set(selected);
		}
	}

	public LayerGraph(OpenImage openImage) {
		this.openImage = openImage;
		super();
		this.dragRectangle.setVisible(false);
		this.overlay.setMouseTransparent(true);
		ObservableDoubleValue buttonWidth = this.addLayerButton.widthProperty();
		Font font = new Font(20);
		this.addFirstLayerButton.setFont(font);
		this.addLayerButton.setFont(font);
		this.duplicateLayerButton.setFont(font);
		this.removeLayerButton.setFont(font);
		this.resizeLayerButton.setFont(font);
		this.centerLayerButton.setFont(font);
		this. addFirstLayerButton.prefWidthProperty().bind(buttonWidth);
		this.duplicateLayerButton.prefWidthProperty().bind(buttonWidth);
		this.   removeLayerButton.prefWidthProperty().bind(buttonWidth);
		this.   resizeLayerButton.prefWidthProperty().bind(buttonWidth);
		this.   centerLayerButton.prefWidthProperty().bind(buttonWidth);
		this.withButtons.setLeft(this.buttons);
		this.withButtons.setCenter(this.mainScroll);
		((MenuButton)(this.addLayerButton)).setPopupSide(Side.RIGHT);
		ObservableBooleanValue noSelection = this.selectedLayer.isNull();
		this.      addLayerButton.disableProperty().bind(noSelection);
		this.duplicateLayerButton.disableProperty().bind(noSelection);
		this.   removeLayerButton.disableProperty().bind(noSelection);
		this.   resizeLayerButton.disableProperty().bind(noSelection);
		this.   centerLayerButton.disableProperty().bind(noSelection);
		this.addLayerAboveItem.setOnAction(this::addLayerAbove);
		this.addLayerBelowItem.setOnAction(this::addLayerBelow);
		this.addLayerLeftItem .setOnAction(this::addLayerLeft );
		this.addLayerRightItem.setOnAction(this::addLayerRight);
		this.addFirstLayerButton.setOnAction(this::addFirstLayer);
		this.duplicateLayerButton.setOnAction(this::duplicateLayer);
		this.removeLayerButton.setOnAction(this::removeLayer);
		this.resizeLayerButton.setOnAction(this::resizeLayer);
		this.centerLayerButton.setOnAction(this::centerLayer);
		this.selectedLayer.addListener(Util.change((LayerNode oldLayer, LayerNode newLayer) -> {
			if (oldLayer != null) {
				oldLayer.getColorNode().pseudoClassStateChanged(LayerNode.SELECTED, false);
			}
			if (newLayer != null) {
				newLayer.getColorNode().pseudoClassStateChanged(LayerNode.SELECTED, true);
			}
		}));
		this.mainGrid.setOnMouseEntered((MouseEvent event) -> this.lastMouseLocation.set(new Point2D(event.getX(), event.getY())));
		this.mainGrid.setOnMouseMoved((MouseEvent event) -> this.lastMouseLocation.set(new Point2D(event.getX(), event.getY())));
		this.mainGrid.setOnMouseExited((MouseEvent _) -> this.lastMouseLocation.set(null));
		this.mainScroll.setOnDragOver((DragEvent event) -> {
			if (event.getDragboard().hasContent(DataFormat.FILES)) {
				event.acceptTransferModes(TransferMode.COPY);
			}
		});
		this.mainScroll.setOnDragDropped((DragEvent event) -> {
			this.openImage.mainWindow.pasteFromClipboardToNewLayer(event.getDragboard(), event);
			event.consume();
		});
	}

	public String adjustName(String displayName) {
		displayName = displayName.trim();
		if (this.getLayerByName(displayName) != null) {
			for (int index = 1; true; index++) {
				String nextName = displayName + " (" + index + ')';
				if (this.getLayerByName(nextName) == null) {
					return nextName;
				}
			}
		}
		else {
			return displayName;
		}
	}

	public @Nullable LayerNode getLayerByName(String name) {
		return this.layersByName.get(name);
	}

	public void shiftRight(int x) {
		for (int index = this.layerList.size(); --index >= 0;) {
			LayerNode layer = this.layerList.get(index);
			int gridx = layer.getGridX();
			if (gridx < x) break;
			layer.setGridX(gridx + 1, true);
		}
	}

	public void shiftDown(int x, int y) {
		int index = this.indexOfPosition(x, y);
		if (index < 0) index = ~index;
		for (; index < this.layerList.size(); index++) {
			LayerNode layer = this.layerList.get(index);
			if (layer.getGridX() > x) break;
			if (layer.getGridY() > y) break;
			layer.setGridY(++y, true);
		}
	}

	public void moveNodeTo(LayerNode layer, int x, int y) {
		if (layer.getDependencies().containsAny((LayerNode node) -> node.getGridX() >= x)) {
			if (new Alert(AlertType.WARNING, "Moving the layer here will disconnect it from at least one of its dependencies.\nAre you sure you wish to continue?", ButtonType.YES, ButtonType.NO).showAndWait().orElse(null) != ButtonType.YES) {
				return;
			}
		}
		int index = this.indexOfPosition(layer.getGridX(), layer.getGridY());
		assert index >= 0;
		this.layerList.remove(index);
		if (this.hasLayerAt(x, y)) {
			this.shiftDown(x, y);
		}
		layer.setGridPos(x, y, true);
		index = this.indexOfPosition(x, y);
		assert index < 0;
		index = ~index;
		this.layerList.add(index, layer);
		this.updateFlow();
	}

	public int indexOfPosition(int x, int y) {
		return Collections.binarySearch(this.layerList, LayerPosition.of(x, y), LayerPosition.POSITION_COMPARATOR);
	}

	public boolean hasLayerAt(int x, int y) {
		return this.indexOfPosition(x, y) >= 0;
	}

	public void updateFlow() {
		int endIndex = 0;
		int endX = 0;
		List<LayerNode> layers = this.layerList;
		for (int index = 0, size = layers.size(); index < size; index++) {
			LayerNode layer = layers.get(index);
			int gridX = layer.getGridX();
			if (gridX > endX) {
				endIndex = index;
				endX = gridX;
			}
			layer.sources.setPotentialInputs(layers.subList(0, endIndex));
			layer.views.setPotentialInputs(layers);
		}
	}

	public List<LayerNode> getPossibleDependencies(LayerNode layer) {
		int index = this.indexOfPosition(layer.getGridX(), 0);
		if (index < 0) index = ~index;
		return this.layerList.subList(0, index);
	}

	public Stream<LayerNode> getDependants(LayerNode layer) {
		int index = this.indexOfPosition(layer.getGridX() + 1, 0);
		if (index < 0) index = ~index;
		return this.layerList.subList(index, this.layerList.size()).stream().filter((LayerNode other) -> {
			return other.getDependencies().dependsOn(layer);
		});
	}

	public void addLayer(LayerNode layer, boolean fadeIn) {
		String oldName = layer.getDisplayName();
		String newName = this.adjustName(oldName);
		if (!oldName.equals(newName)) {
			layer.setDisplayNameDirectly(newName);
		}
		int index = Collections.binarySearch(this.layerList, layer, LayerPosition.POSITION_COMPARATOR);
		if (index >= 0) {
			throw new IllegalArgumentException("Duplicate layer at " + layer.getGridX() + ", " + layer.getGridY());
		}
		this.layerList.add(~index, layer);
		this.layersByName.put(newName, layer);
		this.mainGrid.getChildren().addFirst(layer.getPreviewNode());
		this.updateFlow();
		this.selectedLayer.set(layer);
		this.buttons.getChildren().set(0, this.addLayerButton);
		if (fadeIn) {
			layer.getPreviewNode().setOpacity(0.0D);
			new Transition() {

				{
					this.setInterpolator(Interpolator.LINEAR);
					this.setCycleDuration(Duration.seconds(0.25D));
					this.setOnFinished((ActionEvent _) -> layer.getPreviewNode().setOpacity(1.0D));
				}

				@Override
				public void interpolate(double v) {
					layer.getPreviewNode().setOpacity(v);
				}
			}
			.play();
		}
	}

	public void removeLayer(LayerNode layer) {
		int index = Collections.binarySearch(this.layerList, layer, LayerPosition.POSITION_COMPARATOR);
		if (this.layerList.remove(index) != layer) {
			throw new IllegalStateException("Position overlap when removing layer!");
		}
		if (this.layersByName.remove(layer.getDisplayName()) != layer) {
			throw new IllegalStateException("Name mismatch when removing layer!");
		}
		this.mainGrid.getChildren().remove(layer.getPreviewNode());
		layer.getDependencies().getCurves().forEach((CurveHelper helper) -> helper.selfSourceIsSelected.set(false));
		this.selectedLayer.set(null);
		this.visibleLayer.selectToggle(null);
		this.buttons.getChildren().set(0, this.layerList.isEmpty() ? this.addFirstLayerButton : this.addLayerButton);
		this.updateFlow();
	}

	public void removeLayer(ActionEvent event) {
		LayerNode layer = this.selectedLayer.get();
		if (new Alert(AlertType.WARNING, "Are you sure you want to delete:\n" + layer + "?\nThis action cannot be undone yet!", ButtonType.YES, ButtonType.NO).showAndWait().orElse(null) == ButtonType.YES) {
			this.removeLayer(layer);
		}
	}

	public void addLayerAbove(ActionEvent ignored) {
		LayerNode selected = this.selectedLayer.get();
		int x = selected.getGridX();
		int y = Math.max(selected.getGridY() - 1, 0);
		LayerNode layer = this.promptForLayer(x, y, selected.imageWidth(), selected.imageHeight());
		if (layer != null) {
			if (this.hasLayerAt(x, y)) {
				this.shiftDown(x, y);
			}
			this.addLayer(layer, true);
		}
	}

	public void addLayerBelow(ActionEvent ignored) {
		LayerNode selected = this.selectedLayer.get();
		int x = selected.getGridX();
		int y = selected.getGridY() + 1;
		LayerNode layer = this.promptForLayer(x, y, selected.imageWidth(), selected.imageHeight());
		if (layer != null) {
			if (this.hasLayerAt(x, y)) {
				this.shiftDown(x, y);
			}
			this.addLayer(layer, true);
		}
	}

	public void addLayerLeft(ActionEvent ignored) {
		LayerNode selected = this.selectedLayer.get();
		int x = Math.max(selected.getGridX() - 1, 0);
		int y = selected.getGridY();
		LayerNode layer = this.promptForLayer(x, y, selected.imageWidth(), selected.imageHeight());
		if (layer != null) {
			if (this.hasLayerAt(x, y)) {
				this.shiftRight(x);
			}
			this.addLayer(layer, true);
		}
	}

	public void addLayerRight(ActionEvent ignored) {
		LayerNode selected = this.selectedLayer.get();
		int x = selected.getGridX() + 1;
		int y = selected.getGridY();
		LayerNode layer = this.promptForLayer(x, y, selected.imageWidth(), selected.imageHeight());
		if (layer != null) {
			if (this.hasLayerAt(x, y)) {
				this.shiftRight(x);
			}
			this.addLayer(layer, true);
		}
	}

	public @Nullable LayerNode promptForLayer(int x, int y, int width, int height) {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(this.openImage.mainWindow.stage);
		dialog.setTitle("New Layer");
		Spinner<Integer> widthSpinner = Util.setupSpinner(new Spinner<>(1, 32767, width), 80);
		Spinner<Integer> heightSpinner = Util.setupSpinner(new Spinner<>(1, 32767, height), 80);
		TextField nameField = new TextField("New Layer");
		Label widthLabel = new Label("Width: ");
		Label heightLabel = new Label("Height: ");
		Label nameLabel = new Label("Name: ");
		GridPane gridPane = new GridPane();
		gridPane.add(widthLabel,  0, 0);
		gridPane.add(widthSpinner,       1, 0);
		gridPane.add(heightLabel, 0, 1);
		gridPane.add(heightSpinner,      1, 1);
		gridPane.add(nameLabel,   0, 2);
		gridPane.add(nameField,        1, 2);
		dialog.getDialogPane().setContent(gridPane);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		if (dialog.showAndWait().orElse(null) == ButtonType.OK) {
			return new LayerNode(this, x, y, widthSpinner.getValue(), heightSpinner.getValue(), nameField.getText());
		}
		else {
			return null;
		}
	}

	public void addFirstLayer(ActionEvent ignored) {
		LayerNode layer = this.promptForLayer(0, 0, 16, 16);
		if (layer != null) this.addLayer(layer, true);
	}

	public void duplicateLayer(ActionEvent ignored) {
		LayerNode selected = this.selectedLayer.get();
		int x = selected.getGridX();
		int y = selected.getGridY() + 1;
		LayerNode newLayer = new LayerNode(this, x, y, selected.imageWidth(), selected.imageHeight(), selected.getDisplayName() + " (duplicate)");
		newLayer.load(selected.save());
		if (this.hasLayerAt(x, y)) {
			this.shiftDown(x, y);
		}
		this.addLayer(newLayer, true);
		newLayer.requestRedraw();
	}

	public void resizeLayer(ActionEvent event) {
		LayerNode toResize = this.selectedLayer.get();
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(this.openImage.mainWindow.stage);
		dialog.setTitle("Resize " + toResize.getDisplayName());
		Spinner<Integer> width = Util.setupSpinner(new Spinner<>(1, 32768, toResize.imageWidth()), 80);
		Spinner<Integer> height = Util.setupSpinner(new Spinner<>(1, 32768, toResize.imageHeight()), 80);
		GridPane gridPane = new GridPane();
		gridPane.add(new Label("Width: "), 0, 0);
		gridPane.add(width, 1, 0);
		gridPane.add(new Label("Height: "), 0, 1);
		gridPane.add(height, 1, 1);
		dialog.getDialogPane().setContent(gridPane);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		if (dialog.showAndWait().orElse(null) == ButtonType.OK) {
			toResize.animation.checkSize(width.getValue(), height.getValue(), true, true);
			this.openImage.imageDisplay.center();
			toResize.requestRedraw();
		}
	}

	public void centerLayer(ActionEvent ignored) {
		this.openImage.imageDisplay.center();
	}

	public Parent getRootNode() {
		return this.withButtons;
	}

	public LayerNode getVisibleLayer() {
		return this.visibleLayerProperty.getValue();
	}

	public LayerNode getSelectedLayer() {
		return this.selectedLayer.get();
	}
}