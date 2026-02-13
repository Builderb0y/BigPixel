package builderb0y.bigpixel.tools;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.LayerNode;
import builderb0y.bigpixel.sources.LayerSource.RedrawException;
import builderb0y.bigpixel.sources.ManualLayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.UniformSamplerProvider;
import builderb0y.bigpixel.sources.dependencies.inputs.SamplerProvider.VaryingSamplerProvider;
import builderb0y.bigpixel.views.LayerView.ProjectionResult;

public abstract class Tool<W> extends SourcelessTool<W> {

	public final ManualLayerSource source;
	public SimpleStringProperty labelText;

	public Tool(ToolType type, ManualLayerSource source) {
		super(type);
		this.source = source;
		this.labelText = new SimpleStringProperty();
		this.updateLabelText();
	}

	public abstract void onMouseDown(ProjectionResult position, MouseButton button);

	public abstract void onMouseDragged(ProjectionResult position, MouseButton button);

	@Override
	public void mouseDown(ProjectionResult position, MouseButton button) {
		if (this.canDraw(position.input())) {
			this.onMouseDown(position, button);
		}
	}

	@Override
	public void mouseDragged(ProjectionResult position, MouseButton button) {
		if (this.canDraw(position.input())) {
			this.onMouseDragged(position, button);
		}
	}

	public boolean canDraw(SamplerProvider samplerProvider) {
		return switch (samplerProvider) {
			case UniformSamplerProvider uniform -> false;
			case VaryingSamplerProvider varying -> {
				//very lenient check.
				LayerNode clicked = varying.getBackingLayer(), target = this.layer();
				yield clicked.imageWidth() == target.imageWidth() && clicked.imageHeight() == target.imageHeight();
			}
		};
	}

	public LayerNode layer() {
		return this.source.sources.layer;
	}

	@Override
	public void enter() {
		if (this.work != null) this.source.history.save(this.labelText.get());
		this.source.finishUsingTool();
		this.work = null;
		this.updateLabelText();
	}

	@Override
	public void escape() {
		if (this.work != null) {
			this.work = null;
			this.updateLabelText();
		}
		else {
			this.source.toolWithoutColorPicker.set(null);
		}
		this.source.cancelToolAction();
	}

	public abstract void redraw() throws RedrawException;

	public void requestRedraw() {
		this.layer().requestRedraw();
	}

	public boolean getSelection(Selection selection) {
		return false;
	}

	public abstract @Nullable Node getConfiguration();

	public abstract void updateLabelText();

	public static class Selection {

		public int minX, minY, maxX, maxY;

		@Override
		public String toString() {
			return "(" + this.minX + ", " + this.minY + ") -> (" + (this.maxX + 1) + ", " + (this.maxY + 1) + "): " + (this.maxX - this.minX + 1) + "x" + (this.maxY - this.minY + 1);
		}
	}

	public static record ToolType(String name, Image icon, Cursor cursor) {

		public ToolType(String name, Image icon, double x, double y) {
			this(name, icon, new ImageCursor(icon, x, y));
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}