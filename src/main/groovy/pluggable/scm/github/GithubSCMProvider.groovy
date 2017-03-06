package pluggable.scm.github;

import pluggable.scm.SCMProvider;
import pluggable.configuration.EnvVarProperty;
import pluggable.scm.helpers.*;
import java.util.Properties;
import java.net.URL;

/**
* This class implements the Github SCM Provider.
*/
public class GithubSCMProvider implements SCMProvider {


  // SCM specific variables.
  private final int scmPort;
  private final GithubSCMProtocol scmProtocol;

  // Github specific variables.
  private final String githubEndpoint;
  private final String githubApiEndpoint;
  private final String githubNameSpaceType;
  private final int githubPort;
  private final GithubSCMProtocol githubProtocol;
  private String githubUsername;
  private String githubPassword;

  /**
  * Constructor for class GithubSCMProvider.
  *
  * @param scmPort scm port
  * @param scmProtocol scm clone protocol
  */
  public GithubSCMProvider(int scmPort,
                           GithubSCMProtocol scmProtocol,
                           String githubEndpoint,
                           String githubApiEndpoint,
                           String githubNameSpaceType,
                           GithubSCMProtocol githubProtocol,
                           int githubPort){

      this.scmPort = scmPort;
      this.scmProtocol = scmProtocol;
      this.githubEndpoint = githubEndpoint;
      this.githubApiEndpoint = githubApiEndpoint;
      this.githubNameSpaceType = githubNameSpaceType;
      this.githubProtocol = githubProtocol;
      this.githubPort = githubPort;

      // If not it will thorw IllegalArgumentException.
      GithubSCMProtocol.isProtocolSupported(this.githubProtocol);
  }

  /**
  * Return Github SCM URL.
  * @return SCM url for the provider.
  *     e.g. Github-SSH  ssh://git@github.com/
  *          Github-HTTPS https://github.com/<namespace>/
  *
  * @throws IllegalArgumentException
  *           If the SCM protocol type is not supported.
  **/
  public String getScmUrl(){

      StringBuffer url = new StringBuffer("")


      switch(this.scmProtocol){
        case GithubSCMProtocol.SSH:
          url.append("git@");
          url.append(this.githubEndpoint);
          url.append(":");
          break;
        case GithubSCMProtocol.HTTPS:
          url.append(this.scmProtocol);
          url.append("://");
          url.append(this.githubEndpoint);
          url.append("/");
          break;
        default:
          throw new IllegalArgumentException("SCM Protocol type not supported.");
          break;
      }

      return url;
  }

  /**
  * Return a url encoded value
  * @param value string to encode.
  * @return a url encoded value.
  */
  private String urlEncode(String value){
      return URLEncoder.encode(value)
  }

  /**
  * Creates relevant repositories defined by your cartridge in your chosen SCM provider
  * @param workspace Workspace of the cartridge loader job
  * @param repoNamespace Location in your SCM provider where your repositories will be created
  * @parma overwriteRepos
  **/
  public void createScmRepos(String workspace, String repoNamespace, String codeReviewEnabled, String overwriteRepos) {

    ExecuteShellCommand com = new ExecuteShellCommand()

    String cartHome = "/cartridge"
    String urlsFile = workspace + cartHome + "/src/urls.txt"

    // Create repositories
    String command1 = "cat " + urlsFile
    List<String> repoList = new ArrayList<String>();
    repoList = (com.executeCommand(command1).split("\\r?\\n"));

    repoList.removeAll(Arrays.asList(null,""));

    if(!repoList.isEmpty()){

        EnvVarProperty envVarProperty = EnvVarProperty.getInstance();
        String filePath =  envVarProperty.getProperty("WORKSPACE")+ "@tmp/secretFiles/" +envVarProperty.getProperty("SCM_KEY")
        Properties fileProperties = PropertyUtils.getFileProperties(filePath)

        this.githubUsername = fileProperties.getProperty("SCM_USERNAME");
        this.githubPassword = fileProperties.getProperty("SCM_PASSWORD");

        URL githubApiUrl = new URL(GithubSCMProtocol.HTTPS.toString(), this.githubApiEndpoint, this.githubPort, "");

        GithubRequestUtil.isProjectAvailable(githubApiUrl, this.githubUsername, this.githubPassword, this.githubNameSpaceType, repoNamespace);

        for(String repo: repoList) {

            if (repo.equals("")) {
                break;
            }

            String repoName = repo.substring(repo.lastIndexOf("/") + 1, repo.length());
            if (repoName.contains(".git")) {
                repoName = repoName.replaceAll(".git", "")
            }
            String target_repo_name = repoNamespace + "/" + repoName
            int repo_exists = 0;

            List<String> githubRepoList = GithubRequestUtil.getProjectRepositorys(githubApiUrl, this.githubUsername, this.githubPassword, this.githubNameSpaceType, repoNamespace);
            for (String githubRepo : githubRepoList) {
                if (githubRepo.trim().contains(repoName)) {
                    Logger.info("Found: " + target_repo_name);
                    repo_exists = 1
                    break
                }
            }

            if (repo_exists == 0) {
                GithubRequestUtil.createRepository(githubApiUrl, this.githubUsername, this.githubPassword, this.githubNameSpaceType, repoNamespace, repoName);
            } else {
                Logger.info("Repository already exists, skipping create: " + target_repo_name);
            }

            // Populate repository
            String tempDir = workspace + "/tmp"

            def tempScript = new File(tempDir + '/shell_script.sh')

            tempScript << "git clone " + GithubSCMProtocol.HTTPS.toString() + "://" + this.githubUsername + ":" + this.urlEncode(this.githubPassword) + "@" + this.githubEndpoint + "/" + repoNamespace + "/" + repoName + ".git " + tempDir + "/" + repoName + "\n"
            def gitDir = "--git-dir=" + tempDir + "/" + repoName + "/.git"
            tempScript << "git " + gitDir + " remote add source " + repo + "\n"
            tempScript << "git " + gitDir + " fetch source" + "\n"

            if (overwriteRepos == "true") {
                tempScript << "git " + gitDir + " push origin +refs/remotes/source/*:refs/heads/*\n"
                Logger.info("Repository already exists, overwriting: " + target_repo_name);
            } else {
                tempScript << "git " + gitDir + " push origin refs/remotes/source/*:refs/heads/*\n"
            }

            com.executeCommand('chmod +x ' + tempDir + '/git_ssh.sh')
            com.executeCommand('chmod +x ' + tempDir + '/shell_script.sh')
            com.executeCommand(tempDir + '/shell_script.sh')

            tempScript.delete()
        }
        }
  }

  /**
    Return SCM section.

    @param projectName - name of the project.
    @param repoName  - name of the repository to clone.
    @param branchName - name of branch.
    @param credentialId - name of the credential in the Jenkins credential
            manager to use.
    @param extras - extra closures to add to the SCM section.
    @return a closure representation of the SCM providers SCM section.
  **/
  public Closure get(String projectName, String repoName, String branchName, String credentialId, Closure extras){
    if(extras == null) extras = {}
        return {
            git extras >> {
              remote{
                url(this.getScmUrl() + projectName + "/" + repoName + ".git")
                credentials(credentialId)
              }
              branch(branchName)
            }
        }
    }

    /**
    * Return a closure representation of the SCM providers trigger SCM section.
    *
    * @param projectName - project name.
    * @param repoName - repository name.
    * @param branchName - branch name to trigger.
    * @return a closure representation of the SCM providers trigger SCM section.
    */
    public Closure trigger(String projectName, String repoName, String branchName) {
        return {
              githubPush()
              scm('')
        }
    }
}
