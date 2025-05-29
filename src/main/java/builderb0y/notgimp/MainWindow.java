package builderb0y.notgimp;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
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
import builderb0y.notgimp.sources.ManualLayerSource;
import builderb0y.notgimp.tools.MoveTool;
import builderb0y.notgimp.tools.SourcelessTool;
import builderb0y.notgimp.tools.Tool;
import builderb0y.notgimp.tools.Tool.Selection;

public class MainWindow {

	public Stage
		stage;
	public SplitPane
		rootPane = new SplitPane();
	public BorderPane
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
	public CheckMenuItem
		viewTilingMenuItem = new CheckMenuItem("Tile view");
	public TabPane
		openImages = new TabPane();
	public ColorSelector
		colorPicker = new ColorSelector(this);
	public Histogram
		histogram = new Histogram(this);
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
		this.viewMenu.getItems().addAll(this.viewLightThemeMenuItem, this.viewDarkThemeMenuItem, this.viewTilingMenuItem);
		this.menuBar.getMenus().addAll(this.fileMenu, this.editMenu, this.viewMenu);
		this.leftPane.setTop(this.menuBar);
		this.leftPane.setCenter(this.histogram.rootPane);
		this.leftPane.setBottom(this.colorPicker.mainPane);
		this.rootPane.getItems().add(this.leftPane);
		this.openImages.setTabDragPolicy(TabDragPolicy.REORDER);
		this.openImages.setTabMinHeight(48.0D);
		this.openImages.setTabMaxHeight(48.0D);
		this.rootPane.getItems().add(this.openImages);
		this.rootPane.getDividers().getFirst().setPosition(0.0D);
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
		this.editUndoMenuItem.setOnAction((ActionEvent _) -> this.getCurrentImage().getSelectedLayer().history.undo());
		this.editRedoMenuItem.setOnAction((ActionEvent _) -> this.getCurrentImage().getSelectedLayer().history.redo());
		this.viewLightThemeMenuItem.setOnAction((ActionEvent _) -> {
			this.styleSheetName.set("assets/themes/light.css");
		});
		this.viewDarkThemeMenuItem.setOnAction((ActionEvent _) -> {
			this.styleSheetName.set("assets/themes/dark.css");
		});
		this.rootPane.getStylesheets().add("assets/themes/dark.css");
		this.styleSheetName.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			this.rootPane.getStylesheets().remove(oldValue);
			this.rootPane.getStylesheets().add(newValue);
		});
		this.viewTilingMenuItem.disableProperty().bind(noOpenFile);
		this.viewTilingMenuItem.selectedProperty().bindBidirectional(new SimpleObjectProperty<>() {

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
					History.onImageClosed((OpenImage)(tab.getUserData()));
				}
			}
		});
		this.openImages.setOnScroll((ScrollEvent event) -> {
			Node header = this.openImages.lookup(".tab-header-area");
			if (header != null && header.contains(event.getX(), event.getY())) {
				if      (event.getDeltaY() < 0.0D) this.openImages.getSelectionModel().selectNext();
				else if (event.getDeltaY() > 0.0D) this.openImages.getSelectionModel().selectPrevious();
			}
		});
		this.stage.titleProperty().bind(this.openImages.getSelectionModel().selectedItemProperty().map(Tab::getText).orElse("Not Gimp"));
	}

	public void show() {
		Scene scene = new Scene(this.rootPane, 1536, 896);
		scene.setOnKeyPressed((KeyEvent event) -> {
			OpenImage openImage = this.getCurrentImage();
			boolean shift = event.isShiftDown();
			if (event.isControlDown()) {
				switch (event.getCode()) {
					case Z -> {
						if (openImage != null) {
							History history = openImage.getSelectedLayer().history;
							if (shift) history.redo();
							else history.undo();
						}
					}
					case C -> {
						if (openImage != null) {
							Layer layer = openImage.getSelectedLayer();
							if (layer != null) {
								HDRImage image = layer.image;
								if (layer.sources.getCurrentSource() instanceof ManualLayerSource source) {
									Selection selection = new Selection();
									Tool<?> tool = source.toolWithoutColorPicker.get();
									if (tool != null && tool.getSelection(selection)) {
										HDRImage image2 = new HDRImage(selection.maxX - selection.minX + 1, selection.maxY - selection.minY + 1);
										for (int y = selection.minY; y <= selection.maxY; y++) {
											for (int x = selection.minX; x <= selection.maxX; x++) {
												if (x >= 0 && x < image.width && y >= 0 && y < image.height) {
													image2.setColor(x - selection.minX, y - selection.minY, image.getPixel(x, y));
												}
											}
										}
										image = image2;
									}
								}
								Clipboard.getSystemClipboard().setContent(
									Map.of(
										HDRImage.HDR_DATA_FORMAT, image,
										DataFormat.IMAGE, image.toJfxImage()
									)
								);
							}
						}
					}
					case V -> {
						if (event.isShiftDown()) {
							this.openFromClipboard();
						}
						else if (event.isAltDown()) {
							this.pasteFromClipboardToNewLayer();
						}
						else {
							this.pasteFromClipboardSameLayer();
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
					if (openImage != null) {
						SourcelessTool<?> tool = openImage.toolWithColorPicker.get();
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
		NamedImage[] images = this.getClipboardImage();
		if (images != null) {
			for (NamedImage image : images) {
				OpenImage openImage = new OpenImage(this);
				Layer layer = new Layer(openImage, image.name, new HDRImage(image.image));
				openImage.initFirstLayer(layer, false);
				this.addOpenImage(image.name, openImage);
			}
		}
	}

	public void pasteFromClipboardToNewLayer() {
		OpenImage openImage = this.getCurrentImage();
		if (openImage == null) {
			this.openFromClipboard();
			return;
		}
		NamedImage[] images = this.getClipboardImage();
		if (images != null) {
			TreeItem<Layer> selected = openImage.getSelectedLayer().item;
			List<TreeItem<Layer>> layers = new ArrayList<>(images.length);
			for (NamedImage image : images) {
				Layer layer = new Layer(openImage, image.name, new HDRImage(image.image));
				layers.add(layer.item);
				openImage.addToMap(layer);
			}
			selected.getChildren().addAll(0, layers);
			selected.setExpanded(true);
			for (TreeItem<Layer> layer : layers) {
				layer.getValue().init(false);
			}
		}
	}

	public void pasteFromClipboardSameLayer() {
		OpenImage openImage = this.getCurrentImage();
		if (openImage == null) {
			this.openFromClipboard();
			return;
		}
		NamedImage[] images = this.getClipboardImage();
		if (images != null) {
			if (images.length == 1) {
				Layer layer = openImage.getSelectedLayer();
				if (layer.sources.getCurrentSource() instanceof ManualLayerSource manual) {
					Tool<?> tool = manual.toolWithoutColorPicker.get();
					if (tool != null) {
						tool.enter();
					}
					manual.toolWithoutColorPicker.set(manual.moveTool);
					HDRImage copied = images[0].image;
					int width = Math.min(copied.width, manual.toollessImage.width);
					int height = Math.min(copied.height, manual.toollessImage.height);
					manual.moveTool.work = new MoveTool.Work(copied);
					manual.moveTool.work.x2 = width - 1;
					manual.moveTool.work.y2 = height - 1;

					double zoom = openImage.imageDisplay.zoom.getValue();
					int hoveredX = (int)(Math.floor((openImage.imageDisplay.lastMouseX - openImage.imageDisplay.offsetX) * zoom));
					int hoveredY = (int)(Math.floor((openImage.imageDisplay.lastMouseY - openImage.imageDisplay.offsetY) * zoom));
					manual.moveTool.work.offsetX = hoveredX - (width >> 1);
					manual.moveTool.work.offsetY = hoveredY - (height >> 1);

					layer.requestRedraw();
				}
				else {
					System.out.println("Selected layer is not manual.");
				}
			}
			else {
				System.out.println("Pasting requires exactly one image on clipboard, but there were " + images.length);
			}
		}
	}

	public static record NamedImage(String name, HDRImage image) {}

	public NamedImage @Nullable [] getClipboardImage() {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		HDRImage hdr = (HDRImage)(clipboard.getContent(HDRImage.HDR_DATA_FORMAT));
		if (hdr != null) return new NamedImage[] { new NamedImage("Pasted image", hdr) };
		Image image = clipboard.getImage();
		if (image != null) return new NamedImage[] { new NamedImage("Pasted image", new HDRImage(image)) };
		List<File> files = clipboard.getFiles();
		if (files != null) {
			return (
				files
				.stream()
				.filter((File file) -> switch (Util.getExtension(file).toLowerCase(Locale.ROOT)) {
					case "png", "jpg", "jpeg" -> true;
					default -> {
						System.err.println("Not an image file: " + file.getAbsolutePath());
						yield false;
					}
				})
				.map((File file) -> {
					try {
						return new NamedImage(file.getName(), new HDRImage(new Image(new FileInputStream(file))));
					}
					catch (FileNotFoundException exception) {
						System.err.println("File not found: " + file.getAbsolutePath());
						return null;
					}
				})
				.filter(Objects::nonNull)
				.toArray(NamedImage[]::new)
			);
		}
		System.out.println("No image on clipboard.");
		return null;
	}

	public void fileNew(ActionEvent event) {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(this.stage);
		dialog.setTitle("New image");
		Spinner<Integer> width = Util.setupSpinner(new Spinner<>(1, 32767, 16), 80);
		Spinner<Integer> height = Util.setupSpinner(new Spinner<>(1, 32767, 16), 80);
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
		thumbnail.imageProperty().bind(openImage.layerTree.rootProperty().flatMap((TreeItem<Layer> item) -> item.getValue().thumbnail));
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