package builderb0y.bigpixel.scripting.tree;

import java.lang.invoke.*;
import java.util.Arrays;

import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import builderb0y.bigpixel.scripting.types.VectorType;
import builderb0y.bigpixel.scripting.types.VectorType.GroupShape;
import builderb0y.bigpixel.util.Util;

import static org.objectweb.asm.Opcodes.*;

public class SwizzleSetInsnTree extends InsnTree {

	public final InsnTree vector;
	public final String swizzle;
	public final InsnTree value;

	public SwizzleSetInsnTree(InsnTree vector, String swizzle, InsnTree value) {
		super(vector.type());
		this.vector = vector;
		this.swizzle = swizzle;
		this.value = value;
	}

	public static SwizzleSetInsnTree create(InsnTree vector, String swizzle, InsnTree values) {
		if (vector.type().shape == GroupShape.UNIT) {
			return null;
		}
		if (SwizzleInsnTree.getIndices(swizzle, vector.type().shape.rows, true) == null) {
			return null;
		}
		if (swizzle.length() != values.types().length) {
			return null;
		}
		for (VectorType type : values.types()) {
			if (type.shape != GroupShape.UNIT) {
				return null;
			}
		}
		return new SwizzleSetInsnTree(vector, swizzle, values);
	}

	@Override
	public void emitBytecode(Context context) {
		this.vector.emitBytecode(context);
		this.value.emitBytecode(context);
		context.codeBuilder.invokeDynamic(
			this.swizzle,
			Type.getMethodDescriptor(
				Type.getType(this.vector.type().holderClass()),
				Util.make(new Type[this.value.types().length + 1], (Type[] array) -> {
					array[0] = Type.getType(this.vector.type().holderClass());
					Type componentType = Type.getType(this.type().componentType.holderClass(GroupShape.UNIT));
					Arrays.fill(array, 1, array.length, componentType);
				})
			),
			new Handle(
				H_INVOKESTATIC,
				Type.getInternalName(SwizzleSetInsnTree.class),
				"makeBlender",
				Type.getMethodDescriptor(
					Type.getType(CallSite.class),
					Type.getType(MethodHandles.Lookup.class),
					Type.getType(String.class),
					Type.getType(MethodType.class),
					Type.getType(VectorType.class)
				),
				false
			),
			new ConstantDynamic(
				this.type().name(),
				Type.getDescriptor(VectorType.class),
				new Handle(
					H_INVOKESTATIC,
					Type.getInternalName(ConstantBootstraps.class),
					"enumConstant",
					Type.getMethodDescriptor(
						Type.getType(Enum.class),
						Type.getType(MethodHandles.Lookup.class),
						Type.getType(String.class),
						Type.getType(Class.class)
					),
					false
				)
			)
		);
		/*
		context.codeBuilder.invokedynamic(
			DynamicCallSiteDesc.of(
				ConstantDescs.ofCallsiteBootstrap(
					Util.desc(SwizzleSetInsnTree.class),
					"makeBlender",
					Util.desc(CallSite.class),
					Util.desc(VectorType.class)
				),
				this.swizzle,
				MethodTypeDesc.of(
					Util.desc(this.vector.type().holderClass()),
					Util.make(new ClassDesc[this.value.types().length + 1], (ClassDesc[] array) -> {
						array[0] = Util.desc(this.vector.type().holderClass());
						ClassDesc componentType = Util.desc(this.type().componentType.holderClass(GroupShape.UNIT));
						Arrays.fill(array, 1, array.length, componentType);
					})
				),
				this.type().describeConstable().orElseThrow()
			)
		);
		*/
	}

	public static CallSite makeBlender(
		MethodHandles.Lookup caller,
		String swizzles,
		//(Vector<E>, E...) -> Vector<E>
		MethodType methodType,
		VectorType type
	)
	throws NoSuchMethodException, IllegalAccessException {
		if (methodType.parameterCount() != swizzles.length() + 1) {
			throw new IllegalArgumentException("Swizzle length does not match method type parameter count");
		}
		if (methodType.returnType() != type.holderClass()) {
			throw new IllegalArgumentException("Return type must be vector type");
		}
		if (methodType.returnType() != methodType.parameterType(0)) {
			throw new IllegalArgumentException("First argument must be vector type");
		}
		Class<?> componentType = type.componentType.holderClass(GroupShape.UNIT);
		for (int index = 1; index < methodType.parameterCount(); index++) {
			if (methodType.parameterType(index) != componentType) {
				throw new IllegalArgumentException("All remaining arguments must be of the vector's component type.");
			}
		}
		int[] indices = SwizzleInsnTree.getIndices(swizzles, type.shape.rows, true);
		if (indices == null) {
			throw new IllegalArgumentException("Invalid swizzles: " + swizzles);
		}
		if (indices.length == 1) {
			MethodHandle handle = caller.findVirtual(methodType.returnType(), "withLane", MethodType.methodType(methodType.returnType(), int.class, methodType.parameterType(1)));
			handle = MethodHandles.insertArguments(handle, 1, indices[0]);
			return new ConstantCallSite(handle);
		}
		else {
			int vectorLength = type.shape.rows == 3 ? 4 : type.shape.rows;
			boolean[] maskFlags = new boolean[vectorLength];
			int[] reverse = new int[vectorLength];
			for (int indexIndex = 0; indexIndex < indices.length; indexIndex++) {
				reverse[indices[indexIndex]] = indexIndex;
				maskFlags[indices[indexIndex]] = true;
			}
			Class<?> arrayType = componentType.arrayType();
			//(E...) -> E[]
			MethodHandle arrayConstructor = MethodHandles.identity(arrayType).asCollector(arrayType, indices.length);
			VectorSpecies<?> species = VectorSpecies.of(
				componentType,
				VectorShape.forBitSize(
					vectorLength << (type.componentType.isDoubleWidth ? 6 : 5)
				)
			);
			//(species, values, offset, indices, indexOffset) -> vector
			MethodHandle fromArray = caller.findStatic(methodType.returnType(), "fromArray", MethodType.methodType(methodType.returnType(), VectorSpecies.class, arrayType, int.class, int[].class, int.class));
			//(species, values) -> vector
			fromArray = MethodHandles.insertArguments(fromArray, 2, 0, reverse, 0);
			//(values) -> vector
			fromArray = MethodHandles.insertArguments(fromArray, 0, species);

			//(E...) -> vector
			MethodHandle fromValues = MethodHandles.collectArguments(fromArray, 0, arrayConstructor);
			fromValues = fromValues.asType(fromValues.type().changeReturnType(Vector.class));
			VectorMask<?> mask = VectorMask.fromArray(species, maskFlags, 0);
			//(vector, vector, mask) -> vector
			MethodHandle blend = caller.findVirtual(methodType.returnType(), "blend", MethodType.methodType(methodType.returnType(), Vector.class, VectorMask.class));

			//(vector, vector) -> vector
			MethodHandle blender = MethodHandles.insertArguments(blend, 2, mask);
			//(vector, E...) -> vector.
			blender = MethodHandles.collectArguments(blender, 1, fromValues);
			return new ConstantCallSite(blender);
		}
	}
}