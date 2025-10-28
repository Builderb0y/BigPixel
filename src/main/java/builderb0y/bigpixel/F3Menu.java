package builderb0y.bigpixel;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class F3Menu {

	public static String formatBytes(long memory) {
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

	public int
		labelCount = 0;
	public GridPane
		gridPane = new GridPane();
	public AnchorPane
		anchorPane = new AnchorPane();

	public F3Menu() {
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

	public void add(String text) {
		ObservableList<Node> children = this.gridPane.getChildren();
		if (this.labelCount == children.size()) {
			this.gridPane.add(label(), 0, this.labelCount);
		}
		((Label)(children.get(this.labelCount++))).setText(text);
	}

	public void commit() {
		ObservableList<Node> children = this.gridPane.getChildren();
		while (this.labelCount < children.size()) {
			((Label)(children.get(this.labelCount++))).setText(null);
		}
		this.labelCount = 0;
	}

	public Pane rootNode() {
		return this.anchorPane;
	}
}