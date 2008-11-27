package org.codehaus.mojo.gwt;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

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

/**
 * Abstract Mojo for running GWT Shell and related tools
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public abstract class AbstractGwtRunnerMojo
    extends AbstractGwtMojo
{

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