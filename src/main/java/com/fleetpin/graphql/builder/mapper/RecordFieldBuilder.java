package com.fleetpin.graphql.builder.mapper;

import graphql.GraphQLContext;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class RecordFieldBuilder implements InputTypeBuilder {

	private final InputTypeBuilder map;

	public RecordFieldBuilder(Class<?> type, ArrayList<RecordMapper> mappers) {
		var argTypes = mappers.stream().map(t -> t.type).toArray(Class[]::new);

		try {
			var constructor = type.getConstructor(argTypes);
			map =
				(obj, context, locale) -> {
					try {
						if (type.isInstance(obj)) {
							return obj;
						}

						Map map = (Map) obj;

						var args = new Object[argTypes.length];

						for (int i = 0; i < args.length; i++) {
							var mapper = mappers.get(i);

							if (map.containsKey(mapper.name)) {
								args[i] = mapper.resolver.convert(map.get(mapper.name), context, locale);
							}
						}

						return constructor.newInstance(args);
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

	public static class RecordMapper {

		private final String name;
		private final Class<?> type;
		private final InputTypeBuilder resolver;

		public RecordMapper(String name, Class<?> type, InputTypeBuilder resolver) {
			this.name = name;
			this.type = type;
			this.resolver = resolver;
		}
	}
}
