package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.lib.Utils.implode;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.normalize;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.prepend;

abstract class NodeTaskExecutor {
    private final Logger logger;
    private final String taskName;
    private final String taskLocation;
    private final Platform platform;
    private final File workingDirectory;
    private final List<String> additionalArguments;

    public NodeTaskExecutor(String taskName, String taskLocation, File workingDirectory, Platform platform, List<String> additionalArguments) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.taskName = taskName;
        this.taskLocation = taskLocation;
        this.platform = platform;
        this.workingDirectory = workingDirectory;
        this.additionalArguments = additionalArguments;
    }

    public NodeTaskExecutor(String taskName, String taskLocation, File workingDirectory, Platform platform, List<String> additionalArguments, ProxyConfig proxyConfig) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.taskName = taskName;
        this.taskLocation = taskLocation;
        this.platform = platform;
        this.workingDirectory = workingDirectory;
        
        if (proxyConfig != null) {
        	List<String> newArguments = new ArrayList<String>(additionalArguments);
        	logger.info("using proxy config with host " + proxyConfig.host);
        	String proxyString = "--https-proxy=";
        	if (proxyConfig.protocol != null) {
        		proxyString += proxyConfig.protocol +"://";
        	}
        	if (proxyConfig.username != null && proxyConfig.password != null) {
        		proxyString += proxyConfig.username + ":" + proxyConfig.password + "@";
        	}
        	proxyString += proxyConfig.host;
        	if (proxyConfig.port != 0) {
        		proxyString += ":" + proxyConfig.port;
        	}
        	newArguments.add(proxyString);
        	this.additionalArguments = newArguments;
        } else {
        	this.additionalArguments = additionalArguments;
        	logger.info("using npm without proxy");
        }
    }
    
    public final void execute(String args) throws TaskRunnerException {
        final String absoluteTaskLocation = workingDirectory + normalize(taskLocation);
        final List<String> arguments = getArguments(args);
        logger.info("Running " + taskToString(taskName, arguments) + " in " + workingDirectory);

        try {
            final int result = new NodeExecutor(workingDirectory, prepend(absoluteTaskLocation, arguments), platform).executeAndRedirectOutput(logger);
            if(result != 0){
                throw new TaskRunnerException(taskToString(taskName, arguments) + " failed. (error code "+result+")");
            }
        } catch (ProcessExecutionException e) {
            throw new TaskRunnerException(taskToString(taskName, arguments) + " failed.");
        }
    }

    private List<String> getArguments(String args) {
        List<String> arguments =  new ArrayList<String>();
        if(args != null && !args.equals("null") && !args.isEmpty()) {
            arguments.addAll(Arrays.asList(args.split("\\s+")));
        }

        for(String argument: additionalArguments){
            if(!arguments.contains(argument)){
                arguments.add(argument);
            }
        }
        return arguments;
    }

    private static String taskToString(String taskName, List<String> commands) {
        return "'" + taskName + " " + implode(" ",commands) + "'";
    }
}
