package builderb0y.bigpixel;

import java.util.stream.Collectors;

import jdk.incubator.vector.FloatVector;

import builderb0y.bigpixel.json.*;
import builderb0y.bigpixel.sources.BoundsHandling;
import builderb0y.bigpixel.sources.BoundsHandling.DualBoundsHandling;
import builderb0y.bigpixel.sources.ConvolveLayerSource;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding.UniformSaveData;
import builderb0y.bigpixel.sources.dependencies.inputs.InputBinding.VaryingSaveData;
import builderb0y.bigpixel.tools.Symmetry;
import builderb0y.bigpixel.views.CubeDimensions.CubeSize;
import builderb0y.bigpixel.views.FaceInputBinding.UvParams;

public abstract class JsonConverter<T_Value> {

	public abstract JsonValue toJson(T_Value value);

	public abstract T_Value fromJson(JsonValue value);

	public static class BooleanJsonConverter extends JsonConverter<Boolean> {

		public static final BooleanJsonConverter INSTANCE = new BooleanJsonConverter();

		@Override
		public JsonValue toJson(Boolean value) {
			return JsonBoolean.valueOf(value);
		}

		@Override
		public Boolean fromJson(JsonValue value) {
			return value.asBoolean();
		}
	}

	public static class IntJsonConverter extends JsonConverter<Integer> {

		public static final IntJsonConverter INSTANCE = new IntJsonConverter();

		@Override
		public JsonValue toJson(Integer value) {
			return new JsonFixedPoint(value);
		}

		@Override
		public Integer fromJson(JsonValue value) {
			return value.asNumber().intValue();
		}
	}

	public static class LongJsonConverter extends JsonConverter<Long> {

		public static final LongJsonConverter INSTANCE = new LongJsonConverter();

		@Override
		public JsonValue toJson(Long value) {
			return new JsonFixedPoint(value);
		}

		@Override
		public Long fromJson(JsonValue value) {
			return value.asNumber().longValue();
		}
	}

	public static class FloatJsonConverter extends JsonConverter<Float> {

		public static final FloatJsonConverter INSTANCE = new FloatJsonConverter();

		@Override
		public JsonValue toJson(Float value) {
			return new JsonFloatingPoint(value);
		}

		@Override
		public Float fromJson(JsonValue value) {
			return value.asNumber().floatValue();
		}
	}

	public static class DoubleJsonConverter extends JsonConverter<Double> {

		public static final DoubleJsonConverter INSTANCE = new DoubleJsonConverter();

		@Override
		public JsonValue toJson(Double value) {
			return new JsonFloatingPoint(value);
		}

		@Override
		public Double fromJson(JsonValue value) {
			return value.asNumber().doubleValue();
		}
	}

	public static class StringJsonConverter extends JsonConverter<String> {

		public static final StringJsonConverter INSTANCE = new StringJsonConverter();

		@Override
		public JsonValue toJson(String value) {
			return new JsonString(value);
		}

		@Override
		public String fromJson(JsonValue value) {
			return value.asString();
		}
	}

	public static class MultiLineStringJsonConverter extends JsonConverter<String> {

		public static final MultiLineStringJsonConverter INSTANCE = new MultiLineStringJsonConverter();

		@Override
		public JsonValue toJson(String value) {
			return multiLineStringToJson(value);
		}

		@Override
		public String fromJson(JsonValue value) {
			return multiLineStringFromJson(value.asArray());
		}
	}

	public static JsonArray multiLineStringToJson(String value) {
		JsonArray array = new JsonArray();
		value.lines().forEachOrdered(array::add);
		return array;
	}

	public static String multiLineStringFromJson(JsonArray array) {
		return array.stream().map(JsonValue::asString).collect(Collectors.joining("\n"));
	}

	public static class ColorJsonConverter extends JsonConverter<FloatVector> {

		public static final ColorJsonConverter INSTANCE = new ColorJsonConverter();

		@Override
		public JsonValue toJson(FloatVector value) {
			return colorToJson(value);
		}

		@Override
		public FloatVector fromJson(JsonValue value) {
			return colorFromJson(value.asArray());
		}
	}

	public static JsonArray colorToJson(FloatVector color) {
		return (
			new JsonArray()
			.with(color.lane(0))
			.with(color.lane(1))
			.with(color.lane(2))
			.with(color.lane(3))
			.inline(true)
		);
	}

	public static FloatVector colorFromJson(JsonArray array) {
		return FloatVector.fromArray(
			FloatVector.SPECIES_128,
			new float[] {
				array.getFloat(0),
				array.getFloat(1),
				array.getFloat(2),
				array.getFloat(3)
			},
			0
		);
	}

	public static class EnumJsonConverter<E extends Enum<E>> extends JsonConverter<E> {

		public final Class<E> enumClass;

		public EnumJsonConverter(Class<E> enumClass) {
			this.enumClass = enumClass;
		}

		@Override
		public JsonValue toJson(E value) {
			return new JsonString(value.name());
		}

		@Override
		public E fromJson(JsonValue value) {
			return Enum.valueOf(this.enumClass, value.asString());
		}
	}

	public static class InputBindingSaveDataJsonConverter extends JsonConverter<InputBinding.SaveData> {

		public static final InputBindingSaveDataJsonConverter INSTANCE = new InputBindingSaveDataJsonConverter();

		@Override
		public JsonMap toJson(InputBinding.SaveData provider) {
			return switch (provider) {
				case UniformSaveData uniform -> new JsonMap().with("type", "color").with("color", colorToJson(uniform.color()));
				case VaryingSaveData varying -> new JsonMap().with("type", "layer").with("layer", varying.layerName());
			};
		}

