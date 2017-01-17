import hudson.model.*
import jenkins.model.*
import hudson.security.*
import jenkins.security.plugins.ldap.*
import hudson.util.Secret
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.cloudbees.plugins.credentials.CredentialsScope

/**
 * @author Mihail Ivanov <mihail.ivanov@accenture.com>
 * Sets global credentials
 * Environment variables specified in docker run command/docker compose:
 *  - CREDENTIALS_LDAP_SERVICE_USER_ID
 *  - CREDENTIALS_LDAP_SERVICE_USER
 *  - CREDENTIALS_LDAP_SERVICE_USER_PASSWORD
 *  - CREDENTIALS_LDAP_SERVICE_DESCRIPTION
 *  - CREDENTIALS_SONAR_USER_ID
 *  - CREDENTIALS_SONAR_USER
 *  - CREDENTIALS_SONAR_USER_PASSWORD
 *  - CREDENTIALS_SONAR_USER_DESCRIPTION
 */

// fetch Jenkins state
def env = System.getenv()
def instance = Jenkins.getInstance()


Thread.start {
	// executing the following lines as admin user instead of anonymous
	ACL.impersonate(User.get(env['INITIAL_ADMIN_USER']).impersonate())
	println 'User: ' + User.current()

    println '--> Configuring global credentials'
   	// define credentials
   	// parameters (credential_id, credential_username, credential_password, credential_description)
	def ldap_service_user = new Tuple(env['CREDENTIALS_LDAP_SERVICE_USER_ID'], env['CREDENTIALS_LDAP_SERVICE_USER'], env['CREDENTIALS_LDAP_SERVICE_USER_PASSWORD'], env['CREDENTIALS_LDAP_SERVICE_DESCRIPTION'])
	def sonar_user = new Tuple(env['CREDENTIALS_SONAR_USER_ID'], env['CREDENTIALS_SONAR_USER'], env['CREDENTIALS_SONAR_USER_PASSWORD'], env['CREDENTIALS_SONAR_USER_DESCRIPTION'])

	// TODO(mihail): jenkinsslave + private key
    // define two lists with all the credentials
    user_with_password_list = [sonar_user, ldap_service_user]

	// http://javadoc.jenkins-ci.org/credentials/com/cloudbees/plugins/credentials/SystemCredentialsProvider.html

	user_with_password_list.each {
	    user_with_password = (Tuple) it
	    println "--> Registering credentials"
	    def system_credentials_provider = SystemCredentialsProvider.getInstance()

	    def credential_description = user_with_password.get(3)

	    credentials_exist = false
	    system_credentials_provider.getCredentials().each {
	        credentials = (com.cloudbees.plugins.credentials.Credentials) it
	        if ( credentials.getDescription() == credential_description) {
	            credentials_exist = true
	            println("Found existing credentials: " + credential_description)
	        }
	    }

	    if(!credentials_exist) {
	        def credential_scope = CredentialsScope.GLOBAL
	        def credential_id = user_with_password.get(0)
	        def credential_username = user_with_password.get(1)
	        def credential_password = user_with_password.get(2)

	        def credential_domain = com.cloudbees.plugins.credentials.domains.Domain.global()
	        def credential_creds = new UsernamePasswordCredentialsImpl(credential_scope,credential_id,credential_description,credential_username,credential_password)

	        system_credentials_provider.addCredentials(credential_domain,credential_creds)
	    }
	}

    println '--> Finished global credentials configuration'

    // Save the state
    instance.save()

    println '--> Exiting set_credentials.groovy script'
}
