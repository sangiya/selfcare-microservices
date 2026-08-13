# A local, disposable Jenkins for running this repo's Jenkinsfile + shared library end to
# end on your own machine. This is NOT how a real org Jenkins is set up -- a real instance
# runs the controller separately from build agents (e.g. Kubernetes-pod agents templated
# per Doc 2 sec 3.8), never runs as root, and talks to a real container registry / k8s
# cluster. This image exists purely so `agent { label 'docker' }` in the root Jenkinsfile
# has somewhere to actually run locally, with every CLI tool ci/README.md says it needs
# (Docker, Maven, Helm, Trivy, Gitleaks) baked in.

# Trivy and Gitleaks are pulled as their own published images and their binaries lifted out
# via COPY --from, rather than curl-ing release tarballs straight from GitHub -- some networks
# (this one included, apparently) let github.com/api.github.com through fine but flake on the
# actual release-asset CDN. Going through `docker pull` instead reuses the exact same registry
# path that's already been pulling every other image fine all session.
FROM aquasec/trivy:latest AS trivy-src
FROM zricethezav/gitleaks:latest AS gitleaks-src
FROM grafana/k6:latest AS k6-src

FROM jenkins/jenkins:lts-jdk25

USER root

# --- Docker CLI only (talks to the HOST's Docker Desktop engine through the docker.sock
# bind-mounted in docker-compose.jenkins.yml -- no nested daemon, no Docker-in-Docker) ---
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL --retry 5 --retry-delay 3 --retry-all-errors https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
       > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli git \
    && rm -rf /var/lib/apt/lists/*

# --- Maven (for the Build & Unit Test / Code Quality / Security Scans stages) ---
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

# --- Helm (for the Deploy to Dev stage) ---
RUN curl -fsSL --retry 5 --retry-delay 3 --retry-all-errors -o /tmp/get-helm-3 https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 \
    && chmod +x /tmp/get-helm-3 \
    && /tmp/get-helm-3 \
    && rm /tmp/get-helm-3

# --- Trivy (container image scanning, Package & Scan Images stage) ---
COPY --from=trivy-src /usr/local/bin/trivy /usr/local/bin/trivy

# --- Gitleaks (secrets scanning, Security Scans stage) ---
COPY --from=gitleaks-src /usr/bin/gitleaks /usr/local/bin/gitleaks

# --- k6 (performance/load testing, Performance Test stage) ---
COPY --from=k6-src /usr/bin/k6 /usr/local/bin/k6

# --- Node.js (only for the `npx conventional-changelog` post-build changelog step) ---
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# --- Configuration as Code + init scripts: everything from node label to credentials to the
# shared library to the pipeline job itself, plus SonarQube server registration, applied
# automatically on startup -- no manual setup-wizard clicking required. ---
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/casc.yaml
COPY casc.yaml /usr/share/jenkins/casc.yaml
COPY init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/

USER jenkins

# Bake in every plugin the Jenkinsfile actually needs, so the setup wizard has nothing left
# to install manually.
RUN jenkins-plugin-cli --plugins \
      workflow-aggregator \
      git \
      docker-workflow \
      pipeline-groovy-lib \
      job-dsl \
      configuration-as-code \
      junit \
      jacoco \
      sonar \
      credentials-binding \
      blueocean
