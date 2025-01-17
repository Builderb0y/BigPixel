package builderb0y.notgimp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.css.CssParser;
import javafx.css.Stylesheet;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.controlsfx.dialog.ExceptionDialog;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.ColorSelector.SavedColor;
import builderb0y.notgimp.HDRImage.SaveProgress;
import builderb0y.notgimp.tools.Tool;
import builderb0y.notgimp.tools.Tool.ToolType;

public class MainWindow {

	public Stage
		stage;
	public BorderPane
		rootPane = new BorderPane();
	public MenuBar
		menuBar  = new MenuBar();
	public Menu
		fileMenu = new Menu("File"),
		editMenu = new Menu("Edit"),
		viewMenu = new Menu("View");
	public MenuItem
		fileNewMenuItem = new MenuItem("New..."),
		fileOpenMenuItem = new MenuItem("Open..."),
		fileExportMenuItem = new MenuItem("Export"),
		fileExportAsMenuItem = new MenuItem("Export as..."),
		editUndoMenuItem = new MenuItem(),
		editRedoMenuItem = new MenuItem(),
		viewLightThemeMenuItem = new MenuItem("Light theme"),
		viewDarkThemeMenuItem = new MenuItem("Dark theme");
	public TabPane
		openImages = new TabPane();
	public SimpleObjectProperty<@Nullable ToolType>
		currentTool = new SimpleObjectProperty<>();
	public CheckBox
		tilingCheckbox = new CheckBox("Tile view");
	public Label
		imageSizeLabel = new Label();
	public static record LabelComponents(Tab tab, TreeItem<Layer> layer, ToolType type) {

		public static final SimpleStringProperty
			empty  = new SimpleStringProperty(""),
			noTool = new SimpleStringProperty("No tool active");
	}
	public ObservableValue<String>
		toolInfoText = new MultiBinding<LabelComponents>(this.currentTool, this.openImages.getSelectionModel().selectedItemProperty()) {

			@Override
			public LabelComponents computeValue() {
				Tab tab = MainWindow.this.openImages.getSelectionModel().getSelectedItem();
				if (tab != null) {
					TreeItem<Layer> layer = ((OpenImage)(tab.getUserData())).layerTree.getSelectionModel().getSelectedItem();
					if (layer != null) {
						ToolType type = MainWindow.this.currentTool.get();
						return new LabelComponents(tab, layer, type);
					}
				}
				return new LabelComponents(tab, null, null);
			}
		}
		.flatMap((LabelComponents components) -> {
			if (components.type != null) {
				return components.type.getTool(components.layer.getValue()).labelText;
			}
			if (components.layer != null) {
				return LabelComponents.noTool;
			}
			return LabelComponents.empty;
		});
	public Label
		toolInfoLabel = new Label();
	public HBox
		infoPane = new HBox();
	public SimpleObjectProperty<String>
		styleSheetName = new SimpleObjectProperty<>("assets/themes/light.css");
	public ObservableValue<Stylesheet>
		parsedStylesheet = this.styleSheetName.map((String name) -> {
			try {
				return new CssParser().parse(MainWindow.class.getClassLoader().getResource(name));
			}
			catch (Exception exception) {
				exception.printStackTrace();
				return null;
			}
		});
	public static record CheckerboardColors(Color light, Color dark) {

		public static final CheckerboardColors DEFAULT = new CheckerboardColors(Color.LIGHTGRAY, Color.GRAY);
	}
	/*
	public ObservableValue<CheckerboardColors>
		checkerboardColors = this.parsedStylesheet.map((Stylesheet stylesheet) -> {
			Color light = null, dark = null;
			for (Rule rule : stylesheet.getRules()) {
				for (Selector selector : rule.getSelectors()) {
					if (selector instanceof SimpleSelector simple) {
						for (StyleClass styleClass : simple.getStyleClassSet()) {
							if (styleClass.getStyleClassName().equals("canvasses")) {
								for (Declaration declaration : rule.getDeclarations()) {
									if (declaration.getProperty().equals("-checkerboard-light")) {
										light = (Color)(declaration.getParsedValue().getValue());
									}
									else if (declaration.getProperty().equals("-checkerboard-dark")) {
										dark = (Color)(declaration.getParsedValue().getValue());
									}
								}
							}
						}
					}
				}
			}
			if (light != null && dark != null) {
				return new CheckerboardColors(light, dark);
			}
			return null;
		})
		.orElse(CheckerboardColors.DEFAULT);
	*/

