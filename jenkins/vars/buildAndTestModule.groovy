// Example Jenkins Shared Library function (Doc 2 sec 3.8). Publish the jenkins/ directory of
// this repo as a Jenkins Shared Library ("selfcare-platform-shared-lib" -- Manage Jenkins >
// System > Global Pipeline Libraries, pointed at this repo/folder), and any Jenkinsfile in the
// org can call buildAndTestModule('loyalty-service') instead of re-writing these stages.
//
// This is intentionally a thin, illustrative example -- extend it with the security/perf
// stages from the root Jenkinsfile as your team standardizes the pipeline.
def call(String moduleName) {
    stage("Build & Test: ${moduleName}") {
        sh "mvn -B -pl platform-common,${moduleName} -am test"
        junit "${moduleName}/target/surefire-reports/*.xml"
    }
}
