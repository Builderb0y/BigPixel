package builderb0y.notgimp;

import java.util.function.Consumer;

import builderb0y.notgimp.json.*;

public class SaveVersions {

	public static final int CURRENT = 3;

	@SuppressWarnings({ "fallthrough", "DefaultNotLastCaseInSwitch" })
	public static void process(JsonMap map) {
		int version = map.getInt("version");
		switch (version) {
			default: throw new IllegalArgumentException("Unknown save version: " + version);
			case 0: process0(map);
			case 1: process1(map);
			case 2: process2(map);
			case 3:
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

	public static void process2(JsonMap root) {
		JsonArray layers = new JsonArray();
		JsonMap rootLayer = root.removeMap("root_layer");
		graphifyLayerTree(layers, rootLayer, findMaxDepthOfLayerTree(rootLayer), 0);
		JsonMap layerGraph = (
			new JsonMap()
			.with("layers", layers)
			.with("showing_layer", root.removeString("showing_layer"))
			.with("selected_layer", root.removeString("selected_layer"))
		);
		root.put("layer_graph", layerGraph);
	}

	public static int findMaxDepthOfLayerTree(JsonMap layer) {
		int maxDepth = 0;
		for (JsonValue child : layer.getArray("children")) {
			maxDepth = Math.max(maxDepth, findMaxDepthOfLayerTree(child.asMap()) + 1);
		}
		return maxDepth;
	}

	public static int graphifyLayerTree(JsonArray layers, JsonMap layer, int x, int startY) {
		assert x >= 0;
		JsonArray children = layer.getArray("children");
		layers.add(
			new JsonMap()
			.with("name", layer.getString("name"))
			.with("width", layer.getInt("width"))
			.with("height", layer.getInt("height"))
			.with("sources", applyDependencies(layer.getMap("sources"), children))
			.with("gridX", x)
			.with("gridY", startY)
		);
		if (children.isEmpty()) {
			startY++;
		}
		else for (JsonValue child : children) {
			startY = graphifyLayerTree(layers, child.asMap(), x - 1, startY);
		}
		return startY;
	}

	public static JsonMap applyDependencies(JsonMap sources, JsonArray children) {
		JsonMap dependencies = new JsonMap();
		JsonMap source = sources.getMap("tab");
		String type = source.getString("type");
		switch (type) {
			case "manual", "derived" -> {}
			case "alpha" -> {
				JsonArray main = new JsonArray();
				for (JsonValue child : children) {
					main.add(makeLayerDependency(null, child));
				}
				dependencies.put("main", main);
				source.put("linear", false);
			}
			case "add", "avg", "mul", "screen", "min", "max" -> {
				JsonArray main = new JsonArray();
				for (JsonValue child : children) {
					main.add(makeLayerDependency(null, child));
				}
				dependencies.put("main", main);
			}
			case "invert", "normalize", "clamp", "convolve", "kmeans" -> {
				dependencies
				.with("main", makeLayerDependency(sources, children.getFirst()))
				.with("mask", whiteMask());
			}
			case "gradient_remap" -> {
				dependencies
				.with("main", makeLayerDependency(sources, children.getFirst()))
				.with("mask", whiteMask())
				.with("from_start", wrapColor(source.getArray("from_start")))
				.with("from_end", wrapColor(source.getArray("from_end")))
				.with("to_start", wrapColor(source.getArray("to_start")))
				.with("to_end", wrapColor(source.getArray("to_end")));
			}
			case "color_matrix" -> {
				dependencies
				.with("main", makeLayerDependency(sources, children.getFirst()))
				.with("mask", whiteMask())
				.with("to_red", wrapColor(source.getArray("to_red")))
				.with("to_green", wrapColor(source.getArray("to_green")))
				.with("to_blue", wrapColor(source.getArray("to_blue")));
			}
			case "cliff" -> {
				dependencies
				.with("main", makeLayerDependency(sources, children.getFirst()))
				.with("mask", whiteMask());
				JsonArray strength = new JsonArray();
				if (source.getBoolean("split_rgb")) {
					strength
					.with(computeCliffColor(source.getFloat("red_coefficient")))
					.with(computeCliffColor(source.getFloat("green_coefficient")))
					.with(computeCliffColor(source.getFloat("blue_coefficient")));
				}
				else {
					float rgb = computeCliffColor(source.getFloat("rgb_coefficient"));
					strength.with(rgb).with(rgb).with(rgb);
				}
				dependencies.put("strength", wrapColor(strength.with(computeCliffColor(source.getFloat("alpha_coefficient")))));
				JsonArray midpoint = new JsonArray();
				if (source.getBoolean("dual")) {
					midpoint
					.with(source.getFloat("red_mid"))
					.with(source.getFloat("green_mid"))
					.with(source.getFloat("blue_mid"));
				}
				else {
					float mid = source.getFloat("rgb_mid");
					midpoint.with(mid).with(mid).with(mid);
				}
				dependencies.put("midpoint", wrapColor(midpoint.with(source.getFloat("alpha_mid"))));
			}
			case "wfc" -> {
				dependencies.with("main", makeLayerDependency(sources, children.getFirst()));
			}
			default -> throw new SaveException("Unknown layer type: " + type);
		}
		source.put("dependencies", dependencies);
		return sources;
	}

	public static JsonMap makeLayerDependency(JsonMap sources, JsonValue layer) {
		String inputLayer;
		if (sources != null && sources.get("tab") instanceof JsonMap tab && tab.get("input_layer") instanceof JsonString string) {
			inputLayer = string.value;
		}
		else {
			inputLayer = layer.asMap().getString("name");
		}
		return new JsonMap().with("type", "layer").with("layer", inputLayer);
	}

	public static JsonMap whiteMask() {
		return new JsonMap().with("type", "color").with("color", new JsonArray().with(1.0F).with(1.0F).with(1.0F).with(1.0F));
	}

	public static JsonMap wrapColor(JsonArray color) {
		return new JsonMap().with("type", "color").with("color", color);
	}

	public static float computeCliffColor(float coefficient) {
		return coefficient == 1.0F ? 0.5F : (float)((coefficient - Math.sqrt(coefficient)) / (coefficient - 1.0D));
	}

	public static void recursiveProcessLayers(JsonMap layer, Consumer<JsonMap> action) {
		action.accept(layer);
		for (JsonValue value : layer.getArray("children")) {
			recursiveProcessLayers(value.asMap(), action);
		}
	}
}