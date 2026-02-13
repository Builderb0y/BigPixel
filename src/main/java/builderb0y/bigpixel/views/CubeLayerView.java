package builderb0y.bigpixel.views;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import jdk.incubator.vector.FloatVector;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.ZoomableImage.DrawParams;
import builderb0y.bigpixel.F3Menu;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.ZoomableImage;
import builderb0y.bigpixel.json.JsonMap;
import builderb0y.bigpixel.sources.dependencies.LayerDependencies;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.util.Util;

public class CubeLayerView extends LayerView {

	public static final double CAMERA_DISTANCE = 2.0D;

	public CheckBox
		shade = this.parameters.addCheckbox("shade", "Shade", true);
	public HBox
		topConfigOptions = new HBox(this.drawOutline, this.shade);
	public CubeDependencies
		dependencies = new CubeDependencies(this);
	public BorderPane
		rootConfigPane = new BorderPane();
	public final SimpleDoubleProperty
		yaw = new SimpleDoubleProperty(this, "yaw"),
		pitch = new SimpleDoubleProperty(this, "pitch"),
		fov = new SimpleDoubleProperty(this, "fov", 0.5D);
	public double
		cosYaw   = 1.0D,
		sinYaw   = 0.0D,
		cosPitch = 1.0D,
		sinPitch = 0.0D,
		fovCache = this.fov.get();
	public boolean
		loaded;
	public final ObjectBinding<DrawParams> drawParams;

	public CubeLayerView(LayerViews views) {
		super(LayerViewType.CUBE, views);
		this.dependencies.setAll(views.layer);
		this.rootConfigPane.setTop(this.topConfigOptions);
		this.rootConfigPane.setCenter(this.dependencies.getConfigPane());
		this.yaw.addListener(Util.change((Number yaw) -> {
			this.cosYaw = Math.cos(yaw.doubleValue());
			this.sinYaw = Math.sin(yaw.doubleValue());
		}));
		this.pitch.addListener(Util.change((Number pitch) -> {
			this.cosPitch = Math.cos(pitch.doubleValue());
			this.sinPitch = Math.sin(pitch.doubleValue());
		}));
		this.fov.addListener(Util.change((Number fov) -> this.fovCache = fov.doubleValue()));
		this.drawParams = Bindings.createObjectBinding(
			() -> {
				record Params(
					double yaw,
					double pitch,
					double fov,
					boolean shaded,
					boolean outline,
					CubeDependencies.Params deps
				)
				implements DrawParams {}
				return new Params(
					this.yaw.get(),
					this.pitch.get(),
					this.fov.get(),
					this.shade.isSelected(),
					this.drawOutline.isSelected(),
					this.dependencies.drawParams.get()
				);
			},
			this.yaw,
			this.pitch,
			this.fov,
			this.shade.selectedProperty(),
			this.drawOutline.selectedProperty(),
			this.dependencies.drawParams
		);
	}

	@Override
	public ObservableValue<DrawParams> drawParamsProperty() {
		return this.drawParams;
	}

	@Override
	public LayerDependencies getDependencies() {
		return this.dependencies;
	}

	@Override
	public JsonMap save() {
		return super.save().with("yaw", this.yaw.get()).with("pitch", this.pitch.get()).with("fov", this.fov.get());
	}

	@Override
	public void load(JsonMap root) {
		super.load(root);
		this.setRotation(root.getDouble("yaw"), root.getDouble("pitch"));
		double fov = root.getDouble("fov");
		this.fov.set(Double.isNaN(fov) ? 0.5D : Math.clamp(fov, 0.0D, 1.0D));
		this.loaded = true;
	}

	public void setRotation(double yaw, double pitch) {
		yaw = Double.isNaN(yaw) ? 0.0D : (yaw %= Math.PI * 2.0D) + (yaw < 0.0D ? Math.PI * 2.0D : 0.0D);
		this.yaw.set(yaw);
		pitch = Double.isNaN(pitch) ? 0.0D : Math.clamp(pitch, Math.PI * -0.5D, Math.PI * 0.5D);
		this.pitch.set(pitch);
	}

