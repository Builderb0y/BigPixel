package builderb0y.notgimp;

import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import builderb0y.notgimp.tools.Tool.Selection;

public class F3Menu {

	public static final SimpleLongProperty
		TOTAL_MEMORY = new SimpleLongProperty(),
		FREE_MEMORY  = new SimpleLongProperty(),
		USED_MEMORY  = new SimpleLongProperty();
	public static final SimpleStringProperty
		FORMATTED_MEMORY = new SimpleStringProperty();
	static {
		NotGimp.TIMER.scheduleAtFixedRate(
			new TimerTask() {

				@Override
				public void run() {
					Platform.runLater(() -> {
						long total = Runtime.getRuntime().totalMemory();
						long free = Runtime.getRuntime().freeMemory();
						long used = total - free;
						TOTAL_MEMORY.set(total);
						FREE_MEMORY.set(free);
						USED_MEMORY.set(used);
						FORMATTED_MEMORY.set("Memory: " + format(used) + " / " + format(total));
					});
				}
			},
			0L,
			1000L
		);
	}

	public static String format(long memory) {
		String suffix = " B";
		if (memory >= 1024L) {
			memory >>= 10;
			suffix = " KB";
			if (memory >= 1024L) {
				memory >>= 10;
				suffix = " MB";
				if (memory >= 1024L) {
					memory >>= 10;
					suffix = " GB";
				}
			}
		}
		return memory + suffix;
	}

	public Label
		hover     = label(),
		selection = label(),
		zoom      = label(),
		memory    = label();
	public GridPane
		gridPane = new GridPane();
	public AnchorPane
		anchorPane = new AnchorPane();

	public F3Menu() {
		this.memory.textProperty().bind(FORMATTED_MEMORY);
		this.selection.setText("Selection: null");
		this.gridPane.add(this.hover,     0, 0);
		this.gridPane.add(this.selection, 0, 1);
		this.gridPane.add(this.zoom,      0, 2);
		this.gridPane.add(this.memory,    0, 3);
		this.anchorPane.getChildren().add(this.gridPane);
		AnchorPane.setTopAnchor(this.gridPane, 4.0D);
		AnchorPane.setLeftAnchor(this.gridPane, 8.0D);
		this.anchorPane.setMouseTransparent(true);
		this.rootNode().setVisible(false);
	}

	public static Label label() {
		Label label = new Label();
		label.setFont(new Font(24.0D));
		label.setEffect(new DropShadow(8.0D, Color.BLACK));
		return label;
	}

	public void updateSelection(Selection selection) {
		this.selection.setText("Selection: " + selection);
	}

	public void updatePos(int x, int y) {
		this.hover.setText("Hover: " + x + ", " + y);
	}

	public void updateZoom(double zoom) {
		this.zoom.setText("Zoom: " + zoom + 'x');
	}

	public Pane rootNode() {
		return this.anchorPane;
	}
}