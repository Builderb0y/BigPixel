package builderb0y.bigpixel.json;

public class JsonFixedPoint extends JsonNumber {

	public final long value;

	public JsonFixedPoint(long value) {
		this.value = value;
	}

	@Override
	public int intValue() {
		return Math.toIntExact(this.value);
	}

	@Override
	public long longValue() {
		return this.value;
	}

	@Override
	public float floatValue() {
		return this.value;
	}

	@Override
	public double doubleValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return Long.toString(this.value);
	}
}