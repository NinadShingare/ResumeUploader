package com.example.ResumeLoader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@SpringBootApplication
public class ResumeLoaderApplication implements CommandLineRunner {
	private static final String LOCAL_REPO_PATH = "D:/Projects/My_Portfolio"; // repo path
	private static final String RESUME_PATH = LOCAL_REPO_PATH + "/docs/NinadShingare_resume.pdf";
	private static final String GITHUB_REMOTE = "git@github.com:NinadShingare/My_Portfolio.git"; // SSH remote
	private static final String BRANCH = "docker_run"; // your target branch

	public static void main(String[] args) {
		SpringApplication.run(ResumeLoaderApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		watchResumeFile();
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
		try {
			System.out.println("📂 Opening repo at: " + LOCAL_REPO_PATH);

			Git git = Git.open(new File(LOCAL_REPO_PATH));

			// Add the updated resume
			git.add().addFilepattern("docs/NinadShingare_resume.pdf").call();
			System.out.println("✅ Added updated resume to staging.");

			// Commit the changes
			git.commit().setMessage("Updated resume").call();
			System.out.println("✅ Commit created.");

			// Push to docker_run branch over SSH
			git.push()
					.setRemote("origin")
					.setRefSpecs(new RefSpec(BRANCH + ":" + BRANCH))
					.call();

			System.out.println("🚀 Resume uploaded successfully to: " + GITHUB_REMOTE + " on branch: " + BRANCH);

		} catch (IOException | GitAPIException e) {
			System.err.println("❌ Failed to upload resume to GitHub: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
