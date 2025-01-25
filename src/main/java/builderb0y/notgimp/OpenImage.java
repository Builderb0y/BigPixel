package builderb0y.notgimp;

import java.io.File;

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

public class OpenImage {

	public MainWindow
		mainWindow;
	public SimpleObjectProperty<File>
		file = new SimpleObjectProperty<>();
	public BorderPane
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
		removeLayerButton = new Button("-"),
		duplicateLayerButton = new Button("*"),
		moveLayerUpButton = new Button("/\\"),
		moveLayerDownButton = new Button("\\/");
	public SplitPane
		layersAndTools = new SplitPane(),
		imageAndRightPane = new SplitPane();
	public ZoomableImage
		imageDisplay = new ZoomableImage(this);
	public ToggleGroup
		showingLayer = new ToggleGroup();
	public ObservableValue<Layer>
		showingLayerProperty = this.showingLayer.selectedToggleProperty().map((Toggle toggle) -> (Layer)(toggle.getUserData()));
	public ObservableValue<HDRImage>
		showingImage = this.showingLayerProperty.flatMap((Layer layer) -> layer.image.value);
	public SimpleBooleanProperty
		wrap = new SimpleBooleanProperty();

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
		this.imageAndRightPane.setOrientation(Orientation.HORIZONTAL);
		this.imageAndRightPane.getItems().addAll(this.imageDisplay.display.getRootPane(), this.layersAndTools);
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

	public TreeItem<Layer> createTreeItem(Layer layer) {
		RadioButton showing = new RadioButton();
		showing.setGraphic(layer.thumbnailView);
		showing.setText(" ");
		showing.setToggleGroup(this.showingLayer);
		showing.setUserData(layer);
		showing.setSelected(true);
		this.layerMap.put(layer.name.get(), layer);
		return new TreeItem<>(layer, showing);
	}

	public void initFirstLayer(Layer layer) {
		this.layerTree.setRoot(this.createTreeItem(layer));
		this.layerTree.getSelectionModel().select(0);
		layer.init();
	}

	public void addParentLayer(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		TreeItem<Layer> item = this.createTreeItem(newLayer);
		if (old == this.layerTree.getRoot()) {
			this.layerTree.setRoot(item);
		}
		else {
			ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
			int index = children.indexOf(old);
			children.set(index, item);
		}
		item.getChildren().add(old);
		item.setExpanded(true);
		this.layerTree.getSelectionModel().select(item);
		this.layerTree.edit(item);
		newLayer.init();
	}

	public void addLayerAbove(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		TreeItem<Layer> item = this.createTreeItem(newLayer);
		ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
		children.add(children.indexOf(old), item);
		this.layerTree.getSelectionModel().select(item);
		this.layerTree.edit(item);
		newLayer.init();
	}

	public void addChildLayer(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		TreeItem<Layer> item = this.createTreeItem(newLayer);
		old.getChildren().addFirst(item);
		old.setExpanded(true);
		this.layerTree.getSelectionModel().select(item);
		this.layerTree.edit(item);
		newLayer.init();
	}

	public void addLayerBelow(ActionEvent event) {
		TreeItem<Layer> old = this.layerTree.getSelectionModel().getSelectedItem();
		HDRImage image = old.getValue().image;
		Layer newLayer = new Layer(this, "", image.width, image.height);
		TreeItem<Layer> item = this.createTreeItem(newLayer);
		ObservableList<TreeItem<Layer>> children = old.getParent().getChildren();
		children.add(children.indexOf(old) + 1, item);
		this.layerTree.getSelectionModel().select(item);
		this.layerTree.edit(item);
		newLayer.init();
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
		this.layerMap.remove(toRemove.getValue().name);
		History.onLayerDeleted(toRemove);
		for (TreeItem<Layer> child : toRemove.getChildren()) {
			this.removeLayerRecursive(child);
		}
	}

	public void duplicateLayer(ActionEvent event) {
		TreeItem<Layer> toDuplicate = this.layerTree.getSelectionModel().getSelectedItem();
		Layer duplicate = new Layer(toDuplicate.getValue());
		TreeItem<Layer> item = this.createTreeItem(duplicate);
		ObservableList<TreeItem<Layer>> children = toDuplicate.getParent().getChildren();
		children.add(children.indexOf(toDuplicate), item);
		this.layerTree.getSelectionModel().select(item);
		duplicate.init();
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
	}
}