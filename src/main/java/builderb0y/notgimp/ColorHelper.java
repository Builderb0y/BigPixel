package builderb0y.notgimp;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValueBase;
import jdk.incubator.vector.FloatVector;

import builderb0y.notgimp.tools.ColorPickerTool.ColorPickerCallback;

public class ColorHelper implements ColorPickerCallback {

	public final SimpleFloatProperty
		red   = new SimpleFloatProperty(this, "red",   1.0F),
		green = new SimpleFloatProperty(this, "green", 0.0F),
		blue  = new SimpleFloatProperty(this, "blue",  0.0F),
		hue   = new SimpleFloatProperty(this, "hue",   0.0F),
		dark  = new SimpleFloatProperty(this, "dark",  1.0F),
		light = new SimpleFloatProperty(this, "light", 1.0F),
		alpha = new SimpleFloatProperty(this, "alpha", 1.0F);
	public final ColorHelperValue
		any   = this.new ColorHelperValue();
	public SimpleObjectProperty<FloatVector>
		rgba = new SimpleObjectProperty<>(this, "rgba");
	public float fullRed = 1.0F, fullGreen, fullBlue;

	public ColorHelper() {
		float[] color = new float[4];
		color[HDRImage.RED_OFFSET] =
		color[HDRImage.ALPHA_OFFSET] =
		1.0F;
		this.rgba.set(FloatVector.fromArray(FloatVector.SPECIES_128, color, 0));
		this.red  .addListener(Util.change((Number red  ) -> this.rgba.set(this.rgba.get().withLane(HDRImage.  RED_OFFSET, red  .floatValue()))));
		this.green.addListener(Util.change((Number green) -> this.rgba.set(this.rgba.get().withLane(HDRImage.GREEN_OFFSET, green.floatValue()))));
		this.blue .addListener(Util.change((Number blue ) -> this.rgba.set(this.rgba.get().withLane(HDRImage. BLUE_OFFSET, blue .floatValue()))));
		this.alpha.addListener(Util.change((Number alpha) -> this.rgba.set(this.rgba.get().withLane(HDRImage.ALPHA_OFFSET, alpha.floatValue()))));
	}

	public ColorHelper(ColorHelper from) {
		this();
		this.setFrom(from);
	}

	public void setFrom(ColorHelper from) {
		this.red  .set(from.red  .get());
		this.green.set(from.green.get());
		this.blue .set(from.blue .get());
		this.hue  .set(from.hue  .get());
		this.dark .set(from.dark .get());
		this.light.set(from.light.get());
		this.alpha.set(from.alpha.get());

		this.fullRed   = from.fullRed;
		this.fullGreen = from.fullGreen;
		this.fullBlue  = from.fullBlue;
	}

	public FloatVector toFloatVector() {
		float[] array = new float[4];
		array[HDRImage.  RED_OFFSET] = this.red  .get();
		array[HDRImage.GREEN_OFFSET] = this.green.get();
		array[HDRImage. BLUE_OFFSET] = this.blue .get();
		array[HDRImage.ALPHA_OFFSET] = this.alpha.get();
		return FloatVector.fromArray(FloatVector.SPECIES_128, array, 0);
	}

	public void markDirty() {
		this.any.fireValueChangedEvent();
	}

	public SimpleFloatProperty getComponent(ColorComponent component) {
		return switch (component) {
			case RED   -> this.red;
			case GREEN -> this.green;
			case BLUE  -> this.blue;
			case HUE   -> this.hue;
			case DARK  -> this.dark;
			case LIGHT -> this.light;
			case ALPHA -> this.alpha;
		};
	}

	public void setComponent(ColorComponent component, float value) {
		switch (component) {
			case RED   -> this.setRed(value);
			case GREEN -> this.setGreen(value);
			case BLUE  -> this.setBlue(value);
			case HUE   -> this.setHue(value);
			case DARK  -> this.setDark(value);
			case LIGHT -> this.setLight(value);
			case ALPHA -> this.setAlpha(value);
		}
	}

	@Override
	public void onColorPicked(float red, float green, float blue, float alpha) {
		this.setRGBA(
			Util.clampF(red),
			Util.clampF(green),
			Util.clampF(blue),
			Util.clampF(alpha)
		);
		this.markDirty();
	}

	public void setRed(float red) {
		this.red.set(red);
		this.recomputeFullHDL();
	}

