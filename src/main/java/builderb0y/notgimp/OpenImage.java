package builderb0y.notgimp;

import java.io.File;
import java.util.function.Predicate;

import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.tools.Tool.ToolType;

public class OpenImage {

	public MainWindow
		mainWindow;
	public SimpleObjectProperty<File>
		file = new SimpleObjectProperty<>();
	public BorderPane
		mainPane = new BorderPane(),
		leftPane = new BorderPane(),
		layersAndButtons = new BorderPane(),
		sourcePane = new BorderPane();
	public ColorSelector
		colorPicker = new ColorSelector(this);
	public TreeView<Layer>
		layerTree = new TreeView<>();
	public HBox
		layerButtons = new HBox();
	public MenuButton
		addLayerButton = new MenuButton("Add");
	public MenuItem
		addLayerParentButton = new MenuItem("Wrap with new parent"),
		addLayerAboveButton  = new MenuItem("Add layer above"),
		addChildLayerButton  = new MenuItem("Add child layer"),
		addLayerBelowButton  = new MenuItem("Add layer below");
	public Button
		removeLayerButton = new Button("Remove");
	public Button
		duplicateLayerButton = new Button("Duplicate");
	public SplitPane
		layersAndTools = new SplitPane();
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
		this.leftPane.setBottom(this.colorPicker.mainPane);
		this.mainPane.setLeft(this.leftPane);
		this.layersAndTools.setOrientation(Orientation.VERTICAL);
		this.addLayerParentButton.setOnAction(this::addParentLayer);
		this.addLayerAboveButton.setOnAction(this::addLayerAbove);
		this.addChildLayerButton.setOnAction(this::addChildLayer);
		this.addLayerBelowButton.setOnAction(this::addLayerBelow);
		this.removeLayerButton.setOnAction(this::removeLayer);
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
		this.addLayerButton.setPopupSide(Side.TOP);
		this.addLayerButton.getItems().addAll(
			this.addLayerParentButton,
			this.addLayerAboveButton,
			this.addChildLayerButton,
			this.addLayerBelowButton
		);
		this.duplicateLayerButton.setOnAction(this::duplicateLayer);
		this.layerButtons.getChildren().addAll(this.addLayerButton, this.removeLayerButton, this.duplicateLayerButton);
		this.layersAndButtons.setCenter(this.layerTree);
		this.layersAndButtons.setBottom(this.layerButtons);
		this.layersAndTools.getItems().add(this.layersAndButtons);
		this.sourcePane.centerProperty().bind(
			this.layerTree.getSelectionModel().selectedItemProperty().map(
				(TreeItem<Layer> item) -> item.getValue().sources.tabPane
			)
		);
		this.layersAndTools.getItems().add(this.sourcePane);
		this.mainPane.setRight(this.layersAndTools);
		this.mainPane.setCenter(this.imageDisplay.display.getRootPane());
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
					return object != null ? object.name : "";
				}
			};
			return new TextFieldTreeCell<>(converter);
		});
		this.colorPicker.currentColor.any.addListener((Observable observable) -> {
			ToolType type = this.mainWindow.currentTool.get();
			if (type != null) {
				type.getTool(this.layerTree.getSelectionModel().getSelectedItem().getValue()).colorChanged();
			}
		});
		this.imageDisplay.init();
		this.colorPicker.init();
	}

	public @Nullable TreeItem<Layer> findLayer(String name) {
		TreeItem<Layer> root = this.layerTree.getRoot();
		if (root == null) return null; //still initializing.
		return findLayer(root, (TreeItem<Layer> item) -> name.equals(item.getValue().name));
	}

	public static @Nullable TreeItem<Layer> findLayer(TreeItem<Layer> root, Predicate<TreeItem<Layer>> predicate) {
		if (predicate.test(root)) {
			return root;
		}
		else {
			for (TreeItem<Layer> child : root.getChildren()) {
				TreeItem<Layer> found = findLayer(child, predicate);
				if (found != null) return found;
			}
			return null;
		}
	}

	public TreeItem<Layer> createTreeItem(Layer layer) {
		RadioButton showing = new RadioButton();
		showing.setGraphic(layer.thumbnailView);
		showing.setText(" ");
		showing.setToggleGroup(this.showingLayer);
		showing.setUserData(layer);
		showing.setSelected(true);
		return new TreeItem<>(layer, showing);
	}

	public void initFirstLayer(Layer layer) {
		this.layerTree.setRoot(this.createTreeItem(layer));
		this.layerTree.getSelectionModel().select(0);
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
	}

	public void removeLayer(ActionEvent event) {
		TreeItem<Layer> toRemove = this.layerTree.getSelectionModel().getSelectedItem();
		toRemove.getParent().getChildren().remove(toRemove);
		this.layerTree.getSelectionModel().select(0);
		if (toRemove.getValue() == this.showingLayerProperty.getValue()) {
			((RadioButton)(this.layerTree.getRoot().getGraphic())).setSelected(true);
		}
		History.onLayerDeleted(toRemove);
	}

	public void duplicateLayer(ActionEvent event) {
		TreeItem<Layer> toDuplicate = this.layerTree.getSelectionModel().getSelectedItem();
		Layer duplicate = new Layer(toDuplicate.getValue());
		TreeItem<Layer> item = this.createTreeItem(duplicate);
		ObservableList<TreeItem<Layer>> children = toDuplicate.getParent().getChildren();
		children.add(children.indexOf(toDuplicate), item);
		this.layerTree.getSelectionModel().select(item);
	}
}