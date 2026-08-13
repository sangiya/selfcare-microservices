import hudson.plugins.sonar.SonarGlobalConfiguration
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.model.TriggersConfig
import hudson.util.Secret
import jenkins.model.Jenkins

def installationName = System.getenv('SONARQUBE_INSTALLATION_NAME') ?: 'sonarqube'
def serverUrl = System.getenv('SONARQUBE_URL') ?: 'http://sonarqube:9000'
def credentialsId = System.getenv('SONARQUBE_CREDENTIALS_ID') ?: 'sonarqube-token'

def jenkins = Jenkins.get()
def config = SonarGlobalConfiguration.get()
def remainingInstallations = (config.getInstallations() ?: [])
    .findAll { it.getName() != installationName }

def installation = new SonarInstallation(
    installationName,
    serverUrl,
    credentialsId,
    (Secret) null,
    null,
    '',
    '',
    '',
    new TriggersConfig(false, false, '')
)

config.setInstallations((remainingInstallations + installation) as SonarInstallation[])
config.save()
jenkins.save()

println("Configured SonarQube installation '${installationName}' at ${serverUrl}")
