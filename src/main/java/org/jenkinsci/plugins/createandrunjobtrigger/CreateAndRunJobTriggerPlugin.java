package org.jenkinsci.plugins.createandrunjobtrigger;

import java.io.IOException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Plugin;
import hudson.util.HttpResponses;
import java.util.Enumeration;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

public class CreateAndRunJobTriggerPlugin extends Plugin {

	public HttpResponse doTrigger(StaplerRequest request) throws IOException {
            if("POST".equalsIgnoreCase(request.getMethod()) 
                    && request.getHeader("Content-Type").toLowerCase().contains("json")  ) {
               JsonSlurper slurp = new JsonSlurper();
               JSONObject json = (JSONObject) slurp.parse(request.getInputStream());
//               if("refs/heads/master".equalsIgnoreCase(json.getString("ref"))) {
                  String url = json.getJSONObject("repository").getString("clone_url");
                  new OnTrigger(url).run();
//               }
                
            } else {
                String url = request.getParameter("url");

                if (url == null) {
                        return HttpResponses.error(400, "bad request: url parameter must be set");
                }
                new OnTrigger(url).run();
            }
  
            return HttpResponses.ok();
	}
}