		@Override
		public InputBinding.SaveData fromJson(JsonValue value) {
			JsonMap map = value.asMap();
			String type = map.getString("type");
			return switch (type) {
				case "color" -> new UniformSaveData(colorFromJson(map.getArray("color")));
				case "layer" -> new VaryingSaveData(map.getString("layer"));
				default -> throw new SaveException("Unknown dependency type: " + type);
			};
		}
	}

	public static class UvJsonConverter extends JsonConverter<UvParams> {

		public static final UvJsonConverter INSTANCE = new UvJsonConverter();

		@Override
		public JsonValue toJson(UvParams params) {
			return (
				new JsonMap()
				.with("minU", params.minU())
				.with("minV", params.minV())
				.with("maxU", params.maxU())
				.with("maxV", params.maxV())
				.with("rotation", params.rotation().name())
			);
		}

		@Override
		public UvParams fromJson(JsonValue value) {
			JsonMap map = value.asMap();
			return new UvParams(
				map.getDouble("minU"),
				map.getDouble("minV"),
				map.getDouble("maxU"),
				map.getDouble("maxV"),
				Symmetry.valueOf(map.getString("rotation"))
			);
		}
	}

	public static class CubeSizeJsonConverter extends JsonConverter<CubeSize> {

		public static final CubeSizeJsonConverter INSTANCE = new CubeSizeJsonConverter();

		@Override
		public JsonValue toJson(CubeSize size) {
			return (
				new JsonMap()
				.with("minX", size.minX())
				.with("minY", size.minY())
				.with("minZ", size.minZ())
				.with("maxX", size.maxX())
				.with("maxY", size.maxY())
				.with("maxZ", size.maxZ())
			);
		}

		@Override
		public CubeSize fromJson(JsonValue value) {
			JsonMap map = value.asMap();
			return new CubeSize(
				map.getDouble("minX"),
				map.getDouble("minY"),
				map.getDouble("minZ"),
				map.getDouble("maxX"),
				map.getDouble("maxY"),
				map.getDouble("maxZ")
			);
		}
	}

	public static class ConvolveSaveDataJsonConverter extends JsonConverter<ConvolveLayerSource.SaveData> {

		public static final ConvolveSaveDataJsonConverter INSTANCE = new ConvolveSaveDataJsonConverter();

		@Override
		public JsonValue toJson(ConvolveLayerSource.SaveData data) {
			JsonMap map = (
				new JsonMap()
				.with("shape", data.shape.name())
				.with("preset",data.type().name())
				.with("radius", data.radius)
			);
			switch (data) {
				case ConvolveLayerSource.BoxSaveData _ -> {}
				case ConvolveLayerSource.GaussianSaveData _ -> {}
				case ConvolveLayerSource.BokehSaveData _ -> {}
				case ConvolveLayerSource.ManualSaveData manual -> {
					JsonArray weightList = new JsonArray(manual.weights.size());
					for (int index = 0; index < manual.weights.size(); index++) {
						weightList.add(
							new JsonMap()
							.with("x", manual.weights.getX(index))
							.with("y", manual.weights.getY(index))
							.with("weight", manual.weights.getWeight(index))
							.inline(true)
						);
					}
					map.put("weights", weightList);
				}
				case ConvolveLayerSource.ScriptedSaveData scripted -> {
					map.put("code", multiLineStringToJson(scripted.scriptSource));
				}
			}
			return map;
		}

		@Override
		public ConvolveLayerSource.SaveData fromJson(JsonValue value) {
			JsonMap map = value.asMap();
			ConvolveLayerSource.ConvolveShape shape = ConvolveLayerSource.ConvolveShape.valueOf(map.getString("shape"));
			ConvolveLayerSource.ConvolveWeightType preset = ConvolveLayerSource.ConvolveWeightType.valueOf(map.getString("preset"));
			int radius = map.getInt("radius");
			return switch (preset) {
				case BOX -> new ConvolveLayerSource.BoxSaveData(shape, radius);
				case GAUSSIAN -> new ConvolveLayerSource.GaussianSaveData(shape, radius);
				case BOKEH -> new ConvolveLayerSource.BokehSaveData(shape, radius);
				case MANUAL -> {
					JsonArray jsonWeights = map.getArray("weights");
					ConvolveLayerSource.PackedWeightList packedWeights = new ConvolveLayerSource.PackedWeightList(jsonWeights.size());
					for (JsonValue weight : jsonWeights) {
						JsonMap weightMap = weight.asMap();
						packedWeights.add(
							weightMap.getInt("x"),
							weightMap.getInt("y"),
							weightMap.getFloat("weight")
						);
					}
					yield new ConvolveLayerSource.ManualSaveData(shape, radius, packedWeights);
				}
				case SCRIPTED -> {
					yield new ConvolveLayerSource.ScriptedSaveData(shape, radius, multiLineStringFromJson(map.getArray("code")));
				}
			};
		}
	}

	public static class DualBoundsHandlingJsonConverter extends JsonConverter<DualBoundsHandling> {

		public static final DualBoundsHandlingJsonConverter INSTANCE = new DualBoundsHandlingJsonConverter();

		@Override
		public JsonValue toJson(DualBoundsHandling handling) {
			return (
				new JsonMap()
				.with("horizontal", handling.horizontal().name())
				.with("vertical", handling.vertical().name())
			);
		}

		@Override
		public DualBoundsHandling fromJson(JsonValue value) {
			JsonMap map = value.asMap();
			return new DualBoundsHandling(
				BoundsHandling.valueOf(map.getString("horizontal")),
				BoundsHandling.valueOf(map.getString("vertical"))
			);
		}
	}
}