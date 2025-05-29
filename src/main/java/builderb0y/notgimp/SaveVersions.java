package builderb0y.notgimp;

import java.util.function.Consumer;

import builderb0y.notgimp.json.JsonMap;
import builderb0y.notgimp.json.JsonStructureException;
import builderb0y.notgimp.json.JsonValue;

public class SaveVersions {

	public static final int CURRENT = 2;

	@SuppressWarnings({ "fallthrough", "DefaultNotLastCaseInSwitch" })
	public static void process(JsonMap map) {
		int version = map.getInt("version");
		switch (version) {
			default: throw new IllegalArgumentException("Unknown save version: " + version);
			case 0: process0(map);
			case 1: process1(map);
			case 2:
		}
	}

	public static void process0(JsonMap map) {
		recursiveProcessLayers(map.getMap("root_layer"), (JsonMap layer) -> {
			layer.add("expanded", true);
		});
	}

	public static void process1(JsonMap map) {
		recursiveProcessLayers(map.getMap("root_layer"), (JsonMap layer) -> {
			JsonMap sources = layer.getMap("sources");
			JsonMap derived = sources.removeMap("derived");
			JsonMap procedural = sources.removeMap("procedural");
			JsonMap manual = sources.removeMap("manual");
			String current = sources.removeString("current");
			JsonMap source = switch (current) {
				case "derived" -> derived;
				case "procedural" -> procedural;
				case "manual" -> manual;
				default -> throw new JsonStructureException("Unknown current source: " + current);
			};
			source.add("type", current);
			sources.add("tab", source);
		});
	}

	public static void recursiveProcessLayers(JsonMap layer, Consumer<JsonMap> action) {
		action.accept(layer);
		for (JsonValue value : layer.getArray("children")) {
			recursiveProcessLayers(value.asMap(), action);
		}
	}
}