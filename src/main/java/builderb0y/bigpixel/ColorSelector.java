package builderb0y.bigpixel;

import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.util.converter.FloatStringConverter;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.ColorHelper.ColorComponent;
import builderb0y.bigpixel.tools.ColorPickerTool;
import builderb0y.bigpixel.tools.Tool;

public class ColorSelector {

	public static final Border INSET_BORDER = new Border(
		new BorderStroke(
			null,
			BorderStrokeStyle.NONE,
			CornerRadii.EMPTY,
			BorderWidths.EMPTY,
			new Insets(2.0D, 4.0D, 2.0D, 4.0D)
		)
	);
	public static final int[] SNAP_POSITIONS = new int[257];
	static {
		for (int index = 0; index <= 256; index++) {
			int range = snapRange(index);
			int lo = Math.max(index - range, 0);
			int hi = Math.min(index + range, 256);
			for (int index2 = lo; index2 <= hi; index2++) {
				SNAP_POSITIONS[index2] = index;
			}
			index = hi;
		}
	}

	public static int snapRange(int value) {
		//& 7 makes it so that 0 and 256 both map to no snapping,
		//since it's easy to just drag your mouse outside the slider or gradient.
		return Math.max((Integer.numberOfTrailingZeros(value) & 7) - 3, 0);
	}

	public MainWindow mainWindow;
	public GridPane mainPane = new GridPane();
	public CanvasHelper gradient = new CanvasHelper().checkerboard().popIn().fixedSize(257.0D, 257.0D);
	public ColorHelper currentColor = new ColorHelper();
	public RectangleHelper rectangle = new RectangleHelper().checkerboard().popIn().fixedSize(96, 96);
	public CheckBox fractionalDisplay = new CheckBox("/256");
	public Button colorPickerButton = new Button();
	public ColorSlider
		red   = this.new ColorSlider(ColorComponent.RED),
		green = this.new ColorSlider(ColorComponent.GREEN),
		blue  = this.new ColorSlider(ColorComponent.BLUE),
		hue   = this.new ColorSlider(ColorComponent.HUE),
		dark  = this.new ColorSlider(ColorComponent.DARK),
		light = this.new ColorSlider(ColorComponent.LIGHT),
		alpha = this.new ColorSlider(ColorComponent.ALPHA);
	public SavedColor[] savedColors = new SavedColor[10];
	public VBox savedColorBox = new VBox();

	public ColorSelector(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		super();
		for (int index = 0; index < 10; index++) {
			this.savedColors[index] = new SavedColor(index);
			this.savedColorBox.getChildren().add(this.savedColors[index].box.getRootPane());
		}
		this.mainPane.add(this.savedColorBox, 0, 0, 1, 2);
		this.mainPane.add(this.gradient.getRootPane(), 1, 0, 1, 2);
		this.mainPane.add(this.rectangle.getRootPane(), 2, 0);
		this.colorPickerButton.setGraphic(new ImageView(ColorPickerTool.TYPE.icon()));
		AnchorPane colorPickerButtonPane = new AnchorPane(this.fractionalDisplay, this.colorPickerButton);
		AnchorPane.setLeftAnchor(this.fractionalDisplay, 4.0D);
		AnchorPane.setBottomAnchor(this.fractionalDisplay, 4.0D);
		AnchorPane.setRightAnchor(this.colorPickerButton, 4.0D);
		AnchorPane.setBottomAnchor(this.colorPickerButton, 4.0D);
		this.mainPane.add(colorPickerButtonPane, 2, 1);
		this.currentColor.any.addListener((Observable _) -> this.redrawGradient());
	}

	public static int snap(double pos) {
		return SNAP_POSITIONS[Math.clamp((int)(pos), 0, 256)];
	}

	public void init() {
		this.currentColor.any.addListener((Observable _) -> {
			OpenImage openImage = this.mainWindow.getCurrentImage();
			if (openImage != null) {
				Tool<?> tool = openImage.toolWithoutColorPicker.get();
				if (tool != null) tool.colorChanged();
			}
		});
		EventHandler<MouseEvent> handler = new RateLimitedMouseEventHandler(
			(MouseEvent event) -> {
				this.currentColor.setComponent(this.hue.component.horizontal(), snap(event.getX()) / 256.0F);
				this.currentColor.setComponent(this.hue.component.vertical(), 1.0F - snap(event.getY()) / 256.0F);
				this.currentColor.markDirty();
			}
		);
		this.gradient.display.setOnMousePressed(handler);
		this.gradient.display.setOnMouseDragged(handler);
		this.gradient.display.setOnMouseReleased(handler);
		this.colorPickerButton.setOnAction((ActionEvent _) -> {
			OpenImage image = this.mainWindow.getCurrentImage();
			if (image != null) {
				image.pickColor(this.currentColor);
			}
		});
		this.red.init();
		this.green.init();
		this.blue.init();
		this.hue.init();
		this.dark.init();
		this.light.init();
		this.alpha.init();
		this.redrawGradient();
	}

