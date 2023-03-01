package com.fleetpin.graphql.builder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;

public class EntityUtil {

	private static final Method IS_RECORD_METHOD;

	static {
		Class<? extends Class> classClass = Class.class;
		Method isRecord;
		try {
			isRecord = classClass.getMethod("isRecord");
		} catch (NoSuchMethodException e) {
			isRecord = null;
		}
		IS_RECORD_METHOD = isRecord;
	}

	static boolean isRecord(Class<?> type) {
		if (IS_RECORD_METHOD == null) {
			return false;
		}
		try {
			return (Boolean) IS_RECORD_METHOD.invoke(type);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	static String getName(TypeMeta meta) {
		var type = meta.getType();

		String name = type.getSimpleName();

		var genericType = meta.getGenericType();

		for (int i = 0; i < type.getTypeParameters().length; i++) {
			if (genericType instanceof ParameterizedType) {
				var t = ((ParameterizedType) genericType).getActualTypeArguments()[i];
				if (t instanceof Class) {
					String extra = ((Class) t).getSimpleName();
					name += "_" + extra;
				} else if (t instanceof TypeVariable) {
					var variable = (TypeVariable) t;
					Class extra = meta.resolveToType(variable);
					if (extra != null) {
						name += "_" + extra.getSimpleName();
					}
				}
			} else {
				Class extra = meta.resolveToType(type.getTypeParameters()[i]);
				if (extra != null) {
					name += "_" + extra.getSimpleName();
				}
			}
		}
		if (meta.isDirect()) {
			name += "_DIRECT";
		}

		return name;
	}
}
