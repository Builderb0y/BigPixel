package builderb0y.bigpixel.projectors;

import java.util.function.Function;

import javafx.scene.canvas.Canvas;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.ZoomableImage;

public abstract class ImageProjector {

	public final Type type;
	public final ZoomableImage zoomableImage;
	public double canvasWidth, canvasHeight;
	public int layerWidth, layerHeight;

	public ImageProjector(Type type, ZoomableImage zoomableImage) {
		this.type = type;
		this.zoomableImage = zoomableImage;
	}

	public void beforeRedraw(Canvas canvas, LayerNode layer) {
		this.canvasWidth = canvas.getWidth();
		this.canvasHeight = canvas.getHeight();
		this.layerWidth = layer.image.width;
		this.layerHeight = layer.image.height;
	}

	public abstract @Nullable Texcoord project(double x, double y);

	public static class Texcoord {

		public final int x;
		public final int y;

		public Texcoord(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public FloatVector sample(HDRImage image) {
			return image.getColor(this.x, this.y);
		}
	}

	public abstract void zoom(double x, double y, boolean zoomIn);

	public abstract void drag(double deltaX, double deltaY);

	public abstract void center();

	public abstract void drawOutline(
		byte[] pixels,
		int x1,
		int y1,
		int x2,
		int y2,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight,
		int dark,
		int light
	);

	public static void setColorSafe(byte[] pixels, int x, int y, int width, int height, int argb) {
		if (x >= 0 && x < width && y >= 0 && y < height) {
			int baseIndex = (y * width + x) << 2;
			pixels[baseIndex    ] = (byte)(argb);
			pixels[baseIndex | 1] = (byte)(argb >>> 8);
			pixels[baseIndex | 2] = (byte)(argb >>> 16);
			pixels[baseIndex | 3] = (byte)(argb >>> 24);
		}
	}

	public static enum Type {
		FLAT_CLAMPED(FlatClampedImageProjector::new),
		FLAT_TILING(FlatTilingImageProjector::new),
		CUBE(CubeImageProjector::new);

		public static final Type[] VALUES = values();

		public final Function<ZoomableImage, ImageProjector> constructor;

		Type(Function<ZoomableImage, ImageProjector> constructor) {
			this.constructor = constructor;
		}
	}
}