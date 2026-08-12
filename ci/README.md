# CI setup notes

- **Root `Jenkinsfile`** is the pipeline for this whole monorepo. It assumes a Jenkins agent
  with Docker, Maven, Helm, Trivy, and Gitleaks available (a `docker`-labeled agent, or a
  Jenkins agent pod templated with those tools if you're running Jenkins on the same EKS
  cluster -- see Doc 2 sec 3.8).
- **`jenkins/vars/buildAndTestModule.groovy`** is a starter Shared Library function. Publish the
  `jenkins/` directory as a Global Pipeline Library named `selfcare-platform-shared-lib` (Manage
  Jenkins → System → Global Pipeline Libraries) so the `@Library(...)` line at the top of the
  root `Jenkinsfile` resolves, and so any new service's pipeline can be ~15 lines instead of a
  full copy of the root Jenkinsfile.
- **Required Jenkins credentials** (`credentials(...)` calls in the Jenkinsfile):
  `container-registry-url`, `sonarqube-token`. Add your image registry push credentials and
  kubeconfig/context access separately per your existing Jenkins-to-EKS setup (Doc 1 sec 2.1
  says this already exists for the 3 current Java services -- reuse it, don't rebuild it).
- **SonarQube / Trivy / Gitleaks** are the only genuinely new tools this pipeline introduces
  (Doc 2 sec 1.1) -- install the SonarQube Jenkins plugin and point `withSonarQubeEnv('sonarqube')`
  at your SonarQube CE instance; Trivy and Gitleaks are single static binaries, install on the
  Jenkins agent image.
- **`Deploy to Dev`** assumes a `dev` kube-context is already configured on the Jenkins agent.
  Promotion to QA/staging/prod (Doc 2 sec 6) is deliberately a separate, gated pipeline --
  extend this file or add `Jenkinsfile.promote` once the QA automation suites referenced in the
  `QA Automation (Dev)` stage exist.

## Running Jenkins locally against this repo

`ci/jenkins/Dockerfile` + the root `docker-compose.jenkins.yml` give you a disposable local
Jenkins with Docker CLI, Maven, Helm, Trivy, and Gitleaks already baked in -- everything the
root `Jenkinsfile` expects an `agent { label 'docker' }` to have. This is for testing/learning
the pipeline on your own machine, not a template for a real org instance (see the warnings in
both files for what a real setup does differently: separate build agents, no root, a real
registry/cluster).

1. Make sure this repo is an actual git repository (`git init && git add -A && git commit -m
   "init"` if it isn't yet) -- both the Pipeline job and the shared library need real git
   history to check out from.
2. `docker compose -f docker-compose.jenkins.yml up -d --build` (add `--profile sonar` too if
   you want a local SonarQube for the `Code Quality`/`Quality Gate` stages).
3. Open http://localhost:9090, unlock with the initial admin password
   (`docker compose -f docker-compose.jenkins.yml exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword`),
   skip plugin selection (already baked in), create your admin user.
4. **Manage Jenkins > Nodes > Built-In Node > Configure** -- add label `docker` (this repo has
   no separate agent locally, so the controller itself needs to match `agent { label 'docker' }`).
5. **Manage Jenkins > Credentials > System > Global credentials > Add Credentials** -- add two
   *Secret text* credentials: ID `container-registry-url` (any placeholder value is fine locally
   -- nothing gets pushed unless Jenkins sets `BRANCH_NAME=main`, which a plain Pipeline job
   never does) and ID `sonarqube-token` (a real token from your local SonarQube's
   **My Account > Security > Generate Token** once step 2's `--profile sonar` container is up
   and you've configured it under **Manage Jenkins > System > SonarQube servers** as `sonarqube`
   pointing at `http://sonarqube:9000`; a placeholder is fine if you're skipping Code Quality
   for now).
6. **Manage Jenkins > System > Global Trusted Pipeline Libraries** -- add a library named
   `selfcare-platform-shared-lib`, default version `main`, retrieval method **Modern SCM > Git**,
   repository `file:///workspace/microservices`, and set **Library Path** to `jenkins` (recent
   Jenkins versions have this field for exactly this "library lives in a subdirectory" case). If
   your Jenkins version doesn't show a Library Path field, mirror the subdirectory into its own
   tiny repo instead and point at that: `cp -r jenkins /tmp/shared-lib && cd /tmp/shared-lib &&
   git init && git add -A && git commit -m "lib"`, then use `file:///tmp/shared-lib` (adjust the
   compose bind mount if `/tmp` isn't shared into the container).
7. **New Item > Pipeline** (a plain Pipeline job, not Multibranch -- that's what keeps
   `BRANCH_NAME` unset locally, which is what naturally skips `Push Images`/`Deploy to
   Dev`/`QA Automation`/`Performance Test` below, since none of the infrastructure those need
   exists on a laptop) -- Pipeline section: **Definition: Pipeline script from SCM**, SCM
   **Git**, repository `file:///workspace/microservices`, script path `Jenkinsfile`.
8. **Build Now.** Expect `Checkout` -> `Build & Unit Test` -> (`Code Quality` +
   `Quality Gate` if SonarQube is wired up, otherwise this is where it'll stop) ->
   `Security Scans` -> `Package & Scan Images` to actually run; the four `when { branch 'main'
   }` stages show as skipped, which is expected for this setup.