	public void redrawGradient() {
		//gradient
		PixelWriter writer = this.gradient.display.getGraphicsContext2D().getPixelWriter();
		ColorHelper helper = new ColorHelper(this.currentColor);
		ColorComponent primaryComponent = ColorComponent.HUE;
		byte[] pixels = new byte[257 * 257 * 4];
		for (int y = 0; y <= 256; y++) {
			helper.setComponent(primaryComponent.vertical(), (256 - y) / 256.0F);
			for (int x = 0; x <= 256; x++) {
				helper.setComponent(primaryComponent.horizontal(), x / 256.0F);
				int baseIndex = (y * 257 + x) << 2;
				float alpha = helper.alpha.get();
				pixels[baseIndex    ] = Util.clampB(helper.blue .get() * alpha);
				pixels[baseIndex | 1] = Util.clampB(helper.green.get() * alpha);
				pixels[baseIndex | 2] = Util.clampB(helper.red  .get() * alpha);
				pixels[baseIndex | 3] = Util.clampB(alpha);
			}
		}
		int invertX = ((int)(this.currentColor.getComponent(primaryComponent.horizontal()).get() * 256.0F));
		int invertY = ((int)(256.0F - this.currentColor.getComponent(primaryComponent.vertical()).get() * 256.0F));
		for (int x = 0; x <= 256; x++) {
			invert(pixels, x, invertY);
			if (x != invertX) {
				int tickSize = snapRange(x) << 1;
				for (int y = 0; y < tickSize; y++) {
					invert(pixels, x, y);
					invert(pixels, x, 256 - y);
				}
			}
		}
		for (int y = 0; y <= 256; y++) {
			invert(pixels, invertX, y);
			if (y != invertY) {
				int tickSize = snapRange(y) << 1;
				for (int x = 0; x < tickSize; x++) {
					invert(pixels, x, y);
					invert(pixels, 256 - x, y);
				}
			}
		}
		writer.setPixels(0, 0, 257, 257, PixelFormat.getByteBgraPreInstance(), pixels, 0, 257 << 2);

		//rectangle
		this.rectangle.setColor(
			this.currentColor.red.get(),
			this.currentColor.green.get(),
			this.currentColor.blue.get(),
			this.currentColor.alpha.get()
		);
	}

	public static void invert(byte[] pixels, int x, int y) {
		int baseIndex = (y * 257 + x) << 2;
		pixels[baseIndex    ] = (byte)(~pixels[baseIndex    ]);
		pixels[baseIndex | 1] = (byte)(~pixels[baseIndex | 1]);
		pixels[baseIndex | 2] = (byte)(~pixels[baseIndex | 2]);
		pixels[baseIndex | 3] = -1;
	}

	public class ColorSlider extends GradientSlider {

		public ColorComponent component;
		public ColorHelper scratchColor = new ColorHelper();
		public Label label = new Label();
		public TextField numberBox = new TextField();

		public ColorSlider(ColorComponent component) {
			this.checkerboard().popIn().fixedSize(257.0D, 16.0D);
			this.component = component;
			this.label.setText(component.name.charAt(0) + ":");
			this.numberBox.setPrefWidth(96.0D);
		}

