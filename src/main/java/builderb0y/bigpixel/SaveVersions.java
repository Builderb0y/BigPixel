package builderb0y.bigpixel;

import java.util.function.Consumer;

import builderb0y.bigpixel.json.*;

public class SaveVersions {

	public static final int CURRENT = 10;

	@SuppressWarnings({ "fallthrough", "DefaultNotLastCaseInSwitch" })
	public static void process(JsonMap root) {
		int version = root.getInt("version");
		switch (version) {
			default: throw new IllegalArgumentException("Unknown save version: " + version);
			case 0: process0(root);
			case 1: process1(root);
			case 2: process2(root);
			case 3: process3(root);
			case 4: process4(root);
			case 5: process5(root);
			case 6: process6(root);
			case 7: process7(root);
			case 8: process8(root);
			case 9: process9(root);
			case 10:
		}
	}

	public static void process0(JsonMap root) {
		recursiveProcessLayers(root.getMap("root_layer"), (JsonMap layer) -> {
			layer.add("expanded", true);
		});
	}

	public static void process1(JsonMap root) {
		recursiveProcessLayers(root.getMap("root_layer"), (JsonMap layer) -> {
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

	public static void process3(JsonMap root) {
		for (JsonValue layer : root.getMap("layer_graph").getArray("layers")) {
			JsonMap tab = layer.asMap().getMap("sources").getMap("tab");
			switch (tab.getString("type")) {
				case "add", "alpha", "avg", "max", "min", "mul", "screen" -> {
					for (JsonValue dependency : tab.getMap("dependencies").getArray("main")) {
						dependency.asMap().put("enabled", true);
					}
				}
				default -> {}
			}
		}
	}

	public static void process4(JsonMap root) {
		for (JsonValue layer : root.getMap("layer_graph").getArray("layers")) {
			JsonMap tab = layer.asMap().getMap("sources").getMap("tab");
			if (tab.getString("type").equals("convolve")) {
				tab.put("linear", false);
			}
		}
	}

	public static void process5(JsonMap root) {
		for (JsonValue layer : root.getMap("layer_graph").getArray("layers")) {
			layer.asMap().put("sources", layer.asMap().removeMap("sources").removeMap("tab"));
			layer.asMap().add(
				"views",
				new JsonMap()
				.with("type", "flat_clamped")
				.with("draw_outline", true)
				.with("dependencies", new JsonMap(0))
			);
		}
	}

	public static void process6(JsonMap root) {
		for (JsonValue layer : root.getMap("layer_graph").getArray("layers")) {
			JsonMap views = layer.asMap().getMap("views");
			if (views.getString("type").equals("cube")) {
				views
				.getMap("dependencies")
				.rename("top",    "up"   )
				.rename("back",   "north")
				.rename("right",  "east" )
				.rename("front",  "south")
				.rename("left",   "west" )
				.rename("bottom", "down" )
				.put(
					"dimensions",
					new JsonMap()
					.with("minX",  0.0D)
					.with("minY",  0.0D)
					.with("minZ",  0.0D)
					.with("maxX", 16.0D)
					.with("maxY", 16.0D)
					.with("maxZ", 16.0D)
				);
			}
		}
	}

	public static void process7(JsonMap root) {
		for (JsonValue layer : root.getMap("layer_graph").getArray("layers")) {
			JsonMap views = layer.asMap().getMap("views");
			if (views.getString("type").equals("cube")) {
				JsonMap deps = views.getMap("dependencies");
				addMissingFaceData(deps.getMap("up"));
				addMissingFaceData(deps.getMap("down"));
				addMissingFaceData(deps.getMap("north"));
				addMissingFaceData(deps.getMap("east"));
				addMissingFaceData(deps.getMap("south"));
				addMissingFaceData(deps.getMap("west"));
			}
		}
	}

	public static void addMissingFaceData(JsonMap binding) {
		binding.with("minU", 0.0D).with("minV", 0.0D).with("maxU", 16.0D).with("maxV", 16.0D).with("rotation", "IDENTITY");
	}

	public static void process8(JsonMap root) {
		for (JsonValue layer : root.getMap("layer_graph").getArray("layers")) {
			JsonMap sources = layer.asMap().getMap("sources");
			if (sources.getString("type").equals("convolve")) {
				sources.put("normalize", true);
			}
		}
	}

	public static void process9(JsonMap root) {
		for (JsonValue layer : root.getMap("layer_graph").getArray("layers")) {
			JsonMap sources = layer.asMap().getMap("sources");
			sources.putIfAbsent("clampRGB", JsonBoolean.TRUE);
			sources.putIfAbsent("clampA", JsonBoolean.TRUE);
		}
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
				source.putIfAbsent("per_channel", JsonBoolean.FALSE);
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