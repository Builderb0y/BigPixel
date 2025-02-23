package builderb0y.notgimp;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.controlsfx.dialog.ExceptionDialog;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.ColorSelector.SavedColor;
import builderb0y.notgimp.HDRImage.SaveProgress;
import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.json.JsonReader;
import builderb0y.notgimp.tools.SourcelessTool;

public class MainWindow {

	public Stage
		stage;
	public BorderPane
		rootPane = new BorderPane(),
		leftPane = new BorderPane();
	public MenuBar
		menuBar  = new MenuBar();
	public Menu
		fileMenu = new Menu("File"),
		editMenu = new Menu("Edit"),
		viewMenu = new Menu("View");
	public MenuItem
		fileNewMenuItem = new MenuItem("New..."),
		fileOpenMenuItem = new MenuItem("Open..."),
		fileSaveMenuItem = new MenuItem("Save"),
		fileSaveAsMenuItem = new MenuItem("Save as..."),
		fileExportMenuItem = new MenuItem("Export"),
		fileExportAsMenuItem = new MenuItem("Export as..."),
		editUndoMenuItem = new MenuItem(),
		editRedoMenuItem = new MenuItem(),
		viewLightThemeMenuItem = new MenuItem("Light theme"),
		viewDarkThemeMenuItem = new MenuItem("Dark theme");
	public TabPane
		openImages = new TabPane();
	public ColorSelector
		colorPicker = new ColorSelector(this);
	public CheckBox
		tilingCheckbox = new CheckBox("Tile view");
	public SimpleObjectProperty<String>
		styleSheetName = new SimpleObjectProperty<>("assets/themes/light.css");

	public MainWindow(Stage stage) {
		this.stage = stage;
		this.fileMenu.getItems().addAll(
			this.fileNewMenuItem,
			this.fileOpenMenuItem,
			this.fileSaveMenuItem,
			this.fileSaveAsMenuItem,
			this.fileExportMenuItem,
			this.fileExportAsMenuItem
		);
		this.editMenu.getItems().addAll(
			this.editUndoMenuItem,
			this.editRedoMenuItem
		);
		this.viewMenu.getItems().addAll(this.viewLightThemeMenuItem, this.viewDarkThemeMenuItem);
		this.menuBar.getMenus().addAll(this.fileMenu, this.editMenu, this.viewMenu);
		this.leftPane.setTop(this.menuBar);
		this.leftPane.setBottom(this.colorPicker.mainPane);
		this.rootPane.setLeft(this.leftPane);
		this.openImages.setTabDragPolicy(TabDragPolicy.REORDER);
		this.openImages.setTabMinHeight(48.0D);
		this.openImages.setTabMaxHeight(48.0D);
		this.rootPane.setCenter(this.openImages);
		this.tilingCheckbox.setPadding(new Insets(4.0D));
	}

