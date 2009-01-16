package org.codehaus.mojo.gwt;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public abstract class AbstractGwtModuleMojo
    extends AbstractGwtMojo
{

    /**
     *
     */
    private static final String GWT_MODULE_EXTENSION = ".gwt.xml";

    /**
     * The project GWT modules. If not set, the plugin will scan the project for <code>.gwt.xml</code> files.
     * 
     * @parameter
     * @alias compileTargets
     */
    private String[] modules;

    /**
     *
     */
    public AbstractGwtModuleMojo()
    {
        super();
    }

    /**
     * @param module the module to set
     */
    public void setModule( String module )
    {
        // Note : Plexus will use this setter to inject dependency. The 'module' attribute is unused
        this.modules = new String[] { module };
    }

    /**
     * Return the configured modules or scan the project source/resources folder to find them
     *
     * @return the modules
     */
    @SuppressWarnings( "unchecked" )
    public String[] getModules()
    {
        List < String > mods = new ArrayList < String > ();
        if ( modules == null )
        {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( project.getBuild().getSourceDirectory() );
            scanner.setIncludes( new String[] { "**/*" + GWT_MODULE_EXTENSION } );
            scanner.scan();
            mods.addAll( Arrays.asList( scanner.getIncludedFiles() ) );

            Collection < Resource > resources = ( Collection < Resource > ) project.getResources();
            for ( Resource resource : resources )
            {
                File resourceDirectoryFile = new File( resource.getDirectory() );
                if ( !resourceDirectoryFile.exists() )
                {
                    continue;
                }
                scanner = new DirectoryScanner();
                scanner.setBasedir( resource.getDirectory() );
                scanner.setIncludes( new String[] { "**/*" + GWT_MODULE_EXTENSION } );
                scanner.scan();
                mods.addAll( Arrays.asList( scanner.getIncludedFiles() ) );
            }

            if ( mods.isEmpty() )
            {
                getLog().warn( "GWT plugin is configured to detect modules, but none where found." );
            }

            modules = new String[mods.size()];
            int i = 0;
            for ( String fileName : mods )
            {
                String path = fileName.substring( 0, fileName.length() - GWT_MODULE_EXTENSION.length() );
                modules[i++] = path.replace( File.separatorChar, '.' );
            }
        }
        return modules;
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

}