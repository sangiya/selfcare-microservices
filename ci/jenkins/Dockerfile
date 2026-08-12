# A local, disposable Jenkins for running this repo's Jenkinsfile + shared library end to
# end on your own machine. This is NOT how a real org Jenkins is set up -- a real instance
# runs the controller separately from build agents (e.g. Kubernetes-pod agents templated
# per Doc 2 sec 3.8), never runs as root, and talks to a real container registry / k8s
# cluster. This image exists purely so `agent { label 'docker' }` in the root Jenkinsfile
# has somewhere to actually run locally, with every CLI tool ci/README.md says it needs
# (Docker, Maven, Helm, Trivy, Gitleaks) baked in.
FROM jenkins/jenkins:lts-jdk21

USER root

# --- Docker CLI only (talks to the HOST's Docker Desktop engine through the docker.sock
# bind-mounted in docker-compose.jenkins.yml -- no nested daemon, no Docker-in-Docker) ---
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
       > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli \
    && rm -rf /var/lib/apt/lists/*

# --- Maven (for the Build & Unit Test / Code Quality / Security Scans stages) ---
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

# --- Helm (for the Deploy to Dev stage) ---
RUN curl -fsSL -o /tmp/get-helm-3 https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 \
    && chmod +x /tmp/get-helm-3 \
    && /tmp/get-helm-3 \
    && rm /tmp/get-helm-3

# --- Trivy (container image scanning, Package & Scan Images stage) ---
RUN curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh \
      | sh -s -- -b /usr/local/bin

# --- Gitleaks (secrets scanning, Security Scans stage) ---
# Pin a known-good version rather than "latest" so this build stays reproducible; bump as needed.
ARG GITLEAKS_VERSION=8.30.1
RUN curl -fsSL -o /tmp/gitleaks.tar.gz \
      "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" \
    && tar xzf /tmp/gitleaks.tar.gz -C /usr/local/bin gitleaks \
    && rm /tmp/gitleaks.tar.gz

# --- Node.js (only for the `npx conventional-changelog` post-build changelog step) ---
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

USER jenkins

# Bake in every plugin the Jenkinsfile actually needs, so the setup wizard has nothing left
# to install manually.
RUN jenkins-plugin-cli --plugins \
      workflow-aggregator \
      git \
      docker-workflow \
      pipeline-groovy-lib \
      junit \
      jacoco \
      sonar \
      credentials-binding \
      blueocean
