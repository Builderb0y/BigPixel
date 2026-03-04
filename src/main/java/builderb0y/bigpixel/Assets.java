package builderb0y.bigpixel;

import javafx.scene.image.Image;

public class Assets {

	public static final Image
		ICON = of("/assets/icon.png"),
		DRAGBAR = of("/assets/dragbar.png");

	public static class Tools {

		public static final Image
			BUCKET       = of("/assets/tools/bucket.png"),
			COLOR_PICKER = of("/assets/tools/color_picker.png"),
			FREEHAND     = of("/assets/tools/freehand.png"),
			LINE         = of("/assets/tools/line.png"),
			MOVE         = of("/assets/tools/move.png"),
			RECTANGLE    = of("/assets/tools/rectangle.png"),
			AUTO_UV      = of("/assets/tools/auto_uv.png");

		public static class Move {

			public static final Image
				AGAIN      = of("/assets/tools/move/again.png"),
				FLIP_H     = of("/assets/tools/move/flip_h.png"),
				FLIP_L     = of("/assets/tools/move/flip_l.png"),
				FLIP_R     = of("/assets/tools/move/flip_r.png"),
				FLIP_V     = of("/assets/tools/move/flip_v.png"),
				ROTATE_180 = of("/assets/tools/move/rotate_180.png"),
				ROTATE_CCW = of("/assets/tools/move/rotate_ccw.png"),
				ROTATE_CW  = of("/assets/tools/move/rotate_cw.png");
				//EXTRACT    = of("/assets/tools/move/extract.png");
		}
	}

	public static class BoundsHandling {

		public static final Image
			HORIZONTAL_IGNORE = of("/assets/bounds_handling/horizontal_ignore.png"),
			HORIZONTAL_CLAMP  = of("/assets/bounds_handling/horizontal_clamp.png"),
			HORIZONTAL_WRAP   = of("/assets/bounds_handling/horizontal_wrap.png"),
			VERTICAL_IGNORE   = of("/assets/bounds_handling/vertical_ignore.png"),
			VERTICAL_CLAMP    = of("/assets/bounds_handling/vertical_clamp.png"),
			VERTICAL_WRAP     = of("/assets/bounds_handling/vertical_wrap.png");
	}

	public static Image of(String path) {
		return new Image(Assets.class.getResourceAsStream(path));
	}
}