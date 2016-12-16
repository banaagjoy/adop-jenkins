import jenkins.model.*
import hudson.security.*

/**
 * @author Mihail Ivanov <mihail.ivanov@accenture.com>
 * Loads matrix-based security configuration
 * Environment variables specified in docker run command/docker compose:
 *  - MATRIX_ADMIN_GROUPS
 *  - MATRIX_NON_ADMIN_GROUPS
 * https://wiki.jenkins-ci.org/display/JENKINS/Matrix-based+security
 */

// TODO(mihail): Stop executing the script if environment variables are missing

// fetch environment variables
def env = System.getenv()
def matrix_admin_groups = env['MATRIX_ADMIN_GROUPS']
def matrix_admin_groups_list = matrix_admin_groups.split(',')
def matrix_non_admin_groups = env['MATRIX_NON_ADMIN_GROUPS']
def matrix_non_admin_groups_list = matrix_non_admin_groups.split(',')

// fetch Jenkins state
def instance = Jenkins.getInstance()

Thread.start {
    sleep 15000

    println '--> Configuring matrix-based security'
    // https://github.com/jenkinsci/matrix-auth-plugin/blob/matrix-auth-1.2/src/main/java/hudson/security/GlobalMatrixAuthorizationStrategy.java
    def strategy = new GlobalMatrixAuthorizationStrategy()

    // Permissions - http://javadoc.jenkins.io/archive/jenkins-1.609/hudson/security/Permission.html
    // TODO(mihail): implement a for loop instead
    strategy.add(Jenkins.ADMINISTER, 'GOEP.DEV.DCSC.Mobilisation')

    def non_admin_groups_permissions_map = [:]
    non_admin_groups_permissions_map.add(Permission.fromId('Hudson.Read'))
    non_admin_groups_permissions_map.add(Permission.fromId('View.Read'))
    // def non_admin_groups_permission = new Permission()
    // TODO(mihail): for loop again
    strategy.add(non_admin_groups_permissions_map, 'GOEP.RM.HYB.AO')

    // add matrix-based security
    instance.setAuthorizationStrategy(strategy)

    println '--> Finished matrix configuration'

    // Save the state
    instance.save()

    println '--> Exiting matrix_based_auth.groovy script'
}