package com.fleetpin.graphql.builder.mapper;

import com.fleetpin.graphql.builder.EntityProcessor;
import com.fleetpin.graphql.builder.TypeMeta;
import graphql.GraphQLContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class ObjectFieldBuilder implements InputTypeBuilder {

	private final InputTypeBuilder map;

	public ObjectFieldBuilder(Class<?> type, ArrayList<FieldMapper> mappers) {
		try {
			var constructor = type.getConstructor();
			map =
				(obj, context, locale) -> {
					try {
						if (type.isInstance(obj)) {
							return obj;
						}

						var toReturn = constructor.newInstance();

						Map map = (Map) obj;

						for (var mapper : mappers) {
							var name = mapper.getName();
							if (map.containsKey(name)) {
								mapper.map(toReturn, map.get(name), context, locale);
							}
						}

						return toReturn;
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object convert(Object obj, GraphQLContext graphQLContext, Locale locale) {
		return map.convert(obj, graphQLContext, locale);
	}

	public static class FieldMapper {

		private final Method method;
		private final String name;
		private final InputTypeBuilder mapper;

		private FieldMapper(String name, Method method, InputTypeBuilder objectBuilder) {
			this.name = name;
			this.method = method;
			this.mapper = objectBuilder;
		}

		public String getName() {
			return name;
		}

		protected void map(Object inputType, Object argument, GraphQLContext graphQLContext, Locale locale) throws ReflectiveOperationException {
			method.invoke(inputType, mapper.convert(argument, graphQLContext, locale));
		}

		public static FieldMapper build(EntityProcessor entityProcessor, TypeMeta inputType, String name, Method method) {
			return new FieldMapper(name, method, entityProcessor.getResolver(inputType));
		}
	}
}
