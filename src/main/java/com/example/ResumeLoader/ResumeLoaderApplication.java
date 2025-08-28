package com.example.ResumeLoader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@SpringBootApplication
public class ResumeLoaderApplication implements CommandLineRunner {

	private static final String LOCAL_REPO_PATH = "D:/Projects/My_Portfolio";
	private static final String RESUME_FILE = "docs/NinadShingare_resume.pdf";
	private static final String BRANCH = "docker_run";
	private static final String SSH_KEY_PATH = "C:/Users/svshi/.ssh/id_rsa"; // your private key

	public static void main(String[] args) {
		SpringApplication.run(ResumeLoaderApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		setupSshKey();
		watchResumeFile();
	}

	private void setupSshKey() {
		SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
			@Override
			protected void configure(Host host, Session session) {
				session.setConfig("StrictHostKeyChecking", "no");
			}

			@Override
			protected JSch createDefaultJSch(FS fs) throws JSchException {
				JSch jsch = super.createDefaultJSch(fs);
				jsch.addIdentity(SSH_KEY_PATH);
				return jsch;
			}
		};
		SshSessionFactory.setInstance(sshSessionFactory);
	}

	private void watchResumeFile() throws IOException, InterruptedException {
		Path path = Paths.get(LOCAL_REPO_PATH + "/docs");
		WatchService watchService = FileSystems.getDefault().newWatchService();
		path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

		System.out.println("👀 Watching for resume changes in: " + path);

		while (true) {
			WatchKey key = watchService.take();
			for (WatchEvent<?> event : key.pollEvents()) {
				if (event.context().toString().equals("NinadShingare_resume.pdf")) {
					System.out.println("📄 Resume updated locally!");
					uploadToGitHub();
				}
			}
			key.reset();
		}
	}

	private void uploadToGitHub() {
		try (Git git = Git.open(new File(LOCAL_REPO_PATH))) {

			// Check for changes
			Status status = git.status().call();
			if (!status.hasUncommittedChanges()) {
				System.out.println("ℹ️ No changes detected. Skipping commit and push.");
				return;
			}

			// Add file
			git.add().addFilepattern(RESUME_FILE).call();
			System.out.println("✅ Added updated resume to staging.");

			// Commit changes
			git.commit().setMessage("Updated resume").call();
			System.out.println("✅ Commit created.");

			// Push over SSH
			git.push()
					.setRemote("origin")
					.setRefSpecs(new RefSpec(BRANCH + ":" + BRANCH))
					.call();

			System.out.println("🚀 Resume uploaded successfully to branch: " + BRANCH);

		} catch (IOException | GitAPIException e) {
			System.err.println("❌ Failed to upload resume to GitHub: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