	@Override
	public @Nullable ProjectionResult project(double x, double y, int frameIndex) {
		double uvX = x / this.canvasWidth  * 2.0D - 1.0D;
		double uvY = y / this.canvasHeight * 2.0D - 1.0D;
		if (this.canvasWidth > this.canvasHeight) {
			uvX *= this.canvasWidth / this.canvasHeight;
		}
		else {
			uvY *= this.canvasHeight / this.canvasWidth;
		}

		double cameraX =  uvX * 2.0D * (1.0D - this.fovCache);
		double cameraY = -uvY * 2.0D * (1.0D - this.fovCache);
		double cameraZ =  CAMERA_DISTANCE;
		double rayX    =  uvX * this.fovCache;
		double rayY    = -uvY * this.fovCache;
		double rayZ    = -1.0D;
		if (this.fovCache != 0.0D) {
			double rcpRayMagnitude = 1.0D / Math.sqrt(rayX * rayX + rayY * rayY + rayZ * rayZ);
			rayX *= rcpRayMagnitude;
			rayY *= rcpRayMagnitude;
			rayZ *= rcpRayMagnitude;
		}
		double tmp1 =  this.cosPitch * cameraZ + this.sinPitch * cameraY;
		double tmp2 = -this.sinPitch * cameraZ + this.cosPitch * cameraY;
		cameraZ     =  tmp1;
		cameraY     =  tmp2;
		tmp1        =  this.cosYaw * cameraX + this.sinYaw * cameraZ;
		tmp2        = -this.sinYaw * cameraX + this.cosYaw * cameraZ;
		cameraX     =  tmp1;
		cameraZ     =  tmp2;
		tmp1        =  this.cosPitch * rayZ + this.sinPitch * rayY;
		tmp2        = -this.sinPitch * rayZ + this.cosPitch * rayY;
		rayZ        =  tmp1;
		rayY        =  tmp2;
		tmp1        =  this.cosYaw * rayX + this.sinYaw * rayZ;
		tmp2        = -this.sinYaw * rayX + this.cosYaw * rayZ;
		rayX        =  tmp1;
		rayZ        =  tmp2;

		CubeDimensions dimensions = this.dependencies.dimensions;
		double minX = dimensions.minX * 2.0D - 1.0D;
		double minY = dimensions.minY * 2.0D - 1.0D;
		double minZ = dimensions.minZ * 2.0D - 1.0D;
		double maxX = dimensions.maxX * 2.0D - 1.0D;
		double maxY = dimensions.maxY * 2.0D - 1.0D;
		double maxZ = dimensions.maxZ * 2.0D - 1.0D;
		double dist = 1024.0D;
		double distX = ((rayX < 0.0D ? maxX : minX) - cameraX) / rayX;
		double distY = ((rayY < 0.0D ? maxY : minY) - cameraY) / rayY;
		double distZ = ((rayZ < 0.0D ? maxZ : minZ) - cameraZ) / rayZ;
		double resultX = Double.NaN;
		double resultY = Double.NaN;
		float shade = 1.0F;
		FaceInputBinding hitBinding = null;
		if (distX > 0.0D) {
			double intersectionX = cameraY + rayY * distX;
			if (intersectionX >= minY && intersectionX <= maxY) {
				double intersectionY = cameraZ + rayZ * distX;
				if (intersectionY >= minZ && intersectionY <= maxZ) {
					resultX = multiplySigns(Util.unmix(maxZ, minZ, intersectionY) * 2.0D - 1.0D, -rayX);
					resultY = Util.unmix(maxY, minY, intersectionX) * 2.0D - 1.0D;
					dist = distX;
					shade = 0.6F;
					hitBinding = rayX > 0.0 ? this.dependencies.west : this.dependencies.east;
				}
			}
		}
		if (distY > 0.0D && distY < dist) {
			double intersectionX = cameraX + rayX * distY;
			if (intersectionX >= minX && intersectionX <= maxX) {
				double intersectionY = cameraZ + rayZ * distY;
				if (intersectionY >= minZ && intersectionY <= maxZ) {
					resultX = Util.unmix(minX, maxX, intersectionX) * 2.0D - 1.0D;
					resultY = multiplySigns(Util.unmix(minZ, maxZ, intersectionY) * 2.0D - 1.0D, -rayY);
					dist = distX;
					shade = rayY < 0.0D ? 1.0F : 0.5F;
					hitBinding = rayY > 0.0 ? this.dependencies.down : this.dependencies.up;
				}
			}
		}
		if (distZ > 0.0D && distZ < dist) {
			double intersectionX = cameraY + rayY * distZ;
			if (intersectionX >= minY && intersectionX <= maxY) {
				double intersectionY = cameraX + rayX * distZ;
				if (intersectionY >= minX && intersectionY <= maxX) {
					resultX = multiplySigns(Util.unmix(maxX, minX, intersectionY) * 2.0D - 1.0D, rayZ);
					resultY = Util.unmix(maxY, minY, intersectionX) * 2.0D - 1.0D;
					shade = 0.8F;
					hitBinding = rayZ > 0.0 ? this.dependencies.north : this.dependencies.south;
				}
			}
		}
		if (hitBinding == null || !hitBinding.enabled.isSelected()) {
			return null;
		}
		else {
			return switch (hitBinding.getCurrent()) {
				case UniformSamplerProvider uniform -> {
					FloatVector color = uniform.getColor();
					float
						r = color.lane(HDRImage.  RED_OFFSET),
						g = color.lane(HDRImage.GREEN_OFFSET),
						b = color.lane(HDRImage. BLUE_OFFSET),
						a = color.lane(HDRImage.ALPHA_OFFSET);
					yield new ProjectionResult(
						uniform,
						0.0D,
						0.0D,
						0,
						0,
						r, g, b, a,
						this.shade.isSelected() ? shade : 1.0F
					);
				}
				case VaryingSamplerProvider varying -> {
					HDRImage image = varying.getBackingLayer().getFrame(frameIndex);
					double rawX = hitBinding.getU(resultX, resultY) * this.layerWidth;
					double rawY = hitBinding.getV(resultX, resultY) * this.layerHeight;
					int projectedX = Math.clamp((int)(rawX), 0, this.layerWidth - 1);
					int projectedY = Math.clamp((int)(rawY), 0, this.layerHeight - 1);
					int baseIndex = image.baseIndex(projectedX, projectedY);
					float r = image.pixels[baseIndex | HDRImage.  RED_OFFSET];
					float g = image.pixels[baseIndex | HDRImage.GREEN_OFFSET];
					float b = image.pixels[baseIndex | HDRImage. BLUE_OFFSET];
					float a = image.pixels[baseIndex | HDRImage.ALPHA_OFFSET];
					yield new ProjectionResult(varying, rawX, rawY, projectedX, projectedY, r, g, b, a, this.shade.isSelected() ? shade : 1.0F);
				}
			};
		}
	}

