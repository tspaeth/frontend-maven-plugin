package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import static com.github.eirslett.maven.plugins.frontend.mojo.MojoUtils.setSLF4jLogger;

@Mojo(name="npm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * The password
     * @parameter expression="${password}"
     */
    @Parameter(property = "password", readonly = true, defaultValue = "${password}")
    private String password;

    /**
     * Plexus component for the SecDispatcher
     * @component roleHint="mng-4384"
     */
    private SecDispatcher secDispatcher;

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "arguments", required = false)
    private String arguments;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;


    private String decrypt(String input) {
        try {
            return secDispatcher.decrypt(input);
        } catch (SecDispatcherException sde) {
            getLog().warn(sde.getMessage());
            return input;
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            setSLF4jLogger(getLog());
            String s = decrypt(password);
            getLog().info("The password is " + s);

            ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session);
            new FrontendPluginFactory(workingDirectory, proxyConfig).getNpmRunner()
                    .execute(arguments);
        } catch (TaskRunnerException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }
}
