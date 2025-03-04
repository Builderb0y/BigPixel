package builderb0y.notgimp;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import jdk.incubator.vector.*;

public abstract class Gradient extends CanvasHelper {

	public static final VectorMask<Float> RGB_MASK = VectorMask.fromValues(FloatVector.SPECIES_128, true, true, true, false);
	public static final VectorMask<Byte> BYTE_MASK = VectorMask.fromValues(ByteVector.SPECIES_64, true, true, true, true, false, false, false, false);
	public static final VectorShuffle<Float> RGBA_TO_BRGA = VectorShuffle.fromValues(FloatVector.SPECIES_128, 2, 1, 0, 3);

	public abstract FloatVector computeColor(int pixelPos, float fraction);

	public void redraw() {
		PixelWriter writer = this.canvas.getGraphicsContext2D().getPixelWriter();
		int width = ((int)(this.canvas.getWidth()));
		byte[] colors = new byte[width * 4];
		width--;
		for (int x = 0; x <= width; x++) {
			putColor(this.computeColor(x, ((float)(x)) / ((float)(width))), colors, x << 2);
		}
		writer.setPixels(0, 0, width + 1, ((int)(this.canvas.getHeight())), PixelFormat.getByteBgraPreInstance(), colors, 0, 0);
	}

	public static void putColor(FloatVector color, byte[] colors, int index) {
		(
			(ByteVector)(
				color
				.mul(color.lane(HDRImage.ALPHA_OFFSET), RGB_MASK)
				.mul(256.0F)
				.rearrange(RGBA_TO_BRGA)
				.convert(VectorOperators.F2I, 0)
				.lanewise(VectorOperators.MIN, 255)
				.lanewise(VectorOperators.MAX, 0)
				.convertShape(VectorOperators.I2B, ByteVector.SPECIES_64, 0)
			)
		)
		.intoArray(colors, index, BYTE_MASK);
	}
}