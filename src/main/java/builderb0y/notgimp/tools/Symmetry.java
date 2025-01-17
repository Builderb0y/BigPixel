package builderb0y.notgimp.tools;

public enum Symmetry {
	IDENTITY("Identity"),
	ROTATE_CW("Rotating clockwise"),
	ROTATE_CCW("Rotating counter-clockwise"),
	ROTATE_180("Rotating 180°"),
	FLIP_H("Flipping horizontally"),
	FLIP_V("Flipping vertically"),
	FLIP_L("Flipping top-left to bottom-right"),
	FLIP_R("Flipping top-right to bottom-left");

	public final String description;

	Symmetry(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return this.description;
	}

	//note: getX() and getY() are flipped to account for javaFX flipping the Y axis.
	public double getX(double x, double y) {
		return switch (this) {
			case IDENTITY -> x;
			case ROTATE_CW -> -y;
			case ROTATE_CCW -> y;
			case ROTATE_180 -> -x;
			case FLIP_H -> -x;
			case FLIP_V -> x;
			case FLIP_L -> -y;
			case FLIP_R -> y;
		};
	}

	public double getY(double x, double y) {
		return switch (this) {
			case IDENTITY -> y;
			case ROTATE_CW -> x;
			case ROTATE_CCW -> -x;
			case ROTATE_180 -> -y;
			case FLIP_H -> y;
			case FLIP_V -> -y;
			case FLIP_L -> -x;
			case FLIP_R -> x;
		};
	}

	public Symmetry inverse() {
		return switch (this) {
			case IDENTITY -> IDENTITY;
			case ROTATE_CW -> ROTATE_CCW;
			case ROTATE_CCW -> ROTATE_CW;
			case ROTATE_180 -> ROTATE_180;
			case FLIP_H -> FLIP_H;
			case FLIP_V -> FLIP_V;
			case FLIP_L -> FLIP_L;
			case FLIP_R -> FLIP_R;
		};
	}

	public Symmetry andThen(Symmetry that) {
		return switch (this) {
			case IDENTITY -> switch (that) {
				case IDENTITY -> IDENTITY;
				case ROTATE_CW -> ROTATE_CW;
				case ROTATE_CCW -> ROTATE_CCW;
				case ROTATE_180 -> ROTATE_180;
				case FLIP_H -> FLIP_H;
				case FLIP_V -> FLIP_V;
				case FLIP_L -> FLIP_L;
				case FLIP_R -> FLIP_R;
			};
			case ROTATE_CW -> switch (that) {
				case IDENTITY -> ROTATE_CW;
				case ROTATE_CW -> ROTATE_180;
				case ROTATE_CCW -> IDENTITY;
				case ROTATE_180 -> ROTATE_CCW;
				case FLIP_H -> FLIP_R;
				case FLIP_V -> FLIP_L;
				case FLIP_L -> FLIP_H;
				case FLIP_R -> FLIP_V;
			};
			case ROTATE_CCW -> switch (that) {
				case IDENTITY -> ROTATE_CCW;
				case ROTATE_CW -> IDENTITY;
				case ROTATE_CCW -> ROTATE_180;
				case ROTATE_180 -> ROTATE_CW;
				case FLIP_H -> FLIP_L;
				case FLIP_V -> FLIP_R;
				case FLIP_L -> FLIP_V;
				case FLIP_R -> FLIP_H;
			};
			case ROTATE_180 -> switch (that) {
				case IDENTITY -> ROTATE_180;
				case ROTATE_CW -> ROTATE_CCW;
				case ROTATE_CCW -> ROTATE_CW;
				case ROTATE_180 -> IDENTITY;
				case FLIP_H -> FLIP_V;
				case FLIP_V -> FLIP_H;
				case FLIP_L -> FLIP_R;
				case FLIP_R -> FLIP_L;
			};
			case FLIP_H -> switch (that) {
				case IDENTITY -> FLIP_H;
				case ROTATE_CW -> FLIP_L;
				case ROTATE_CCW -> FLIP_R;
				case ROTATE_180 -> FLIP_V;
				case FLIP_H -> IDENTITY;
				case FLIP_V -> ROTATE_180;
				case FLIP_L -> ROTATE_CW;
				case FLIP_R -> ROTATE_CCW;
			};
			case FLIP_V -> switch (that) {
				case IDENTITY -> FLIP_V;
				case ROTATE_CW -> FLIP_R;
				case ROTATE_CCW -> FLIP_L;
				case ROTATE_180 -> FLIP_H;
				case FLIP_H -> ROTATE_180;
				case FLIP_V -> IDENTITY;
				case FLIP_L -> ROTATE_CCW;
				case FLIP_R -> ROTATE_CW;
			};
			case FLIP_L -> switch (that) {
				case IDENTITY -> FLIP_L;
				case ROTATE_CW -> FLIP_V;
				case ROTATE_CCW -> FLIP_H;
				case ROTATE_180 -> FLIP_R;
				case FLIP_H -> ROTATE_CCW;
				case FLIP_V -> ROTATE_CW;
				case FLIP_L -> IDENTITY;
				case FLIP_R -> ROTATE_180;
			};
			case FLIP_R -> switch (that) {
				case IDENTITY -> FLIP_R;
				case ROTATE_CW -> FLIP_H;
				case ROTATE_CCW -> FLIP_V;
				case ROTATE_180 -> FLIP_L;
				case FLIP_H -> ROTATE_CW;
				case FLIP_V -> ROTATE_CCW;
				case FLIP_L -> ROTATE_180;
				case FLIP_R -> IDENTITY;
			};
		};
	}

	/*
	static {
		Symmetry[] values = values();
		double x = 4.0D, y = 3.0D;
		for (Symmetry value : values) {
			check(value.andThen(value.inverse()).equals(IDENTITY));
			check(value.inverse().andThen(value).equals(IDENTITY));
			if (value != value.inverse()) {
				check(value.andThen(value).andThen(value).andThen(value).equals(IDENTITY));
			}

			double x2 = value.getX(x, y);
			double y2 = value.getY(x, y);
			check(value.inverse().getX(x2, y2) == x && value.inverse().getY(x2, y2) == y);

			if (value != value.inverse()) {
				double x3 = value.getX(x2, y2);
				double y3 = value.getY(x2, y2);

				double x4 = value.getX(x3, y3);
				double y4 = value.getY(x3, y3);

				check(value.getX(x4, y4) == x && value.getY(x4, y4) == y);
			}
		}
	}

	public static void check(boolean condition) {
		if (!condition) throw new AssertionError();
	}

	public static void main(String[] args) {

	}
	*/
}