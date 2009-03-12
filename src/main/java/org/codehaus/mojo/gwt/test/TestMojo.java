package org.codehaus.mojo.gwt.test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.booter.output.StandardOutputConsumer;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.CommandLineUtils;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.Commandline;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.cli.StreamConsumer;
import org.apache.maven.surefire.report.ReporterManager;
import org.codehaus.mojo.gwt.AbstractGwtMojo;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mimic surefire to run GWTTestCases during integration-test phase, until SUREFIRE-508 is fixed
 *
 * @goal test
 * @phase integration-test
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @requiresDependencyResolution test
 * @version $Id$
 */
public class TestMojo
    extends AbstractGwtMojo
{

    /**
     * Set this to 'true' to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @parameter expression="${skipTests}"
     */
    private boolean skipTests;

    /**
     * DEPRECATED This old parameter is just like skipTests, but bound to the old property maven.test.skip.exec. Use
     * -DskipTests instead; it's shorter.
     *
     * @deprecated
     * @parameter expression="${maven.test.skip.exec}"
     */
    private boolean skipExec;

    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you enable it using
     * the "maven.test.skip" property, because maven.test.skip disables both running the tests and compiling the tests.
     * Consider using the skipTests parameter instead.
     *
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * output directory for code generated by GWT for tests
     * @parameter default-value="target/www-test"
     */
    private String out;

    /**
     * Time out for test execution in dedicated JVM
     * @parameter default-value="60"
     */
    private int timeOut;

    /**
     * Comma separated list of ant-style inclusion patterns for GWT integration tests. For example, can be set to
     * <code>**\/*GwtTest.java</code> to match all test class following this naming convention. Surefire plugin may then
     * ne configured to exclude such tests.
     * <p>
     * It is recommended to use a TestSuite to run GwtTests, as they require some huge setup and are very slow. Running
     * inside a suite allow to execute the setup only once. The default value is defined with this best practice in
     * mind.
     * 
     * @parameter default-value="**\/GwtTest*.java,**\/Gwt*Suite.java"
     */
    protected String includes;

    /**
     * Comma separated list of ant-style exclusion patterns for GWT integration tests
     * 
     * @parameter
     */
    protected String excludes;

    /**
     * Directory for test reports, defaults to surefire one to match the surefire-report plugin
     * @parameter default-value="${project.build.directory}/surefire-reports"
     */
    private File reportsDirectory;


    /** failures counter */
    private int failures;

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip || skipTests || skipExec )
        {
            return;
        }

        final List < String > classpath = new ArrayList < String > ();
        classpath.addAll( getProject().getCompileSourceRoots() );
        classpath.addAll( getProject().getTestCompileSourceRoots() );
        try
        {
            classpath.addAll( getProject().getTestClasspathElements() );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Failed to resolve dependencies", e );
        }

        final String jvm = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        getLog().debug( "Using JVM: " + jvm );

        new TestTemplate( getProject(), includes, excludes, new TestTemplate.CallBack()
        {
            public void doWithTest( File sourceDir, String test )
            throws MojoExecutionException
            {
                forkToRunTest( classpath, jvm, test );
            }
        } );

        if ( failures > 0 )
        {
            throw new MojoExecutionException( "There was test failures." );
        }
    }


    /**
     * @param classpath the test execution classpath
     * @param jvm the JVM process command
     * @param test the test to run
     * @throws MojoExecutionException some error occured
     */
    private void forkToRunTest( List < String > classpath, String jvm, String test )
        throws MojoExecutionException
    {
        classpath.add( getClassPathElementFor( TestMojo.class ) );
        classpath.add( getClassPathElementFor( ReporterManager.class ) );

        test = test.substring( 0, test.length() - 5 );
        test = StringUtils.replace( test, File.separator, "." );
        try
        {
            Commandline cli = new Commandline();
            cli.setExecutable( jvm );
            cli.createArg( false ).setLine( "-classpath" );
            cli.createArg( false ).setLine( StringUtils.join( classpath.iterator(), File.pathSeparator ) );
            cli.createArg( false ).setLine( " -Xmx256M " );
            cli.createArg( false ).setLine( " -Dsurefire.reports=\"" + reportsDirectory + "\"" );
            cli.createArg( false ).setLine( " -Dgwt.args=\"-out " + out + "\"" );
            new File( getProject().getBasedir(), out ).mkdirs();
            cli.createArg( false ).setLine( MavenTestRunner.class.getName() );
            cli.createArg( false ).setLine( test );

            getLog().debug( "execute : " + cli.toString() );
            StreamConsumer systemOut = new ForkingStreamConsumer( new StandardOutputConsumer() );
            StreamConsumer systemErr = new ForkingStreamConsumer( new StandardOutputConsumer() );
            if ( CommandLineUtils.executeCommandLine( cli, systemOut, systemErr, timeOut ) != 0 )
            {
                failures++;
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to run GWT tests", e );
        }
    }

    /**
     * @param clazz class to check for classpath resolution
     * @return The classpath element this class was loaded from
     */
    private String getClassPathElementFor( Class < ? > clazz )
    {
        String classFile = clazz.getName().replace( '.', '/' ) + ".class";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if ( cl == null )
        {
            cl = getClass().getClassLoader();
        }
        URL url = cl.getResource( classFile );
        String path = url.toString();
        if ( path.startsWith( "jar:" ) )
        {
            path = path.substring( 4, path.indexOf( "!" ) );
        }
        else
        {
            path = path.substring( 0, path.length() - classFile.length() );
        }
        if ( path.startsWith( "file:" ) )
        {
            path = path.substring( 5 );
        }
        return path;
    }

    /**
     * @return the project classloader
     * @throws DependencyResolutionRequiredException failed to resolve project dependencies
     * @throws MalformedURLException configuration issue ?
     */
    protected ClassLoader getProjectClassLoader()
        throws DependencyResolutionRequiredException, MalformedURLException
    {
        getLog().debug( "AbstractMojo#getProjectClassLoader()" );

        List<?> compile = project.getCompileClasspathElements();
        URL[] urls = new URL[compile.size()];
        int i = 0;
        for ( Object object : compile )
        {
            if ( object instanceof Artifact )
            {
                urls[i] = ( (Artifact) object ).getFile().toURI().toURL();
            }
            else
            {
                urls[i] = new File( (String) object ).toURI().toURL();
            }
            i++;
        }
        return new URLClassLoader( urls, ClassLoader.getSystemClassLoader() );
    }

    /**
     * @param path file to add to the project compile directories
     */
    protected void addCompileSourceRoot( File path )
    {
        project.addCompileSourceRoot( path.getAbsolutePath() );
    }

    /**
     * Add project classpath element to a classpath URL set
     *
     * @param originalUrls the initial URL set
     * @return full classpath URL set
     * @throws MojoExecutionException some error occured
     */
    protected URL[] addProjectClasspathElements( URL[] originalUrls )
        throws MojoExecutionException
    {
        Collection<?> sources = project.getCompileSourceRoots();
        Collection<?> resources = project.getResources();
        Collection<?> dependencies = project.getArtifacts();
        URL[] urls = new URL[originalUrls.length + sources.size() + resources.size() + dependencies.size() + 2];

        int i = originalUrls.length;
        getLog().debug( "add compile source roots to GWTCompiler classpath " + sources.size() );
        i = addClasspathElements( sources, urls, i );
        getLog().debug( "add resources to GWTCompiler classpath " + resources.size() );
        i = addClasspathElements( resources, urls, i );
        getLog().debug( "add project dependencies to GWTCompiler  classpath " + dependencies.size() );
        i = addClasspathElements( dependencies, urls, i );
        try
        {
            urls[i++] = generateDirectory.toURL();
            urls[i] = new File( project.getBuild().getOutputDirectory() ).toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Failed to convert project.build.outputDirectory to URL", e );
        }
        return urls;
    }

    /**
     * Need this to run both pre- and post- PLX-220 fix.
     *
     * @return a ClassLoader including plugin dependencies and project source foler
     * @throws MojoExecutionException failed to configure ClassLoader
     */
    protected ClassLoader getClassLoader( GwtRuntime runtime )
        throws MojoExecutionException
    {
        try
        {
            Collection<File> classpath = getClasspath( Artifact.SCOPE_COMPILE, runtime );
            URL[] urls = new URL[classpath.size()];
            int i = 0;
            for ( File file : classpath )
            {
                urls[i++] = file.toURL();
            }
            ClassLoader parent = getClass().getClassLoader();
            return new URLClassLoader( urls, parent.getParent() );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Failed to resolve project dependencies" );
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Unexpecetd internal error" );
        }
    }

}
