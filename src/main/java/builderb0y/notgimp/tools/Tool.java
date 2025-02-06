package builderb0y.notgimp.tools;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.NotGimp;
import builderb0y.notgimp.sources.ManualLayerSource;

public abstract class Tool<W> extends SourcelessTool<W> {

	public final ManualLayerSource source;
	public SimpleStringProperty labelText;

	public Tool(ToolType type, ManualLayerSource source) {
		super(type);
		this.source = source;
		this.labelText = new SimpleStringProperty();
		this.updateLabelText();
	}

	public Layer layer() {
		return this.source.sources.layer;
	}

	@Override
	public void enter() {
		if (this.work != null) this.layer().history.save(this.labelText.get());
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