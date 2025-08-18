package builderb0y.notgimp;

import java.util.Comparator;

public interface LayerPosition {

	Comparator<LayerPosition> POSITION_COMPARATOR = (LayerPosition layer1, LayerPosition layer2) -> {
		int compare = Integer.compare(layer1.getGridX(), layer2.getGridX());
		if (compare != 0) return compare;
		return Integer.compare(layer1.getGridY(), layer2.getGridY());
	};

	public abstract int getGridX();

	public abstract int getGridY();

	public static LayerPosition of(int x, int y) {
		return new LayerPosition() {

			@Override
			public int getGridX() {
				return x;
			}

			@Override
			public int getGridY() {
				return y;
			}
		};
	}
}