	public void init() {
		this.fileNewMenuItem.setOnAction(this::fileNew);
		this.fileOpenMenuItem.setOnAction(this::fileOpen);
		this.fileSaveMenuItem.setOnAction(this::save);
		this.fileSaveAsMenuItem.setOnAction(this::saveAs);
		this.fileExportMenuItem.setOnAction(this::fileExport);
		this.fileExportAsMenuItem.setOnAction(this::fileExportAs);
		BooleanBinding noOpenFile = this.openImages.getSelectionModel().selectedItemProperty().isNull();
		this.fileSaveAsMenuItem.disableProperty().bind(noOpenFile);
		this.fileExportAsMenuItem.disableProperty().bind(noOpenFile);
		//can't use .map() because that returns an ObservableValue<Boolean> instead of an ObservableBooleanValue.
		//can't use .isNull() because .flatMap() returns an ObservableValue<File> instead of an ObjectExpression.
		ObservableValue<File> fileValue = this.openImages.getSelectionModel().selectedItemProperty().flatMap(tab -> ((OpenImage)(tab.getUserData())).file);
		BooleanBinding noNamedFile = noOpenFile.or(new BooleanBinding() {

			{
				this.bind(fileValue);
			}

			@Override
			public boolean computeValue() {
				return fileValue.getValue() == null;
			}

			@Override
			public void dispose() {
				this.unbind(fileValue);
			}
		});
		this.fileSaveMenuItem.disableProperty().bind(noNamedFile);
		this.fileExportMenuItem.disableProperty().bind(noNamedFile);
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
		ObservableValue<String> undoText = currentAction.map((History.Entry entry) -> entry.prev).map((History.Entry entry) -> entry.next.name).map("Undo "::concat);
		ObservableValue<String> redoText = currentAction.map((History.Entry entry) -> entry.next).map((History.Entry entry) -> entry.name).map("Redo "::concat);
		this.editUndoMenuItem.textProperty().bind(undoText.orElse("Nothing to undo"));
		this.editRedoMenuItem.textProperty().bind(redoText.orElse("Nothing to redo"));
		this.editUndoMenuItem.disableProperty().bind(undoText.map((String _) -> Boolean.FALSE).orElse(Boolean.TRUE));
		this.editRedoMenuItem.disableProperty().bind(redoText.map((String _) -> Boolean.FALSE).orElse(Boolean.TRUE));
		this.editUndoMenuItem.setOnAction((ActionEvent event) -> this.getCurrentImage().getSelectedLayer().history.undo());
		this.editRedoMenuItem.setOnAction((ActionEvent event) -> this.getCurrentImage().getSelectedLayer().history.redo());
		this.viewLightThemeMenuItem.setOnAction((ActionEvent event) -> {
			this.styleSheetName.set("assets/themes/light.css");
		});
		this.viewDarkThemeMenuItem.setOnAction((ActionEvent event) -> {
			this.styleSheetName.set("assets/themes/dark.css");
		});
		this.rootPane.getStylesheets().add("assets/themes/dark.css");
		this.styleSheetName.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			this.rootPane.getStylesheets().remove(oldValue);
			this.rootPane.getStylesheets().add(newValue);
		});
		this.tilingCheckbox.disableProperty().bind(noOpenFile);
		this.tilingCheckbox.selectedProperty().bindBidirectional(new SimpleObjectProperty<>() {

			{
				MainWindow.this.openImages.getSelectionModel().selectedItemProperty().addListener(
					Util.change(this::fireValueChangedEvent)
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
		this.colorPicker.init();
		this.openImages.getTabs().addListener((Change<? extends Tab> change) -> {
			while (change.next()) {
				for (Tab tab : change.getRemoved()) {
					History.onImageClosed(((OpenImage)(tab.getUserData())));
				}
			}
		});
		this.stage.titleProperty().bind(this.openImages.getSelectionModel().selectedItemProperty().map(Tab::getText).orElse("Not Gimp"));
	}

	public void show() {
		Scene scene = new Scene(this.rootPane, 1536, 896);
		scene.setOnKeyPressed((KeyEvent event) -> {
			OpenImage openImage = this.getCurrentImage();
			if (openImage == null) return; // || scene.getFocusOwner() != openImage.imageDisplay.display.canvas) return;
			boolean shift = event.isShiftDown();
			if (event.isControlDown()) {
				switch (event.getCode()) {
					case Z -> {
						History history = openImage.getSelectedLayer().history;
						if (shift) history.redo();
						else history.undo();
					}
					case C -> {
						Layer layer = openImage.getSelectedLayer();
						if (layer != null) {
							Clipboard.getSystemClipboard().setContent(
								Map.of(DataFormat.IMAGE, layer.image.toJfxImage())
							);
						}
					}
					case V -> {
						if (event.isShiftDown()) {
							this.openFromClipboard();
						}
					}
					case N -> {
						this.fileNew(null);
					}
					case null, default -> {}
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
					OpenImage image = openImage;
					if (image != null) {
						SourcelessTool<?> tool = image.toolWithColorPicker.get();
						if (tool != null) tool.keyPressed(event.getCode());
					}
				}
			}
		});
		this.stage.setScene(scene);
		this.stage.getIcons().add(new Image(NotGimp.class.getClassLoader().getResourceAsStream("assets/icon.png")));
		this.stage.show();
	}

	public void swapColor(int index, boolean shift) {
		SavedColor color = this.colorPicker.savedColors[index];
		if (shift) color.save();
		else color.apply();
	}

	public @Nullable OpenImage getCurrentImage() {
		Tab tab = this.openImages.getSelectionModel().getSelectedItem();
		return tab != null ? ((OpenImage)(tab.getUserData())) : null;
	}

	public void openFromClipboard() {
		Object content = Clipboard.getSystemClipboard().getContent(DataFormat.IMAGE);
		if (content instanceof Image image) {
			OpenImage openImage = new OpenImage(this);
			Layer layer = new Layer(openImage, "Pasted image", new HDRImage(image));
			openImage.initFirstLayer(layer, false);
			this.addOpenImage("Pasted image", openImage);
		}
		else {
			System.err.println("No image in clipboard: " + content);
		}
	}

	public void fileNew(ActionEvent event) {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(this.stage);
		dialog.setTitle("New image");
		Spinner<Integer> width = Util.setupSpinner(new Spinner<>(1, 32767, 16), 80);
		width.setEditable(true);
		Spinner<Integer> height = Util.setupSpinner(new Spinner<>(1, 32767, 16), 80);
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
			image.initFirstLayer(layer, false);
			this.addOpenImage(nameField.getText(), image);
		}
	}

	public void fileOpen(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().addAll(
			new ExtensionFilter("NotGimp files", "*.png", "*.jpg", "*.jpeg", "*.json"),
			new ExtensionFilter("All files", "*.*")
		);
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		File file = fileChooser.showOpenDialog(this.stage);
		if (file != null) {
			this.doOpen(file);
		}
	}

	public void doOpen(File file) {
		OpenImage openImage;
		try {
			if (Util.getExtension(file).equals("json")) {
				String contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
				JsonMap map = new JsonReader(contents).readValueAfterWhitespace().asMap();
				SaveVersions.process(map);
				openImage = new OpenImage(this);
				openImage.load(map);
			}
			else {
				Image image = new Image(new FileInputStream(file));
				Exception exception = image.getException();
				if (exception != null) throw exception;
				openImage = new OpenImage(this);
				Layer layer = new Layer(openImage, file.getName(), new HDRImage(image));
				openImage.initFirstLayer(layer, false);
			}
		}
		catch (Exception exception) {
			exception.printStackTrace();
			new ExceptionDialog(exception).showAndWait();
			return;
		}
		openImage.file.set(file);
		this.addOpenImage(file.getName(), openImage);
	}

	public void save(ActionEvent event) {
		OpenImage openImage = this.getCurrentImage();
		if (openImage == null) return;
		File file = openImage.file.get();
		if (file != null) {
			file = Util.changeExtension(file, "json");
			this.doSave(openImage, file);
		}
	}

	public void saveAs(ActionEvent event) {
		OpenImage openImage = this.getCurrentImage();
		if (openImage == null) return;
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("JSON files", "*.json"));
		if (openImage.file.get() != null) {
			fileChooser.setInitialDirectory(openImage.file.get().getParentFile());
		}
		File file = fileChooser.showSaveDialog(this.stage);
		if (file == null) {
			return;
		}
		file = Util.changeExtension(file, "json");
		openImage.file.set(file);
		this.doSave(openImage, file);
	}

	public void doSave(OpenImage openImage, File file) {
		try {
			JsonMap map = openImage.save();
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
				map.write(writer, 0);
			}
		}
		catch (Exception exception) {
			exception.printStackTrace();
			new ExceptionDialog(exception).showAndWait();
		}
	}

	public void fileExport(ActionEvent event) {
		OpenImage openImage = this.getCurrentImage();
		if (openImage == null) return;
		File file = openImage.file.get();
		if (file != null) {
			file = Util.changeExtension(file, "png");
			try {
				HDRImage hdrImage = openImage.layerTree.getRoot().getValue().image;
				BufferedImage bufferedImage = hdrImage.toAwtImage(openImage.animationSource);
				byte[] bytes = HDRImage.toPngByteArray(bufferedImage, new SaveProgress());
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
		BufferedImage bufferedImage = image.toAwtImage(openImage.animationSource);
		SaveProgress progress = new SaveProgress();
		CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> HDRImage.toPngByteArray(bufferedImage, progress));
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("PNG files", "*.png"));
		if (openImage.file.get() != null) {
			fileChooser.setInitialDirectory(openImage.file.get().getParentFile());
		}
		File file = fileChooser.showSaveDialog(this.stage);
		if (file == null) {
			progress.cancel();
			return;
		}
		file = Util.changeExtension(file, "png");
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
		Tab tab = new Tab(name, openImage.getMainNode());
		tab.setUserData(openImage);
		ImageView thumbnail = new ImageView();
		thumbnail.imageProperty().bind(openImage.layerTree.rootProperty().map((TreeItem<Layer> item) -> item.getValue().thumbnail));
		HDRImage image = openImage.layerTree.getRoot().getValue().image;
		if (image.width >= image.height) thumbnail.setFitWidth(32.0D);
		else thumbnail.setFitHeight(32.0D);
		thumbnail.setPreserveRatio(true);
		tab.setGraphic(thumbnail);
		tab.textProperty().bind(
			this
			.openImages
			.getSelectionModel()
			.selectedItemProperty()
			.flatMap((Tab selected) -> selected == tab ? openImage.layerTree.rootProperty() : null)
			.flatMap((TreeItem<Layer> layer) -> layer.getValue().name)
		);
		int index = this.openImages.getSelectionModel().getSelectedIndex() + 1;
		this.openImages.getTabs().add(index, tab);
		this.openImages.getSelectionModel().select(index);
		openImage.init();
	}
}