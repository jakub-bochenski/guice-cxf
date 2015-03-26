The most important binding provided by the CXFModule is the `JAXRSServerFactoryBean`. You can use it to easily create a server by doing `injector.getInstance(JAXRSServerFactoryBean.class).create()`

A set of `ResourceProvider`s will be bound using the multibinder.
For each resource class a `GuicePerRequestResourceProvider` parametrized with the resource type will be registered.

A `Set<Object>` annotated with `CXFModule.@JaxRsProvider` will be bound containting an instance of each registered JAX-RS Provider

A singleton instance of `CXFModule.ServerConfiguration` will be bound with configuration options.

If a custom `Invoker` was configured it will be bound, otherwise a `CXFModule.DefaultInvoker` will be bound.


With the exception of `ServerConfiguration` bean no instances of business classes are created during binding.