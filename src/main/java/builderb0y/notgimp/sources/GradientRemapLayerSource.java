package builderb0y.notgimp.sources;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import builderb0y.notgimp.ColorHelper;
import builderb0y.notgimp.Gradient;
import builderb0y.notgimp.HDRImage;
import builderb0y.notgimp.Util;
import builderb0y.notgimp.scripting.types.UtilityOperations;
import builderb0y.notgimp.scripting.types.VectorOperations;

public class GradientRemapLayerSource extends SingleInputEffectLayerSource {

	public ColorBoxGroup
		activeBox;
	public GradientRow
		from = new GradientRow(this, false),
		to   = new GradientRow(this, true);
	public CheckBox
		perChannel    = this.addCheckbox("per_channel", "Per Channel", false),
		preserveAlpha = this.addCheckbox("preserve_alpha", "Preserve Alpha", false);
	public HBox
		toggles = new HBox(this.perChannel, this.preserveAlpha);
	public GridPane
		gridPane = new GridPane();

	public GradientRemapLayerSource(LayerSources sources) {
		super(sources, "gradient_remap", "Gradient Remap");
	}

	@Override
	public void init(boolean fromSave) {
		super.init(fromSave);
		ColorHelper colorHelper = this.sources.layer.openImage.mainWindow.colorPicker.currentColor;
		this.activeBox = new ColorBoxGroup(colorHelper, this.rootNode, this.from.start, this.from.end, this.to.start, this.to.end);
		this.from.init();
		this.to.init();
		this.from.addTo(this.gridPane, 0, "From: ");
		this.to.addTo(this.gridPane, 1, "To: ");
		this.gridPane.add(this.toggles, 0, 2, 4, 1);
		this.rootNode.setCenter(this.gridPane);
	}

	@Override
	public void doRedraw() throws RedrawException {
		HDRImage source = this.getSingleInput(true).image;
		HDRImage destination = this.sources.layer.image;
		FloatVector
			fromStart = this.from.start.color.get(),
			fromEnd   = this.from.end  .color.get(),
			toStart   = this.to  .start.color.get(),
			toEnd     = this.to  .end  .color.get();
		if (this.perChannel.isSelected()) {
			VectorMask<Float> usedChannels = fromStart.compare(VectorOperators.NE, fromEnd);
			if (this.preserveAlpha.isSelected()) {
				//why is there no withLane() for masks?
				boolean[] array = usedChannels.toArray();
				array[HDRImage.ALPHA_OFFSET] = false;
				usedChannels = VectorMask.fromArray(FloatVector.SPECIES_128, array, 0);
			}
			for (int index = 0; index < source.pixels.length; index += 4) {
				FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
				FloatVector fraction = VectorOperations.unmix_float4_float4_float4(fromStart, fromEnd, color);
				FloatVector remixed = VectorOperations.mix_float4_float4_float4(toStart, toEnd, fraction);
				color.blend(remixed, usedChannels).intoArray(destination.pixels, index);
			}
		}
		else {
			if (this.preserveAlpha.isSelected()) {
				for (int index = 0; index < source.pixels.length; index += 4) {
					FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
					float fraction = UtilityOperations.projectLineFrac_float3_float3_float3(fromStart, fromEnd, color);
					color = color.blend(VectorOperations.mix_float3_float3_float(toStart, toEnd, fraction), RGB_MASK);
					color.intoArray(destination.pixels, index);
				}
			}
			else {
				for (int index = 0; index < source.pixels.length; index += 4) {
					FloatVector color = FloatVector.fromArray(FloatVector.SPECIES_128, source.pixels, index);
					float fraction = UtilityOperations.projectLineFrac_float4_float4_float4(fromStart, fromEnd, color);
					color = VectorOperations.mix_float4_float4_float(toStart, toEnd, fraction);
					color.intoArray(destination.pixels, index);
				}
			}
		}
	}

	public static class GradientRow extends Gradient {

		public ColorBox start, end;

		public GradientRow(GradientRemapLayerSource source, boolean to) {
			this.checkerboard().popOut().fixedSize(129.0D, 16.0D);
			this.start = source.addColorBox((to ? "to" : "from") + "_start", 0.0F, 0.0F, 0.0F, 1.0F);
			this.end = source.addColorBox((to ? "to" : "from") + "_end", 1.0F, 1.0F, 1.0F, 1.0F);
		}

		public void init() {
			ChangeListener<Object> redrawer = Util.change(this::redraw);
			this.start.color.addListener(redrawer);
			this.end.color.addListener(redrawer);
			this.redraw();
		}

		@Override
		public FloatVector computeColor(int pixelPos, float fraction) {
			return VectorOperations.mix_float4_float4_float(this.start.color.get(), this.end.color.get(), fraction);
		}

		public void addTo(GridPane gridPane, int y, String name) {
			gridPane.add(new Label(name), 0, y);
			gridPane.add(this.start.box.getRootPane(), 1, y);
			gridPane.add(this.getRootPane(), 2, y);
			gridPane.add(this.end.box.getRootPane(), 3, y);
		}
	}
}