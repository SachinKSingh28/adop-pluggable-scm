package pluggable.scm.github;

import pluggable.scm.helpers.*;
import java.net.URL;
import java.io.IOException;
import java.io.DataOutputStream;
import java.nio.charset.Charset;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;


public class GithubRequestUtil {

  public static void isProjectAvailable(URL githubApiUrl, String username, String password, String githubNameSpaceType, String githubNameSpace){

    URL url = new URL(githubApiUrl, getContext(githubNameSpaceType, githubNameSpace ));
    def auth = "${username}:${password}".bytes.encodeBase64().toString();

    HttpURLConnection http = (HttpURLConnection) url.openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty ("Authorization", "Basic ${auth}");

    switch (http.getResponseCode()) {
      case 200:
        Logger.info("Project ${githubNameSpace} found.");
        break;
      case 401:
        Logger.log(LogLevel.ERROR, "Credentials are invalid.");
        break;
      case {it > 401}:
        throw new IOException("Github ${githubNameSpaceType} : ${githubNameSpace} doesn't exist or Github is not available!");
        break;
    }
  }

  public static String[] getProjectRepositorys(URL githubApiUrl, String username, String password, String githubNameSpaceType, String githubNameSpace){

    JsonSlurper jsonSlurper = new JsonSlurper();
    URL url = new URL(githubApiUrl, getContext(githubNameSpaceType, githubNameSpace) + "/repos?per_page=1000");
    def auth = "${username}:${password}".bytes.encodeBase64().toString();
    List<String> repositoryList = [];

    HttpURLConnection http = (HttpURLConnection) url.openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty ("Authorization", "Basic ${auth}");
    http.setRequestProperty("Content-Type", "application/json");

    switch (http.getResponseCode()) {
      case 200:
        def json = jsonSlurper.parse(http.getInputStream())
        json.each {
          repositoryList.add(it.name);
        }
        break;
      case 401:
        Logger.log(LogLevel.ERROR, "Credentials are invalid.");
        break;
      case 404:
        throw new IOException("URI not found :" + url.toString() + " not found!");
        break;
      default:
        def json = jsonSlurper.parse(http.getInputStream())
        Logger.info(json.errors.message);
        break;
    }

    return repositoryList;
  }
 
  public static void createRepository(URL githubApiUrl, String username, String password, String githubNameSpaceType, String githubNameSpace, String repoName){

    JsonSlurper jsonSlurper = new JsonSlurper();
    URL url = new URL(githubApiUrl, getContext(githubNameSpaceType, githubNameSpace) + "/repos");
    def auth = "${username}:${password}".bytes.encodeBase64().toString();
    def body =  JsonOutput.toJson([name: repoName])
    byte[] postData = body.getBytes(Charset.forName("UTF-8"));

    HttpURLConnection http = (HttpURLConnection) url.openConnection();
    http.setRequestMethod("POST");
    http.setDoOutput(true);
    http.setInstanceFollowRedirects(false);
    http.setRequestProperty ("Authorization", "Basic ${auth}");
    http.setRequestProperty("Content-Type", "application/json");
    http.setRequestProperty("charset", "utf-8");
    http.setRequestProperty("Content-Length", postData.length.toString());
    http.setUseCaches(false);

    DataOutputStream wr = new DataOutputStream( http.getOutputStream())
    wr.write( postData );
    wr.flush();
    wr.close();

    switch (http.getResponseCode()) {
	    case 201:
        println("Repository created in Github : " + githubNameSpace + "/" + repoName);
        break;
	    case 404:
        throw new IOException("URI not found: " + url.toString() + "/repos not found!");
        break;
	    default:
        def json = jsonSlurper.parse(http.getInputStream())
        println(json.errors.message);
        break;
    }
  }

  public static String getContext(String githubNameSpaceType, String githubNameSpace) {
     StringBuffer apiContext = new StringBuffer("");
    switch(githubNameSpaceType){
      case "user":
        apiContext.append("/user");
        break;
      case "org":
        apiContext.append("/orgs/");
        apiContext.append(githubNameSpace);
        break;
      default:
        throw new IllegalArgumentException("Invalid github namespace type : " + githubNameSpaceType + ". Allowed values are 'user' or 'org'." );
        break;
    }
    return apiContext.toString();

  }

}