	public MainWindow(Stage stage) {
		this.stage = stage;
		this.openImages.getSelectionModel().clearSelection();
		this.fileNewMenuItem.setOnAction(this::fileNew);
		this.fileOpenMenuItem.setOnAction(this::fileOpen);
		this.fileExportMenuItem.setOnAction(this::fileExport);
		this.fileExportAsMenuItem.setOnAction(this::fileExportAs);
		BooleanBinding noOpenFile = this.openImages.getSelectionModel().selectedItemProperty().isNull();
		this.fileExportAsMenuItem.disableProperty().bind(noOpenFile);
		//can't use .map() because that returns an ObservableValue<Boolean> instead of an ObservableBooleanValue.
		//can't use .isNull() because .flatMap() returns an ObservableValue<File> instead of an ObjectExpression.
		ObservableValue<File> fileValue = this.openImages.getSelectionModel().selectedItemProperty().flatMap(tab -> ((OpenImage)(tab.getUserData())).file);
		this.fileExportMenuItem.disableProperty().bind(noOpenFile.or(new BooleanBinding() {

			{ this.bind(fileValue); }

			@Override
			public boolean computeValue() {
				return fileValue.getValue() == null;
			}

			@Override
			public void dispose() {
				this.unbind(fileValue);
			}
		}));
		this.fileMenu.getItems().addAll(
			this.fileNewMenuItem,
			this.fileOpenMenuItem,
			this.fileExportMenuItem,
			this.fileExportAsMenuItem
		);
		ObservableValue<History.Entry> currentAction = (
			this
			.openImages
			.getSelectionModel()
			.selectedItemProperty()
			.map(Tab::getUserData)
			.map(OpenImage.class::cast)
			.flatMap((OpenImage image) -> image.layerTree.getSelectionModel().selectedItemProperty())
			.flatMap((TreeItem<Layer> item) -> item.getValue().history.currentEntry)
		);
		this.editUndoMenuItem.textProperty().bind(currentAction.map((History.Entry entry) -> entry.name).map("Undo "::concat).orElse("Nothing to undo"));
		this.editRedoMenuItem.textProperty().bind(currentAction.map((History.Entry entry) -> entry.next).map((History.Entry entry) -> entry.name).map("Redo "::concat).orElse("Nothing to redo"));
		this.editMenu.getItems().addAll(
			this.editUndoMenuItem,
			this.editRedoMenuItem
		);
		this.viewLightThemeMenuItem.setOnAction((ActionEvent event) -> {
			this.styleSheetName.set("assets/themes/light.css");
		});
		this.viewDarkThemeMenuItem.setOnAction((ActionEvent event) -> {
			this.styleSheetName.set("assets/themes/dark.css");
		});
		this.rootPane.getStylesheets().add("assets/themes/light.css");
		this.styleSheetName.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			this.rootPane.getStylesheets().remove(oldValue);
			this.rootPane.getStylesheets().add(newValue);
		});
		this.tilingCheckbox.disableProperty().bind(noOpenFile);
		this.tilingCheckbox.selectedProperty().bindBidirectional(new SimpleObjectProperty<>() {

			{
				MainWindow.this.openImages.getSelectionModel().selectedItemProperty().addListener(
					Util.change((Tab newValue) -> this.fireValueChangedEvent())
				);
			}

			@Override
			public Boolean get() {
				Tab tab = MainWindow.this.openImages.getSelectionModel().getSelectedItem();
				return tab != null && ((OpenImage)(tab.getUserData())).wrap.get();
			}

			@Override
			public void set(Boolean newValue) {
				Tab tab = MainWindow.this.openImages.getSelectionModel().getSelectedItem();
				if (tab != null) {
					((OpenImage)(tab.getUserData())).wrap.set(newValue);
				}
			}
		});
		this.viewMenu.getItems().addAll(this.viewLightThemeMenuItem, this.viewDarkThemeMenuItem);
		this.menuBar.getMenus().addAll(this.fileMenu, this.editMenu, this.viewMenu);
		this.rootPane.setTop(this.menuBar);
		this.openImages.setTabDragPolicy(TabDragPolicy.REORDER);
		this.openImages.getTabs().addListener((Change<? extends Tab> change) -> {
			while (change.next()) {
				for (Tab tab : change.getRemoved()) {
					History.onImageClosed(((OpenImage)(tab.getUserData())));
				}
			}
		});
		this.rootPane.setCenter(this.openImages);
		this.imageSizeLabel.textProperty().bind(
			this.openImages.getSelectionModel().selectedItemProperty().map((Tab tab) -> {
				TreeItem<Layer> root = ((OpenImage)tab.getUserData()).layerTree.getRoot();
				if (root != null) {
					HDRImage image = root.getValue().image;
					return "" + image.width + 'x' + image.height;
				}
				else {
					return "";
				}
			})
			.orElse("No image loaded")
		);
		this.toolInfoLabel.textProperty().bind(this.toolInfoText);
		this.tilingCheckbox.setPadding(new Insets(4.0D));
		this.imageSizeLabel.setPadding(new Insets(4.0D));
		this.toolInfoLabel.setPadding(new Insets(4.0D));
		this.infoPane.getChildren().addAll(
			this.tilingCheckbox,
			new Separator(Orientation.VERTICAL),
			this.imageSizeLabel,
			new Separator(Orientation.VERTICAL),
			this.toolInfoLabel
		);
		this.rootPane.setBottom(this.infoPane);
		stage.titleProperty().bind(this.openImages.getSelectionModel().selectedItemProperty().map(Tab::getText).orElse("Not Gimp"));
	}

	public void init() {
		Scene scene = new Scene(this.rootPane, 1536, 896);
		scene.setOnKeyPressed((KeyEvent event) -> {
			boolean shift = event.isShiftDown();
			if (event.isControlDown()) {
				if (event.getCode() == KeyCode.Z) {
					History history = ((OpenImage)(this.openImages.getSelectionModel().getSelectedItem().getUserData())).layerTree.getSelectionModel().getSelectedItem().getValue().history;
					if (shift) history.redo();
					else history.undo();
				}
			}
			else switch (event.getCode()) {
				case DIGIT1 -> this.swapColor(0, shift);
				case DIGIT2 -> this.swapColor(1, shift);
				case DIGIT3 -> this.swapColor(2, shift);
				case DIGIT4 -> this.swapColor(3, shift);
				case DIGIT5 -> this.swapColor(4, shift);
				case DIGIT6 -> this.swapColor(5, shift);
				case DIGIT7 -> this.swapColor(6, shift);
				case DIGIT8 -> this.swapColor(7, shift);
				case DIGIT9 -> this.swapColor(8, shift);
				case DIGIT0 -> this.swapColor(9, shift);
				default -> {
					OpenImage image = this.getCurrentImage();
					if (image != null) {
						Layer layer = image.layerTree.getSelectionModel().getSelectedItem().getValue();
						ToolType type = this.currentTool.get();
						if (type != null) {
							Tool<?> tool = type.getTool(layer);
							if (tool != null) {
								tool.keyPressed(layer, event.getCode());
							}
						}
					}
				}
			}
		});
		this.stage.setScene(scene);
		this.stage.show();
	}

	public void swapColor(int index, boolean shift) {
		OpenImage image = this.getCurrentImage();
		if (image != null) {
			SavedColor color = image.colorPicker.savedColors[index];
			if (shift) color.save();
			else color.apply();
		}
	}

	public @Nullable OpenImage getCurrentImage() {
		Tab tab = this.openImages.getSelectionModel().getSelectedItem();
		return tab != null ? ((OpenImage)(tab.getUserData())) : null;
	}

	public void fileNew(ActionEvent event) {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(this.stage);
		dialog.setTitle("New image");
		Spinner<Integer> width = Util.setupSpinner(new Spinner<>(1, 32767, 16));
		width.setEditable(true);
		Spinner<Integer> height = Util.setupSpinner(new Spinner<>(1, 32767, 16));
		height.setEditable(true);
		Label widthLabel = new Label("Width: ");
		Label heightLabel = new Label("Height: ");
		TextField nameField = new TextField("New image");
		GridPane gridPane = new GridPane();
		gridPane.add(widthLabel, 0, 0);
		gridPane.add(width, 1, 0);
		gridPane.add(heightLabel, 0, 1);
		gridPane.add(height, 1, 1);
		gridPane.add(nameField, 0, 2, 2, 1);
		dialog.getDialogPane().setContent(gridPane);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		if (dialog.showAndWait().orElse(null) == ButtonType.OK) {
			OpenImage image = new OpenImage(this);
			Layer layer = new Layer(image, nameField.getText(), width.getValue(), height.getValue());
			image.initFirstLayer(layer);
			this.addOpenImage(nameField.getText(), image);
		}
	}

	public void fileOpen(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().addAll(
			new ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg"),
			new ExtensionFilter("All files", "*.*")
		);
		File file = fileChooser.showOpenDialog(this.stage);
		if (file != null) {
			this.doOpen(file);
		}
	}

	public void doOpen(File file) {
		OpenImage openImage;
		try {
			Image image = new Image(new FileInputStream(file));
			Exception exception = image.getException();
			if (exception != null) throw exception;
			openImage = new OpenImage(this);
			openImage.file.set(file);
			Layer layer = new Layer(openImage, file.getName(), new HDRImage(image));
			openImage.initFirstLayer(layer);
		}
		catch (Exception exception) {
			exception.printStackTrace();
			new ExceptionDialog(exception).showAndWait();
			return;
		}
		this.addOpenImage(file.getName(), openImage);
	}

	public void fileExport(ActionEvent event) {
		Tab tab = this.openImages.getSelectionModel().getSelectedItem();
		if (tab == null) return;
		OpenImage openImage = ((OpenImage)(tab.getUserData()));
		File file = openImage.file.get();
		if (file != null) {
			file = Util.changeExtension(file, "png");
			try {
				byte[] bytes = openImage.layerTree.getRoot().getValue().image.toPngByteArray(new SaveProgress());
				try (FileOutputStream stream = new FileOutputStream(file)) {
					stream.write(bytes);
				}
			}
			catch (Exception exception) {
				exception.printStackTrace();
				new ExceptionDialog(exception).showAndWait();
			}
		}
	}

	public void fileExportAs(ActionEvent event) {
		Tab tab = this.openImages.getSelectionModel().getSelectedItem();
		if (tab == null) return;
		OpenImage openImage = ((OpenImage)(tab.getUserData()));
		Layer layer = openImage.layerTree.getRoot().getValue();
		HDRImage image = layer.image;
		SaveProgress progress = new SaveProgress();
		CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> image.toPngByteArray(progress));
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("PNG files", "*.png"));
		File file = fileChooser.showSaveDialog(this.stage);
		if (file == null) {
			progress.cancel();
			return;
		}
		file = Util.changeExtension(file, "png");

		if (file.exists()) {
			Dialog<ButtonType> dialog = new Dialog<>();
			dialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
			if (dialog.showAndWait().orElse(null) != ButtonType.YES) {
				progress.cancel();
				return;
			}
		}
		openImage.file.set(file);

		try {
			byte[] bytes = future.join();
			if (bytes != null) {
				try (FileOutputStream stream = new FileOutputStream(file)) {
					stream.write(bytes);
				}
			}
		}
		catch (Exception exception) {
			exception.printStackTrace();
			new ExceptionDialog(exception).showAndWait();
		}
	}

	public void addOpenImage(String name, OpenImage openImage) {
		Tab tab = new Tab(name, openImage.mainPane);
		tab.setUserData(openImage);
		ImageView thumbnail = new ImageView();
		thumbnail.imageProperty().bind(openImage.layerTree.rootProperty().map((TreeItem<Layer> item) -> item.getValue().thumbnail));
		HDRImage image = openImage.layerTree.getRoot().getValue().image;
		if (image.width >= image.height) thumbnail.setFitWidth(16.0D);
		else thumbnail.setFitHeight(16.0D);
		thumbnail.setPreserveRatio(true);
		tab.setGraphic(thumbnail);
		int index = this.openImages.getSelectionModel().getSelectedIndex() + 1;
		this.openImages.getTabs().add(index, tab);
		this.openImages.getSelectionModel().select(index);
	}
}