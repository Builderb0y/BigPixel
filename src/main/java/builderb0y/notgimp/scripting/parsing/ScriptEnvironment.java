package builderb0y.notgimp.scripting.parsing;

import java.util.*;

import builderb0y.notgimp.scripting.parsing.ScriptHandlers.*;
import builderb0y.notgimp.scripting.tree.InsnTree;
import builderb0y.notgimp.scripting.types.VectorType;

public class ScriptEnvironment implements
	VariableHandler,
	FieldHandler,
	IndexHandler,
	FunctionHandler,
	MethodHandler,
	StaticFunctionHandler,
	KeywordHandler
{

	public static record NamedVectorType(String name, VectorType type) {}
	public Map<String, VariableHandler> variables = new HashMap<>();
	public Map<NamedVectorType, FieldHandler> fields = new HashMap<>();
	public Map<VectorType, IndexHandler> indexes = new HashMap<>();
	public Map<String, List<FunctionHandler>> functions = new HashMap<>();
	public Map<NamedVectorType, List<MethodHandler>> methods = new HashMap<>();
	public Map<NamedVectorType, List<StaticFunctionHandler>> staticFunctions = new HashMap<>();
	public Map<String, KeywordHandler> keywords = new HashMap<>();

	public ScriptEnvironment() {}

	public ScriptEnvironment(ScriptEnvironment that) {
		this.putAll(that);
	}

	public void putAll(ScriptEnvironment that) {
		this.variables.putAll(that.variables);
		this.fields.putAll(that.fields);
		this.indexes.putAll(that.indexes);
		this.functions.putAll(that.functions);
		this.methods.putAll(that.methods);
		this.staticFunctions.putAll(that.staticFunctions);
		this.keywords.putAll(that.keywords);
	}

	@Override
	public InsnTree getVariable(ExpressionParser<?> parser, String name) throws ScriptParsingException {
		VariableHandler handler = this.variables.get(name);
		if (handler == null) throw new ScriptParsingException("Unknown variable: " + name, parser.reader);
		InsnTree variable = handler.getVariable(parser, name);
		if (variable == null) throw new ScriptParsingException("No such variable: " + name, parser.reader);
		return variable;
	}

	@Override
	public InsnTree getField(ExpressionParser<?> parser, InsnTree receiver, String name) throws ScriptParsingException {
		FieldHandler handler = this.fields.get(new NamedVectorType(name, receiver.type));
		if (handler == null) throw new ScriptParsingException("Unknown field: " + name, parser.reader);
		InsnTree field = handler.getField(parser, receiver, name);
		if (field == null) throw new ScriptParsingException("No such field: " + name, parser.reader);
		return field;
	}

	@Override
	public InsnTree getIndex(ExpressionParser<?> parser, InsnTree receiver, InsnTree[] params) throws ScriptParsingException {
		IndexHandler handler = this.indexes.get(receiver.type);
		if (handler == null) throw new ScriptParsingException("Unknown index: " + receiver.type, parser.reader);
		InsnTree index = handler.getIndex(parser, receiver, params);
		if (index == null) throw new ScriptParsingException("No such index: " + receiver.type, parser.reader);
		return index;
	}

	@Override
	public InsnTree getFunction(ExpressionParser<?> parser, String name, InsnTree[] params) throws ScriptParsingException {
		List<FunctionHandler> handlers = this.functions.get(name);
		if (handlers == null) throw new ScriptParsingException("Unknown function: " + name, parser.reader);
		for (FunctionHandler handler : handlers) {
			InsnTree result = handler.getFunction(parser, name, params);
			if (result != null) return result;
		}
		throw new ScriptParsingException(STR."Incorrect arguments: \{name}(\{Arrays.toString(params)})", parser.reader);
	}

	@Override
	public InsnTree getMethod(ExpressionParser<?> parser, InsnTree receiver, String name, InsnTree[] params) throws ScriptParsingException {
		List<MethodHandler> handlers = this.methods.get(new NamedVectorType(name, receiver.type));
		if (handlers == null) throw new ScriptParsingException("Unknown method: " + name, parser.reader);
		for (MethodHandler handler : handlers) {
			InsnTree result = handler.getMethod(parser, receiver, name, params);
			if (result != null) return result;
		}
		throw new ScriptParsingException(STR."Incorrect arguments: \{name}(\{Arrays.toString(params)})", parser.reader);
	}

	@Override
	public InsnTree getStaticFunction(ExpressionParser<?> parser, VectorType type, String name, InsnTree[] params) throws ScriptParsingException {
		List<StaticFunctionHandler> handlers = this.staticFunctions.get(new NamedVectorType(name, type));
		if (handlers == null) throw new ScriptParsingException("Unknown static function: " + name, parser.reader);
		for (StaticFunctionHandler handler : handlers) {
			InsnTree result = handler.getStaticFunction(parser, type, name, params);
			if (result != null) return result;
		}
		throw new ScriptParsingException(STR."Incorrect arguments: \{type}.\{name}(\{Arrays.toString(params)})", parser.reader);
	}

	@Override
	public InsnTree getKeyword(ExpressionParser<?> parser, String name, boolean statement) throws ScriptParsingException {
		KeywordHandler handler = this.keywords.get(name);
		return handler != null ? handler.getKeyword(parser, name, statement) : null;
	}

	public ScriptEnvironment addVariable(String name, VariableHandler handler) {
		if (this.variables.putIfAbsent(name, handler) != null) {
			throw new IllegalArgumentException("Duplicate variable handler: " + name);
		}
		return this;
	}

	public ScriptEnvironment addField(VectorType type, String name, FieldHandler handler) {
		if (this.fields.putIfAbsent(new NamedVectorType(name, type), handler) != null) {
			throw new IllegalArgumentException(STR."Duplicate field handler: \{type}.\{name}");
		}
		return this;
	}

	public ScriptEnvironment addIndex(VectorType type, IndexHandler handler) {
		if (this.indexes.putIfAbsent(type, handler) != null) {
			throw new IllegalArgumentException("Duplicate index handler: " + type);
		}
		return this;
	}

	public ScriptEnvironment addFunction(String name, FunctionHandler handler) {
		this.functions.computeIfAbsent(name, (String _) -> new ArrayList<>()).add(handler);
		return this;
	}

	public ScriptEnvironment addMethod(VectorType type, String name, MethodHandler handler) {
		this.methods.computeIfAbsent(new NamedVectorType(name, type), (NamedVectorType _) -> new ArrayList<>()).add(handler);
		return this;
	}

	public ScriptEnvironment addStaticFunction(VectorType type, String name, StaticFunctionHandler handler) {
		this.staticFunctions.computeIfAbsent(new NamedVectorType(name, type), (NamedVectorType _) -> new ArrayList<>()).add(handler);
		return this;
	}

	public ScriptEnvironment addKeyword(String name, KeywordHandler handler) {
		if (this.keywords.putIfAbsent(name, handler) != null) {
			throw new IllegalArgumentException("Duplicate keyword: " + name);
		}
		return this;
	}
}