package builderb0y.notgimp.sources.dependencies;

import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

public record DependencyType(String name, Color color) {

	public static final DependencyType
		MAIN = new DependencyType("main", new Color(0.25D, 1.0D, 0.25D, 1.0D)),
		MASK = new DependencyType("mask", new Color(1.0D, 0.875D, 0.0D, 1.0D));

	@Override
	public @NotNull String toString() {
		return this.name;
	}
}