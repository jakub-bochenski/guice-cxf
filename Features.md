Guice-CXF lets you use a EDSL to easily configure CXF JAX-RS module.

Extend the `CXFModule` class to bind the needed classes and configure the server.

### Language elements ###

Use `publish()` to register a resource class - a custom `ResourceProvider` will be bound for each resource class.
It's a 'per-request' type and will get a new instance for each incoming request.

Use `serve()` to configure server, e.g. set the root address.

The following methods let you register JAX-RS `@Provider`s:

  * `handleRequest()` - register a `RequestHandler`;
  * `handleResponse()` - register a `ResponseHandler`;
  * `mapExceptions()` - register an `ExceptionMapper`;
  * `readBody()` - register a `MessageBodyReader`;
  * `writeBody()` - register a `MessageBodyWriter`;
  * `writeAndReadBody()` - register a class that is both a `MessageBodyReader` and a `MessageBodyWriter` (e.g. `JAXBElementProvider` or `JSONProvider`);
  * `provide()` - generic method to register any JAX-RS `@Provider`, it's best to use specific methods if available, since they are type safe;

_Please note that a single instance of each `@Provider` class will be passed to the `ServerFactoryBean`, regardless of the scope._

Use `invokeVia()` to register custom invoker.

### Binding resources and providers ###
It is possible to bind concrete classes, but a very nice feature is the ability to bind interfaces.
```
protected void configureResources() {
  publish(ResourceInterface.class);
}
```

Then in a separate module you can do something like
`bind(ResourceInterface.class).to(ResourceImpl.class)`
to define the concrete implementation. This let's you easily register mock objects for testing.

Another use of indirect binding is configuring the `@Provider`s, see here an example configuration of the `JSONProvider`.

```
@Provides
public JSONProvider provdeJsonProvider(@Named("ignoreNamespaces") boolean ignoreNamespaces) {
  final JSONProvider json = new JSONProvider();
  json.setIgnoreNamespaces(ignoreNamespaces);
  return json;
}
```
_Of course if you implement your own `@Provider`s it's best to use constructor/method injections directly on them._


### Injecting dependencies ###
Since all resources and providers are created by Guice you can use all the usual bindings (remember to provide an `@Inject` annotated constructor).

In addition you can use `CXFScopes.REQUEST` scope (or `Provider<T>` interface) to get access to bindings provided by Jax-Rs, or share your own classes inside a single request.

The available bindings are:
  * `javax.ws.rs.core.HttpHeaders`;
  * `javax.ws.rs.core.Request`;
  * `javax.ws.rs.core.SecurityContext`;
  * `javax.ws.rs.core.UriInfo`;
  * `javax.ws.rs.ext.Providers`;
  * `org.apache.cxf.jaxrs.ext.MessageContext`;
  * `org.apache.cxf.message.Exchange`;
  * `org.apache.cxf.service.Service`;

_You can register providers to bind any specific dependency you can extract from those classes too._

```
public class BookTitleResolver {
   @Inject
   private HttpHeaders headers;

   public String resolveTitleFor(String id){
     return getBook(id).getTitleForLanguage(headers.getLanguage());
   }

   // [...]
  
}
```

### Subresource injection ###
With guice-cxf you can have Guice construct subresource instances, completely with injecting them with appropriate dependencies.

First, you have to enable this feature
```
protected void configure() {	
  serve().atAddress("/rest").withSubresourcesInjection();
}
```

Then all you need to do is annotate the subresource locator method with @Injected annotation. The method must also be non-final.
```
@Path("foo");
public class LibraryResource {
   // [...]
   
   @Injected
   @Path("{id}")
   public BookResource getBook(@PathParam("id") String id){
      throw new UnsupportedOperationException("will be implemented by Guice");
   }

}
```

In one of modules you need to bind the declared class.
```

protected void configure(){
 bind(BookResource.class).to(CachedBookResource.class).in(REQUEST);
 // [...]
 }
```

Guice will then take care of implementing the locator method for you, using AOP.

### Misc ###

The server has to be started manually, this might be a bit cumbersome, however it will save you from running into this [bug](http://code.google.com/p/google-guice/issues/detail?id=183).

See the [Bindings](Bindings.md) page for a list of resulting bindings.