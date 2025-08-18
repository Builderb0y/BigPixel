package builderb0y.bigpixel.json;

public class JsonFloatingPoint extends JsonNumber {

	public final double value;

	public JsonFloatingPoint(double value) {
		this.value = value;
	}

	@Override
	public int intValue() {
		int value = (int)(this.value);
		if (this.value == value) return value;
		else return super.intValue();
	}

	@Override
	public long longValue() {
		long value = (long)(this.value);
		if (this.value == value) return value;
		else return super.longValue();
	}

	@Override
	public float floatValue() {
		return (float)(this.value);
	}

	@Override
	public double doubleValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return Double.toString(this.value);
	}
}