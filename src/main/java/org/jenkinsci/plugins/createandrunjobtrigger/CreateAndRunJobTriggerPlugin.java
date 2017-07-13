package org.jenkinsci.plugins.createandrunjobtrigger;

import java.io.IOException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Plugin;
import hudson.util.HttpResponses;

public class CreateAndRunJobTriggerPlugin extends Plugin {

	public HttpResponse doTrigger(StaplerRequest request) throws IOException {
		String url = request.getParameter("url");

		if (url == null) {
			return HttpResponses.error(400, "bad request: url parameter must be set");
		}
		new OnTrigger(url).run();

		return HttpResponses.ok();
	}
}
