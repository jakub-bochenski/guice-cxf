It can of course be done in many ways, here is one simple example.

All you need to do is save the injector in a context attribute (preferably using a `ServletListener`) then you can use this simple servlet.

```
public class CustomNonSpringJaxrsServlet extends CXFNonSpringJaxrsServlet {

	public static final String ATTRIBUTE_INJECTOR = "GuiceCXF#Injector";

	@Override
	protected void createServerFromApplication(String cName,
			ServletConfig servletConfig) throws ServletException {

		final Injector injector = (Injector) servletConfig.getServletContext()
				.getAttribute(ATTRIBUTE_INJECTOR);
		final JAXRSServerFactoryBean bean = injector
				.getInstance(JAXRSServerFactoryBean.class);

// this will use standard CXFNonSpringJaxrsServlet config for features not configurable with Guice-CXF
		setExtensions(bean, servletConfig);
		setSchemasLocations(bean, servletConfig);
		bean.setBus(getBus());
		bean.create();
	}

}
```


There are [examples](http://stackoverflow.com/a/8228288/1237617) on the web of using guice-servlet to do it in a simpler way, however I'd advice against it due to this [collection of bugs](http://code.google.com/p/google-guice/issues/list?q=requestdispatcher)