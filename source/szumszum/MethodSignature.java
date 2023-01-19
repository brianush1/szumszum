package szumszum;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class MethodSignature implements Serializable {
	private static final long serialVersionUID = 1L;

	public String returnType;
	public String[] modifiers;
	public String name;
	public int numParams;
	public String[] paramTypes;
	public String[] paramNames;

	public MethodSignature(
		String[] modifiers,
		String returnType,
		String name,
		int numParams,
		String[] paramTypes,
		String[] paramNames
	) {
		this.returnType = returnType;
		this.modifiers = modifiers;
		this.name = name;
		this.numParams = numParams;
		this.paramTypes = paramTypes;
		this.paramNames = paramNames;
		sortModifiers();
	}

	public MethodSignature(Method method) {
		returnType = canonicalizeType(method.getGenericReturnType());

		numParams = method.getParameterCount();

		paramTypes = new String[numParams];
		paramNames = new String[numParams];

		for (int j = 0; j < numParams; j += 1) {
			String paramType = canonicalizeType(method.getGenericParameterTypes()[j]);
			String paramName = method.getParameters()[j].getName();
			paramTypes[j] = paramType;
			paramNames[j] = paramName;
		}

		ArrayList<String> modifierList = new ArrayList<>();

		int m = method.getModifiers();
		if (Modifier.isPublic(m))
			modifierList.add("public");
		if (Modifier.isPrivate(m))
			modifierList.add("private");
		if (Modifier.isProtected(m))
			modifierList.add("protected");
		if (Modifier.isFinal(m))
			modifierList.add("final");
		if (Modifier.isAbstract(m))
			modifierList.add("abstract");
		if (Modifier.isStatic(m))
			modifierList.add("static");

		modifiers = modifierList.toArray(new String[modifierList.size()]);
		sortModifiers();

		name = method.getName();
	}

	private static final String[] MODIFIER_ORDER = {
			"public", "private", "protected",
			"final",
			"abstract",
			"static"
	};

	private void sortModifiers() {
		Arrays.sort(modifiers, (String a, String b) -> {
			int aOrder = Arrays.asList(MODIFIER_ORDER).indexOf(a);
			int bOrder = Arrays.asList(MODIFIER_ORDER).indexOf(b);

			if (aOrder == -1 && bOrder == -1) {
				return a.compareTo(b);
			} else if (aOrder == -1) {
				return 1;
			} else if (bOrder == -1) {
				return -1;
			} else {
				return aOrder - bOrder;
			}
		});
	}

	private static String canonicalizeType(Type type) {
		return type.toString()
				.replaceAll("java(\\.[a-zA-Z0-9_]+)+\\.([A-Z][a-zA-Z0-9_]*)", "$2")
				.replaceAll("class ([A-Z][a-zA-Z0-9_]*)", "$1");
	}

	private static String highlightType(String type) {
		return type.replaceAll("([a-zA-Z0-9_]+)", "\u001B[1;36m$1\u001B[0m");
	}

	public String highlight() {
		return highlight(null);
	}

	public String highlight(boolean[] underlinedParams) {
		String result = "";

		result += "\u001B[1;34m";
		for (String modifier : modifiers) {
			result += modifier + " ";
		}

		result += highlightType(returnType);

		result += " ";
		result += name;

		result += "(";

		for (int j = 0; j < numParams; j += 1) {
			if (j > 0)
				result += ", ";
			result += highlightType(paramTypes[j]);

			result += " ";

			if (underlinedParams != null && underlinedParams[j]) {
				result += "\u001B[4m";
				result += paramNames[j];
				result += "\u001B[0m";
			} else {
				result += paramNames[j];
			}
		}

		result += ")";

		return result;
	}

	public boolean checkCovariance(MethodSignature other) {
		return returnType.equals(other.returnType)
				&& Arrays.equals(paramTypes, other.paramTypes)
				&& name.equals(other.name)
				&& Arrays.equals(modifiers, other.modifiers);
	}

}
