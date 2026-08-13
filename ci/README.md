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
- **`QA Automation (Dev)`** now really runs the API (REST-Assured) and web (Playwright) suites
  from `qa-automation/` -- see `qa-automation/README.md` for the full toolchain, what's real
  today vs. a placeholder, and how to run each suite standalone without Jenkins at all.

## Running Jenkins locally against this repo

`ci/jenkins/Dockerfile` + the root `docker-compose.jenkins.yml` give you a disposable local
Jenkins with Docker CLI, Maven, Helm, Trivy, and Gitleaks already baked in -- everything the
root `Jenkinsfile` expects an `agent { label 'docker' }` to have. This is for testing/learning
the pipeline on your own machine, not a template for a real org instance (see the warnings in
both files for what a real setup does differently: separate build agents, no root, a real
registry/cluster).

Setup is almost entirely automatic via Jenkins Configuration as Code (`ci/jenkins/casc.yaml`)
and a startup script (`ci/jenkins/init.groovy.d/prepare-shared-lib.groovy`) -- no manual UI
clicking to label the node, add credentials, register the shared library, or create the
pipeline job.

1. Make sure this repo is an actual git repository (`git init && git add -A && git commit -m
   "init"` if it isn't yet) -- both the Pipeline job and the shared library need real git
   history to check out from.
2. `docker compose -f docker-compose.jenkins.yml up -d --build` (add `--profile sonar` too if
   you want a local SonarQube for the `Code Quality`/`Quality Gate` stages).
3. Open http://localhost:9090 and log straight in with `admin` / `admin` (or whatever you set
   `JENKINS_ADMIN_PASSWORD` to) -- no unlock screen, no setup wizard, no plugin selection: all
   of that is skipped/declared already. By the time you log in, the built-in node is already
   labeled `docker`, the `container-registry-url` and `sonarqube-token` credentials already
   exist (placeholder values), the `selfcare-platform-shared-lib` global library is already
   registered, and a Pipeline job named `selfcare-microservices` already exists pointed at
   `${SCM_REPO_URL:-file:///workspace/microservices}` / `Jenkinsfile`.
4. Open the `selfcare-microservices` job and **Build Now.** Expect `Checkout` ->
   `Build & Unit Test` -> (`Code Quality` + `Quality Gate` if SonarQube is wired up, otherwise
   this is where it'll stop) -> `Security Scans` -> `Package & Scan Images` to actually run.
   `Push Images` and `Deploy to Dev` still need a real main-branch promotion path, registry, and
   kube context; `QA Automation (Dev)`, `DAST (ZAP Baseline)`, and `Performance Test (Dev)` can
   also run locally if you set `DEV_GATEWAY_URL=http://host.docker.internal:8080`.

   **To make Jenkins pick up real GitLab pushes automatically**, set `SCM_REPO_URL` to your
   GitLab repository URL instead of the local `file:///workspace/microservices` default. The job
   already polls SCM every 2 minutes, so new pushed commits are picked up automatically without a
   manual Build Now click. If you want instant rather than polling-based pickup, add your normal
   GitLab webhook/Jenkins integration on top of this.

**The one deliberately-manual step:** registering the SonarQube server itself. The SonarQube
Jenkins plugin's JCasC support has a history of breaking across plugin-version combinations
(see the linked GitHub issues below), so it's not worth automating for a throwaway local
instance. If you brought up `--profile sonar`: open http://localhost:9000 (default login
admin/admin, it'll make you change it), **My Account > Security > Generate Token**, then in
Jenkins go to **Manage Jenkins > System > SonarQube servers**, add one named `sonarqube`, URL
`http://sonarqube:9000`, and pick the `sonarqube-token` credential (or set the token via the
`SONARQUBE_TOKEN` env var in `docker-compose.jenkins.yml` and recreate the container instead of
editing the credential by hand). Skip this entirely and the pipeline still runs fine up through
`Build & Unit Test` -- it'll just stop at `Code Quality`.
