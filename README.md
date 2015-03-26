A simple EDSL (Embedded Domain-Specific Language) for configuring the Apache CXF implementation of JSR-311.

Provides a configured JAXRSServerFactoryBean instance that you can use to start a Server in any particular way you need.

Resources and providers will be created with Guice, then they will have the normal CXF/JAX-RS injections performed on them.

Example:
```java
protected void configure() {		 
 serve().atAddress("/rest");
  
 publish(LibraryResource.class);
  
 readAndWriteBody(JAXBElementProvider.class);
 readAndWriteBody(JSONProvider.class);
 
 mapExceptions(ApplicationExceptionMapper.class);
}
```

guice-cxf supports injecting Jax-Rs resource with dependencies bound in Guice.
```java
@Path("foo");
public class LibraryResource {

   @Inject private BookService service;

   @GET
   public Books search(@QueryParam("q") String q){
      return service.findBooks(q);
   }
   
   // [...]
   
}
```

You can also enable injecting sub-resources instead of creating them manually;

```java
protected void configure(){
 bind(BookResource.class).in(REQUEST);
}
```
```java
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

Finally you can inject @Context dependencies (`HttpHeaders`, `Request`, `UriInfo` etc.), provided you are in appropriate scope (there is a special REQUEST scope provided for Jax-Rs requets).

```java
public class BookTitleResolver {
   @Inject
   private HttpHeaders headers;

   public String resolveTitleFor(String id){
     return getBook(id).getTitleForLanguage(headers.getLanguage());
   }

   // [...]
  
}
```

To create a server do:

```java
  injector.getInstance(JAXRSServerFactoryBean.class).create();
```

The current version is well tested and working, although not all CXF features can be configured yet (you can always set them on the provided JAXRSServerFactoryBean instance).

See the [Features] page for a more complete list.
