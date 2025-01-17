package builderb0y.notgimp.tools;

import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.LayerSources;
import builderb0y.notgimp.NotGimp;

public abstract class Tool<W extends Tool.Work> {

	public final ToolType type;
	public @Nullable W work;
	public SimpleStringProperty labelText;

	public Tool(ToolType type) {
		this.type = type;
		this.labelText = new SimpleStringProperty();
		this.updateLabelText();
	}

	public abstract void mouseDown(Layer layer, int x, int y, MouseButton button);

	public abstract void mouseDragged(int x, int y, MouseButton button);

	public abstract void colorChanged();

	public void keyPressed(Layer layer, KeyCode key) {
		if (key == KeyCode.ENTER) {
			this.enter(layer);
		}
		else if (key == KeyCode.ESCAPE) {
			this.escape(layer);
		}
	}

	public void enter(Layer layer) {
		if (this.work != null) layer.history.save(this.labelText.get());
		layer.finishUsingTool();
		this.work = null;
		this.updateLabelText();
	}

	public void escape(Layer layer) {
		if (this.work != null) {
			this.work = null;
			this.updateLabelText();
		}
		else {
			layer.openImage.mainWindow.currentTool.set(null);
		}
		layer.cancelToolAction();
	}

	public boolean getSelection(Selection selection) {
		return false;
	}

	public abstract @Nullable Node getConfiguration();

	public abstract void updateLabelText();

	public static class Work {

		public Layer layer;

		public Work(Layer layer) {
			this.layer = layer;
		}
	}

	public static class Selection {

		public int minX, minY, maxX, maxY;
	}

	public static record ToolType(String name, Image icon, Cursor cursor, Function<Tools, Tool<?>> getter) {

		public ToolType(String name, Image icon, double x, double y, Function<Tools, Tool<?>> getter) {
			this(name, icon, new ImageCursor(icon, x, y), getter);
		}

		public ToolType(String name, double x, double y, Function<Tools, Tool<?>> getter) {
			this(name, new Image(NotGimp.class.getClassLoader().getResourceAsStream("assets/tools/" + name + ".png")), x, y, getter);
		}

		public Tool<?> getTool(Tools tools) {
			return this.getter.apply(tools);
		}

		public Tool<?> getTool(LayerSources sources) {
			return this.getTool(sources.tools);
		}

		public Tool<?> getTool(Layer layer) {
			return this.getTool(layer.sources);
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}