	public static double multiplySigns(double magnitude, double sign) {
		return Double.longBitsToDouble(Double.doubleToRawLongBits(magnitude) ^ (Double.doubleToRawLongBits(sign) & Long.MIN_VALUE));
	}

	@Override
	public void zoom(double x, double y, boolean zoomIn) {
		double nextFov = zoomIn ? Math.min(this.fov.get() + 0.0625D, 1.0D) : Math.max(this.fov.get() - 0.0625D, 0.0D);
		if (this.fov.get() != nextFov) {
			this.fov.set(nextFov);
			//this.views.layer.graph.openImage.imageDisplay.redrawLater();
		}
	}

	@Override
	public void drag(double deltaX, double deltaY) {
		ZoomableImage display = this.views.layer.graph.openImage.imageDisplay;
		Canvas canvas = display.canvasHolder.display;
		this.setRotation(
			this.yaw.get() - deltaX * Math.PI / canvas.getWidth(),
			this.pitch.get() - deltaY * Math.PI / canvas.getHeight()
		);
		//display.redrawLater();
	}

	@Override
	public void center() {
		if (this.loaded) {
			this.loaded = false;
		}
		else {
			this.fov.set(0.5D);
			this.setRotation(0.0D, 0.0D);
		}
	}

	@Override
	public void drawLayerOutline(
		byte[] pixels,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight
	) {
		double
			cameraX = 0.0D,
			cameraY = 0.0D,
			cameraZ = CAMERA_DISTANCE,
			tmp1, tmp2;
		tmp1    =  this.cosPitch * cameraZ + this.sinPitch * cameraY;
		tmp2    = -this.sinPitch * cameraZ + this.cosPitch * cameraY;
		cameraZ =  tmp1;
		cameraY =  tmp2;
		tmp1    =  this.cosYaw * cameraX + this.sinYaw * cameraZ;
		tmp2    = -this.sinYaw * cameraX + this.cosYaw * cameraZ;
		cameraX =  tmp1;
		cameraZ =  tmp2;

		double x000 = -1.0D, y000 = -1.0D, z000 = -1.0D;
		tmp1 = this.cosYaw * x000 - this.sinYaw * z000;
		tmp2 = this.sinYaw * x000 + this.cosYaw * z000;
		x000 = tmp1;
		z000 = tmp2;
		tmp1 = this.cosPitch * z000 - this.sinPitch * y000;
		tmp2 = this.sinPitch * z000 + this.cosPitch * y000;
		z000 = tmp1;
		y000 = tmp2;

		double x001 = -1.0D, y001 = -1.0D, z001 = 1.0D;
		tmp1 = this.cosYaw * x001 - this.sinYaw * z001;
		tmp2 = this.sinYaw * x001 + this.cosYaw * z001;
		x001 = tmp1;
		z001 = tmp2;
		tmp1 = this.cosPitch * z001 - this.sinPitch * y001;
		tmp2 = this.sinPitch * z001 + this.cosPitch * y001;
		z001 = tmp1;
		y001 = tmp2;

		double x010 = -1.0D, y010 = 1.0D, z010 = -1.0D;
		tmp1 = this.cosYaw * x010 - this.sinYaw * z010;
		tmp2 = this.sinYaw * x010 + this.cosYaw * z010;
		x010 = tmp1;
		z010 = tmp2;
		tmp1 = this.cosPitch * z010 - this.sinPitch * y010;
		tmp2 = this.sinPitch * z010 + this.cosPitch * y010;
		z010 = tmp1;
		y010 = tmp2;

		double x011 = -1.0D, y011 = 1.0D, z011 = 1.0D;
		tmp1 = this.cosYaw * x011 - this.sinYaw * z011;
		tmp2 = this.sinYaw * x011 + this.cosYaw * z011;
		x011 = tmp1;
		z011 = tmp2;
		tmp1 = this.cosPitch * z011 - this.sinPitch * y011;
		tmp2 = this.sinPitch * z011 + this.cosPitch * y011;
		z011 = tmp1;
		y011 = tmp2;

		double x100 = 1.0D, y100 = -1.0D, z100 = -1.0D;
		tmp1 = this.cosYaw * x100 - this.sinYaw * z100;
		tmp2 = this.sinYaw * x100 + this.cosYaw * z100;
		x100 = tmp1;
		z100 = tmp2;
		tmp1 = this.cosPitch * z100 - this.sinPitch * y100;
		tmp2 = this.sinPitch * z100 + this.cosPitch * y100;
		z100 = tmp1;
		y100 = tmp2;

		double x101 = 1.0D, y101 = -1.0D, z101 = 1.0D;
		tmp1 = this.cosYaw * x101 - this.sinYaw * z101;
		tmp2 = this.sinYaw * x101 + this.cosYaw * z101;
		x101 = tmp1;
		z101 = tmp2;
		tmp1 = this.cosPitch * z101 - this.sinPitch * y101;
		tmp2 = this.sinPitch * z101 + this.cosPitch * y101;
		z101 = tmp1;
		y101 = tmp2;

		double x110 = 1.0D, y110 = 1.0D, z110 = -1.0D;
		tmp1 = this.cosYaw * x110 - this.sinYaw * z110;
		tmp2 = this.sinYaw * x110 + this.cosYaw * z110;
		x110 = tmp1;
		z110 = tmp2;
		tmp1 = this.cosPitch * z110 - this.sinPitch * y110;
		tmp2 = this.sinPitch * z110 + this.cosPitch * y110;
		z110 = tmp1;
		y110 = tmp2;

		double x111 = 1.0D, y111 = 1.0D, z111 = 1.0D;
		tmp1 = this.cosYaw * x111 - this.sinYaw * z111;
		tmp2 = this.sinYaw * x111 + this.cosYaw * z111;
		x111 = tmp1;
		z111 = tmp2;
		tmp1 = this.cosPitch * z111 - this.sinPitch * y111;
		tmp2 = this.sinPitch * z111 + this.cosPitch * y111;
		z111 = tmp1;
		y111 = tmp2;

		if (cameraX < -this.fovCache || cameraZ < -this.fovCache) this.drawTransformedLine(pixels, x000, y000, z000, x010, y010, z010, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraX >  this.fovCache || cameraZ < -this.fovCache) this.drawTransformedLine(pixels, x100, y100, z100, x110, y110, z110, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraX < -this.fovCache || cameraZ >  this.fovCache) this.drawTransformedLine(pixels, x001, y001, z001, x011, y011, z011, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraX >  this.fovCache || cameraZ >  this.fovCache) this.drawTransformedLine(pixels, x101, y101, z101, x111, y111, z111, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);

		if (cameraX < -this.fovCache || cameraY < -this.fovCache) this.drawTransformedLine(pixels, x000, y000, z000, x001, y001, z001, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraX >  this.fovCache || cameraY < -this.fovCache) this.drawTransformedLine(pixels, x100, y100, z100, x101, y101, z101, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraX < -this.fovCache || cameraY >  this.fovCache) this.drawTransformedLine(pixels, x010, y010, z010, x011, y011, z011, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraX >  this.fovCache || cameraY >  this.fovCache) this.drawTransformedLine(pixels, x110, y110, z110, x111, y111, z111, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);

		if (cameraY < -this.fovCache || cameraZ < -this.fovCache) this.drawTransformedLine(pixels, x000, y000, z000, x100, y100, z100, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraY >  this.fovCache || cameraZ < -this.fovCache) this.drawTransformedLine(pixels, x010, y010, z010, x110, y110, z110, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraY < -this.fovCache || cameraZ >  this.fovCache) this.drawTransformedLine(pixels, x001, y001, z001, x101, y101, z101, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
		if (cameraY >  this.fovCache || cameraZ >  this.fovCache) this.drawTransformedLine(pixels, x011, y011, z011, x111, y111, z111, canvasWidth, canvasHeight, LAYER_OUTLINE_DARK, LAYER_OUTLINE_LIGHT);
	}

	@Override
	public void drawSelectionOutline(
		LayerNode layer,
		byte[] pixels,
		int x1,
		int y1,
		int x2,
		int y2,
		int layerWidth,
		int layerHeight,
		int canvasWidth,
		int canvasHeight
	) {
		double
			cameraX = 0.0D,
			cameraY = 0.0D,
			cameraZ = CAMERA_DISTANCE,
			tmp1, tmp2;
		tmp1    =  this.cosPitch * cameraZ + this.sinPitch * cameraY;
		tmp2    = -this.sinPitch * cameraZ + this.cosPitch * cameraY;
		cameraZ =  tmp1;
		cameraY =  tmp2;
		tmp1    =  this.cosYaw * cameraX + this.sinYaw * cameraZ;
		tmp2    = -this.sinYaw * cameraX + this.cosYaw * cameraZ;
		cameraX =  tmp1;
		cameraZ =  tmp2;

		double scaledX1 = ((double)(x1)) / ((double)(layerWidth ));
		double scaledY1 = ((double)(y1)) / ((double)(layerHeight));
		double scaledX2 = ((double)(x2)) / ((double)(layerWidth ));
		double scaledY2 = ((double)(y2)) / ((double)(layerHeight));

		double plane;

		CubeDependencies deps = this.dependencies;
		CubeDimensions dims = deps.dimensions;
		if (cameraY > this.fovCache * (plane = dims.maxY * 2.0D - 1.0D)) {
			this.drawUntransformedQuad(
				pixels,
				Util.mix(dims.minX, dims.maxX, deps.up.ungetU(scaledX1, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.minZ, dims.maxZ, deps.up.ungetV(scaledX1, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.minX, dims.maxX, deps.up.ungetU(scaledX1, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.minZ, dims.maxZ, deps.up.ungetV(scaledX1, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.minX, dims.maxX, deps.up.ungetU(scaledX2, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.minZ, dims.maxZ, deps.up.ungetV(scaledX2, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.minX, dims.maxX, deps.up.ungetU(scaledX2, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.minZ, dims.maxZ, deps.up.ungetV(scaledX2, scaledY1)) * 2.0D - 1.0D,
				canvasWidth,
				canvasHeight,
				SELECTION_OUTLINE_DARK,
				SELECTION_OUTLINE_LIGHT
			);
		}
		if (cameraY < this.fovCache * (plane = dims.minY * 2.0D - 1.0D)) {
			this.drawUntransformedQuad(
				pixels,
				Util.mix(dims.minX, dims.maxX, deps.down.ungetU(scaledX1, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxZ, dims.minZ, deps.down.ungetV(scaledX1, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.minX, dims.maxX, deps.down.ungetU(scaledX1, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxZ, dims.minZ, deps.down.ungetV(scaledX1, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.minX, dims.maxX, deps.down.ungetU(scaledX2, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxZ, dims.minZ, deps.down.ungetV(scaledX2, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.minX, dims.maxX, deps.down.ungetU(scaledX2, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxZ, dims.minZ, deps.down.ungetV(scaledX2, scaledY1)) * 2.0D - 1.0D,
				canvasWidth,
				canvasHeight,
				SELECTION_OUTLINE_DARK,
				SELECTION_OUTLINE_LIGHT
			);
		}
		if (cameraZ > this.fovCache * (plane = dims.maxZ * 2.0D - 1.0D)) {
			this.drawUntransformedQuad(
				pixels,
				Util.mix(dims.minX, dims.maxX, deps.south.ungetU(scaledX1, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.south.ungetV(scaledX1, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.minX, dims.maxX, deps.south.ungetU(scaledX1, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.south.ungetV(scaledX1, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.minX, dims.maxX, deps.south.ungetU(scaledX2, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.south.ungetV(scaledX2, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.minX, dims.maxX, deps.south.ungetU(scaledX2, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.south.ungetV(scaledX2, scaledY1)) * 2.0D - 1.0D,
				plane,
				canvasWidth,
				canvasHeight,
				SELECTION_OUTLINE_DARK,
				SELECTION_OUTLINE_LIGHT
			);
		}
		if (cameraZ < this.fovCache * (plane = dims.minZ * 2.0D - 1.0D)) {
			this.drawUntransformedQuad(
				pixels,
				Util.mix(dims.maxX, dims.minX, deps.north.ungetU(scaledX1, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.north.ungetV(scaledX1, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxX, dims.minX, deps.north.ungetU(scaledX1, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.north.ungetV(scaledX1, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxX, dims.minX, deps.north.ungetU(scaledX2, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.north.ungetV(scaledX2, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxX, dims.minX, deps.north.ungetU(scaledX2, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.maxY, dims.minY, deps.north.ungetV(scaledX2, scaledY1)) * 2.0D - 1.0D,
				plane,
				canvasWidth,
				canvasHeight,
				SELECTION_OUTLINE_DARK,
				SELECTION_OUTLINE_LIGHT
			);
		}
		if (cameraX > this.fovCache * (plane = dims.maxX * 2.0D - 1.0D)) {
			this.drawUntransformedQuad(
				pixels,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.east.ungetV(scaledX1, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.maxZ, dims.minZ, deps.east.ungetU(scaledX1, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.east.ungetV(scaledX1, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.maxZ, dims.minZ, deps.east.ungetU(scaledX1, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.east.ungetV(scaledX2, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.maxZ, dims.minZ, deps.east.ungetU(scaledX2, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.east.ungetV(scaledX2, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.maxZ, dims.minZ, deps.east.ungetU(scaledX2, scaledY1)) * 2.0D - 1.0D,
				canvasWidth,
				canvasHeight,
				SELECTION_OUTLINE_DARK,
				SELECTION_OUTLINE_LIGHT
			);
		}
		if (cameraX < this.fovCache * (plane = dims.minX * 2.0D - 1.0D)) {
			this.drawUntransformedQuad(
				pixels,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.west.ungetV(scaledX1, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.minZ, dims.maxZ, deps.west.ungetU(scaledX1, scaledY1)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.west.ungetV(scaledX1, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.minZ, dims.maxZ, deps.west.ungetU(scaledX1, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.west.ungetV(scaledX2, scaledY2)) * 2.0D - 1.0D,
				Util.mix(dims.minZ, dims.maxZ, deps.west.ungetU(scaledX2, scaledY2)) * 2.0D - 1.0D,
				plane,
				Util.mix(dims.maxY, dims.minY, deps.west.ungetV(scaledX2, scaledY1)) * 2.0D - 1.0D,
				Util.mix(dims.minZ, dims.maxZ, deps.west.ungetU(scaledX2, scaledY1)) * 2.0D - 1.0D,
				canvasWidth,
				canvasHeight,
				SELECTION_OUTLINE_DARK,
				SELECTION_OUTLINE_LIGHT
			);
		}
	}

	public void drawUntransformedQuad(
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

		tmp1 = this.cosYaw * x1 - this.sinYaw * z1;
		tmp2 = this.sinYaw * x1 + this.cosYaw * z1;
		x1   = tmp1;
		z1   = tmp2;
		tmp1 = this.cosPitch * z1 - this.sinPitch * y1;
		tmp2 = this.sinPitch * z1 + this.cosPitch * y1;
		z1   = tmp1;
		y1   = tmp2;

		tmp1 = this.cosYaw * x2 - this.sinYaw * z2;
		tmp2 = this.sinYaw * x2 + this.cosYaw * z2;
		x2   = tmp1;
		z2   = tmp2;
		tmp1 = this.cosPitch * z2 - this.sinPitch * y2;
		tmp2 = this.sinPitch * z2 + this.cosPitch * y2;
		z2   = tmp1;
		y2   = tmp2;

		tmp1 = this.cosYaw * x3 - this.sinYaw * z3;
		tmp2 = this.sinYaw * x3 + this.cosYaw * z3;
		x3   = tmp1;
		z3   = tmp2;
		tmp1 = this.cosPitch * z3 - this.sinPitch * y3;
		tmp2 = this.sinPitch * z3 + this.cosPitch * y3;
		z3   = tmp1;
		y3   = tmp2;

		tmp1 = this.cosYaw * x4 - this.sinYaw * z4;
		tmp2 = this.sinYaw * x4 + this.cosYaw * z4;
		x4   = tmp1;
		z4   = tmp2;
		tmp1 = this.cosPitch * z4 - this.sinPitch * y4;
		tmp2 = this.sinPitch * z4 + this.cosPitch * y4;
		z4   = tmp1;
		y4   = tmp2;

		this.drawTransformedLine(pixels, x1, y1, z1, x2, y2, z2, canvasWidth, canvasHeight, dark, light);
		this.drawTransformedLine(pixels, x2, y2, z2, x3, y3, z3, canvasWidth, canvasHeight, dark, light);
		this.drawTransformedLine(pixels, x3, y3, z3, x4, y4, z4, canvasWidth, canvasHeight, dark, light);
		this.drawTransformedLine(pixels, x4, y4, z4, x1, y1, z1, canvasWidth, canvasHeight, dark, light);
	}

	public void drawTransformedLine(
		byte[] pixels,
		double x1, double y1, double z1,
		double x2, double y2, double z2,
		int canvasWidth,
		int canvasHeight,
		int dark,
		int light
	) {
		double screenX1 = (CAMERA_DISTANCE * x1) / (CAMERA_DISTANCE - this.fovCache * z1);
		double screenY1 = (CAMERA_DISTANCE * y1) / (CAMERA_DISTANCE - this.fovCache * z1);
		double screenX2 = (CAMERA_DISTANCE * x2) / (CAMERA_DISTANCE - this.fovCache * z2);
		double screenY2 = (CAMERA_DISTANCE * y2) / (CAMERA_DISTANCE - this.fovCache * z2);
		if (canvasWidth > canvasHeight) {
			screenX1 *= ((double)(canvasHeight)) / ((double)(canvasWidth));
			screenX2 *= ((double)(canvasHeight)) / ((double)(canvasWidth));
		}
		else {
			screenY1 *= ((double)(canvasWidth)) / ((double)(canvasHeight));
			screenY2 *= ((double)(canvasWidth)) / ((double)(canvasHeight));
		}
		double canvasX1 = (screenX1 *  0.25D + 0.5D) * canvasWidth ;
		double canvasY1 = (screenY1 * -0.25D + 0.5D) * canvasHeight;
		double canvasX2 = (screenX2 *  0.25D + 0.5D) * canvasWidth ;
		double canvasY2 = (screenY2 * -0.25D + 0.5D) * canvasHeight;

		drawLine(pixels, canvasX1, canvasY1, canvasX2, canvasY2, canvasWidth, canvasHeight, dark, light);
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

	@Override
	public Node getRootConfigPane() {
		return this.rootConfigPane;
	}

	@Override
	public void updateF3(F3Menu f3) {
		f3.add("FOV: " + this.fov);
	}
}