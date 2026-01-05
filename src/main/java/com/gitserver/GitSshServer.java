package com.gitserver;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.git.GitLocationResolver;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SSH Git Server implementation using Apache SSHD and JGit
 */
public class GitSshServer {
    private static final Logger logger = LoggerFactory.getLogger(GitSshServer.class);

    private final SshServer sshServer;
    private final int port;
    private final File repositoryDir;

    public GitSshServer(int port, File repositoryDir) {
        this.port = port;
        this.repositoryDir = repositoryDir;
        this.sshServer = SshServer.setUpDefaultServer();
        configureServer();
    }

    private void configureServer() {
        sshServer.setPort(port);

        // Set up host key provider
        File hostKeyFile = new File(repositoryDir.getParentFile() != null ? 
                                     repositoryDir.getParentFile() : new File("."), 
                                     "hostkey.ser");
        Path hostKeyPath = hostKeyFile.toPath();
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));

        // Configure authentication
        // ⚠️ SECURITY WARNING: This accepts ALL password authentication attempts without validation
        // This is ONLY suitable for demo/development purposes in isolated environments
        // For production use, you MUST implement:
        //   1. Proper user/password verification against a secure credential store
        //   2. SSH public key authentication
        //   3. Access control lists (ACLs) for repository permissions
        //   4. Rate limiting to prevent brute force attacks
        //   5. Audit logging for all authentication attempts
        sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                logger.info("Password authentication for user: {}", username);
                // Accept any username/password for demo purposes
                // TODO: Implement proper authentication for production use
                return true;
            }
        });

        // Also accept public key authentication
        sshServer.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);

        // Set up virtual file system
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(repositoryDir.toPath()));

        // Configure Git command factory with custom resolver
        GitPackCommandFactory gitCommandFactory = new GitPackCommandFactory();
        gitCommandFactory.withGitLocationResolver(new CustomGitLocationResolver(repositoryDir));
        sshServer.setCommandFactory(gitCommandFactory);

        logger.info("SSH Git Server configured on port {}", port);
    }

    public void start() throws IOException {
        sshServer.start();
        logger.info("SSH server started successfully on port {}", port);
    }

    public void stop() throws IOException {
        if (sshServer != null && sshServer.isOpen()) {
            sshServer.stop();
            logger.info("SSH server stopped");
        }
    }

    public int getPort() {
        return port;
    }

    /**
     * Custom Git location resolver that handles repository paths
     */
    private static class CustomGitLocationResolver implements GitLocationResolver {
        private final File baseDir;

        public CustomGitLocationResolver(File baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public Path resolveRootDirectory(String command, String[] args, ServerSession session, 
                                          java.nio.file.FileSystem fs) 
                throws IOException {
            
            if (args == null || args.length == 0) {
                throw new IOException("No repository path specified");
            }

            String repoPath = args[0];
            
            // Sanitize repository path
            if (repoPath.startsWith("/")) {
                repoPath = repoPath.substring(1);
            }
            if (repoPath.startsWith("'") && repoPath.endsWith("'")) {
                repoPath = repoPath.substring(1, repoPath.length() - 1);
            }
            if (!repoPath.endsWith(".git")) {
                repoPath = repoPath + ".git";
            }

            File repoDir = new File(baseDir, repoPath);
            
            // Create repository if it doesn't exist
            if (!repoDir.exists()) {
                logger.info("Creating new repository via SSH: {}", repoPath);
                repoDir.mkdirs();
                
                try {
                    FileRepositoryBuilder builder = new FileRepositoryBuilder();
                    Repository repo = builder.setGitDir(repoDir)
                            .setBare()
                            .build();
                    repo.create(true);
                    repo.close();
                    logger.info("Repository created via SSH: {}", repoDir.getAbsolutePath());
                } catch (IOException e) {
                    logger.error("Failed to create repository: {}", repoPath, e);
                    throw e;
                }
            }
            
            logger.info("Resolving Git repository: {} -> {}", repoPath, repoDir.getAbsolutePath());
            return repoDir.toPath();
        }
    }
}
