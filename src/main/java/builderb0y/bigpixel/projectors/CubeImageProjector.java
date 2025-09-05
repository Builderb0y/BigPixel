package builderb0y.bigpixel.projectors;

import javafx.scene.canvas.Canvas;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.Util;
import builderb0y.bigpixel.ZoomableImage;

public class CubeImageProjector extends ImageProjector {

	public static final double CAMERA_DISTANCE = 2.0D;

	public double
		yaw,
		pitch,
		cosYaw   = 1.0D,
		sinYaw   = 0.0D,
		cosPitch = 1.0D,
		sinPitch = 0.0D,
		fov      = 0.5D;

	public CubeImageProjector(ZoomableImage zoomableImage) {
		super(Type.CUBE, zoomableImage);
		zoomableImage.f3.updateZoom(this.fov);
	}

	public void setRotation(double yaw, double pitch) {
		yaw = (yaw %= Math.PI * 2.0D) + (yaw < 0.0D ? Math.PI * 2.0D : 0.0D);
		this.yaw = yaw;
		this.cosYaw = Math.cos(yaw);
		this.sinYaw = Math.sin(yaw);

		pitch = Math.clamp(pitch, Math.PI * -0.5D, Math.PI * 0.5D);
		this.pitch = pitch;
		this.cosPitch = Math.cos(pitch);
		this.sinPitch = Math.sin(pitch);
	}

	@Override
	public @Nullable Texcoord project(double x, double y) {
		double uvX = x / this.canvasWidth  * 2.0D - 1.0D;
		double uvY = y / this.canvasHeight * 2.0D - 1.0D;
		if (this.canvasWidth > this.canvasHeight) {
			uvX *= this.canvasWidth / this.canvasHeight;
		}
		else {
			uvY *= this.canvasHeight / this.canvasWidth;
		}

		double cameraX =  CAMERA_DISTANCE;
		double cameraY =  uvX * 2.0D * (1.0D - this.fov);
		double cameraZ =  uvY * 2.0D * (1.0D - this.fov);
		double rayX    = -1.0D;
		double rayY    =  uvX * this.fov;
		double rayZ    =  uvY * this.fov;
		if (this.fov != 0.0D) {
			double rcpRayMagnitude = 1.0D / Math.sqrt(rayX * rayX + rayY * rayY + rayZ * rayZ);
			rayX *= rcpRayMagnitude;
			rayY *= rcpRayMagnitude;
			rayZ *= rcpRayMagnitude;
		}
		double tmp1 =  this.cosPitch * cameraX + this.sinPitch * cameraZ;
		double tmp2 = -this.sinPitch * cameraX + this.cosPitch * cameraZ;
		cameraX     =  tmp1;
		cameraZ     =  tmp2;
		tmp1        =  this.cosYaw * cameraX + this.sinYaw * cameraY;
		tmp2        = -this.sinYaw * cameraX + this.cosYaw * cameraY;
		cameraX     =  tmp1;
		cameraY     =  tmp2;
		tmp1        =  this.cosPitch * rayX + this.sinPitch * rayZ;
		tmp2        = -this.sinPitch * rayX + this.cosPitch * rayZ;
		rayX        =  tmp1;
		rayZ        =  tmp2;
		tmp1        =  this.cosYaw * rayX + this.sinYaw * rayY;
		tmp2        = -this.sinYaw * rayX + this.cosYaw * rayY;
		rayX        =  tmp1;
		rayY        =  tmp2;

		double dist = 1024.0D;
		double distX = (cameraX + Math.copySign(1.0D, rayX)) / -rayX;
		double distY = (cameraY + Math.copySign(1.0D, rayY)) / -rayY;
		double distZ = (cameraZ + Math.copySign(1.0D, rayZ)) / -rayZ;
		double resultX = Double.NaN;
		double resultY = Double.NaN;
		float shade = 1.0F;
		if (distX > 0.0D) {
			double intersectionX = cameraY + rayY * distX;
			if (intersectionX >= -1.0D && intersectionX <= 1.0D) {
				double intersectionY = cameraZ + rayZ * distX;
				if (intersectionY >= -1.0D && intersectionY <= 1.0D) {
					resultX = intersectionX * Math.copySign(0.5D, -rayX) + 0.5D;
					resultY = intersectionY * 0.5D + 0.5D;
					dist = distX;
					shade = 0.8F;
				}
			}
		}
		if (distY > 0.0D && distY < dist) {
			double intersectionX = cameraX + rayX * distY;
			if (intersectionX >= -1.0D && intersectionX <= 1.0D) {
				double intersectionY = cameraZ + rayZ * distY;
				if (intersectionY >= -1.0D && intersectionY <= 1.0D) {
					resultX = intersectionX * Math.copySign(0.5D, rayY) + 0.5D;
					resultY = intersectionY * 0.5D + 0.5D;
					dist = distX;
					shade = 0.6F;
				}
			}
		}
		if (distZ > 0.0D && distZ < dist) {
			double intersectionX = cameraY + rayY * distZ;
			if (intersectionX >= -1.0D && intersectionX <= 1.0D) {
				double intersectionY = cameraX + rayX * distZ;
				if (intersectionY >= -1.0D && intersectionY <= 1.0D) {
					resultX = intersectionX * 0.5D + 0.5D;
					resultY = intersectionY * Math.copySign(0.5D, rayZ) + 0.5D;
					shade = rayZ > 0.0D ? 1.0F : 0.5F;
				}
			}
		}
		if (Double.isNaN(resultX)) {
			return null;
		}
		else {
			final float shade_ = shade;
			return new Texcoord(
				Math.clamp((int)(resultX * this.layerWidth), 0, this.layerWidth  - 1),
				Math.clamp((int)(resultY * this.layerHeight), 0, this.layerHeight - 1)
			) {

				@Override
				public FloatVector sample(HDRImage image) {
					return super.sample(image).mul(shade_, Util.RGB_MASK);
				}
			};
		}
	}

