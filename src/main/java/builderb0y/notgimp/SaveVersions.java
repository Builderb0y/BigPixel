package builderb0y.notgimp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SaveVersions {

	public static final int CURRENT = 1;

	public static void process(JsonObject object) {
		switch (object.get("version").getAsInt()) {
			case 0: process0(object);
		}
	}

	public static void process0(JsonObject object) {
		JsonObject layer = object.getAsJsonObject("root_layer");
	}

	public static void recursiveProcess0(JsonObject layer) {
		layer.addProperty("expanded", true);
		for (JsonElement child : layer.getAsJsonArray("children")) {
			recursiveProcess0(child.getAsJsonObject());
		}
	}
}