package builderb0y.bigpixel.scripting.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jdk.incubator.vector.*;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigpixel.Util;

public enum VectorType {
	INT     (ComponentType.INT, GroupShape.UNIT),
	INT2    (ComponentType.INT, GroupShape.VEC2),
	INT3    (ComponentType.INT, GroupShape.VEC3),
	INT4    (ComponentType.INT, GroupShape.VEC4),

	FLOAT   (ComponentType.FLOAT, GroupShape.UNIT),
	FLOAT2  (ComponentType.FLOAT, GroupShape.VEC2),
	FLOAT3  (ComponentType.FLOAT, GroupShape.VEC3),
	FLOAT4  (ComponentType.FLOAT, GroupShape.VEC4),

	LONG    (ComponentType.LONG, GroupShape.UNIT),
	LONG2   (ComponentType.LONG, GroupShape.VEC2),
	LONG3   (ComponentType.LONG, GroupShape.VEC3),
	LONG4   (ComponentType.LONG, GroupShape.VEC4),

	DOUBLE  (ComponentType.DOUBLE, GroupShape.UNIT),
	DOUBLE2 (ComponentType.DOUBLE, GroupShape.VEC2),
	DOUBLE3 (ComponentType.DOUBLE, GroupShape.VEC3),
	DOUBLE4 (ComponentType.DOUBLE, GroupShape.VEC4),

	BOOLEAN (ComponentType.BOOLEAN, GroupShape.UNIT),
	BOOLEAN2(ComponentType.BOOLEAN, GroupShape.VEC2),
	BOOLEAN3(ComponentType.BOOLEAN, GroupShape.VEC3),
	BOOLEAN4(ComponentType.BOOLEAN, GroupShape.VEC4),

	VOID(ComponentType.VOID, GroupShape.UNIT),

	;

	public static final VectorType[] VALUES = values();
	public static final Map<String, VectorType> nameLookup = Arrays.stream(VALUES).collect(Collectors.toMap((VectorType model) -> model.name, Function.identity()));
	public static record Key(ComponentType componentType, GroupShape shape) {}
	public static final Map<Key, VectorType> typeLookup = Arrays.stream(VALUES).collect(Collectors.toMap((VectorType type) -> new Key(type.componentType, type.shape), Function.identity()));

	public final String name;
	public final ComponentType componentType;
	public final GroupShape shape;

	VectorType(ComponentType componentType, GroupShape shape) {
		this.name = this.name().toLowerCase(Locale.ROOT);
		this.componentType = componentType;
		this.shape = shape;
	}

	public static VectorType forName(String name) {
		return nameLookup.get(name);
	}

	public static VectorType get(ComponentType componentType, GroupShape shape) {
		return typeLookup.get(new Key(componentType, shape));
	}

	public static VectorType get(AnnotatedType type) {
		if (type.getType() instanceof Class<?> clazz) {
			return switch (clazz.getName()) {
				case "int" -> INT;
				case "long" -> LONG;
				case "float" -> FLOAT;
				case "double" -> DOUBLE;
				case "boolean" -> BOOLEAN;
				case "jdk.incubator.vector.IntVector" -> switch (type.getAnnotation(Vec.class).value()) {
					case 2 -> INT2;
					case 3 -> INT3;
					case 4 -> INT4;
					default -> throw new IllegalArgumentException(type.toString());
				};
				case "jdk.incubator.vector.LongVector" -> switch (type.getAnnotation(Vec.class).value()) {
					case 2 -> LONG2;
					case 3 -> LONG3;
					case 4 -> LONG4;
					default -> throw new IllegalArgumentException(type.toString());
				};
				case "jdk.incubator.vector.FloatVector" -> switch (type.getAnnotation(Vec.class).value()) {
					case 2 -> FLOAT2;
					case 3 -> FLOAT3;
					case 4 -> FLOAT4;
					default -> throw new IllegalArgumentException(type.toString());
				};
				case "jdk.incubator.vector.DoubleVector" -> switch (type.getAnnotation(Vec.class).value()) {
					case 2 -> DOUBLE2;
					case 3 -> DOUBLE3;
					case 4 -> DOUBLE4;
					default -> throw new IllegalArgumentException(type.toString());
				};
				case "jdk.incubator.vector.VectorMask" -> switch (type.getAnnotation(Vec.class).value()) {
					case 2 -> BOOLEAN2;
					case 3 -> BOOLEAN3;
					case 4 -> BOOLEAN4;
					default -> throw new IllegalArgumentException(type.toString());
				};
				case "void" -> VOID;
				default -> throw new IllegalArgumentException(type.toString());
			};
			/*
			if (clazz == IntMatrix2x2.class) return INT2X2;
			if (clazz == IntMatrix4x4.class) return INT4X4;
			if (clazz == IntMatrix8x8.class) return INT8X8;
			if (clazz == LongMatrix2x2.class) return LONG2X2;
			if (clazz == LongMatrix4x4.class) return LONG4X4;
			if (clazz == LongMatrix8x8.class) return LONG8X8;
			if (clazz == FloatMatrix2x2.class) return FLOAT2X2;
			if (clazz == FloatMatrix4x4.class) return FLOAT4X4;
			if (clazz == FloatMatrix8x8.class) return FLOAT8X8;
			if (clazz == DoubleMatrix2x2.class) return DOUBLE2X2;
			if (clazz == DoubleMatrix4x4.class) return DOUBLE4X4;
			if (clazz == DoubleMatrix8x8.class) return DOUBLE8X8;
			*/
		}
		throw new IllegalArgumentException(type.toString());
	}

