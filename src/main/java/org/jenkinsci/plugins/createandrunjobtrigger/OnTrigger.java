package org.jenkinsci.plugins.createandrunjobtrigger;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.reactor.ReactorException;

import com.cloudbees.hudson.plugins.folder.Folder;

import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.TopLevelItem;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.browser.GitWeb;
import hudson.plugins.git.extensions.GitSCMExtension;
import jenkins.model.Jenkins;

public class OnTrigger implements Runnable {
	static final Logger log =  Logger.getLogger(OnTrigger.class.getName());
	final String jobName;
	private final String gitUrl;
	private final String organisation;
	private final String repo;

	public OnTrigger(String gitUrl) {
		this.gitUrl = gitUrl;
		String[] tokens = gitUrl.split("/");
		if(tokens.length < 3) throw new IllegalArgumentException(gitUrl + " invalid");
		organisation = tokens[tokens.length-2];
		repo = stripGit(tokens[tokens.length-1]);
                jobName = repo + "-master";
                
	}

	String stripGit(String repo) {
		return repo.endsWith(".git") ?  repo.substring(0, repo.lastIndexOf(".git")) : repo ;
	}

	@Override
	public void run() {
		try {
			Job<?,?> job = getOrCreateJob();
			if(!job.isBuilding()) {
	            jenkins().getQueue().schedule((Queue.Task) job, 2);
	        }
		} catch (IllegalArgumentException | IOException e) {
			log.log(Level.SEVERE, "Could not run "+this, e);
		}
	}

	Job<?,?> getOrCreateJob() throws IllegalArgumentException, IOException {
		Folder repoFolder =  getOrCreateFolder(getOrCreateFolder(jenkins(), organisation), repo);
		TopLevelItem jobItem = repoFolder.getItem(jobName);
		return jobItem == null ? createJob(repoFolder) : assignBuildNumber(asJob(jobItem));
	}

	private Job<?,?> assignBuildNumber(Job<?, ?> job) throws IOException {
		Path buildsPath = jenkins().getRootDir().toPath().resolve("jobs").resolve(organisation).resolve("jobs").resolve(repo).resolve("jobs").resolve(jobName).resolve("builds");
		while(buildsPath.resolve(""+job.getNextBuildNumber()).toFile().exists()) {
			int newBuildNumber = job.assignBuildNumber();
			log.log(Level.FINE, format("assigned nr %s to build %s", newBuildNumber, job.getName()));
		}
		return job;
	}

	private WorkflowJob createJob(Folder repoFolder) throws IOException {
		
		WorkflowJob job = new WorkflowJob(repoFolder, jobName);

        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        String credId = Credentials.generateId("git", gitUrl);
        remoteConfigs.add(new UserRemoteConfig(gitUrl, "", "", credId));

        List<GitSCMExtension> extensions = new ArrayList<GitSCMExtension>();
        extensions.add(new hudson.plugins.git.extensions.impl.CleanBeforeCheckout());
        
        final int timeoutMinutes = 10;
		extensions.add(new hudson.plugins.git.extensions.impl.CloneOption(true, "", timeoutMinutes)); 

        GitSCM git = new GitSCM(remoteConfigs, null, false, null, new GitWeb(gitUrl), "", extensions);
        CpsScmFlowDefinition flowDefinition = new CpsScmFlowDefinition(git, "Jenkinsfile");
        job.setDefinition(flowDefinition);
        job.save();

        reloadConfig();
        
        return job;
	}

	private void reloadConfig() throws IOException {
		try {
            jenkins().reload();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ReactorException e) {
            e.printStackTrace();
        }
	}


	private Folder getOrCreateFolder(Jenkins jenkins, String subFolder) throws IllegalArgumentException, IOException {
		TopLevelItem item = jenkins.getItem(subFolder);
		if(item == null) {
			log.info(format("creating folder %s in %s", subFolder, "jenkins"));
			item = new Folder(jenkins, subFolder);
			jenkins.add(item, subFolder).save();
		}
		return asFolder(item);
	}
	
	private Folder getOrCreateFolder(Folder folder, String subFolder) throws IllegalArgumentException, IOException {
		TopLevelItem item = folder.getItem(subFolder);
		if(item == null) {
			log.info(format("creating folder %s in %s", subFolder, folder.getName()));
			item = new Folder(folder, subFolder);
			folder.add(item, subFolder).save();
		}
		return asFolder(item);
	}

	private Folder asFolder(Item item) {
		if (item  instanceof Folder) {
			return  (Folder) item;
		} else {
			throw new IllegalArgumentException(format("expected %s to be a folder but was %s", item.getName(), item));
		}
	}
	
	private Job<?,?> asJob(Item item) {
		if (item  instanceof Job) {
			return  (Job<?,?>) item;
		} else {
			throw new IllegalArgumentException(format("expected %s to be a job but was %s", item.getName(), item));
		}
	}

	private Jenkins jenkins() {
		return Jenkins.getInstance();
	}

	
	@Override
	public String toString() {
		return format("OnTrigger[%s, %s, %s]", gitUrl, organisation, repo);
	}

}
