// Runs automatically on first boot (official Jenkins Docker image convention: everything
// under /usr/share/jenkins/ref/init.groovy.d/ is copied into $JENKINS_HOME/init.groovy.d/ and
// executed once Jenkins core has started, before jobs load).
//
// Mirrors this repo's jenkins/ subdirectory into its own standalone git repo, because Jenkins
// Global Pipeline Libraries need vars/ (and optionally src/) at the checkout ROOT -- our
// jenkins/vars/*.groovy lives in a subdirectory of the main monorepo checkout instead. This
// sidesteps relying on the Modern SCM "Library Path" field, which isn't consistently present
// across Jenkins versions.

def src = new File("/workspace/microservices/jenkins")
def dest = new File("/var/jenkins_home/shared-lib-src")

if (!src.exists()) {
    println "[prepare-shared-lib] ${src.absolutePath} not found (repo not mounted yet?) -- skipping"
    return
}

if (new File(dest, ".git").exists()) {
    println "[prepare-shared-lib] ${dest.absolutePath} already initialized -- skipping"
    return
}

dest.mkdirs()
def gitEnv = [
    "GIT_AUTHOR_NAME=ci", "GIT_AUTHOR_EMAIL=ci@local",
    "GIT_COMMITTER_NAME=ci", "GIT_COMMITTER_EMAIL=ci@local",
    "PATH=/usr/bin:/bin:/usr/local/bin",
    "HOME=/var/jenkins_home"
]

def run = { List<String> cmd ->
    def proc = cmd.execute(gitEnv, dest)
    proc.waitFor()
    if (proc.exitValue() != 0) {
        println "[prepare-shared-lib] '${cmd.join(' ')}' failed: ${proc.err.text}"
    }
    return proc.exitValue()
}

["bash", "-c", "cp -r '${src.absolutePath}'/. '${dest.absolutePath}'/"].execute().waitFor()
run(["git", "init", "-q"])
run(["git", "add", "-A"])
run(["git", "commit", "-q", "-m", "shared library mirror"])
run(["git", "branch", "-M", "main"])

println "[prepare-shared-lib] mirrored ${src.absolutePath} into ${dest.absolutePath} as its own git repo on branch 'main'"
