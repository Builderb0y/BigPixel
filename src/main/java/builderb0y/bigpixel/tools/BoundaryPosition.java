package builderb0y.bigpixel.tools;

public enum BoundaryPosition {
	OUTSIDE,
	CORNER_X1_Y1,
	CORNER_X1_Y2,
	CORNER_X2_Y1,
	CORNER_X2_Y2,
	EDGE_X1,
	EDGE_Y1,
	EDGE_X2,
	EDGE_Y2,
	INSIDE;

	public static BoundaryPosition get(
		int x,  int y,
		int x1, int y1,
		int x2, int y2
	) {
		if (x == x1) {
			if (y == y1) {
				return CORNER_X1_Y1;
			}
			else if (y == y2) {
				return CORNER_X1_Y2;
			}
			else if (y > Math.min(y1, y2) && y < Math.max(y1, y2)) {
				return EDGE_X1;
			}
		}
		else if (x == x2) {
			if (y == y1) {
				return CORNER_X2_Y1;
			}
			else if (y == y2) {
				return CORNER_X2_Y2;
			}
			else if (y > Math.min(y1, y2) && y < Math.max(y1, y2)) {
				return EDGE_X2;
			}
		}
		else if (x > Math.min(x1, x2) && x < Math.max(x1, x2)) {
			if (y == y1) {
				return EDGE_Y1;
			}
			else if (y == y2) {
				return EDGE_Y2;
			}
			else if (y > Math.min(y1, y2) && y < Math.max(y1, y2)) {
				return INSIDE;
			}
		}
		return OUTSIDE;
	}
}