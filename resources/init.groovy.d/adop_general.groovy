import hudson.model.*;
import jenkins.model.*;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsScope;

// Variables
def env = System.getenv()
def root_Url = env['ROOT_URL']
def gitGlobalConfigName = env['GIT_GLOBAL_CONFIG_NAME']
def gitGlobalConfigEmail = env['GIT_GLOBAL_CONFIG_EMAIL']
def adopPlatformManagementVersion = env['ADOP_PLATFORM_MANAGEMENT_VERSION']
def awsKeypair = env['AWS_KEYPAIR']
def awsVpcId = env['AWS_VPC_ID']
def awsSubnetId = env['AWS_SUBNET_ID']
def awsInstanceType = env['AWS_INSTANCE_TYPE']
def awsDefaultRegion = env['AWS_DEFAULT_REGION']
def dockerTLSVerify = env['DOCKER_TLS_VERIFY']
def dockerHost = env['DOCKER_HOST']
def dockerCertPath = env['DOCKER_CLIENT_CERT_PATH']
def dockerNetworkName = env['DOCKER_NETWORK_NAME']
def gerritUsername = env['GERRIT_JENKINS_USERNAME']

// Constants
def instance = Jenkins.getInstance()

Thread.start {
    println "--> Configuring General Settings"

    // Base URL
    println "--> Setting Base URL"
    jlc = JenkinsLocationConfiguration.get()
    jlc.setUrl(root_Url)
    jlc.save()

    // Global Environment Variables
    // Source: https://groups.google.com/forum/#!topic/jenkinsci-users/KgCGuDmED1Q
    globalNodeProperties = instance.getGlobalNodeProperties()
    envVarsNodePropertyList = globalNodeProperties.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class)

    newEnvVarsNodeProperty = null
    envVars = null

    if ( envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0 ) {
      newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
      globalNodeProperties.add(newEnvVarsNodeProperty)
      envVars = newEnvVarsNodeProperty.getEnvVars()
    } else {
      envVars = envVarsNodePropertyList.get(0).getEnvVars()
    }

    // Example
    //envVars.put("FOO", "bar")
	
	// Set AWS environment variables
    if ( awsKeypair != null ) {
		envVars.put("AWS_KEYPAIR", awsKeypair)
    }
	if ( awsVpcId != null ) {
		envVars.put("AWS_VPC_ID", awsVpcId)
    }
	if ( awsSubnetId != null ) {
		envVars.put("AWS_SUBNET_ID", awsSubnetId)
    }
	if ( awsInstanceType != null ) {
		envVars.put("AWS_INSTANCE_TYPE", awsInstanceType)
    }
	if ( awsDefaultRegion != null ) {
		envVars.put("AWS_DEFAULT_REGION", awsDefaultRegion)
    }

    // Set Platform variables
    if ( adopPlatformManagementVersion != null ) {
        envVars.put("ADOP_PLATFORM_MANAGEMENT_VERSION", adopPlatformManagementVersion)
    }

    // Set Docker environment
    if ( dockerTLSVerify != null && dockerTLSVerify.toBoolean()) {
        envVars.put("DOCKER_TLS_VERIFY", env['DOCKER_TLS_VERIFY'])
    } else {
        envVars.remove("DOCKER_TLS_VERIFY")
    }
    if ( dockerHost != null ) {
        envVars.put("DOCKER_HOST", dockerHost)
    }
    if ( dockerCertPath != null ) {
        envVars.put("DOCKER_CERT_PATH", dockerCertPath)
    }
    if ( dockerNetworkName != null ) {
        envVars.put("DOCKER_NETWORK_NAME", dockerNetworkName)
    }
	
    //Set Gerrit username variable
    println "--> Setting GERRIT_JENKINS_USERNAME..."
    if ( gerritUsername != null) {
        envVars.put("GERRIT_JENKINS_USERNAME", gerritUsername)
    }

    // Jenkins SSH Credentialscd 
    println "--> Registering SSH Credentials"
    def system_credentials_provider = SystemCredentialsProvider.getInstance()

    def ssh_key_description = "ADOP Jenkins Master"

    ssh_credentials_exist = false
    system_credentials_provider.getCredentials().each {
        credentials = (com.cloudbees.plugins.credentials.Credentials) it
        if ( credentials.getDescription() == ssh_key_description) {
            ssh_credentials_exist = true
            println("Found existing credentials: " + ssh_key_description)
        }
    }

    if(!ssh_credentials_exist) {
        def ssh_key_scope = CredentialsScope.GLOBAL
        def ssh_key_id = "adop-jenkins-master"
        def ssh_key_username = "jenkins"
        def ssh_key_private_key_source = new BasicSSHUserPrivateKey.UsersPrivateKeySource()
        def ssh_key_passphrase = null

        def ssh_key_domain = com.cloudbees.plugins.credentials.domains.Domain.global()
        def ssh_key_creds = new BasicSSHUserPrivateKey(ssh_key_scope,ssh_key_id,ssh_key_username,ssh_key_private_key_source,ssh_key_passphrase,ssh_key_description)

        system_credentials_provider.addCredentials(ssh_key_domain,ssh_key_creds)
    }

    // Git Identity
    println "--> Configuring Git Identity"
    def desc_git_scm = instance.getDescriptor("hudson.plugins.git.GitSCM")
    desc_git_scm.setGlobalConfigName(gitGlobalConfigName)
    desc_git_scm.setGlobalConfigEmail(gitGlobalConfigEmail)

    // Save the state
    instance.save()
}
