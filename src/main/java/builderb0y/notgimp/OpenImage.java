package builderb0y.notgimp;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.sources.LayerSource;

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
	public ObservableMap<String, Layer>
		layerMap = FXCollections.observableHashMap();
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
		removeLayerButton    = new Button("-"),
		duplicateLayerButton = new Button("*"),
		moveLayerUpButton    = new Button("/\\"),
		moveLayerDownButton  = new Button("\\/");
	public SplitPane
		layersAndTools = new SplitPane(),
		imageAndRightPane = new SplitPane();
	public ZoomableImage
		imageDisplay = new ZoomableImage(this);
	public ToggleGroup
		showingLayer = new ToggleGroup();
	public ObservableValue<Layer>
		showingLayerProperty = this.showingLayer.selectedToggleProperty().map((Toggle toggle) -> (Layer)(toggle.getUserData()));
	public SimpleBooleanProperty
		wrap = new SimpleBooleanProperty();
	public AnimationSource
		animationSource = new AnimationSource(this);

	public JsonObject save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", SaveVersions.CURRENT);
		TreeItem<Layer> rootItem = this.layerTree.getRoot();
		root.add("root_layer", rootItem.getValue().save());
		root.addProperty("showing_layer", this.getVisibleLayer().name.get());
		root.addProperty("selected_layer", this.getSelectedLayer().name.get());
		root.addProperty("wrap", this.wrap.getValue());
		root.add("animation", this.animationSource.save());
		return root;
	}

	public void load(JsonObject object) {
		this.initFirstLayer(new Layer(this, object.getAsJsonObject("root_layer")), true);
		this.showingLayer.selectToggle((RadioButton)(this.layerMap.get(object.get("showing_layer").getAsString()).item.getGraphic()));
		this.layerTree.getSelectionModel().select(this.layerMap.get(object.get("selected_layer").getAsString()).item);
		this.animationSource.load(object.getAsJsonObject("animation"));
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
			this.removeLayerButton,
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
			StringConverter<Layer> converter = new StringConverter<>() {

				@Override
				public Layer fromString(String string) {
					Layer layer = view.getSelectionModel().getSelectedItem().getValue();
					layer.setName(string);
					return layer;
				}

				@Override
				public String toString(Layer object) {
					//this shouldn't be necessary, but without a null check
					//it crashes when wrapping a layer with a new parent.
					return object != null ? object.name.get() : "";
				}
			};
			return new TextFieldTreeCell<>(converter);
		});
	}

	public void init() {
		this.addLayerParentButton.setOnAction(this::addParentLayer);
		this.addLayerAboveButton.setOnAction(this::addLayerAbove);
		this.addChildLayerButton.setOnAction(this::addChildLayer);
		this.addLayerBelowButton.setOnAction(this::addLayerBelow);
		this.removeLayerButton.setOnAction(this::removeLayer);
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
		this.removeLayerButton.disableProperty().bind(rootSelected);
		this.addLayerAboveButton.disableProperty().bind(rootSelected);
		this.addLayerBelowButton.disableProperty().bind(rootSelected);
		this.duplicateLayerButton.disableProperty().bind(rootSelected);
		this.moveLayerUpButton.disableProperty().bind(this.layerTree.getSelectionModel().selectedItemProperty().map(this::cantMoveUp));
		this.moveLayerDownButton.disableProperty().bind(this.layerTree.getSelectionModel().selectedItemProperty().map(this::cantMoveDown));
		this.sourcePane.centerProperty().bind(
			this.layerTree.getSelectionModel().selectedItemProperty().map(
				(TreeItem<Layer> item) -> item.getValue().sources.tabPane
			)
		);
		this.imageDisplay.init();
	}

	public Node getMainNode() {
		return this.imageAndRightPane;
	}

	public Layer getSelectedLayer() {
		return this.layerTree.getSelectionModel().getSelectedItem().getValue();
	}

	public Layer getVisibleLayer() {
		return this.showingLayerProperty.getValue();
	}

	public @Nullable Layer findLayer(String name) {
		return this.layerMap.get(name);
	}

	public void tickAnimation() {
		this.tickAnimation(this.layerTree.getRoot());
	}

	public Set<Layer> tickAnimation(TreeItem<Layer> layer) {
		Set<Layer> changedLayers = new HashSet<>();
		for (TreeItem<Layer> child : layer.getChildren()) {
			changedLayers.addAll(this.tickAnimation(child));
		}
		LayerSource source = layer.getValue().sources.getCurrentSource();
		if (source.isAnimated() || !Collections.disjoint(source.getDependencies(), changedLayers)) {
			source.redraw(true);
			changedLayers.add(layer.getValue());
		}
		return changedLayers;
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
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
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
		newLayer.init(false);
		this.layerTree.getSelectionModel().select(newLayer.item);
		this.layerTree.edit(newLayer.item);
	}

	public void addLayerAbove(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		this.addToMap(newLayer);
		ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
		children.add(children.indexOf(old), newLayer.item);
		newLayer.init(false);
		this.layerTree.getSelectionModel().select(newLayer.item);
		this.layerTree.edit(newLayer.item);
	}

	public void addChildLayer(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		this.addToMap(newLayer);
		old.getChildren().addFirst(newLayer.item);
		old.setExpanded(true);
		newLayer.init(false);
		this.layerTree.getSelectionModel().select(newLayer.item);
		this.layerTree.edit(newLayer.item);
	}

	public void addLayerBelow(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		this.addToMap(newLayer);
		ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
		children.add(children.indexOf(old) + 1, newLayer.item);
		newLayer.init(false);
		this.layerTree.getSelectionModel().select(newLayer.item);
		this.layerTree.edit(newLayer.item);
	}

	public void removeLayer(ActionEvent event) {
		TreeItem<Layer> toRemove = this.layerTree.getSelectionModel().getSelectedItem();
		toRemove.getParent().getChildren().remove(toRemove);
		this.layerTree.getSelectionModel().select(0);
		if (toRemove.getValue() == this.showingLayerProperty.getValue()) {
			((RadioButton)(this.layerTree.getRoot().getGraphic())).setSelected(true);
		}
		this.removeLayerRecursive(toRemove);
	}

	public void removeLayerRecursive(TreeItem<Layer> toRemove) {
		this.layerMap.remove(toRemove.getValue().name.get());
		History.onLayerDeleted(toRemove);
		for (TreeItem<Layer> child : toRemove.getChildren()) {
			this.removeLayerRecursive(child);
		}
	}

	public void duplicateLayer(ActionEvent event) {
		TreeItem<Layer> toDuplicate = this.layerTree.getSelectionModel().getSelectedItem();
		Layer duplicate = new Layer(toDuplicate.getValue());
		this.addToMap(duplicate);
		ObservableList<TreeItem<Layer>> children = toDuplicate.getParent().getChildren();
		children.add(children.indexOf(toDuplicate), duplicate.item);
		this.layerTree.getSelectionModel().select(duplicate.item);
		duplicate.init(false);
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
			parent.getValue().sources.invalidateAll();
		}
		else {
			int newIndex = oldIndex - 1;
			siblings.remove(oldIndex);
			siblings.get(newIndex).getChildren().add(toMove);
		}
		this.layerTree.getSelectionModel().select(toMove);
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
			parent.getValue().sources.invalidateAll();
		}
		else {
			siblings.remove(oldIndex);
			siblings.get(oldIndex).getChildren().add(0, toMove);
		}
		this.layerTree.getSelectionModel().select(toMove);
	}
}