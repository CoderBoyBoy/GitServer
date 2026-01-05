package com.gitserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Main application class for Git Server
 * Supports both HTTP and SSH protocols using JGit
 */
public class GitServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(GitServerApplication.class);

    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_SSH_PORT = 2222;
    private static final String DEFAULT_REPO_DIR = "repositories";

    private final GitHttpServer httpServer;
    private final GitSshServer sshServer;

    public GitServerApplication(int httpPort, int sshPort, String repositoryDir) throws IOException {
        File repoDir = new File(repositoryDir);
        if (!repoDir.exists()) {
            repoDir.mkdirs();
            logger.info("Created repository directory: {}", repoDir.getAbsolutePath());
        }

        this.httpServer = new GitHttpServer(httpPort, repoDir);
        this.sshServer = new GitSshServer(sshPort, repoDir);
    }

    public void start() throws Exception {
        logger.info("Starting Git Server...");
        
        // Start HTTP server
        httpServer.start();
        logger.info("HTTP Git Server started on port {}", httpServer.getPort());
        
        // Start SSH server
        sshServer.start();
        logger.info("SSH Git Server started on port {}", sshServer.getPort());
        
        logger.info("Git Server is ready!");
        logger.info("HTTP URL: http://localhost:{}/", httpServer.getPort());
        logger.info("SSH URL: ssh://git@localhost:{}/", sshServer.getPort());
    }

    public void stop() throws Exception {
        logger.info("Stopping Git Server...");
        
        if (httpServer != null) {
            httpServer.stop();
            logger.info("HTTP server stopped");
        }
        
        if (sshServer != null) {
            sshServer.stop();
            logger.info("SSH server stopped");
        }
        
        logger.info("Git Server stopped");
    }

    public static void main(String[] args) {
        int httpPort = DEFAULT_HTTP_PORT;
        int sshPort = DEFAULT_SSH_PORT;
        String repoDir = DEFAULT_REPO_DIR;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--http-port":
                    if (i + 1 < args.length) {
                        httpPort = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--ssh-port":
                    if (i + 1 < args.length) {
                        sshPort = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--repo-dir":
                    if (i + 1 < args.length) {
                        repoDir = args[++i];
                    }
                    break;
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
            }
        }

        try {
            GitServerApplication app = new GitServerApplication(httpPort, sshPort, repoDir);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    app.stop();
                } catch (Exception e) {
                    logger.error("Error stopping server", e);
                }
            }));
            
            app.start();
            
            // Keep the application running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start Git Server", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Git Server - JGit-based Git server with HTTP and SSH support");
        System.out.println();
        System.out.println("Usage: java -jar gitserver.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --http-port <port>    HTTP server port (default: 8080)");
        System.out.println("  --ssh-port <port>     SSH server port (default: 2222)");
        System.out.println("  --repo-dir <path>     Repository directory (default: repositories)");
        System.out.println("  --help                Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar gitserver.jar");
        System.out.println("  java -jar gitserver.jar --http-port 9090 --ssh-port 2223");
        System.out.println("  java -jar gitserver.jar --repo-dir /var/git/repos");
    }
}
