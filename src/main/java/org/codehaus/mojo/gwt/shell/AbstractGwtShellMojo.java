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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineTimeOutException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.shell.Shell;

/**
 * Abstract Mojo for GWT-Maven.
 *
 * @author ccollins
 * @author cooper
 * @author willpugh
 * @version $Id$
 */
public abstract class AbstractGwtShellMojo
    extends AbstractGwtModuleMojo
{
    /**
     * Location on filesystem where GWT will write generated content for review (-gen option to GWTCompiler).
     * <p>
     * Can be set from command line using '-Dgwt.gen=...'
     *
     * @parameter default-value="${project.build.directory}/.generated" expression="${gwt.gen}"
     */
    private File gen;

    /**
     * GWT logging level (-logLevel ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL).
     * <p>
     * Can be set from command line using '-Dgwt.logLevel=...'
     *
     * @parameter default-value="INFO" expression="${gwt.logLevel}"
     */
    private String logLevel;

    /**
     * GWT JavaScript compiler output style (-style OBF[USCATED], PRETTY, or DETAILED).
     * <p>
     * Can be set from command line using '-Dgwt.style=...'
     *
     * @parameter default-value="OBF" expression="${gwt.style}"
     */
    private String style;

    /**
     * Extra JVM arguments that are passed to the GWT-Maven generated scripts (for compiler, shell, etc - typically use
     * -Xmx512m here, or -XstartOnFirstThread, etc).
     * <p>
     * Can be set from command line using '-Dgwt.extraJvmArgs=...', defaults to setting max Heap size to be large enough
     * for most GWT use cases.
     *
     * @parameter expression="${gwt.extraJvmArgs}" default-value="-Xmx512m"
     */
    private String extraJvmArgs;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking scripts. For the default, the
     * jvm will be the same as the one used to run Maven.
     *
     * @parameter expression="${gwt.jvm}"
     * @since 1.1
     */
    private String jvm;

    /**
     * Forked process execution timeOut. Usefull to avoid maven to hang in continuous integration server.
     *
     * @parameter
     */
    private int timeOut;

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

    public String getExtraJvmArgs()
    {
        String extra = extraJvmArgs;
        try {
            if ( Os.isFamily( Os.FAMILY_MAC ) && !extraJvmArgs.contains( "-XstartOnFirstThread" ) && !getGwtRuntime().getVersion().supportOOPHM())
            {
                getLog().debug("Adding -XstartOnFirstThread because of version: " + getGwtRuntime().getVersion() + " and os:" + Os.FAMILY_MAC );
                extra += " -XstartOnFirstThread";
            }
        }
        catch (MojoExecutionException ex){
            throw new RuntimeException(ex);
        }
        return extra;
    }

    public File getGen()
    {
        return this.gen;
    }

    public String getLogLevel()
    {
        return this.logLevel;
    }

    public String getStyle()
    {
        return this.style;
    }


    /**
     * A plexus-util StreamConsumer to redirect messages to plugin log
     */
    protected StreamConsumer out = new StreamConsumer()
    {
        public void consumeLine( String line )
        {
            getLog().info( line );
        }
    };

    /**
     * A plexus-util StreamConsumer to redirect errors to plugin log
     */
    protected StreamConsumer err = new StreamConsumer()
    {
        public void consumeLine( String line )
        {
            getLog().error( line );
        }
    };

    public String getJvm()
    {
        return jvm;
    }

    public void setJvm( String jvm )
    {
        this.jvm = jvm;
    }

    /**
     * hook to post-process the dependency-based classpath
     */
    protected void postProcessClassPath( Collection<File> classpath )
    {
        // Nothing to do in most case
    }

    private List<String> getJvmArgs()
    {
        List<String> extra = new ArrayList<String>();
        String userExtraJvmArgs = getExtraJvmArgs();
        if ( userExtraJvmArgs != null )
        {
            for ( String extraArg : userExtraJvmArgs.split( " " ) )
            {
                extra.add( extraArg );
            }
        }
        try {
            if ( Os.isFamily( Os.FAMILY_MAC ) && !extraJvmArgs.contains( "-XstartOnFirstThread" ) && !getGwtRuntime().getVersion().supportOOPHM())
            {
                getLog().debug("Adding -XstartOnFirstThread because of version: " + getGwtRuntime().getVersion() + " and os:" + Os.FAMILY_MAC );
                extra.add( "-XstartOnFirstThread" );
            }
        }
        catch (MojoExecutionException ex){
            throw new RuntimeException(ex);
        }
        return extra;
    }

    private String getJavaCommand()
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( jvm ) )
        {
            // use the same JVM as the one used to run Maven (the "java.home" one)
            jvm = System.getProperty( "java.home" );
        }

        // does-it exists ? is-it a directory or a path to a java executable ?
        File jvmFile = new File( jvm );
        if ( !jvmFile.exists() )
        {
            throw new MojoExecutionException( "the configured jvm " + jvm
                + " doesn't exists please check your environnement" );
        }
        if ( jvmFile.isDirectory() )
        {
            // it's a directory we construct the path to the java executable
            return jvmFile.getAbsolutePath() + File.separator + "bin" + File.separator + "java";
        }
        return jvm;
    }

    protected String quote( String arg )
    {
        if ( arg.startsWith( "'" ) || arg.startsWith( "\"" ) )
        {
            // Allready quoted
            return arg;
        }
        return '"' + arg + '"';
    }

    /**
     * plexus-util hack to run a command WITHOUT a shell
     *
     * @see PLXUTILS-107
     */
    private class JavaShell
        extends Shell
    {
        protected List<String> getRawCommandLine( String executable, String[] arguments )
        {
            List<String> commandLine = new ArrayList<String>();
            if ( executable != null )
            {
                commandLine.add( executable );
            }
            for ( String arg : arguments )
            {
                commandLine.add( arg );
            }
            return commandLine;
        }
    }

    /**
     * @param timeOut the timeOut to set
     */
    public void setTimeOut( int timeOut )
    {
        this.timeOut = timeOut;
    };

    /**
     * Create a command to execute using builder pattern
     *
     * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
     */
    public class JavaCommand
    {
        private String className;

        private GwtRuntime runtime;

        private List<File> classpath = new LinkedList<File>();

        private List<String> args = new ArrayList<String>();

        private Properties systemProperties = new Properties();

        private Properties env = new Properties();

        public JavaCommand( String className, GwtRuntime runtime )
        {
            this.className = className;
            this.runtime = runtime;
        }

        public JavaCommand withinScope( String scope )
            throws MojoExecutionException
        {
            classpath.addAll( classpathBuilder.buildClasspathList( getProject(), scope, runtime, getProjectArtifacts() ) );
            postProcessClassPath( classpath );
            return this;
        }

        public JavaCommand withinClasspath( File... path )
        {
            for ( File file : path )
            {
                classpath.add( file );
            }
            return this;
        }

        public JavaCommand arg( String arg )
        {
            args.add( arg );
            return this;
        }

        public JavaCommand arg( boolean condition, String arg )
        {
            if ( condition )
            {
                args.add( arg );
            }
            return this;
        }

        public JavaCommand systemProperty( String name, String value )
        {
            systemProperties.setProperty( name, value );
            return this;
        }

        public JavaCommand environment( String name, String value )
        {
            env.setProperty( name, value );
            return this;
        }

        public void execute()
            throws MojoExecutionException
        {
            List<String> command = new ArrayList<String>();
            command.addAll( getJvmArgs() );
            command.add( "-classpath" );
            List<String> path = new ArrayList<String>( classpath.size() );
            for ( File file : classpath )
            {
                path.add( quote( file.getAbsolutePath() ) );
            }
            command.add( StringUtils.join( path.iterator(), File.pathSeparator ) );
            if ( systemProperties != null )
            {
                for ( Map.Entry entry : systemProperties.entrySet() )
                {
                    command.add( "-D" + entry.getKey() + "=" + entry.getValue() );
                }
            }
            command.add( className );
            command.addAll( args );

            try
            {
                String[] arguments = (String[]) command.toArray( new String[command.size()] );

                // On windows, the default Shell will fall into command line length limitation issue
                // On Unixes, not using a Shell breaks the classpath (NoClassDefFoundError:
                // com/google/gwt/dev/Compiler).
                Commandline cmd =
                    Os.isFamily( Os.FAMILY_WINDOWS ) ? new Commandline( new JavaShell() ) : new Commandline();

                cmd.setExecutable( getJavaCommand() );
                cmd.addArguments( arguments );
                if ( env != null )
                {
                    for ( Map.Entry entry : env.entrySet() )
                    {
                        cmd.addEnvironment( (String) entry.getKey(), (String) entry.getValue() );
                    }
                }
                getLog().debug( "Execute command :\n" + cmd.toString() );
                int status;
                if ( timeOut > 0 )
                {
                    status = CommandLineUtils.executeCommandLine( cmd, out, err, timeOut );
                }
                else
                {
                    status = CommandLineUtils.executeCommandLine( cmd, out, err );
                }

                if ( status != 0 )
                {
                    throw new ForkedProcessExecutionException( "Command [[\n" + cmd.toString()
                        + "\n]] failed with status " + status );
                }
            }
            catch ( CommandLineTimeOutException e )
            {
                if ( timeOut > 0 )
                {
                    getLog().warn( "Forked JVM has been killed on time-out after " + timeOut + " seconds" );
                    return;
                }
                throw new MojoExecutionException( "Time-out on command line execution :\n" + command );
            }
            catch ( CommandLineException e )
            {
                throw new MojoExecutionException( "Failed to execute command line :\n" + command );
            }
        }

        public void withinClasspathFirst( File oophmJar )
        {
            classpath.add( 0, oophmJar );
        }
    }

}
