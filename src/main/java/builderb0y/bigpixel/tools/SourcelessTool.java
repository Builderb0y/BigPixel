package builderb0y.bigpixel.tools;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.tools.Tool.ToolType;

public abstract class SourcelessTool<W> {

	public final ToolType type;
	public @Nullable W work;

	public SourcelessTool(ToolType type) {
		this.type = type;
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

	public abstract void enter();

	public abstract void escape();
}