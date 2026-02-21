package builderb0y.bigpixel;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import builderb0y.bigpixel.util.CanvasHelper;
import builderb0y.bigpixel.util.Util;

public class Histogram {

	public MainWindow window;
	public CanvasHelper canvas = new CanvasHelper() {

		@Override
		public void redraw() {
			Histogram.this.redrawCanvas(this);
		}
	}
	.resizeable()
	.popIn();
	public CheckBox log = new CheckBox("Log Scale");
	public CheckBox alpha = new CheckBox("Alpha");
	public HBox settings = new HBox(this.log, this.alpha);
	public BorderPane rootPane = new BorderPane();

	public Histogram(MainWindow window) {
		this.window = window;
		window.openImages.getSelectionModel().selectedItemProperty().flatMap(
			(Tab tab) -> ((OpenImage)(tab.getUserData())).layerGraph.visibleLayerProperty
		)
		.addListener(Util.change(this::redrawLayer));
		this.rootPane.setCenter(this.canvas.getRootPane());
		this.settings.setPadding(new Insets(4.0D));
		this.settings.setSpacing(4.0D);
		this.rootPane.setBottom(this.settings);
		ChangeListener<Boolean> redrawer = Util.change(this::redrawNone);
		this.log.selectedProperty().addListener(redrawer);
		this.alpha.selectedProperty().addListener(redrawer);
	}

	public void redrawNone() {
		this.redrawCanvas(this.canvas);
	}

	public void redrawCanvas(CanvasHelper canvas) {
		OpenImage image = this.window.getCurrentImage();
		if (image != null) {
			this.redraw(canvas, image.layerGraph.visibleLayerProperty.getValue());
		}
	}

	public void redrawLayer(LayerNode layer) {
		this.redraw(this.canvas, layer);
	}

	public void redraw(CanvasHelper canvas, LayerNode layer) {
		int width = (int)(canvas.display.getWidth());
		int height = (int)(canvas.display.getHeight());
		if (width == 0 || height == 0) return;
		WritableImage image = canvas.getImage();
		byte[] colors = this.canvas.pixels.get();
		for (int index = 0; index < colors.length; index += 4) {
			colors[index    ] =  0;
			colors[index | 1] =  0;
			colors[index | 2] =  0;
			colors[index | 3] = -1;
		}
		if (layer != null) {
			if (this.alpha.isSelected()) {
				int[] alphas = new int[width];
				double max = this.collectValues(alphas, layer, HDRImage.ALPHA_OFFSET);
				if (this.log.isSelected()) max = Math.log1p(max);
				this.drawBar(image, colors, alphas, max, 0);
				this.drawBar(image, colors, alphas, max, 1);
				this.drawBar(image, colors, alphas, max, 2);
			}
			else {
				int[] reds = new int[width], greens = new int[width], blues = new int[width];
				double max = max3(
					this.collectValues(reds, layer, HDRImage.RED_OFFSET),
					this.collectValues(greens, layer, HDRImage.GREEN_OFFSET),
					this.collectValues(blues, layer, HDRImage.BLUE_OFFSET)
				);
				if (this.log.isSelected()) max = Math.log1p(max);
				this.drawBar(image, colors, reds, max, 2);
				this.drawBar(image, colors, greens, max, 1);
				this.drawBar(image, colors, blues, max, 0);
			}
		}
		image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), colors, 0, width << 2);
		canvas.blit();
	}

	public static int max3(int a, int b, int c) {
		return Math.max(Math.max(a, b), c);
	}

	public int collectValues(int[] buckets, LayerNode layer, int component) {
		int max = 0;
		float[] pixels = layer.getFrame().pixels;
		for (int index = component; index < pixels.length; index += 4) {
			float pixel = pixels[index];
			if (pixel >= 0.0F && pixel <= 1.0F) {
				max = Math.max(max, ++buckets[Math.min((int)(pixel * buckets.length), buckets.length - 1)]);
			}
		}
		return max;
	}

	public void drawBar(WritableImage image, byte[] colors, int[] buckets, double max, int component) {
		int canvasWidth  = (int)(image.getWidth());
		int canvasHeight = (int)(image.getHeight());
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

	public Node getRootNode() {
		return this.rootPane;
	}
}