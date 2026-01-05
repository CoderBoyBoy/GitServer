package com.gitserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

/**
 * HTTP Git Server implementation using JGit and Jetty
 */
public class GitHttpServer {
    private static final Logger logger = LoggerFactory.getLogger(GitHttpServer.class);

    private final Server server;
    private final int port;
    private final File repositoryDir;

    public GitHttpServer(int port, File repositoryDir) {
        this.port = port;
        this.repositoryDir = repositoryDir;
        this.server = new Server(port);
        configureServer();
    }

    private void configureServer() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Create and configure GitServlet
        GitServlet gitServlet = new GitServlet();
        gitServlet.setRepositoryResolver(new CustomRepositoryResolver(repositoryDir));

        ServletHolder holder = new ServletHolder(gitServlet);
        context.addServlet(holder, "/*");

        logger.info("HTTP Git Server configured on port {}", port);
    }

    public void start() throws Exception {
        server.start();
        logger.info("HTTP server started successfully on port {}", port);
    }

    public void stop() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
            logger.info("HTTP server stopped");
        }
    }

    public int getPort() {
        return port;
    }

    /**
     * Custom repository resolver that handles repository creation and access
     */
    private static class CustomRepositoryResolver implements RepositoryResolver<HttpServletRequest> {
        private final File baseDir;

        public CustomRepositoryResolver(File baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public Repository open(HttpServletRequest req, String name) 
                throws RepositoryNotFoundException, ServiceNotAuthorizedException, 
                       ServiceNotEnabledException {
            
            // Sanitize repository name
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            if (!name.endsWith(".git")) {
                name = name + ".git";
            }

            File repoDir = new File(baseDir, name);
            
            try {
                // If repository doesn't exist, create it
                if (!repoDir.exists()) {
                    logger.info("Creating new repository: {}", name);
                    repoDir.mkdirs();
                    
                    FileRepositoryBuilder builder = new FileRepositoryBuilder();
                    Repository repo = builder.setGitDir(repoDir)
                            .setBare()
                            .build();
                    repo.create(true);
                    logger.info("Repository created: {}", repoDir.getAbsolutePath());
                    return repo;
                }
                
                // Open existing repository
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                Repository repo = builder.setGitDir(repoDir)
                        .setMustExist(true)
                        .build();
                
                logger.info("Opened repository: {}", repoDir.getAbsolutePath());
                return repo;
                
            } catch (IOException e) {
                logger.error("Failed to open/create repository: {}", name, e);
                throw new RepositoryNotFoundException(name, e);
            }
        }
    }

    /**
     * Exception thrown when repository is not found
     */
    public static class RepositoryNotFoundException extends ServiceNotEnabledException {
        public RepositoryNotFoundException(String name, Throwable cause) {
            super("Repository not found: " + name, cause);
        }
    }
}