		public void init() {
			this.addToGrid(ColorSelector.this.mainPane, this.component.ordinal() + 2);
			boolean[] changing = new boolean[1];
			ColorSelector.this.currentColor.getComponent(this.component).addListener(
				Util.change((Number number) -> {
					if (changing[0]) return;
					changing[0] = true;
					try {
						this.clickedPosition.set((int)(number.floatValue() * 256.0F));
					}
					finally {
						changing[0] = false;
					}
				})
			);
			this.clickedPosition.addListener(
				Util.change((Number number) -> {
					if (changing[0]) return;
					changing[0] = true;
					try {
						ColorSelector.this.currentColor.setComponent(this.component, number.floatValue() / 256.0F);
						ColorSelector.this.currentColor.markDirty();
					}
					finally {
						changing[0] = false;
					}
				})
			);
			ColorSelector.this.currentColor.getComponent(this.component).addListener(
				Util.change((Number newValue) -> {
					((TextFormatter)(this.numberBox.getTextFormatter())).setValue(newValue);
				})
			);
			this.numberBox.textFormatterProperty().bind(
				ColorSelector.this.fractionalDisplay.selectedProperty().map((Boolean fractional) -> {
					return new TextFormatter<>(
						new FloatStringConverter() {

							@Override
							public Float fromString(String string) {
								Float value = super.fromString(string);
								if (fractional) value /= 256.0F;
								value = Math.clamp(value, 0.0F, 1.0F);
								ColorSelector.this.currentColor.setComponent(ColorSlider.this.component, value);
								ColorSelector.this.currentColor.markDirty();
								return value;
							}

							@Override
							public String toString(Float value) {
								if (fractional) value *= 256.0F;
								return super.toString(value);
							}
						},
						ColorSelector.this.currentColor.getComponent(this.component).getValue()
					);
				})
			);
			EventHandler<ScrollEvent> eventHandler = (ScrollEvent event) -> {
				float value = (float)(this.numberBox.getTextFormatter().getValue());
				value *= 256.0F;
				float floorValue = (float)(Math.floor(value));
				if (floorValue == value) value += (float)(Math.signum(event.getDeltaY()));
				else value = Math.round(value);
				value /= 256.0F;
				value = Math.clamp(value, 0.0F, 1.0F);
				ColorSelector.this.currentColor.setComponent(this.component, value);
				ColorSelector.this.currentColor.markDirty();
			};
			this.display.setOnScroll(eventHandler);
			this.numberBox.setOnScroll(eventHandler);
			ColorSelector.this.currentColor.any.addListener((Observable _) -> this.redraw());
			this.redraw();
		}

		public void addToGrid(GridPane pane, int row) {
			BorderPane labelPane = new BorderPane();
			labelPane.setLeft(this.label);
			labelPane.setBorder(INSET_BORDER);
			pane.add(labelPane, 0, row);

			pane.add(this.getRootPane(), 1, row);

			BorderPane numberBoxPane = new BorderPane(this.numberBox);
			numberBoxPane.setBorder(INSET_BORDER);
			pane.add(numberBoxPane, 2, row);
		}

		@Override
		public int castPosition(double pos) {
			return snap(pos);
		}

		@Override
		public FloatVector computeColor0(int pixelPos, float fraction) {
			this.scratchColor.setComponent(this.component, fraction);
			return this.scratchColor.toFloatVector();
		}

		@Override
		public void redraw() {
			this.scratchColor.setFrom(ColorSelector.this.currentColor);
			super.redraw();
		}
	}

	public class SavedColor {

		public TriangleHelper box = new TriangleHelper().fixedSize(16.0D, 16.0D);
		public ColorHelper color = new ColorHelper();

		public SavedColor(int index) {
			switch (index) {
				case 0  -> this.color.setRGBA(0.0F, 0.0F, 0.0F, 1.0F);
				case 1  -> this.color.setRGBA(1.0F, 1.0F, 1.0F, 1.0F);
				case 2  -> this.color.setRGBA(1.0F, 0.0F, 0.0F, 1.0F);
				case 3  -> this.color.setRGBA(1.0F, 1.0F, 0.0F, 1.0F);
				case 4  -> this.color.setRGBA(0.0F, 1.0F, 0.0F, 1.0F);
				case 5  -> this.color.setRGBA(0.0F, 1.0F, 1.0F, 1.0F);
				case 6  -> this.color.setRGBA(0.0F, 0.0F, 1.0F, 1.0F);
				case 7  -> this.color.setRGBA(1.0F, 0.0F, 1.0F, 1.0F);
				default -> this.color.setRGBA(0.0F, 0.0F, 0.0F, 0.0F);
			}

			this.box.pop(this.box.display.pressedProperty());
			this.box.color.bind(this.color.rgba);
			this.box.display.setOnMouseClicked((MouseEvent event) -> {
				if (event.getButton() == MouseButton.PRIMARY) {
					this.apply();
				}
				else if (event.getButton() == MouseButton.SECONDARY) {
					this.save();
				}
			});
		}

		public void save() {
			this.color.setFrom(ColorSelector.this.currentColor);
		}

		public void apply() {
			ColorSelector.this.currentColor.setFrom(this.color);
			ColorSelector.this.currentColor.markDirty();
		}
	}
}