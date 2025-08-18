package builderb0y.bigpixel.sources;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.ColorHelper;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.Util;

public class ColorBoxGroup {

	public SimpleObjectProperty<ColorBox> selected = new SimpleObjectProperty<>();
	public boolean updateColor;

	public ColorBoxGroup(ColorHelper color, Node rootPane) {
		this.selected.addListener(Util.change((ColorBox oldValue, ColorBox newValue) -> {
			if (oldValue != null) {
				oldValue.color.unbind();
			}
			if (newValue != null) {
				if (this.updateColor) {
					FloatVector current = newValue.color.get();
					color.setRGBA(
						current.lane(HDRImage.RED_OFFSET),
						current.lane(HDRImage.GREEN_OFFSET),
						current.lane(HDRImage.BLUE_OFFSET),
						current.lane(HDRImage.ALPHA_OFFSET)
					);
					color.markDirty();
				}
				newValue.color.bind(color.rgba);
			}
		}));
		new com.sun.javafx.scene.TreeShowingProperty(rootPane).addListener(Util.change((Boolean visible) -> {
			if (!visible) this.selected.set(null);
		}));
	}

	public ColorBoxGroup(ColorHelper color, Node rootPane, ColorBox... boxes) {
		this(color, rootPane);
		for (ColorBox box : boxes) {
			this.add(box);
		}
	}

	public void add(ColorBox box) {
		box.selectWhen(this.selected);
		box.box.display.setOnMouseClicked((MouseEvent event) -> {
			if (!box.disabled.get()) {
				switch (event.getButton()) {
					case PRIMARY -> {
						this.updateColor = true;
					}
					case SECONDARY -> {
						this.updateColor = false;
					}
					default -> {
						return;
					}
				}
				this.selected.set(this.selected.getValue() == box ? null : box);
			}
		});
	}

	public ColorBoxGroup addGroup(ColorBox box) {
		this.add(box);
		return this;
	}

	public ColorBox addBox(ColorBox box) {
		this.add(box);
		return box;
	}
}