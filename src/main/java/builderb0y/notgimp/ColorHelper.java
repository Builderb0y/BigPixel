package builderb0y.notgimp;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ObservableValueBase;

public class ColorHelper {

	public final SimpleFloatProperty
		red   = new SimpleFloatProperty(1.0F),
		green = new SimpleFloatProperty(0.0F),
		blue  = new SimpleFloatProperty(0.0F),
		hue   = new SimpleFloatProperty(0.0F),
		dark  = new SimpleFloatProperty(1.0F),
		light = new SimpleFloatProperty(1.0F),
		alpha = new SimpleFloatProperty(1.0F);
	public final ColorHelperValue
		any   = this.new ColorHelperValue();
	public float fullRed = 1.0F, fullGreen, fullBlue;

	public ColorHelper() {}

	public ColorHelper(ColorHelper from) {
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
		this.fullRed   = ColorSelector.clamp(Math.abs(hue - 3.0F) - 1.0F);
		this.fullGreen = ColorSelector.clamp(2.0F - Math.abs(hue - 2.0F));
		this.fullBlue  = ColorSelector.clamp(2.0F - Math.abs(hue - 4.0F));
		this.recomputePartialRGB();
	}

	public void recomputePartialRGB() {
		this.red  .set(ColorSelector.mix(ColorSelector.mix(0.5F, 0.0F, this.dark.get()), ColorSelector.mix(1.0F, this.fullRed,   this.dark.get()), this.light.get()));
		this.green.set(ColorSelector.mix(ColorSelector.mix(0.5F, 0.0F, this.dark.get()), ColorSelector.mix(1.0F, this.fullGreen, this.dark.get()), this.light.get()));
		this.blue .set(ColorSelector.mix(ColorSelector.mix(0.5F, 0.0F, this.dark.get()), ColorSelector.mix(1.0F, this.fullBlue,  this.dark.get()), this.light.get()));
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
			this.fullRed = ColorSelector.unmix(min, max, red);
			this.fullGreen = ColorSelector.unmix(min, max, green);
			this.fullBlue = ColorSelector.unmix(min, max, blue);
			float hue = this.fullBlue + this.fullGreen - this.fullRed;
			hue = this.fullGreen < this.fullBlue ? 5.0F - hue : hue + 1.0F;
			this.hue.set(Math.clamp(hue / 6.0F, 0.0F, 1.0F));
		}
		//I don't know how I derived this, but it works for my mandelbrot program.
		if (min + max >= 1.0F) {
			this.dark.set(max - min);
			this.light.set(max * 2.0F - 1.0F);
		}
		else {
			this.dark.set(min * -2.0F + 1.0F);
			this.light.set(max - min);
		}
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