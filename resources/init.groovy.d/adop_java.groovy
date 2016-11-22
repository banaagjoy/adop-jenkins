import hudson.model.*;
import jenkins.model.*;
import hudson.tools.*;

// Check if enabled
def env = System.getenv()
if (env['JDK_VERSION'] == null) {
    println "--> ADOP Multi JDK Disabled"
    return
}

// Variables
// JDK_VERSION can be defined as a single version or a comma separated string of versions
// eg. 
// JDK_VERSION=jdk-7u25-oth-JPR
// JDK_VERSION=jdk-7u25-oth-JPR,jdk-8u60-oth-JPR

def jdk_version = env['JDK_VERSION']
def jdk_version_list = jdk_version.split(',')

// Constants
def instance = Jenkins.getInstance()

Thread.start {
    sleep 10000

    // JDK
    println "--> Configuring JDK"
    def desc_jdkTool = new JDK.DescriptorImpl()
    def jdk_installations = desc_jdkTool.getInstallations()

    jdk_version_list.eachWithIndex { version, index ->
	
		println "Version: $version index: $index"
		
        def jdkInstaller = new JDKInstaller(version,true)
        def installSourceProperty = new InstallSourceProperty([jdkInstaller])
        
        def name="ADOP JDK_" + version

        // This makes the solution backwards-compatible, and will treat the first version in the array as "ADOP JDK"
        if (index == 0)
        {
            name="ADOP JDK"
        }
		println "jdk name: $name"
		
        def jdk_inst = new JDK(
          name, // Name
          "", // Home
          [installSourceProperty]
        )

        // Only add a Java installation if it does not already exist - do not overwrite existing config
        
        def jdk_inst_exists = false
        jdk_installations.each {
          installation = (JDK) it
            if ( jdk_inst.getName() ==  installation.getName() ) {
                    jdk_inst_exists = true
                    println("Found existing installation: " + installation.getName())
            }
        }
        
        if (!jdk_inst_exists) {
            jdk_installations += jdk_inst
        }
    }

    desc_jdkTool.setInstallations((JDK[]) jdk_installations)
    desc_jdkTool.save()
	
	println "Saving the jdk's"

    // Save the state
    instance.save()
	
	println "Saving the instances"
}