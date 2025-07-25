package builderb0y.notgimp;

import java.io.File;
import java.util.*;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.sources.LayerSource;
import builderb0y.notgimp.tools.ColorPickerTool;
import builderb0y.notgimp.tools.ColorPickerTool.ColorPickerCallback;
import builderb0y.notgimp.tools.SourcelessTool;
import builderb0y.notgimp.tools.Tool;

public class OpenImage {

	public MainWindow
		mainWindow;
	public SimpleObjectProperty<File>
		file = new SimpleObjectProperty<>();
	public BorderPane
		rightPane = new BorderPane(),
		layersAndButtons = new BorderPane(),
		sourcePane = new BorderPane();
	public TreeView<Layer>
		layerTree = new TreeView<>();
	public Map<String, Layer>
		layerMap = new HashMap<>();
	public HBox
		layerButtons = new HBox();
	public MenuButton
		addLayerButton = new MenuButton("+");
	public MenuItem
		addLayerParentButton = new MenuItem("Wrap with new parent"),
		addLayerAboveButton  = new MenuItem("Add layer above"),
		addChildLayerButton  = new MenuItem("Add child layer"),
		addLayerBelowButton  = new MenuItem("Add layer below");
	public Button
		duplicateLayerButton = new Button("*"),
		moveLayerUpButton    = new Button("⏶"),
		moveLayerDownButton  = new Button("⏷");
	public SplitPane
		layersAndTools = new SplitPane(),
		imageAndRightPane = new SplitPane();
	public ZoomableImage
		imageDisplay = new ZoomableImage(this);
	public ToggleGroup
		showingLayer = new ToggleGroup();
	public ObservableValue<Layer>
		showingLayerProperty = this.showingLayer.selectedToggleProperty().map((Toggle toggle) -> (Layer)(toggle.getUserData())),
		selectedLayerProperty = this.layerTree.getSelectionModel().selectedItemProperty().map(TreeItem<Layer>::getValue);
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
			this.selectedLayerProperty.flatMap(
				(Layer layer) -> layer.sources.toolWithoutColorPicker
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
	public ObjectBinding<String> title = (
		new When(this.file.isNotNull())
		.then((ObservableObjectValue<String>)(this.file.map(File::getName)))
		.otherwise((ObservableObjectValue<String>)(this.layerTree.rootProperty().flatMap((TreeItem<Layer> layer) -> layer.getValue().name)))
	);
	public boolean
		redrawQueued;
	public Runnable
		redrawer = () -> this.redrawAll(true);

	public JsonMap save() {
		JsonMap root = new JsonMap();
		root.add("version", SaveVersions.CURRENT);
		TreeItem<Layer> rootItem = this.layerTree.getRoot();
		root.add("root_layer", rootItem.getValue().save());
		root.add("showing_layer", this.getVisibleLayer().name.get());
		root.add("selected_layer", this.getSelectedLayer().name.get());
		root.add("wrap", this.wrap.getValue());
		root.add("animation", this.animationSource.save());
		return root;
	}

	public void load(JsonMap map) {
		this.initFirstLayer(new Layer(this, map.getMap("root_layer")), true);
		this.showingLayer.selectToggle((RadioButton)(this.layerMap.get(map.getString("showing_layer")).item.getGraphic()));
		this.layerTree.getSelectionModel().select(this.layerMap.get(map.getString("selected_layer")).item);
		this.animationSource.load(map.getMap("animation"));
	}

	public OpenImage(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.layersAndTools.setOrientation(Orientation.VERTICAL);
		this.addLayerButton.setPopupSide(Side.TOP);
		this.addLayerButton.getItems().addAll(
			this.addLayerParentButton,
			this.addLayerAboveButton,
			this.addChildLayerButton,
			this.addLayerBelowButton
		);
		this.layerButtons.getChildren().addAll(
			this.addLayerButton,
			this.duplicateLayerButton,
			this.moveLayerUpButton,
			this.moveLayerDownButton
		);
		this.layersAndButtons.setCenter(this.layerTree);
		this.layersAndButtons.setBottom(this.layerButtons);
		this.layersAndTools.getItems().add(this.layersAndButtons);
		this.layersAndTools.getItems().add(this.sourcePane);
		this.rightPane.setCenter(this.layersAndTools);
		this.rightPane.setBottom(this.animationSource.hbox);
		this.imageAndRightPane.setOrientation(Orientation.HORIZONTAL);
		this.imageAndRightPane.getItems().addAll(this.imageDisplay.display.getRootPane(), this.rightPane);
		this.imageAndRightPane.setDividerPositions(0.75D);
		this.layerTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		this.layerTree.setEditable(true);
		this.layerTree.setCellFactory((TreeView<Layer> view) -> {
			TextFieldTreeCell<Layer> cell = new TextFieldTreeCell<>();
			StringConverter<Layer> converter = new StringConverter<>() {

				@Override
				public Layer fromString(String string) {
					Layer layer = cell.getItem();
					if (layer != null) layer.setName(string);
					return layer;
				}

				@Override
				public String toString(Layer object) {
					//this shouldn't be necessary, but without a null check
					//it crashes when wrapping a layer with a new parent.
					return object != null ? object.name.get() : "";
				}
			};
			cell.setConverter(converter);
			cell.styleProperty().bind(
				cell
				.itemProperty()
				.flatMap((Layer layer) -> layer.redrawException)
				.map((Throwable _) -> "-fx-text-fill: #FF3F3F;")
			);
			Tooltip tooltip = new Tooltip();
			tooltip.textProperty().bind(
				cell
				.itemProperty()
				.flatMap((Layer layer) -> layer.redrawException)
				.map(Throwable::getLocalizedMessage)
			);
			cell.tooltipProperty().bind(
				new When(tooltip.textProperty().isNotEmpty())
				.then(tooltip)
				.otherwise((Tooltip)(null))
			);
			MenuItem removeLayer = new MenuItem();
			removeLayer.setOnAction((ActionEvent _) -> this.removeLayer(cell.getTreeItem()));
			removeLayer.textProperty().bind(cell.itemProperty().map((Layer layer) -> "Delete layer " + layer.name.get()));
			MenuItem unwrapLayer = new MenuItem();
			unwrapLayer.setOnAction((ActionEvent _) -> this.unwrapLayer(cell.getTreeItem()));
			unwrapLayer.textProperty().bind(cell.itemProperty().map((Layer layer) -> "Unwrap layer " + layer.name.get()));
			MenuItem resizeLayer = new MenuItem();
			resizeLayer.setOnAction((ActionEvent _) -> this.resizeLayer(cell.getTreeItem()));
			resizeLayer.textProperty().bind(cell.itemProperty().map((Layer layer) -> "Resize layer " + layer.name.get()));
			MenuItem redrawLayer = new MenuItem();
			redrawLayer.setOnAction((ActionEvent _) -> cell.getItem().requestRedraw());
			redrawLayer.textProperty().bind(cell.itemProperty().map((Layer layer) -> "Redraw layer " + layer.name.get()));
			cell.setContextMenu(new ContextMenu(removeLayer, unwrapLayer, resizeLayer, redrawLayer));
			return cell;
		});
	}

	public void init() {
		this.addLayerParentButton.setOnAction(this::addParentLayer);
		this.addLayerAboveButton.setOnAction(this::addLayerAbove);
		this.addChildLayerButton.setOnAction(this::addChildLayer);
		this.addLayerBelowButton.setOnAction(this::addLayerBelow);
		this.duplicateLayerButton.setOnAction(this::duplicateLayer);
		this.moveLayerUpButton.setOnAction(this::moveLayerUp);
		this.moveLayerDownButton.setOnAction(this::moveLayerDown);
		BooleanBinding rootSelected = (
			this
			.layerTree
			.getSelectionModel()
			.selectedItemProperty()
			.isEqualTo(this.layerTree.rootProperty())
		);
		this.addLayerAboveButton.disableProperty().bind(rootSelected);
		this.addLayerBelowButton.disableProperty().bind(rootSelected);
		this.duplicateLayerButton.disableProperty().bind(rootSelected);
		this.moveLayerUpButton.disableProperty().bind(this.layerTree.getSelectionModel().selectedItemProperty().map(this::cantMoveUp));
		this.moveLayerDownButton.disableProperty().bind(this.layerTree.getSelectionModel().selectedItemProperty().map(this::cantMoveDown));
		this.sourcePane.centerProperty().bind(
			this.layerTree.getSelectionModel().selectedItemProperty().map(
				(TreeItem<Layer> item) -> item.getValue().sources.rootPane
			)
		);
		this.imageDisplay.init();
		this.requestRedraw();
	}

	public Node getMainNode() {
		return this.imageAndRightPane;
	}

	public Layer getSelectedLayer() {
		TreeItem<Layer> item = this.layerTree.getSelectionModel().getSelectedItem();
		return item != null ? item.getValue() : this.layerTree.getRoot().getValue();
	}

	public Layer getVisibleLayer() {
		return this.showingLayerProperty.getValue();
	}

	public @Nullable Layer findLayer(String name) {
		return this.layerMap.get(name);
	}

	public void requestRedraw() {
		if (!this.redrawQueued) {
			this.redrawQueued = true;
			Platform.runLater(this.redrawer);
		}
	}

	public void redrawAll(boolean updateView) {
		this.redrawLayer(this.layerTree.getRoot());
		this.redrawQueued = false;
		if (updateView) {
			this.imageDisplay.redraw();
		}
		if (this.mainWindow.getCurrentImage() == this) {
			this.mainWindow.histogram.redrawLayer(this.getVisibleLayer());
		}
	}

	public Set<Layer> redrawLayer(TreeItem<Layer> layer) {
		Set<Layer> changedLayers = new HashSet<>();
		for (TreeItem<Layer> child : layer.getChildren()) {
			changedLayers.addAll(this.redrawLayer(child));
		}
		Layer actualLayer = layer.getValue();
		LayerSource source = actualLayer.sources.getCurrentSource();
		if (actualLayer.needsRedraw || source.isAnimated() || !Collections.disjoint(source.getDependencies(), changedLayers)) {
			source.redraw();
			actualLayer.redrawThumbnail();
			actualLayer.needsRedraw = false;
			changedLayers.add(actualLayer);
		}
		return changedLayers;
	}

	public void invalidateAllLayerStructures() {
		TreeItem<Layer> root = this.layerTree.getRoot();
		if (root != null) { //can be null during initialization.
			this.invalidateRecursive(root);
		}
	}

	public void invalidateRecursive(TreeItem<Layer> layer) {
		layer.getChildren().forEach(this::invalidateRecursive);
		layer.getValue().sources.invalidateStructure();
	}

	public void addToMap(Layer layer) {
		this.layerMap.put(layer.name.get(), layer);
		for (TreeItem<Layer> child : layer.item.getChildren()) {
			this.addToMap(child.getValue());
		}
	}

	public void initFirstLayer(Layer layer, boolean fromSave) {
		this.addToMap(layer);
		this.layerTree.setRoot(layer.item);
		this.layerTree.getSelectionModel().select(0);
		layer.init(fromSave);
	}

	public void addParentLayer(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		this.createParentLayer(old, null);
	}

	public Layer createParentLayer(TreeItem<Layer> old, @Nullable String name) {
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, name != null ? name : "", image.width, image.height);
		this.addToMap(newLayer);
		if (old == this.layerTree.getRoot()) {
			this.layerTree.setRoot(newLayer.item);
		}
		else {
			ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
			int index = children.indexOf(old);
			children.set(index, newLayer.item);
		}
		newLayer.item.getChildren().add(old);
		newLayer.item.setExpanded(true);
		if (name == null) this.layerTree.getSelectionModel().select(newLayer.item);
		newLayer.init(false);
		if (name == null) this.layerTree.edit(newLayer.item);
		return newLayer;
	}

