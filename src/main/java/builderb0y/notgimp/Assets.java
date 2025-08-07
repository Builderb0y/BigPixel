package builderb0y.notgimp;

import javafx.scene.image.Image;

public class Assets {

	public static final Image
		ICON      = of("/assets/icon.png"),
		SWAP_VIEW = of("/assets/swap_view.png");

	public static class Tools {

		public static final Image
			BUCKET       = of("/assets/tools/bucket.png"),
			COLOR_PICKER = of("/assets/tools/color_picker.png"),
			FREEHAND     = of("/assets/tools/freehand.png"),
			LINE         = of("/assets/tools/line.png"),
			MOVE         = of("/assets/tools/move.png"),
			RECTANGLE    = of("/assets/tools/rectangle.png");

		public static class Move {

			public static final Image
				AGAIN      = of("/assets/tools/move/again.png"),
				FLIP_H     = of("/assets/tools/move/flip_h.png"),
				FLIP_L     = of("/assets/tools/move/flip_l.png"),
				FLIP_R     = of("/assets/tools/move/flip_r.png"),
				FLIP_V     = of("/assets/tools/move/flip_v.png"),
				ROTATE_180 = of("/assets/tools/move/rotate_180.png"),
				ROTATE_CCW = of("/assets/tools/move/rotate_ccw.png"),
				ROTATE_CW  = of("/assets/tools/move/rotate_cw.png"),
				EXTRACT    = of("/assets/tools/move/extract.png");
		}
	}

	public static Image of(String path) {
		return new Image(Assets.class.getResourceAsStream(path));
	}
}