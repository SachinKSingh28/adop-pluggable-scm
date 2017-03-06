package pluggable.scm.github;

/**
* Set of named constants representing the supported Github SCM protocols.
*/
enum GithubSCMProtocol {
    SSH("ssh"),
    HTTPS("https")

    private final String protocol = "";

    /**
    * Constructor for class GithubSCMProtocol.
    *
    * @param protocal a string representation of the protocol e.g. ssh, https
    */
    public GithubSCMProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static void isProtocolSupported(GithubSCMProtocol protocol){
      switch(protocol){
        case GithubSCMProtocol.SSH:
        case GithubSCMProtocol.HTTPS:
          break;
        default:
          throw new IllegalArgumentException("SCM Protocol type not supported.");
          break;
      }
    }

    /**
    * Return a string representation of the SCM protocol.
    * @return a string representation of the SCM protocol.
    */
    @Override
    public String toString(){
      return this.protocol;
    }

}