	public void addLayerAbove(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		this.createLayerAbove(old, null);
	}

	public Layer createLayerAbove(TreeItem<Layer> old, @Nullable String name) {
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, name != null ? name : "", image.width, image.height);
		this.addToMap(newLayer);
		ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
		children.add(children.indexOf(old), newLayer.item);
		if (name == null) this.layerTree.getSelectionModel().select(newLayer.item);
		newLayer.init(false);
		if (name == null) this.layerTree.edit(newLayer.item);
		return newLayer;
	}

	public void addChildLayer(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		this.addToMap(newLayer);
		old.getChildren().addFirst(newLayer.item);
		old.setExpanded(true);
		this.layerTree.getSelectionModel().select(newLayer.item);
		newLayer.init(false);
		this.layerTree.edit(newLayer.item);
	}

	public void addLayerBelow(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		this.addToMap(newLayer);
		ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
		children.add(children.indexOf(old) + 1, newLayer.item);
		this.layerTree.getSelectionModel().select(newLayer.item);
		newLayer.init(false);
		this.layerTree.edit(newLayer.item);
	}

	public void removeLayer(TreeItem<Layer> toRemove) {
		if (new Alert(AlertType.WARNING, "Are you sure you want to delete " + toRemove.getValue().name.get() + "?\nThis action will remove the layer and its children abd cannot be undone yet!", ButtonType.YES, ButtonType.NO).showAndWait().orElse(null) != ButtonType.YES) {
			return;
		}
		this.removeLayerRecursive(toRemove);
		toRemove.getParent().getChildren().remove(toRemove);
		this.layerTree.getSelectionModel().select(0);
		if (toRemove.getValue() == this.showingLayerProperty.getValue()) {
			((RadioButton)(this.layerTree.getRoot().getGraphic())).setSelected(true);
		}
		this.invalidateAllLayerStructures();
	}

	public void removeLayerRecursive(TreeItem<Layer> toRemove) {
		for (TreeItem<Layer> child : toRemove.getChildren()) {
			this.removeLayerRecursive(child);
		}
		this.postRemoveLayer(toRemove);
	}

	public void postRemoveLayer(TreeItem<Layer> toRemove) {
		History.onLayerDeleted(toRemove);
		Layer removed = this.layerMap.remove(toRemove.getValue().name.get());
		if (removed != toRemove.getValue()) {
			System.err.println("Removed " + toRemove.getValue() + ", but got " + removed + " instead?");
		}
	}

	public void unwrapLayer(TreeItem<Layer> toUnwrap) {
		if (new Alert(AlertType.WARNING, "Are you sure you want to unwrap " + toUnwrap.getValue().name.get() + "?\nThis action will delete the layer, but not its children, and cannot be undone yet!", ButtonType.YES, ButtonType.NO).showAndWait().orElse(null) != ButtonType.YES) {
			return;
		}
		TreeItem<Layer> parent = toUnwrap.getParent();
		if (parent == null) { //unwrapping root.
			if (toUnwrap.getChildren().size() == 1) {
				TreeItem<Layer> stored = toUnwrap.getChildren().get(0);
				toUnwrap.getChildren().clear();
				this.postRemoveLayer(toUnwrap);
				this.layerTree.setRoot(stored);
				this.layerTree.getSelectionModel().select(0);
				if (toUnwrap.getValue() == this.showingLayerProperty.getValue()) {
					((RadioButton)(this.layerTree.getRoot().getGraphic())).setSelected(true);
				}
			}
		}
		else { //unwrapping non-root.
			List<TreeItem<Layer>> siblings = parent.getChildren();
			List<TreeItem<Layer>> children = new ArrayList<>(toUnwrap.getChildren());
			toUnwrap.getChildren().clear();
			int index = siblings.indexOf(toUnwrap);
			this.postRemoveLayer(toUnwrap);
			switch (children.size()) {
				case 0 -> {
					siblings.remove(index);
				}
				case 1 -> {
					siblings.set(index, children.get(0));
				}
				default -> {
					siblings.remove(index);
					siblings.addAll(index, children);
				}
			}
			this.layerTree.getSelectionModel().select(index);
		}
		this.invalidateAllLayerStructures();
	}

	public void duplicateLayer(ActionEvent event) {
		TreeItem<Layer> toDuplicate = this.layerTree.getSelectionModel().getSelectedItem();
		Layer duplicate = new Layer(toDuplicate.getValue());
		this.addToMap(duplicate);
		ObservableList<TreeItem<Layer>> children = toDuplicate.getParent().getChildren();
		children.add(children.indexOf(toDuplicate), duplicate.item);
		this.layerTree.getSelectionModel().select(duplicate.item);
		duplicate.init(false);
		this.invalidateAllLayerStructures();
	}

	public void resizeLayer(TreeItem<Layer> toResize) {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(this.mainWindow.stage);
		dialog.setTitle("Resize " + toResize.getValue().name.get());
		Spinner<Integer> width = Util.setupSpinner(new Spinner<>(1, 32768, toResize.getValue().image.width), 80);
		Spinner<Integer> height = Util.setupSpinner(new Spinner<>(1, 32768, toResize.getValue().image.height), 80);
		GridPane gridPane = new GridPane();
		gridPane.add(new Label("Width: "), 0, 0);
		gridPane.add(width, 1, 0);
		gridPane.add(new Label("Height: "), 0, 1);
		gridPane.add(height, 1, 1);
		dialog.getDialogPane().setContent(gridPane);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		if (dialog.showAndWait().orElse(null) == ButtonType.OK) {
			toResize.getValue().image.resize(width.getValue(), height.getValue(), true);
			toResize.getValue().requestRedraw();
		}
	}

	public boolean cantMoveUp(TreeItem<Layer> toMove) {
		if (toMove == null) return true;
		TreeItem<Layer> parent = toMove.getParent();
		if (parent == null) return true;
		ObservableList<TreeItem<Layer>> siblings = parent.getChildren();
		int oldIndex = siblings.indexOf(toMove);
		if (oldIndex > 0) return false;
		return parent.getParent() == null;
	}

	public void moveLayerUp(ActionEvent event) {
		TreeItem<Layer> toMove = this.layerTree.getSelectionModel().getSelectedItem();
		TreeItem<Layer> parent = toMove.getParent();
		ObservableList<TreeItem<Layer>> siblings = parent.getChildren();
		int oldIndex = siblings.indexOf(toMove);
		if (oldIndex == 0) {
			TreeItem<Layer> grandparent = parent.getParent();
			int parentIndex = grandparent.getChildren().indexOf(parent);
			siblings.remove(oldIndex);
			grandparent.getChildren().add(parentIndex, toMove);
		}
		else {
			int newIndex = oldIndex - 1;
			siblings.remove(oldIndex);
			siblings.get(newIndex).getChildren().add(toMove);
		}
		this.layerTree.getSelectionModel().select(toMove);
		this.invalidateAllLayerStructures();
	}

	public boolean cantMoveDown(TreeItem<Layer> toMove) {
		if (toMove == null) return true;
		TreeItem<Layer> parent = toMove.getParent();
		if (parent == null) return true;
		ObservableList<TreeItem<Layer>> siblings = parent.getChildren();
		int oldIndex = siblings.indexOf(toMove);
		if (oldIndex < siblings.size() - 1) return false;
		return parent.getParent() == null;
	}

	public void moveLayerDown(ActionEvent event) {
		TreeItem<Layer> toMove = this.layerTree.getSelectionModel().getSelectedItem();
		TreeItem<Layer> parent = toMove.getParent();
		ObservableList<TreeItem<Layer>> siblings = parent.getChildren();
		int oldIndex = siblings.indexOf(toMove);
		if (oldIndex == siblings.size() - 1) {
			TreeItem<Layer> grandparent = parent.getParent();
			int parentIndex = grandparent.getChildren().indexOf(parent);
			siblings.remove(oldIndex);
			grandparent.getChildren().add(parentIndex + 1, toMove);
		}
		else {
			siblings.remove(oldIndex);
			siblings.get(oldIndex).getChildren().add(0, toMove);
		}
		this.layerTree.getSelectionModel().select(toMove);
		this.invalidateAllLayerStructures();
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