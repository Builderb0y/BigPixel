package builderb0y.notgimp.tools;

import java.util.*;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.sources.ManualLayerSource;

public class BucketTool extends Tool<BucketTool.Work> {

	public static final ToolType TYPE = new ToolType("bucket", 6.0D, 24.0D);
	public static final short OFFSETS = 0b0001_0011_0100_1100;

	public Spinner<Double> threshold = Util.setupSpinner(new Spinner<>(0.0D, 1.0D, 0.0D, 1.0D / 512.0D));
	public CheckBox fillAll = new CheckBox("Fill all");
	public HBox settingsBox = new HBox();

	public BucketTool(ManualLayerSource source) {
		super(TYPE, source);
		this.fillAll.setPadding(new Insets(4.0D));
		this.fillAll.selectedProperty().addListener(Util.change((Boolean fillAll) -> {
			if (this.work != null) {
				this.spreadAutoAndRedraw(fillAll);
			}
		}));
		this.threshold.getValueFactory().setConverter(new StringConverter<>() {

			@Override
			public String toString(Double object) {
				return object * 256.0D + "/256";
			}

			@Override
			public Double fromString(String string) {
				int slash = string.indexOf('/');
				if (slash >= 0) string = string.substring(0, slash);
				return Double.valueOf(string);
			}
		});
		this.threshold.valueProperty().addListener(Util.change(() -> {
			if (this.work != null) {
				this.spreadAutoAndRedraw(this.fillAll.isSelected());
			}
		}));
		this.settingsBox.getChildren().addAll(this.fillAll, this.threshold);
	}

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		if (this.work == null) {
			this.source.beginUsingTool();
			this.work = new Work();
		}
		switch (button) {
			case PRIMARY -> {
				Point startingPoint = new Point(x, y);
				if (!this.work.endingPoints.containsKey(startingPoint)) {
					this.work.startingPoints.add(startingPoint);
					this.spreadAutoAndRedraw(this.fillAll.isSelected());
				}
			}
			case SECONDARY -> {
				Point startingPoint = this.work.endingPoints.get(new Point(x, y));
				if (startingPoint != null) {
					this.work.startingPoints.remove(startingPoint);
					this.spreadAutoAndRedraw(this.fillAll.isSelected());
				}
			}
		}
	}

	@Override
	public void mouseDragged(int x, int y, MouseButton button) {

	}

	public void spreadAutoAndRedraw(boolean all) {
		if (all) this.spreadAll();
		else this.spread();
		this.redraw();
		this.updateLabelText();
	}

	public void spread() {
		Work work = this.work;
		work.endingPoints.clear();
		for (Point point : work.startingPoints) {
			work.spreadingPoints.put(point, point);
		}
		HDRImage image = this.source.toollessImage;
		float threshold = this.threshold.getValue().floatValue();
		while (!work.spreadingPoints.isEmpty()) {
			Map.Entry<Point, Point> entry = work.spreadingPoints.pollFirstEntry();
			Point from = entry.getKey();
			Point origin = entry.getValue();
			if (work.endingPoints.putIfAbsent(from, origin) != null) continue;
			int originBaseIndex = image.baseIndex(origin.x, origin.y);
			for (int shift = 16; shift < 32; shift += 4) {
				int adjacentX = from.x + ((OFFSETS << (shift + 2)) >> 30);
				int adjacentY = from.y + ((OFFSETS <<  shift     ) >> 30);
				if (adjacentX >= 0 && adjacentX < image.width && adjacentY >= 0 && adjacentY < image.height) {
					int toBaseIndex = image.baseIndex(adjacentX, adjacentY);
					float
						red          = image.pixels[    toBaseIndex | HDRImage.  RED_OFFSET],
						green        = image.pixels[    toBaseIndex | HDRImage.GREEN_OFFSET],
						blue         = image.pixels[    toBaseIndex | HDRImage. BLUE_OFFSET],
						alpha        = image.pixels[    toBaseIndex | HDRImage.ALPHA_OFFSET],
						compareRed   = image.pixels[originBaseIndex | HDRImage.  RED_OFFSET],
						compareGreen = image.pixels[originBaseIndex | HDRImage.GREEN_OFFSET],
						compareBlue  = image.pixels[originBaseIndex | HDRImage. BLUE_OFFSET],
						compareAlpha = image.pixels[originBaseIndex | HDRImage.ALPHA_OFFSET],
						correction   = Math.max(alpha, compareAlpha);
					if (
						Math.abs(red   - compareRed  ) * correction < threshold &&
						Math.abs(green - compareGreen) * correction < threshold &&
						Math.abs(blue  - compareBlue ) * correction < threshold
					) {
						work.spreadingPoints.put(new Point(adjacentX, adjacentY), origin);
					}
				}
			}
		}
	}

	public void spreadAll() {
		Work work = this.work;
		work.endingPoints.clear();
		HDRImage image = this.source.toollessImage;
		float threshold = this.threshold.getValue().floatValue();
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				int baseIndex = image.baseIndex(x, y);
				float
					red   = image.pixels[baseIndex | HDRImage.  RED_OFFSET],
					green = image.pixels[baseIndex | HDRImage.GREEN_OFFSET],
					blue  = image.pixels[baseIndex | HDRImage. BLUE_OFFSET],
					alpha = image.pixels[baseIndex | HDRImage.ALPHA_OFFSET];
				for (Point startingPoint : work.startingPoints) {
					int startingIndex = image.baseIndex(startingPoint.x, startingPoint.y);
					float
						startRed   = image.pixels[startingIndex | HDRImage.  RED_OFFSET],
						startGreen = image.pixels[startingIndex | HDRImage.GREEN_OFFSET],
						startBlue  = image.pixels[startingIndex | HDRImage. BLUE_OFFSET],
						startAlpha = image.pixels[startingIndex | HDRImage.ALPHA_OFFSET],
						correction = Math.max(alpha, startAlpha);
					if (
						Math.abs(red   - startRed  ) * correction < threshold &&
						Math.abs(green - startGreen) * correction < threshold &&
						Math.abs(blue  - startBlue ) * correction < threshold
					) {
						work.endingPoints.put(new Point(x, y), startingPoint);
					}
				}
			}
		}
	}

	@Override
	public void colorChanged() {
		this.redraw();
	}

	public void redraw() {
		Work work = this.work;
		this.source.beforeToolChanged();
		Layer layer = this.layer();
		ColorHelper color = layer.openImage.mainWindow.colorPicker.currentColor;
		for (Point point : work.endingPoints.keySet()) {
			layer.image.setColor(point.x, point.y, color);
		}
		layer.image.markDirty(false);
	}

	@Override
	public @Nullable Node getConfiguration() {
		return this.settingsBox;
	}

	@Override
	public void updateLabelText() {
		if (this.work != null) {
			this.labelText.set("Filling " + this.work.endingPoints.size() + " pixel(s) from bucket");
		}
		else {
			this.labelText.set("Bucket tool inactive");
		}
	}

	public static class Work {

		public Set<Point> startingPoints = new HashSet<>();
		public Map<Point, Point> endingPoints = new HashMap<>();
		public SequencedMap<Point, Point> spreadingPoints = new LinkedHashMap<>();
	}

	public static record Point(int x, int y) {}
}