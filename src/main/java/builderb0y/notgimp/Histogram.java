package builderb0y.notgimp;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class Histogram {

	public MainWindow window;
	public CanvasHelper canvas = new CanvasHelper().resizeable(this::redrawCanvas).popIn();
	public CheckBox log = new CheckBox("Log Scale");
	public CheckBox alpha = new CheckBox("Alpha");
	public HBox settings = new HBox(this.log, this.alpha);
	public BorderPane rootPane = new BorderPane();

	public Histogram(MainWindow window) {
		this.window = window;
		window.openImages.getSelectionModel().selectedItemProperty().flatMap((Tab tab) -> ((OpenImage)(tab.getUserData())).showingLayerProperty).addListener(Util.change(this::redrawLayer));
		this.rootPane.setCenter(this.canvas.getRootPane());
		this.settings.setPadding(new Insets(4.0D));
		this.settings.setSpacing(4.0D);
		this.rootPane.setBottom(this.settings);
		ChangeListener<Boolean> redrawer = Util.change(this::redrawNone);
		this.log.selectedProperty().addListener(redrawer);
		this.alpha.selectedProperty().addListener(redrawer);
	}

	public void redrawNone() {
		this.redrawCanvas(this.canvas.canvas);
	}

	public void redrawCanvas(Canvas canvas) {
		OpenImage image = this.window.getCurrentImage();
		if (image != null) {
			this.redraw(canvas, image.getVisibleLayer());
		}
	}

	public void redrawLayer(Layer layer) {
		this.redraw(this.canvas.canvas, layer);
	}

	public void redraw(Canvas canvas, Layer layer) {
		int width = (int)(canvas.getWidth());
		int height = (int)(canvas.getHeight());
		byte[] colors = new byte[width * height * 4];
		for (int index = 3; index < colors.length; index += 4) {
			colors[index] = -1;
		}
		if (layer != null) {
			if (this.alpha.isSelected()) {
				int[] alphas = new int[width];
				double max = this.collectValues(alphas, layer, HDRImage.ALPHA_OFFSET);
				if (this.log.isSelected()) max = Math.log1p(max);
				this.drawBar(colors, alphas, max, 0);
				this.drawBar(colors, alphas, max, 1);
				this.drawBar(colors, alphas, max, 2);
			}
			else {
				int[] reds = new int[width], greens = new int[width], blues = new int[width];
				double max = max3(
					this.collectValues(reds, layer, HDRImage.RED_OFFSET),
					this.collectValues(greens, layer, HDRImage.GREEN_OFFSET),
					this.collectValues(blues, layer, HDRImage.BLUE_OFFSET)
				);
				if (this.log.isSelected()) max = Math.log1p(max);
				this.drawBar(colors, reds, max, 2);
				this.drawBar(colors, greens, max, 1);
				this.drawBar(colors, blues, max, 0);
			}
			/*
			int max = 0;
			int[] heights = new int[width * 3];
			float[] pixels = layer.image.pixels;
			for (int index = 0; index < pixels.length; index += 4) {
				float pixel = pixels[index | HDRImage.RED_OFFSET];
				if (pixel >= 0.0F && pixel <= 1.0F) {
					max = Math.max(max, ++heights[Math.min((int)(Math.floor(pixel * width)), width - 1) * 3]);
				}
				pixel = pixels[index | HDRImage.GREEN_OFFSET];
				if (pixel >= 0.0F && pixel <= 1.0F) {
					max = Math.max(max, ++heights[Math.min((int)(Math.floor(pixel * width)), width - 1) * 3 + 1]);
				}
				pixel = pixels[index | HDRImage.BLUE_OFFSET];
				if (pixel >= 0.0F && pixel <= 1.0F) {
					max = Math.max(max, ++heights[Math.min((int)(Math.floor(pixel * width)), width - 1) * 3 + 2]);
				}
			}
			for (int x = 0; x < width; x++) {
				int red   = heights[x * 3    ] * height / max;
				int green = heights[x * 3 + 1] * height / max;
				int blue  = heights[x * 3 + 2] * height / max;
				for (int y = 0; y < red; y++) {
					colors[(((height + ~y) * width + x) << 2) | 2] = -1;
				}
				for (int y = 0; y < green; y++) {
					colors[(((height + ~y) * width + x) << 2) | 1] = -1;
				}
				for (int y = 0; y < blue; y++) {
					colors[(((height + ~y) * width + x) << 2) | 0] = -1;
				}
				for (int y = 0; y < height; y++) {
					colors[((y * width + x) << 2) | 3] = -1;
				}
			}
			*/
		}
		this.canvas.canvas.getGraphicsContext2D().getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), colors, 0, width << 2);
	}

	public static int max3(int a, int b, int c) {
		return Math.max(Math.max(a, b), c);
	}

	public int collectValues(int[] buckets, Layer layer, int component) {
		int max = 0;
		float[] pixels = layer.image.pixels;
		for (int index = component; index < pixels.length; index += 4) {
			float pixel = pixels[index];
			if (pixel >= 0.0F && pixel < 1.0F) {
				max = Math.max(max, ++buckets[Math.min((int)(pixel * buckets.length), buckets.length - 1)]);
			}
		}
		return max;
	}

	public void drawBar(byte[] colors, int[] buckets, double max, int component) {
		int canvasWidth  = (int)(this.canvas.canvas.getWidth());
		int canvasHeight = (int)(this.canvas.canvas.getHeight());
		boolean log = this.log.isSelected();
		for (int x = 0; x < buckets.length; x++) {
			double height = buckets[x];
			if (log) height = Math.log1p(height);
			height *= canvasHeight / max;
			int limit = (int)(height);
			for (int y = 0; y < limit; y++) {
				colors[(((canvasHeight + ~y) * canvasWidth + x) << 2) | component] = -1;
			}
		}
	}
}