	public void setGreen(float green) {
		this.green.set(green);
		this.recomputeFullHDL();
	}

	public void setBlue(float blue) {
		this.blue.set(blue);
		this.recomputeFullHDL();
	}

	public void setRGBA(float red, float green, float blue, float alpha) {
		this.red.set(red);
		this.green.set(green);
		this.blue.set(blue);
		this.alpha.set(alpha);
		this.recomputeFullHDL();
	}

	public void setHue(float hue) {
		this.hue.set(hue);
		this.recomputeFullRGB();
	}

	public void setDark(float dark) {
		this.dark.set(dark);
		this.recomputePartialRGB();
	}

	public void setLight(float light) {
		this.light.set(light);
		this.recomputePartialRGB();
	}

	public void setAlpha(float alpha) {
		this.alpha.set(alpha);
	}

	public void recomputeFullRGB() {
		float hue = this.hue.get() * 6.0F;
		this.fullRed   = Util.clampF(Math.abs(hue - 3.0F) - 1.0F);
		this.fullGreen = Util.clampF(2.0F - Math.abs(hue - 2.0F));
		this.fullBlue  = Util.clampF(2.0F - Math.abs(hue - 4.0F));
		this.recomputePartialRGB();
	}

	public void recomputePartialRGB() {
		this.red  .set(Util.mix(Util.mix(0.5F, 0.0F, this.dark.get()), Util.mix(1.0F, this.fullRed,   this.dark.get()), this.light.get()));
		this.green.set(Util.mix(Util.mix(0.5F, 0.0F, this.dark.get()), Util.mix(1.0F, this.fullGreen, this.dark.get()), this.light.get()));
		this.blue .set(Util.mix(Util.mix(0.5F, 0.0F, this.dark.get()), Util.mix(1.0F, this.fullBlue,  this.dark.get()), this.light.get()));
	}

	public void recomputeFullHDL() {
		float red   = this.red  .get();
		float green = this.green.get();
		float blue  = this.blue .get();
		float min   = Math.min(Math.min(red, green), blue);
		float max   = Math.max(Math.max(red, green), blue);
		if (min == max) {
			this.fullRed = 1.0F;
			this.fullGreen = 0.0F;
			this.fullBlue = 0.0F;
			this.hue.set(0.0F);
		}
		else {
			this.fullRed = Util.unmix(min, max, red);
			this.fullGreen = Util.unmix(min, max, green);
			this.fullBlue = Util.unmix(min, max, blue);
			float hue = this.fullBlue + this.fullGreen - this.fullRed;
			hue = this.fullGreen < this.fullBlue ? 5.0F - hue : hue + 1.0F;
			this.hue.set(Math.clamp(hue / 6.0F, 0.0F, 1.0F));
		}
		double sum = ((double)(min)) + ((double)(max));
		double common = Math.sqrt(sum * sum - 6.0F * min + 2.0F * max + 1.0F);
		this.dark.set((float)((common - sum + 1.0D) * 0.5D));
		this.light.set((float)((common + sum - 1.0D) * 0.5D));
	}

	public static enum ColorComponent {
		RED("Red"),
		GREEN("Green"),
		BLUE("Blue"),
		HUE("Hue"),
		DARK("Dark"),
		LIGHT("Light"),
		ALPHA("Alpha");

		public final String name;

		ColorComponent(String name) {
			this.name = name;
		}

		public ColorComponent horizontal() {
			return switch (this) {
				case RED   -> GREEN;
				case GREEN -> RED;
				case BLUE  -> RED;
				case HUE   -> DARK;
				case DARK  -> HUE;
				case LIGHT -> HUE;
				case ALPHA -> throw new UnsupportedOperationException();
			};
		}

		public ColorComponent vertical() {
			return switch (this) {
				case RED   -> BLUE;
				case GREEN -> BLUE;
				case BLUE  -> GREEN;
				case HUE   -> LIGHT;
				case DARK  -> LIGHT;
				case LIGHT -> DARK;
				case ALPHA -> throw new UnsupportedOperationException();
			};
		}
	}

	public class ColorHelperValue extends ObservableValueBase<ColorHelper> {

		@Override
		public ColorHelper getValue() {
			return ColorHelper.this;
		}

		@Override
		public void fireValueChangedEvent() {
			super.fireValueChangedEvent();
		}
	}
}