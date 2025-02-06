package builderb0y.notgimp.tools;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Layer;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.types.UtilityOperations;
import builderb0y.notgimp.scripting.types.VectorOperations;
import builderb0y.notgimp.sources.ManualLayerSource;

public class BucketTool extends Tool<BucketTool.Work> {

	public static final ToolType TYPE = new ToolType("bucket", 6.0D, 24.0D);
	public static final short OFFSETS = 0b0001_0011_0100_1100;
	public static final VectorMask<Float> RGB_MASK = VectorMask.fromValues(FloatVector.SPECIES_128, true, true, true, false);

	public CheckBox fillAll = new CheckBox("Fill all");
	public CheckBox blend   = new CheckBox("Blend");
	public HBox settingsBox = new HBox();

	public BucketTool(ManualLayerSource source) {
		super(TYPE, source);
		this.fillAll.setPadding(new Insets(4.0D));
		this.fillAll.selectedProperty().addListener(Util.change((Boolean fillAll) -> {
			if (this.work != null && this.work.start != null) {
				this.spreadAutoAndRedraw(fillAll);
			}
		}));
		this.settingsBox.getChildren().addAll(this.fillAll, this.blend);
	}

	@Override
	public void mouseDown(int x, int y, MouseButton button) {
		if (this.work == null) {
			this.source.beginUsingTool();
			this.work = new Work();
		}
		Point clickedPoint = new Point(x, y);
		if (button == MouseButton.PRIMARY) {
			this.work.start = new Start(clickedPoint, this.source.toollessImage.getPixel(x, y), null);
			this.spreadAutoAndRedraw(this.fillAll.isSelected());
		}
	}

	@Override
	public void mouseDragged(int x, int y, MouseButton button) {
		if (this.work != null && this.work.start != null) {
			this.source.beforeToolChanged();
			this.work.start = this.work.start.withBorder(this.source.toollessImage.getPixel(x, y));
			this.spreadAutoAndRedraw(this.fillAll.isSelected());
		}
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
		HDRImage image = this.source.toollessImage;
		work.enqueue(work.start.origin, 1.0F);
		float threshold = work.start.borderColor != null ? work.start.borderColor.sub(work.start.color).abs().reduceLanes(VectorOperators.MAX) : 1.0F / 512.0F;
		boolean blend = this.blend.isSelected();
		while (!work.spreadingPoints.isEmpty()) {
			Map.Entry<Point, Float> entry = work.spreadingPoints.pollFirstEntry();
			Point from = entry.getKey();
			for (int shift = 16; shift < 32; shift += 4) {
				int adjacentX = from.x + ((OFFSETS << (shift + 2)) >> 30);
				int adjacentY = from.y + ((OFFSETS <<  shift     ) >> 30);
				if (adjacentX >= 0 && adjacentX < image.width && adjacentY >= 0 && adjacentY < image.height) {
					Point adjacentPoint = new Point(adjacentX, adjacentY);
					if (work.endingPoints.containsKey(adjacentPoint)) continue;
					FloatVector adjacentColor = image.getPixel(adjacentX, adjacentY);
					if (blend && work.start.borderColor != null && !work.start.color.equals(work.start.borderColor)) {
						float fraction = UtilityOperations.projectLineFrac_float4_float4_float4(work.start.borderColor, work.start.color, adjacentColor);
						if (fraction >= 1.0F / 512.0F && fraction < 1.0F + 1.0F / 512.0F) {
							work.enqueue(adjacentPoint, fraction);
						}
					}
					else {
						if (adjacentColor.sub(work.start.color).abs().compare(VectorOperators.LE, threshold).allTrue()) {
							work.enqueue(adjacentPoint, 1.0F);
						}
					}
				}
			}
		}
	}

	public void spreadAll() {
		Work work = this.work;
		work.endingPoints.clear();
		HDRImage image = this.source.toollessImage;
		float threshold = work.start.borderColor != null ? work.start.borderColor.sub(work.start.color).abs().reduceLanes(VectorOperators.MAX) : 1.0F / 512.0F;
		boolean blend = this.blend.isSelected();
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				Point point = new Point(x, y);
				FloatVector currentPixel = image.getPixel(x, y);
				if (blend && work.start.borderColor != null && !work.start.color.equals(work.start.borderColor)) {
					float fraction = UtilityOperations.projectLineFrac_float4_float4_float4(work.start.borderColor, work.start.color, currentPixel);
					if (fraction >= 1.0F / 512.0F && fraction < 1.0F + 1.0F / 512.0F) {
						work.endingPoints.put(point, fraction);
					}
				}
				else {
					if (currentPixel.sub(work.start.color).abs().compare(VectorOperators.LE, threshold).allTrue()) {
						work.endingPoints.put(point, 1.0F);
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
		if (work == null || work.endingPoints.isEmpty()) return;
		this.source.beforeToolChanged();
		Layer layer = this.layer();
		FloatVector to = layer.openImage.mainWindow.colorPicker.currentColor.toFloatVector();
		FloatVector from = this.work.start.borderColor;
		if (from == null) from = to;
		for (Map.Entry<Point, Float> entry : work.endingPoints.entrySet()) {
			layer.image.setColor(entry.getKey().x, entry.getKey().y, VectorOperations.mix_float3_float3_float(from, to, entry.getValue()));
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

		public Start start;
		public Map<Point, Float> endingPoints = new HashMap<>();
		public SequencedMap<Point, Float> spreadingPoints = new LinkedHashMap<>();

		public void enqueue(Point point, Float mixLevel) {
			this.spreadingPoints.put(point, mixLevel);
			this.endingPoints.put(point, mixLevel);
		}
	}

	public static record Point(int x, int y) {}

	public static record Start(Point origin, FloatVector color, @Nullable FloatVector borderColor) {

		public Start withColor(FloatVector color) {
			return new Start(this.origin, color, this.borderColor);
		}

		public Start withBorder(FloatVector border) {
			return new Start(this.origin, this.color, border);
		}
	}
}