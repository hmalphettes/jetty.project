package org.eclipse.jetty.osgi.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.profile;
import static org.ops4j.pax.exam.spi.PaxExamRuntime.createContainer;
import static org.ops4j.pax.exam.spi.PaxExamRuntime.createTestSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TimeoutException;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.libraries.JUnitBundlesOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
//import org.eclipse.jetty.osgi.boot.internal.serverfactory.DefaultJettyAtJettyHomeHelper;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.http.HttpService;

@RunWith( JUnit4TestRunner.class )
//@ExamReactorStrategy( AllConfinedStagedReactorFactory.class )
public class TestJettyOSGiBootCore {

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config()
    {
        ArrayList<Option> options = new ArrayList<Option>();
        options.addAll(provisionCoreJetty());
        options.add(CoreOptions.junitBundles());
        options.addAll(httpServiceJetty());
        return options.toArray(new Option[options.size()]);
    }
    
    public static List<Option> provisionCoreJetty()
    {
    	List<Option> res = new ArrayList<Option>();
    	// get the jetty home config from the osgi boot bundle.
    	res.add(CoreOptions.systemProperty("jetty.port").value("9876"));
    	res.add(CoreOptions.systemProperty("jetty.home.bundle").value("org.eclipse.jetty.osgi.boot"));
    	res.addAll(coreJettyDependencies());
    	return res;
    }

    
    public static List<Option> coreJettyDependencies() {
    	List<Option> res = new ArrayList<Option>();

        res.add(mavenBundle().groupId( "org.eclipse.jetty.osgi" ).artifactId( "jetty-osgi-boot" ).versionAsInProject().start());

    	res.add(mavenBundle().groupId( "org.eclipse.jetty.orbit" ).artifactId( "javax.servlet" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-deploy" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-server" ).versionAsInProject().noStart());  
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-servlet" ).versionAsInProject().noStart());  
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-util" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-http" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-xml" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-webapp" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-io" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-continuation" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-security" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-servlets" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-client" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty.websocket" ).artifactId( "websocket-core" ).versionAsInProject().noStart());
    	res.add(mavenBundle().groupId( "org.eclipse.jetty.websocket" ).artifactId( "websocket-server" ).versionAsInProject().noStart());
        //optional:
    	//res.add(mavenBundle().groupId( "org.eclipse.jetty.websocket" ).artifactId( "websocket-client" ).versionAsInProject().noStart());
    	return res;
    }
    
    public static List<Option> httpServiceJetty()
    {
    	List<Option> res = new ArrayList<Option>();
        res.add(mavenBundle().groupId( "org.eclipse.jetty.osgi" ).artifactId( "jetty-httpservice" ).versionAsInProject().start());
        res.add(mavenBundle().groupId( "org.eclipse.equinox.http" ).artifactId( "servlet" ).version("1.0.0-v20070606").start());
        return res;
    }
    /**
     * You will get a list of bundles installed by default
     * plus your testcase, wrapped into a bundle called pax-exam-probe
     */
    @Test
    public void testHttpService() throws Exception
    {
//      ServletContextHandler sch = null;
//      sch.addServlet("className", "pathSpec").setInitOrder("0");
        
        
        Map<String,Bundle> bundlesIndexedBySymbolicName = new HashMap<String, Bundle>();
        for( Bundle b : bundleContext.getBundles() )
        {
            bundlesIndexedBySymbolicName.put(b.getSymbolicName(), b);
        }
        Bundle osgiBoot = bundlesIndexedBySymbolicName.get("org.eclipse.jetty.osgi.boot");
        Assert.assertNotNull("Could not find the org.eclipse.jetty.osgi.boot bundle", osgiBoot);
        Assert.assertTrue(osgiBoot.getState() == Bundle.ACTIVE);

        Bundle httpServiceOSGiBundle = bundlesIndexedBySymbolicName.get("org.eclipse.jetty.osgi.httpservice");
        Assert.assertNotNull(httpServiceOSGiBundle);
        Assert.assertTrue(httpServiceOSGiBundle.getState() == Bundle.ACTIVE);

        Bundle equinoxServlet = bundlesIndexedBySymbolicName.get("org.eclipse.equinox.http.servlet");
        Assert.assertNotNull(equinoxServlet);
        //interestingly with equinox the bundle is not started. probably a difference in pax-exam and
        //the way the bundles are activated. the rest of the test goes fine.
        Assert.assertTrue(equinoxServlet.getState() == Bundle.ACTIVE);
        
        //in the OSGi world this would be bad code and we should use a bundle tracker.
        //here we purposely want to make sure that the httpService is actually ready.
        ServiceReference sr  =  bundleContext.getServiceReference(HttpService.class.getName());
        Assert.assertNotNull("The httpServiceOSGiBundle is started and should have deployed a service reference for HttpService" ,sr);
        HttpService http = (HttpService)bundleContext.getService(sr);
        http.registerServlet("/greetings", new HttpServlet() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void doGet(HttpServletRequest req,
                    HttpServletResponse resp) throws ServletException,
                    IOException {
                resp.getWriter().write("Hello");
            }
        }, null, null);
        
        //now test the servlet
        HttpClient client = new HttpClient();
        try
        {
            client.start();
            Response response = client.GET("http://127.0.0.1:9876/greetings").get(5, TimeUnit.SECONDS);;
            Assert.assertEquals(HttpStatus.OK_200, response.status());
     
            String content = new String(((HttpContentResponse)response).content());
            Assert.assertEquals("Hello", content);
        }
        finally
        {
            client.stop();
        }
    }
    	
	
}
