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
		this.redrawCanvas(this.canvas.display);
	}

	public void redrawCanvas(Canvas canvas) {
		OpenImage image = this.window.getCurrentImage();
		if (image != null) {
			this.redraw(canvas, image.getVisibleLayer());
		}
	}

	public void redrawLayer(Layer layer) {
		this.redraw(this.canvas.display, layer);
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
		}
		this.canvas.display.getGraphicsContext2D().getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), colors, 0, width << 2);
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
		int canvasWidth  = (int)(this.canvas.display.getWidth());
		int canvasHeight = (int)(this.canvas.display.getHeight());
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