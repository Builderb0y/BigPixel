package builderb0y.bigpixel.sources;

import builderb0y.bigpixel.ConfigParameters;
import builderb0y.bigpixel.Util;

public class SourceParameters extends ConfigParameters {

	public SourceParameters(LayerSource source) {
		this.parameterListener = Util.change(source::redrawLater);
	}
}