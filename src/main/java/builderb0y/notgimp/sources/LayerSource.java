package builderb0y.notgimp.sources;

import java.util.Collection;

import javafx.collections.MapChangeListener;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tooltip;

import builderb0y.notgimp.Layer;
import builderb0y.notgimp.json.JsonMap;

public abstract class LayerSource implements MapChangeListener<String, Layer> {

	public LayerSources sources;
	public String displayName;

	public LayerSource(LayerSources sources, String displayName) {
		this.sources = sources;
		this.displayName = displayName;
	}

	public abstract JsonMap save();

	public abstract void load(JsonMap map);

	public void onSelected() {
		this.sources.layer.openImage.layerMap.addListener(this);
		this.invalidateStructure();
	}

	public void onDeselected() {
		this.sources.layer.openImage.layerMap.removeListener(this);
		this.sources.layer.redrawException.set(null);
	}

	public abstract void invalidateStructure();

	public abstract Collection<Layer> getDependencies();

	public abstract boolean isAnimated();

	public abstract Node getRootNode();

	public void requestRedraw() {
		this.sources.layer.requestRedraw();
	}

	public void redraw() {
		try {
			this.doRedraw();
			this.sources.layer.redrawException.set(null);
			((RadioButton)(this.sources.layer.item.getGraphic())).setTooltip(null);
		}
		catch (RedrawException exception) {
			this.sources.layer.item.getGraphic().setStyle("-fx-text-fill: #FF3F3F;");
			RadioButton graphic = (RadioButton)(this.sources.layer.item.getGraphic());
			Tooltip tooltip = graphic.getTooltip();
			if (tooltip != null) {
				tooltip.setText(exception.getLocalizedMessage());
			}
			else {
				graphic.setTooltip(new Tooltip(exception.getLocalizedMessage()));
			}
		}
	}

	public abstract void doRedraw() throws RedrawException;

	@Override
	public String toString() {
		return this.displayName;
	}

	public static class RedrawException extends Exception {

		public RedrawException(String reason) {
			super(reason, null, false, false);
		}
	}
}