	@Override
	public void zoom(double x, double y, boolean zoomIn) {
		double nextFov = zoomIn ? Math.min(this.fov + 0.0625D, 1.0D) : Math.max(this.fov - 0.0625D, 0.0D);
		if (this.fov != nextFov) {
			this.fov = nextFov;
			this.zoomableImage.f3.updateZoom(nextFov);
			this.zoomableImage.redraw();
		}
	}

	@Override
	public void drag(double deltaX, double deltaY) {
		Canvas canvas = this.zoomableImage.display.display;
		this.setRotation(
			this.yaw + deltaX * Math.PI / canvas.getWidth(),
			this.pitch + deltaY * Math.PI / canvas.getHeight()
		);
		this.zoomableImage.redraw();
	}

	@Override
	public void center() {
		this.setRotation(0.0D, 0.0D);
	}

	@Override
	public void drawOutline(
		byte[] pixels,
		int texelX1,
		int texelY1,
		int texelX2,
		int texelY2,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight,
		int dark,
		int light
	) {
		double cameraX = CAMERA_DISTANCE;
		double cameraY = 0.0D;
		double cameraZ = 0.0D;
		double tmp1    =  this.cosPitch * cameraX + this.sinPitch * cameraZ;
		double tmp2    = -this.sinPitch * cameraX + this.cosPitch * cameraZ;
		cameraX        =  tmp1;
		cameraZ        =  tmp2;
		tmp1           =  this.cosYaw * cameraX + this.sinYaw * cameraY;
		tmp2           = -this.sinYaw * cameraX + this.cosYaw * cameraY;
		cameraX        =  tmp1;
		cameraY        =  tmp2;

		double scaledTexelX1 = ((double)(texelX1)) / ((double)(layerWidth )) * 2.0D - 1.0D;
		double scaledTexelY1 = ((double)(texelY1)) / ((double)(layerHeight)) * 2.0D - 1.0D;
		double scaledTexelX2 = ((double)(texelX2)) / ((double)(layerWidth )) * 2.0D - 1.0D;
		double scaledTexelY2 = ((double)(texelY2)) / ((double)(layerHeight)) * 2.0D - 1.0D;
		if (cameraX > this.fov) {
			this.drawOutlinePart(
				pixels,
				1.0D, scaledTexelX1, scaledTexelY1,
				1.0D, scaledTexelX2, scaledTexelY1,
				1.0D, scaledTexelX2, scaledTexelY2,
				1.0D, scaledTexelX1, scaledTexelY2,
				canvasWidth,
				canvasHeight,
				dark,
				light
			);
		}
		if (cameraX < -this.fov) {
			this.drawOutlinePart(
				pixels,
				-1.0D, -scaledTexelX1, scaledTexelY1,
				-1.0D, -scaledTexelX2, scaledTexelY1,
				-1.0D, -scaledTexelX2, scaledTexelY2,
				-1.0D, -scaledTexelX1, scaledTexelY2,
				canvasWidth,
				canvasHeight,
				dark,
				light
			);
		}
		if (cameraY > this.fov) {
			this.drawOutlinePart(
				pixels,
				-scaledTexelX1, 1.0D, scaledTexelY1,
				-scaledTexelX2, 1.0D, scaledTexelY1,
				-scaledTexelX2, 1.0D, scaledTexelY2,
				-scaledTexelX1, 1.0D, scaledTexelY2,
				canvasWidth,
				canvasHeight,
				dark,
				light
			);
		}
		if (cameraY < -this.fov) {
			this.drawOutlinePart(
				pixels,
				scaledTexelX1, -1.0D, scaledTexelY1,
				scaledTexelX2, -1.0D, scaledTexelY1,
				scaledTexelX2, -1.0D, scaledTexelY2,
				scaledTexelX1, -1.0D, scaledTexelY2,
				canvasWidth,
				canvasHeight,
				dark,
				light
			);
		}
		if (cameraZ > this.fov) {
			this.drawOutlinePart(
				pixels,
				-scaledTexelY1, scaledTexelX1, 1.0D,
				-scaledTexelY1, scaledTexelX2, 1.0D,
				-scaledTexelY2, scaledTexelX2, 1.0D,
				-scaledTexelY2, scaledTexelX1, 1.0D,
				canvasWidth,
				canvasHeight,
				dark,
				light
			);
		}
		if (cameraZ < -this.fov) {
			this.drawOutlinePart(
				pixels,
				scaledTexelY1, scaledTexelX1, -1.0D,
				scaledTexelY1, scaledTexelX2, -1.0D,
				scaledTexelY2, scaledTexelX2, -1.0D,
				scaledTexelY2, scaledTexelX1, -1.0D,
				canvasWidth,
				canvasHeight,
				dark,
				light
			);
		}
	}