	public boolean isReallyDoubleWidth() {
		return this.componentType.isDoubleWidth() && this.shape == GroupShape.UNIT;
	}

	public Class<?> holderClass() {
		return this.componentType.holderClass(this.shape);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static enum ComponentType {
		INT    (false, false, int.class,     IntVector.class),
		LONG   (false, true,  long.class,    LongVector.class),
		FLOAT  (true,  false, float.class,   FloatVector.class),
		DOUBLE (true,  true,  double.class,  DoubleVector.class),
		BOOLEAN(false, false, boolean.class, VectorMask.class),
		VOID   (false, false, void.class,    void.class);

		public static final ComponentType[] VALUES = values();

		public final boolean isFloatingPoint;
		public final boolean isDoubleWidth;
		public final ClassDesc unitDesc, vectorDesc;

		ComponentType(boolean isFloatingPoint, boolean isDoubleWidth, Class<?> unitClass, Class<?> vectorDesc) {
			this.isFloatingPoint = isFloatingPoint;
			this.isDoubleWidth = isDoubleWidth;
			this.unitDesc = Util.desc(unitClass);
			this.vectorDesc = Util.desc(vectorDesc);
		}

		public boolean isFixedPoint() {
			return !this.isFloatingPoint;
		}

		public boolean isFloatingPoint() {
			return this.isFloatingPoint;
		}

		public boolean isSingleWidth() {
			return !this.isDoubleWidth;
		}

		public boolean isDoubleWidth() {
			return this.isDoubleWidth;
		}

		public Class<?> holderClass(GroupShape shape) {
			return switch (this) {
				case INT -> shape.intType;
				case LONG -> shape.longType;
				case FLOAT -> shape.floatType;
				case DOUBLE -> shape.doubleType;
				case BOOLEAN -> shape.booleanType;
				case VOID -> void.class;
			};
		}
	}

	public static enum GroupShape {
		UNIT(1, 1, int.class, long.class, float.class, double.class, boolean.class),
		VEC2(2, 1, IntVector.class, LongVector.class, FloatVector.class, DoubleVector.class, VectorMask.class),
		VEC3(3, 1, IntVector.class, LongVector.class, FloatVector.class, DoubleVector.class, VectorMask.class),
		VEC4(4, 1, IntVector.class, LongVector.class, FloatVector.class, DoubleVector.class, VectorMask.class),
		/*
		MAT2(2, 2, IntMatrix2x2.class, LongMatrix2x2.class, FloatMatrix2x2.class, DoubleMatrix2x2.class, null),
		MAT4(4, 4, IntMatrix4x4.class, LongMatrix4x4.class, FloatMatrix4x4.class, DoubleMatrix4x4.class, null),
		MAT8(8, 8, IntMatrix8x8.class, LongMatrix8x8.class, FloatMatrix8x8.class, DoubleMatrix8x8.class, null),
		*/
		;

		public static final GroupShape[] VALUES = values();

		public final int rows, columns;
		public final Class<?> intType, longType, floatType, doubleType;
		public final @Nullable Class<?> booleanType;

		GroupShape(int rows, int columns, Class<?> intType, Class<?> longType, Class<?> floatType, Class<?> doubleType, @Nullable Class<?> booleanType) {
			this.rows = rows;
			this.columns = columns;
			this.intType = intType;
			this.longType = longType;
			this.floatType = floatType;
			this.doubleType = doubleType;
			this.booleanType = booleanType;
		}

		public int rows() {
			return this.rows;
		}

		public int columns() {
			return this.columns;
		}

		public int componentCount() {
			return this.rows * this.columns;
		}
	}

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Vec {

		public abstract int value();
	}
}