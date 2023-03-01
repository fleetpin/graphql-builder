package com.fleetpin.graphql.builder;

import com.fleetpin.graphql.builder.annotations.Scalar;
import com.fleetpin.graphql.builder.annotations.Union;
import com.fleetpin.graphql.builder.mapper.InputTypeBuilder;
import graphql.Scalars;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EntityProcessor {

	private final GraphQLCodeRegistry.Builder codeRegistry;
	private final DirectivesSchema directives;

	private final Map<String, EntityHolder> entities;

	EntityProcessor(GraphQLCodeRegistry.Builder codeRegistry, DirectivesSchema diretives) {
		this.entities = new HashMap<>();
		addDefaults();

		this.codeRegistry = codeRegistry;
		this.directives = diretives;
	}

	private void addDefaults() {
		put(Boolean.class, new ScalarEntity(Scalars.GraphQLBoolean));
		put(Boolean.TYPE, new ScalarEntity(Scalars.GraphQLBoolean));

		put(Float.class, new ScalarEntity(Scalars.GraphQLFloat));
		put(Float.TYPE, new ScalarEntity(Scalars.GraphQLFloat));

		put(Double.class, new ScalarEntity(Scalars.GraphQLFloat));
		put(Double.TYPE, new ScalarEntity(Scalars.GraphQLFloat));

		put(Integer.class, new ScalarEntity(Scalars.GraphQLInt));
		put(Integer.TYPE, new ScalarEntity(Scalars.GraphQLInt));

		put(String.class, new ScalarEntity(Scalars.GraphQLString));

		put(Long.class, new ScalarEntity(SchemaBuilder.LONG_SCALAR));
		put(Long.TYPE, new ScalarEntity(SchemaBuilder.LONG_SCALAR));

		put(Instant.class, new ScalarEntity(SchemaBuilder.INSTANT_SCALAR));
		put(LocalDate.class, new ScalarEntity(SchemaBuilder.DATE_SCALAR));
		put(ZoneId.class, new ScalarEntity(SchemaBuilder.ZONE_ID_SCALAR));
		put(Duration.class, new ScalarEntity(SchemaBuilder.DURATION_SCALAR));
		put(MonthDay.class, new ScalarEntity(SchemaBuilder.MONTH_DAY_SCALAR));
		put(YearMonth.class, new ScalarEntity(SchemaBuilder.YEAR_MONTH_SCALAR));
	}

	private void put(Class<?> type, ScalarEntity entity) {
		var name = EntityUtil.getName(new TypeMeta(null, type, type));
		entities.put(name, entity);
	}

	Set<GraphQLType> getAdditionalTypes() {
		return entities.values().stream().flatMap(s -> s.types()).collect(Collectors.toSet());
	}

	public EntityHolder getEntity(Class<?> type) {
		return getEntity(new TypeMeta(null, type, type));
	}

	EntityHolder getEntity(TypeMeta meta) {
		String name = EntityUtil.getName(meta);
		return entities.computeIfAbsent(
			name,
			__ -> {
				Class<?> type = meta.getType();
				Type genericType = meta.getGenericType();
				if (genericType == null) {
					genericType = type;
				}
				try {
					if (type.isAnnotationPresent(Scalar.class)) {
						return new ScalarEntity(directives, meta);
					}
					if (type.isEnum()) {
						return new EnumEntity(directives, meta);
					} else {
						return new ObjectEntity(this, meta);
					}
				} catch (ReflectiveOperationException | RuntimeException e) {
					throw new RuntimeException("Failed to build schema for class " + type, e);
				}
			}
		);
	}

	public GraphQLOutputType getType(TypeMeta meta, Annotation[] annotations) {
		for (var annotation : annotations) {
			if (annotation instanceof Union) {
				var union = (Union) annotation;
				return getUnionType(meta, union);
			}
		}

		return getEntity(meta).getType(meta, annotations);
	}

	private GraphQLOutputType getUnionType(TypeMeta meta, Union union) {
		var name = UnionType.name(union);

		return entities
			.computeIfAbsent(
				name,
				__ -> {
					try {
						return new UnionType(this, union);
					} catch (RuntimeException e) {
						throw new RuntimeException("Failed to build schema for union " + union, e);
					}
				}
			)
			.getType(meta, null);
	}

	public GraphQLInputType getInputType(TypeMeta meta, Annotation[] annotations) {
		return getEntity(meta).getInputType(meta, annotations);
	}

	void addSchemaDirective(AnnotatedElement element, Class<?> location, Consumer<GraphQLAppliedDirective> builder) {
		this.directives.addSchemaDirective(element, location, builder);
	}

	DirectivesSchema getDirectives() {
		return directives;
	}

	GraphQLCodeRegistry.Builder getCodeRegistry() {
		return codeRegistry;
	}

	public InputTypeBuilder getResolver(TypeMeta meta) {
		return getEntity(meta).getResolver(meta);
	}

	public InputTypeBuilder getResolver(Class<?> type) {
		var meta = new TypeMeta(null, type, type);
		return getEntity(meta).getResolver(meta);
	}
	//
	//
	//	private static Class<?> extraOptionalType(Type type) {
	//		if (type instanceof Class) {
	//			return (Class<?>) type;
	//		} else if (type instanceof ParameterizedType) {
	//			return extraOptionalType(((ParameterizedType) type).getActualTypeArguments()[0]);
	//		}
	//		throw new RuntimeException("extraction failure for " + type.getClass());
	//	}

	//
	//	private String getNameInput(TypeMeta meta) {
	//		var type = meta.getType();
	//		String name = null;
	//		if (type.isEnum()) {
	//			name = type.getSimpleName();
	//		}else if (type.isAnnotationPresent(Scalar.class)) {
	//			name = type.getSimpleName();
	//		}else if (type.isAnnotationPresent(OneOf.class)) {
	//			name = type.getSimpleName();
	//		}else if (type.isAnnotationPresent(Entity.class)) {
	//			if (type.getAnnotation(Entity.class).value() == SchemaOption.BOTH) {
	//				name = type.getSimpleName() + "Input";
	//			} else {
	//				name = type.getSimpleName();
	//			}
	//		}else {
	//			name = type.getSimpleName() + "Input";
	//		}
	//
	//		var genericType = meta.getGenericType();
	//
	//		if (genericType instanceof ParameterizedType) {
	//			var parameterizedTypes = ((ParameterizedType) genericType).getActualTypeArguments();
	//
	//			for (var t : parameterizedTypes) {
	//				if (t instanceof Class) {
	//					String extra = ((Class) t).getSimpleName();
	//					name += "_" + extra;
	//				}
	//			}
	//		}
	//
	//		return name;
	//	}

	//	public String processInput(TypeMeta meta) {
	//		String name = getNameInput(meta);
	//		if (name != null && !this.additionalTypes.containsKey(name) && !inputTypeBuilders.containsKey(meta.getGenericType())) {
	//			this.additionalTypes.put(name, null); // so we don't go around in circles if depend on self
	//			addType(meta, true);
	//		}
	//		return name;
	//	}
	//
	//	public ObjectBuilder getResolver(TypeMeta meta) {
	//		processInput(meta);
	//		return InputTypeBuilder.process(inputTypeBuilders, meta.getTypes().iterator());
	//	}
	//
	//	public EntitiesHolder getEntities() {
	//		return entities;
	//	}

}