	public void drawOutlinePart(
		byte[] pixels,
		double x1, double y1, double z1,
		double x2, double y2, double z2,
		double x3, double y3, double z3,
		double x4, double y4, double z4,
		int canvasWidth,
		int canvasHeight,
		int dark,
		int light
	) {
		double tmp1, tmp2;
		tmp1 = this.cosYaw * x1 - this.sinYaw * y1;
		tmp2 = this.sinYaw * x1 + this.cosYaw * y1;
		x1   = tmp1;
		y1   = tmp2;
		tmp1 = this.cosPitch * x1 - this.sinPitch * z1;
		tmp2 = this.sinPitch * x1 + this.cosPitch * z1;
		x1   = tmp1;
		z1   = tmp2;

		tmp1 = this.cosYaw * x2 - this.sinYaw * y2;
		tmp2 = this.sinYaw * x2 + this.cosYaw * y2;
		x2   = tmp1;
		y2   = tmp2;
		tmp1 = this.cosPitch * x2 - this.sinPitch * z2;
		tmp2 = this.sinPitch * x2 + this.cosPitch * z2;
		x2   = tmp1;
		z2   = tmp2;

		tmp1 = this.cosYaw * x3 - this.sinYaw * y3;
		tmp2 = this.sinYaw * x3 + this.cosYaw * y3;
		x3   = tmp1;
		y3   = tmp2;
		tmp1 = this.cosPitch * x3 - this.sinPitch * z3;
		tmp2 = this.sinPitch * x3 + this.cosPitch * z3;
		x3   = tmp1;
		z3   = tmp2;

		tmp1 = this.cosYaw * x4 - this.sinYaw * y4;
		tmp2 = this.sinYaw * x4 + this.cosYaw * y4;
		x4   = tmp1;
		y4   = tmp2;
		tmp1 = this.cosPitch * x4 - this.sinPitch * z4;
		tmp2 = this.sinPitch * x4 + this.cosPitch * z4;
		x4   = tmp1;
		z4   = tmp2;

		double screenX1 = (CAMERA_DISTANCE * y1) / (CAMERA_DISTANCE - this.fov * x1);
		double screenY1 = (CAMERA_DISTANCE * z1) / (CAMERA_DISTANCE - this.fov * x1);
		double screenX2 = (CAMERA_DISTANCE * y2) / (CAMERA_DISTANCE - this.fov * x2);
		double screenY2 = (CAMERA_DISTANCE * z2) / (CAMERA_DISTANCE - this.fov * x2);
		double screenX3 = (CAMERA_DISTANCE * y3) / (CAMERA_DISTANCE - this.fov * x3);
		double screenY3 = (CAMERA_DISTANCE * z3) / (CAMERA_DISTANCE - this.fov * x3);
		double screenX4 = (CAMERA_DISTANCE * y4) / (CAMERA_DISTANCE - this.fov * x4);
		double screenY4 = (CAMERA_DISTANCE * z4) / (CAMERA_DISTANCE - this.fov * x4);
		if (canvasWidth > canvasHeight) {
			screenX1 *= ((double)(canvasHeight)) / ((double)(canvasWidth));
			screenX2 *= ((double)(canvasHeight)) / ((double)(canvasWidth));
			screenX3 *= ((double)(canvasHeight)) / ((double)(canvasWidth));
			screenX4 *= ((double)(canvasHeight)) / ((double)(canvasWidth));
		}
		else {
			screenY1 *= ((double)(canvasWidth)) / ((double)(canvasHeight));
			screenY2 *= ((double)(canvasWidth)) / ((double)(canvasHeight));
			screenY3 *= ((double)(canvasWidth)) / ((double)(canvasHeight));
			screenY4 *= ((double)(canvasWidth)) / ((double)(canvasHeight));
		}
		double canvasX1 = (screenX1 * 0.25D + 0.5D) * canvasWidth ;
		double canvasY1 = (screenY1 * 0.25D + 0.5D) * canvasHeight;
		double canvasX2 = (screenX2 * 0.25D + 0.5D) * canvasWidth ;
		double canvasY2 = (screenY2 * 0.25D + 0.5D) * canvasHeight;
		double canvasX3 = (screenX3 * 0.25D + 0.5D) * canvasWidth ;
		double canvasY3 = (screenY3 * 0.25D + 0.5D) * canvasHeight;
		double canvasX4 = (screenX4 * 0.25D + 0.5D) * canvasWidth ;
		double canvasY4 = (screenY4 * 0.25D + 0.5D) * canvasHeight;

		drawLine(pixels, canvasX1, canvasY1, canvasX2, canvasY2, canvasWidth, canvasHeight, dark, light);
		drawLine(pixels, canvasX2, canvasY2, canvasX3, canvasY3, canvasWidth, canvasHeight, dark, light);
		drawLine(pixels, canvasX3, canvasY3, canvasX4, canvasY4, canvasWidth, canvasHeight, dark, light);
		drawLine(pixels, canvasX4, canvasY4, canvasX1, canvasY1, canvasWidth, canvasHeight, dark, light);
	}

	public static void drawLine(
		byte[] pixels,
		double x1,
		double y1,
		double x2,
		double y2,
		int canvasWidth,
		int canvasHeight,
		int dark,
		int light
	) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		int steps = (int)(Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
		double rcpSteps = 1.0D / steps;
		for (int step = 0; step < steps; step++) {
			double frac = step * rcpSteps;
			int x = (int)(x1 + dx * frac);
			int y = (int)(y1 + dy * frac);
			setColorSafe(pixels, x, y, canvasWidth, canvasHeight, (step & 8) == 0 ? dark : light);
		}
	}
}