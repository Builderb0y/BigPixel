package builderb0y.notgimp.tools;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.NotGimp;
import builderb0y.notgimp.sources.ManualLayerSource;

public abstract class Tool<W> {

	public final ToolType type;
	public final ManualLayerSource source;
	public @Nullable W work;
	public SimpleStringProperty labelText;

	public Tool(ToolType type, ManualLayerSource source) {
		this.type = type;
		this.source = source;
		this.labelText = new SimpleStringProperty();
		this.updateLabelText();
	}

	public Layer layer() {
		return this.source.sources.layer;
	}

	public abstract void mouseDown(int x, int y, MouseButton button);

	public abstract void mouseDragged(int x, int y, MouseButton button);

	public abstract void colorChanged();

	public void keyPressed(KeyCode key) {
		if (key == KeyCode.ENTER) {
			this.enter();
		}
		else if (key == KeyCode.ESCAPE) {
			this.escape();
		}
	}

	public void enter() {
		if (this.work != null) this.layer().history.save(this.labelText.get());
		this.source.finishUsingTool();
		this.work = null;
		this.updateLabelText();
	}

	public void escape() {
		if (this.work != null) {
			this.work = null;
			this.updateLabelText();
		}
		else {
			this.source.currentTool.set(null);
		}
		this.source.cancelToolAction();
	}

	public boolean getSelection(Selection selection) {
		return false;
	}

	public abstract @Nullable Node getConfiguration();

	public abstract void updateLabelText();

	public static class Selection {

		public int minX, minY, maxX, maxY;
	}

	public static record ToolType(String name, Image icon, Cursor cursor) {

		public ToolType(String name, Image icon, double x, double y) {
			this(name, icon, new ImageCursor(icon, x, y));
		}

		public ToolType(String name, double x, double y) {
			this(name, new Image(NotGimp.class.getClassLoader().getResourceAsStream("assets/tools/" + name + ".png")), x, y);
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}