# graphql-builder
Builds a graphql schema from a model using reflection.
It reads parameter and method names of the java classes to build the schema.
It requires java11 and `-parameters` compile argument. This allows method argument names to be read via reflection preventing annotations per argument.
This aproach means your method / argument names are limmited to valid java names.


An example using this library can be found [here](https://github.com/ashley-taylor/graphql-aws-lamba-example)


## Getting Started
To build the object you pass the package name to the schema builder class
```java
GraphQL build = SchemaBuilder.build("com.example.graph.schema.app").build();
```

## Creating an Entity

### type entity
```java
@Entity
public class User {
  private String id;
  private String name;
  
  @Id
  public String getId() {
    return id;
  }
  
  public String getName() {
    return name;
  }
}
```

This defines a GraphQL output type that matches this schema

```graphql
type User {
  id: ID!
  name: String!
}
```
### Input entity

To create an input entity specify input on the `@Entity` annotaion you can also specify `both` the input entity will sufixed with `Input`

```java
@Entity(SchemaOption.INPUT)
public class UserInput {
  private String id;
  private String name;
  
  
  public void setId(@Id String id) {
    this.id = id;
  }
  
  public void setName(String name) {
    this.name = name;
  }
}
```

This defines a graphql input entity that matches
```graphql
input User {
  id: ID!
  name: String!
}
```

## Optional vs Required
by default using this library all fields are required. If you want something to be optional wrap it with Optional<type>. It is done this way since optional is a type built into the JDK with existing support.
  ```java
@Entity
public class User {
  private String id;
  private Optional<String> name;
  
  @Id
  public String getId() {
    return id;
  }
  
  public Optional<String> getName() {
    return name;
  }
}
```

This defines a GraphQL output type that matches this schema

```graphql
type User {
  id: ID!
  name: String
}
```



## Context
any method may include the context as a parameter. The context class must include the `@Context` annotation so it knows not to treat it as an argument.

Defining context
```java
@Context
public class ApiContext {
  private Database database;
  public Database getDatabase() {
    return database;
  }
}
```

Calling
```java
public CompletableFuture<Address> getAddress(ApiContext context) {
  return context.getDatabase().getLink(this, Address.class);
}
```

## Query
To perform a query you add the `@Query` annotation to a static method. It does not need to be on the matching type

```java
@Query
public static CompletableFuture<List<User>> users(ApiContext context, @Id String organisationId) {
  return context.getDatabase().query(User.class);
}
```
This will create the following schema
```graphql
extend type Query {
	users(organisationId: ID!): [User!]!
}
```
Again if you want anything to be optional use that java `Optional` class
  
## Mutation
Mutatations are similar queries `@Mutation` must be applied to a static method.
  
```java
@Mutation
public static CompletableFuture<User> putUser(ApiContext context, @Id String organisationId, @Id Optional<String> userId, String name) {
  //insert logic
}
```
  
This will create the following schema
```graphql
extend type Mutation {
  putUser(organisationId: ID!, userId: ID, name: String): User!
}
```

## Subscriptions
very similar to query add `@Subscription` and method must return a `Publisher` type

```java
@Subscription
public static Publisher<User> usersUpdated(ApiContext context, @Id String organisationId) {
  //subscription logic
}
```
  
This will create the following schema
```graphql
extend type Subscription {
  usersUpdated(organisationId: ID!): User!
}
```

## Inheritance
To create an inheritance object you can use `interface` or `abstract class` you also need to add the `@Entity` annotation to the parent as well

```java
@Entity
public abstract class Animal {
  String name;
  
  public String getName() {
    return name;
  }
}

@Entity
public class Cat extends Animal {
  String meow;
  
  public String getMeow() {
    return meow;
  }
}
```
This will create the following schema
```graphql
interface Animal {
  name: String!
}

type Cat implements Animal {
  name: String!
  meow: String!
}
```

## Ignore method
If there is a getter that you don't want exposed in the graphql schema add `@GraphQLIgnore` to the method

```java
@Entity
public class User {
  String id;
  String dbId;
  
  @Id
  public String getId() {
    return id;
  }
  
  @GraphQLIgnore
  public String getDbId() {
    return dbId;
  }
}
```
This will create the following schema
```graphql
type User {
  id: ID!
}
```

## Enum
to create a GraphQL enum add the `@Entity` annotation to a java enum

```java
public enum Animal {
  CAT,
  DOG
}
```
This will create the following schema
```graphql
enum Animal {
  CAT
  DOG
}
```


## Package Authorizer
The base package requires an Authorizer. This is a call that will determine if an endpoint is accessable. This will also flow down to child packages.

This is designed for things like organisation access

This class needs to implement a method called allow, that could look like something like the following.
```java
public class UserAuthorizer implements Authorizer {
  public CompletableFuture<Boolean> allow(DataFetchingEnvironment env) {
  ApiContext context = env.getContext();
  context.setOrganisationId(env.getArgument("organisationId"));
  if(context.getUser() == null) {
    return Promise.done(false);
  }
  return context.getUser().getMembership(context, context.getOrganisationId()).thenApply(membership -> {
  if(membership == null) {
    return false;
  }
  context.setOrganisationMembership(membership);
    return true;
  });
}
```

## Entity type restrictions
If you have a permissions matrix that needs implemented this feature makes this easy.
The methods will still return the objects and then be filtered in the graphql library.
This means where ever that object is returned it will apply the security model

To implement this you need to add an annotation to the class and implement the restriction factory 
```java
@Entity
@Restrict(AnimalRestriction.class)
public class Animal {
  ...
}


public class AnimalRestriction implements RestrictTypeFactory<Animal> {

  @Override
  public CompletableFuture<RestrictType<Animal>> create(DataFetchingEnvironment env) {
    ...
  }
}

public class AssetRestrict implements RestrictType<Animal> {

  @Override
  public CompletableFuture<Boolean> allow(Animal animal) {
    ...
  }
}
```

## Directives
These are similar to GraphQL directives but just implemented on the java model
You define a custom annotation and add the `@Directive` to it

```java
@Retention(RUNTIME)
@Directive(AdminOnly.AdminOnlyDirective.class)
public @interface AdminOnly {
  ...
}

public class AdminOnlyDirective implements DirectiveCaller<AdminOnly> {
  
  @Override
  public Object process(AdminOnly annotation, DataFetchingEnvironment env, DataFetcher<?> fetcher) throws Exception {
    ...
  }
}
```

The annotation can then be used on any method

```java
@Query
@AdminOnly
public static CompletableFuture<List<User>> users(ApiContext context, @Id String organisationId) {
  return context.getDatabase().query(User.class);
}
```
