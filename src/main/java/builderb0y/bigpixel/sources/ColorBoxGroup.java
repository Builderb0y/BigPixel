package builderb0y.bigpixel.sources;

import com.sun.javafx.scene.TreeShowingProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.ColorHelper;
import builderb0y.bigpixel.HDRImage;
import builderb0y.bigpixel.OpenImage;
import builderb0y.bigpixel.ParameterSetTop;
import builderb0y.bigpixel.util.Util;

public class ColorBoxGroup {

	public ParameterSetTop top;
	public SimpleObjectProperty<ColorBox> selected;
	public boolean updateColor;

	public ColorBoxGroup(OpenImage image, Node rootPane) {
		this.top = image.parameterSet;
		ColorHelper color = image.mainWindow.colorPicker.currentColor;
		this.selected = new SimpleObjectProperty<>();
		this.selected.addListener(Util.change((ColorBox oldValue, ColorBox newValue) -> {
			if (oldValue != null) {
				oldValue.color.unbindBidirectional(color.rgba);
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
				newValue.color.bindBidirectional(color.rgba);
				image.mainWindow.leftTabs.getSelectionModel().select(0);
			}
		}));
		new TreeShowingProperty(rootPane).addListener(Util.change((Boolean visible) -> {
			if (!visible) this.selected.set(null);
		}));
	}

	public ColorBoxGroup(OpenImage image, Node rootPane, ColorBox... boxes) {
		this(image, rootPane);
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

	public ColorBox addBox(FloatVector color) {
		ColorBox box = new ColorBox(color);
		this.add(box);
		return box;
	}
}