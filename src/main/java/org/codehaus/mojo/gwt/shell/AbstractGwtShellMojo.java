package org.codehaus.mojo.gwt.shell;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.scripting.GwtShellScriptConfiguration;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriterFactory;

/**
 * Abstract Mojo for GWT-Maven.
 *
 * @author ccollins
 * @author cooper
 * @author willpugh
 */
public abstract class AbstractGwtShellMojo
    extends AbstractGwtModuleMojo
    implements GwtShellScriptConfiguration
{

    /**
     * @component
     */
    protected ScriptWriterFactory scriptWriterFactory;

    /**
     * @component
     */
    protected ClasspathBuilder buildClasspathUtil;


    // GWT-Maven properties

    /**
     * Location on filesystem where project should be built.
     *
     * @parameter expression="${project.build.directory}"
     */
    private File buildDir;

    /**
     * Location on filesystem where GWT will write output files (-out option to GWTCompiler).
     *
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     */
    private File output;

    /**
     * Location on filesystem where GWT will write generated content for review (-gen option to GWTCompiler).
     *
     * @parameter expression="${project.build.directory}/.generated"
     */
    private File gen;

    /**
     * GWT logging level (-logLevel ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL).
     *
     * @parameter default-value="INFO"
     */
    private String logLevel;

    /**
     * GWT JavaScript compiler output style (-style OBF[USCATED], PRETTY, or DETAILED).
     *
     * @parameter default-value="OBF"
     */
    private String style;

    /**
     * Prevents the embedded GWT Tomcat server from running (even if a port is specified).
     *
     * @parameter default-value="false"
     */
    private boolean noServer;

    /**
     * Extra JVM arguments that are passed to the GWT-Maven generated scripts (for compiler, shell, etc - typically use
     * -Xmx512m here, or -XstartOnFirstThread, etc).
     * 
     * @parameter expression="${google.webtoolkit.extrajvmargs}"
     */
    private String extraJvmArgs;

    /**
     * Whether or not to add compile source root to classpath.
     *
     * @parameter default-value="true"
     */
    protected boolean sourcesOnPath;

    /**
     * Whether or not to add resources root to classpath.
     *
     * @parameter default-value="true"
     */
    protected boolean resourcesOnPath;

    /**
     * Whether or not to enable assertions in generated scripts (-ea).
     *
     * @parameter default-value="false"
     */
    private boolean enableAssertions;

    /**
     * Specifies the mapping URL to be used with the shell servlet.
     *
     * @parameter default-value="/*"
     */
    private String shellServletMappingURL;

    // methods

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        GwtRuntime runtime = getGwtRuntime();
        doExecute( runtime );
    }

    protected abstract void doExecute( GwtRuntime runtime )
        throws MojoExecutionException, MojoFailureException;

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getBuildDir()
     */
    public File getBuildDir()
    {
        return this.buildDir;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getCompileTarget()
     */
    public String[] getCompileTarget()
    {
        return getModules();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getExtraJvmArgs()
     */
    public String getExtraJvmArgs()
    {
        return this.extraJvmArgs;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGen()
     */
    public File getGen()
    {
        return this.gen;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getLogLevel()
     */
    public String getLogLevel()
    {
        return this.logLevel;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isNoServer()
     */
    public boolean isNoServer()
    {
        return this.noServer;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getOutput()
     */
    public File getOutput()
    {
        return this.output;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getStyle()
     */
    public String getStyle()
    {
        return this.style;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getShellServletMappingURL()
     */
    public String getShellServletMappingURL()
    {
        return this.shellServletMappingURL;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getSourcesOnPath()
     */
    public boolean getSourcesOnPath()
    {
        return this.sourcesOnPath;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getResourcesOnPath()
     */
    public boolean getResourcesOnPath()
    {
        return resourcesOnPath;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isEnableAssertions()
     */
    public boolean isEnableAssertions()
    {
        return this.enableAssertions;
    }


}
