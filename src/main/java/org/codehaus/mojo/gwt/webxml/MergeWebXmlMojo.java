package org.codehaus.mojo.gwt.webxml;

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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.AbstractGwtWebMojo;

/**
 * Merges GWT servlet elements into deployment descriptor (and non GWT servlets into shell).
 *
 * @goal mergewebxml
 * @phase process-resources
 * @requiresDependencyResolution compile
 * @description Merges GWT servlet elements into deployment descriptor (and non GWT servlets into shell).
 * @author cooper
 * @version $Id$
 */
public class MergeWebXmlMojo
    extends AbstractGwtWebMojo
{

    private Set<String> checkedModules = new HashSet<String>();

    /**
     * Location on filesystem where merged web.xml will be created. The maven-war-plugin must be configured to use this
     * path as <a href="http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html#webXml"> webXml</a> parameter
     * 
     * @parameter default-value="${project.build.directory}/web.xml"
     */
    private File mergedWebXml;

    /** Creates a new instance of MergeWebXmlMojo */
    public MergeWebXmlMojo()
    {
        super();
    }

    public void doExecute( GwtRuntime runtime )
        throws MojoExecutionException, MojoFailureException
    {

        try
        {
            if ( !mergedWebXml.exists() )
            {
                mergedWebXml.getParentFile().mkdirs();
                mergedWebXml.createNewFile();
            }

            FileUtils.copyFile( getWebXml(), mergedWebXml );

            Set<ServletDescriptor> servlets = new LinkedHashSet<ServletDescriptor>();
            for ( String module : getModules() )
            {
                GwtModule gwtModule = readModule( module );
                Map<String, String> moduleServlets = gwtModule.getServlets();
                getLog().debug( "merge " + moduleServlets.size() + " servlets from module " + module );
                for ( Map.Entry<String, String> servlet : moduleServlets.entrySet() )
                {
                    servlets.add( new ServletDescriptor( servlet.getKey(), servlet.getValue() ) );
                }
            }
            new GwtWebInfProcessor().process( mergedWebXml, mergedWebXml, servlets );
            getLog().info( servlets.size() + " servlet(s) merged into " + mergedWebXml );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to merge web.xml", e );
        }
    }

    /**
     * Helper hack for classpath problems, used as a fallback.
     *
     * @param runtime TODO
     *
     * @return
     */
    protected ClassLoader fixThreadClasspath( GwtRuntime runtime )
    {
        try
        {
            ClassWorld world = new ClassWorld();

            // use the existing ContextClassLoader in a realm of the classloading space
            ClassRealm root = world.newRealm( "gwt-plugin", Thread.currentThread().getContextClassLoader() );
            ClassRealm realm = root.createChildRealm( "gwt-project" );

            Collection classpath = classpathBuilder.buildClasspathList( getProject(), Artifact.SCOPE_COMPILE,
                                                                          runtime, getProjectArtifacts() );
            for ( Iterator it = classpath.iterator(); it.hasNext(); )
            {
                realm.addConstituent( ( (File) it.next() ).toURI().toURL() );
            }

            Thread.currentThread().setContextClassLoader( realm.getClassLoader() );
            // /System.out.println("AbstractGwtMojo realm classloader = " + realm.getClassLoader().toString());

            return realm.getClassLoader();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }

}
