// Root pipeline for the whole monorepo (Doc 2 sec 3.8: Jenkins stays the CI orchestrator,
// extended with new stages -- no parallel CI system). One Jenkinsfile, one shared library
// (jenkins/vars/*.groovy, referenced below) means adding a 5th microservice does NOT mean
// hand-writing a new pipeline -- see ci/Jenkinsfile.loyalty-service for the ~15-line
// per-service file this enables once the shared library is published to your Jenkins instance.
//
// Maps directly onto Doc 5 sec 6's automation checklist: every stage below is one numbered
// item from that list.

@Library('selfcare-platform-shared-lib') _ // TODO: publish jenkins/vars/*.groovy (see ci/README.md) as this shared library

pipeline {
    agent { label 'docker' }
    options { skipDefaultCheckout(true) }

    parameters {
        string(name: 'SERVICES', defaultValue: 'platform-common,config-tenant-service,api-gateway,loyalty-service,reports-service,notification-service,content-service',
               description: 'Comma-separated modules to build this run (CI normally computes this from changed paths).')
    }

    environment {
        REGISTRY = credentials('container-registry-url')
        NVD_API_KEY = credentials('nvd-api-key')
        ODC_DATA_DIR = '/var/jenkins_home/dependency-check-data'
        SONAR_ANALYSIS_READY = 'false'
        IMAGE_TAG = ''
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
                script {
                    env.IMAGE_TAG = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    echo "Using image tag ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build & Unit Test') {
            // Doc 5 sec 6, item 1: unit testing, coverage-gated (jacoco-check in the parent
            // pom fails the build below the threshold -- this stage's failure IS the gate).
            steps {
                script {
                    retry(2) {
                        sh """
                          set -eu
                          (
                            while true; do
                              sleep 30
                              echo "[keepalive] build-and-test still running at \$(date -Iseconds)"
                            done
                          ) &
                          keepalive_pid=\$!
                          trap 'kill \$keepalive_pid 2>/dev/null || true' EXIT
                          mvn -B -pl ${params.SERVICES} -am test
                        """
                    }
                }
            }
            post {
                always {
                    junit skipPublishingChecks: true, testResults: '**/target/surefire-reports/*.xml'
                    jacoco execPattern: '**/target/jacoco.exec'
                }
            }
        }

        stage('Code Quality') {
            // Doc 5 sec 6, item 5.
            steps {
                script {
                    withSonarQubeEnv('sonarqube') {
                        // Run from the workspace root and pass the Jenkins-provided Sonar
                        // connection details explicitly so the Maven scanner always writes the
                        // report-task metadata Jenkins needs for waitForQualityGate().
                        sh """
                          set -eu
                          mvn -f "\$WORKSPACE/pom.xml" -B -pl ${params.SERVICES} -am \\
                            -Dsonar.host.url="\$SONAR_HOST_URL" \\
                            -Dsonar.token="\$SONAR_AUTH_TOKEN" \\
                            org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar
                        """
                        def reportTask = sh(
                                script: 'find "$WORKSPACE" -name report-task.txt -print -quit',
                                returnStdout: true
                        ).trim()
                        if (!reportTask) {
                            error("Sonar analysis completed without producing report-task.txt; refusing to run waitForQualityGate().")
                        }
                        env.SONAR_ANALYSIS_READY = 'true'
                        echo "Sonar report task metadata: ${reportTask}"
                    }
                }
            }
        }

        stage('Quality Gate') {
            when {
                expression { env.SONAR_ANALYSIS_READY == 'true' }
            }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Security Scans') {
            // Doc 5 sec 6, item 6: SCA + secrets scanning before a container is even built.
            parallel {
                stage('Dependency check (SCA)') {
                    steps {
                        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                            sh """
                              cd "\$WORKSPACE"
                              mkdir -p "\$ODC_DATA_DIR"
                              echo "SCA workspace: \$WORKSPACE"
                              echo "Dependency-Check data dir: \$ODC_DATA_DIR"
                              pwd
                              ls -la pom.xml
                              if [ -z "\${NVD_API_KEY:-}" ]; then
                                echo 'SKIPPED: NVD_API_KEY is not configured; set it to enable OWASP Dependency-Check with live NVD data.'
                                exit 0
                              fi
                              if [ -z "\$(find "\$ODC_DATA_DIR" -mindepth 1 -print -quit 2>/dev/null)" ]; then
                                echo 'Dependency-Check cache is empty.'
                                echo 'Run the Jenkins job dependency-check-cache-warmup once, or wait for its nightly schedule, then rerun this pipeline.'
                                exit 1
                              fi
                              echo "NVD_API_KEY length: \${#NVD_API_KEY}"
                              echo "NVD_API_KEY sha256 prefix: \$(printf '%s' \"\$NVD_API_KEY\" | sha256sum | cut -c1-12)"
                              mvn -f "\$WORKSPACE/pom.xml" -B -pl ${params.SERVICES} -am \
                                -DdataDirectory="\$ODC_DATA_DIR" \
                                -DautoUpdate=false \
                                org.owasp:dependency-check-maven:aggregate
                            """
                        }
                    }
                }
                stage('Secrets scan (Gitleaks)') {
                    steps { sh 'gitleaks detect --source . --no-git -v --redact=100 --exit-code 1' }
                }
                stage('IaC scan (Checkov)') {
                    steps {
                        sh '''
                          mkdir -p qa-automation/checkov
                          jenkins_container="${JENKINS_CONTAINER_NAME:-}"
                          if [ -n "$jenkins_container" ] && ! docker inspect "$jenkins_container" >/dev/null 2>&1; then
                            jenkins_container=""
                          fi
                          if [ -z "$jenkins_container" ]; then
                            jenkins_container="$(docker ps \
                              --filter label=com.docker.compose.service=jenkins \
                              --format '{{.Names}}' \
                              | head -n 1)"
                          fi
                          if [ -z "$jenkins_container" ]; then
                            echo "Unable to locate the running Jenkins container for Checkov volume mounting."
                            docker ps --format 'table {{.Names}}\t{{.Status}}'
                            exit 1
                          fi
                          docker run --rm --volumes-from "$jenkins_container" -w "$PWD" bridgecrew/checkov:latest \
                            --directory deploy \
                            --skip-check CKV_K8S_43 \
                            --output junitxml \
                            --output-file-path console,qa-automation/checkov/checkov-report.xml
                        '''
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, skipPublishingChecks: true, testResults: 'qa-automation/checkov/checkov-report.xml'
                    archiveArtifacts artifacts: 'qa-automation/checkov/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package & Scan Images') {
            steps {
                script {
                    def imageTag = env.IMAGE_TAG?.trim()
                    if (!imageTag) {
                        imageTag = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                        env.IMAGE_TAG = imageTag
                    }
                    params.SERVICES.split(',').each { svc ->
                        if (fileExists("${svc}/Dockerfile")) {
                            withEnv([
                                "SERVICE=${svc}",
                                "IMAGE_TAG=${imageTag}",
                                "DOCKER_BUILDKIT=1",
                                "COMPOSE_DOCKER_CLI_BUILD=1"
                            ]) {
                                sh '''
                                  set -eu
                                  docker build -f "$SERVICE/Dockerfile" -t "$REGISTRY/$SERVICE:$IMAGE_TAG" .
                                  trivy image --exit-code 1 --severity HIGH,CRITICAL "$REGISTRY/$SERVICE:$IMAGE_TAG"
                                '''
                            }
                            // Doc 5 sec 6, item 6 (continued): container image scanning.
                        }
                    }
                }
            }
        }

        stage('Push Images') {
            when { branch 'main' }
            steps {
                script {
                    def imageTag = env.IMAGE_TAG?.trim()
                    if (!imageTag) {
                        imageTag = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                        env.IMAGE_TAG = imageTag
                    }
                    params.SERVICES.split(',').each { svc ->
                        if (fileExists("${svc}/Dockerfile")) {
                            withEnv([
                                "SERVICE=${svc}",
                                "IMAGE_TAG=${imageTag}"
                            ]) {
                                sh '''
                                  set -eu
                                  docker push "$REGISTRY/$SERVICE:$IMAGE_TAG"
                                '''
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            // Doc 2 sec 3.9 promotion flow: merge -> Jenkins builds/tests/scans/packages ->
            // auto-deploy dev. QA/staging/prod promotion gates live in a separate downstream
            // pipeline (post-QA-automation, see Doc 2 sec 6) triggered off this one's success.
            when { branch 'main' }
            steps {
                script {
                    def imageTag = env.IMAGE_TAG?.trim()
                    if (!imageTag) {
                        imageTag = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                        env.IMAGE_TAG = imageTag
                    }
                    params.SERVICES.split(',').each { svc ->
                        if (fileExists("deploy/helm/values/${svc}-values.yaml")) {
                            withEnv([
                                "SERVICE=${svc}",
                                "IMAGE_TAG=${imageTag}"
                            ]) {
                                sh '''
                                  set -eu
                                  helm upgrade --install "$SERVICE" deploy/helm/microservice-chart \
                                    -f "deploy/helm/values/$SERVICE-values.yaml" \
                                    --set "image.tag=$IMAGE_TAG" \
                                    --namespace dev --kube-context dev-cluster
                                '''
                            }
                        }
                    }
                }
            }
        }

        stage('QA Automation (Dev)') {
            // Doc 5 sec 6, items 2-4: backend/API, frontend, and QA/E2E automation run against
            // the environment just deployed above. See qa-automation/README.md for the full
            // toolchain writeup (Playwright + REST-Assured + Karate + Pact + Detox).
            when {
                anyOf {
                    branch 'main'
                    expression { return (env.DEV_GATEWAY_URL ?: '').trim() }
                }
            }
            environment {
                // Same dev namespace the "Deploy to Dev" stage above just rolled out to.
                // Override these per-environment (e.g. via Jenkins job/folder-level env vars)
                // if the dev gateway/web app aren't reachable at these defaults from the agent.
                GATEWAY_URL = "${env.DEV_GATEWAY_URL ?: 'http://api-gateway.dev.svc.cluster.local:8080'}"
                LOYALTY_URL = "${env.DEV_LOYALTY_URL ?: 'http://loyalty-service.dev.svc.cluster.local:8082'}"
                REPORTS_URL = "${env.DEV_REPORTS_URL ?: 'http://reports-service.dev.svc.cluster.local:8083'}"
                NOTIFICATION_URL = "${env.DEV_NOTIFICATION_URL ?: 'http://notification-service.dev.svc.cluster.local:8084'}"
                CONTENT_URL = "${env.DEV_CONTENT_URL ?: 'http://content-service.dev.svc.cluster.local:8085'}"
                CONFIG_TENANT_URL = "${env.DEV_CONFIG_TENANT_URL ?: 'http://config-tenant-service.dev.svc.cluster.local:8081'}"
                WEB_BASE_URL = "${env.DEV_WEB_BASE_URL ?: 'http://web-app.dev.svc.cluster.local:3000'}"
            }
            steps {
                script {
                    def useLocalComposeQaStack =
                        (env.QA_BOOTSTRAP_LOCAL_STACK ?: '').trim().toBoolean() ||
                        (env.DEV_GATEWAY_URL ?: '').contains('host.docker.internal')

                    if (useLocalComposeQaStack) {
                        sh '''
                          set -eu
                          mkdir -p qa-automation/local-stack-logs
                          docker compose -p microservices --profile app up -d --build

                          wait_for_http() {
                            expected="$1"
                            name="$2"
                            url="$3"
                            shift 3
                            output_file="qa-automation/local-stack-logs/${name}.out"
                            attempt=1
                            max_attempts=24

                            while [ "$attempt" -le "$max_attempts" ]; do
                              code="$(curl -ksS -o "$output_file" -w '%{http_code}' "$@" "$url" || true)"
                              if [ "$code" = "$expected" ]; then
                                echo "[qa-ready] ${name} is ready at ${url} (HTTP ${code})"
                                return 0
                              fi

                              echo "[qa-ready] waiting for ${name} at ${url}: got HTTP ${code} (attempt ${attempt}/${max_attempts})"
                              sleep 5
                              attempt=$((attempt + 1))
                            done

                            echo "[qa-ready] timed out waiting for ${name} at ${url}; last response:"
                            cat "$output_file" || true
                            docker compose -p microservices ps || true
                            return 1
                          }

                          wait_for_http 200 gateway-liveness "$GATEWAY_URL/actuator/health/liveness"
                          wait_for_http 200 content-health "$CONTENT_URL/actuator/health"
                          wait_for_http 200 config-health "$CONFIG_TENANT_URL/actuator/health"
                          wait_for_http 200 loyalty-liveness "$LOYALTY_URL/actuator/health/liveness"
                          wait_for_http 200 reports-liveness "$REPORTS_URL/actuator/health/liveness"
                          wait_for_http 200 notification-liveness "$NOTIFICATION_URL/actuator/health/liveness"

                          wait_for_http 200 gateway-content "$GATEWAY_URL/api/v1/content/articles?category=billing" \
                            -H "X-Tenant-Id: ${TEST_TENANT_ID}"
                          wait_for_http 400 gateway-loyalty-validation "$GATEWAY_URL/api/v1/loyalty/register" \
                            -X POST \
                            -H "Content-Type: application/json" \
                            -H "X-Tenant-Id: ${TEST_TENANT_ID}" \
                            --data '{"msisdn":"94771234567"}'
                          wait_for_http 404 gateway-reports-not-found "$GATEWAY_URL/api/v1/reports/requests/999999999" \
                            -H "X-Tenant-Id: ${TEST_TENANT_ID}"
                        '''
                    }

                    parallel(
                        'API (REST-Assured)': {
                            dir('qa-automation/api') {
                                sh 'mvn -B test'
                            }
                        },
                        'API Contracts (Karate)': {
                            dir('qa-automation/karate') {
                                sh 'mvn -B test'
                            }
                        },
                        'Consumer Contracts (Pact)': {
                            dir('qa-automation/pact') {
                                sh 'mvn -B test'
                            }
                        },
                        'Web (Playwright)': {
                            dir('qa-automation/web') {
                                withEnv(['NODE_OPTIONS=--dns-result-order=ipv4first']) {
                                    sh '''
                                      if [ -f package-lock.json ]; then
                                        npm ci
                                      else
                                        npm install
                                      fi
                                      npx playwright install --with-deps chromium
                                      npm test
                                    '''
                                }
                            }
                        },
                        'Mobile (Detox)': {
                            // No React Native app exists in this repo yet -- see
                            // qa-automation/mobile/README.md for activation steps. Left as an
                            // explicit no-op (not a silently-skipped stage) so it's obvious in
                            // the Blue Ocean UI that this leg is intentionally not wired up yet.
                            echo 'SKIPPED: no React Native app in this repo yet -- see qa-automation/mobile/README.md.'
                        }
                    )
                }
            }
            post {
                failure {
                    script {
                        def useLocalComposeQaStack =
                            (env.QA_BOOTSTRAP_LOCAL_STACK ?: '').trim().toBoolean() ||
                            (env.DEV_GATEWAY_URL ?: '').contains('host.docker.internal')

                        if (useLocalComposeQaStack) {
                            sh '''
                              mkdir -p qa-automation/local-stack-logs
                              docker compose -p microservices ps > qa-automation/local-stack-logs/compose-ps.txt || true
                              for svc in api-gateway loyalty-service reports-service notification-service content-service config-tenant-service; do
                                docker compose -p microservices logs --tail=200 "$svc" > "qa-automation/local-stack-logs/${svc}.log" || true
                              done
                            '''
                        }
                    }
                }
                always {
                    junit allowEmptyResults: true, skipPublishingChecks: true, testResults: 'qa-automation/**/target/surefire-reports/*.xml'
                    sh '''
                      npm install -g allure-commandline --silent || true
                      allure generate qa-automation/api/target/allure-results qa-automation/web/allure-results \
                        -o qa-automation/allure-report --clean || true
                    '''
                    archiveArtifacts artifacts: 'qa-automation/web/playwright-report/**, qa-automation/allure-report/**, qa-automation/pact/target/pacts/**, qa-automation/local-stack-logs/**', allowEmptyArchive: true
                }
            }
        }

        stage('DAST (ZAP Baseline)') {
            when {
                anyOf {
                    allOf {
                        branch 'main'
                        expression { return (env.DEV_DAST_BASE_URL ?: env.DEV_GATEWAY_URL ?: '').trim() }
                    }
                    expression { return (env.DEV_DAST_BASE_URL ?: '').trim() }
                }
            }
            environment {
                DAST_BASE_URL = "${env.DEV_DAST_BASE_URL ?: env.DEV_GATEWAY_URL}"
            }
            steps {
                script {
                    def exitCode = sh(returnStatus: true, script: '''
                      set -eu
                      mkdir -p qa-automation/zap
                      # This Jenkins agent is itself a container sharing the host's docker.sock,
                      # so a bind mount like "-v $PWD:/zap/wrk" is resolved by the DAEMON against
                      # ITS OWN filesystem, not this container's -- $PWD only exists inside the
                      # jenkins-home named volume, so the daemon silently mounts an empty
                      # directory and zap-baseline.py fails with "No such file or directory:
                      # /zap/wrk/rules.tsv". docker cp sidesteps this entirely: it streams file
                      # content through the Docker API rather than resolving a host path, so it
                      # works the same regardless of where the CLI process runs. -v /zap/wrk
                      # (no host source) gives the container a real anonymous-volume mountpoint
                      # there, which zap-baseline.py requires before it'll write any file-based
                      # options; --user root avoids a permission error writing the report once
                      # docker cp (running as root) has touched that volume.
                      docker rm -f zap-baseline-scan >/dev/null 2>&1 || true
                      docker create --name zap-baseline-scan --network host --user root -v /zap/wrk \
                        ghcr.io/zaproxy/zaproxy:stable \
                        zap-baseline.py -t "$DAST_BASE_URL" -c rules.tsv \
                        -J zap-report.json -r zap-report.html -w zap-report.md -m 5
                      docker cp qa-automation/zap/rules.tsv zap-baseline-scan:/zap/wrk/rules.tsv

                      set +e
                      docker start -a zap-baseline-scan
                      scan_exit=$?
                      set -e

                      docker cp zap-baseline-scan:/zap/wrk/zap-report.json qa-automation/zap/zap-report.json || true
                      docker cp zap-baseline-scan:/zap/wrk/zap-report.html qa-automation/zap/zap-report.html || true
                      docker cp zap-baseline-scan:/zap/wrk/zap-report.md qa-automation/zap/zap-report.md || true
                      docker rm -f zap-baseline-scan >/dev/null 2>&1 || true
                      exit $scan_exit
                    ''')

                    // zap-baseline.py: 0 = clean, 1 = at least one FAIL, 2 = WARN(s) only, no
                    // FAIL. Only block the pipeline on real FAILs -- WARN-level findings are
                    // meant to be visible in the archived report for triage, not build-breaking.
                    if (exitCode == 1) {
                        error("ZAP baseline scan found FAIL-level issues -- see the archived qa-automation/zap report.")
                    } else if (exitCode != 0 && exitCode != 2) {
                        error("ZAP baseline scan failed unexpectedly (exit code ${exitCode}) -- see the archived qa-automation/zap report.")
                    } else if (exitCode == 2) {
                        echo "ZAP baseline scan found WARN-level issues only (exit code 2) -- not failing the build, see the archived qa-automation/zap report."
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'qa-automation/zap/**', allowEmptyArchive: true
                }
            }
        }

        stage('Performance Test (Dev)') {
            // Doc 5 sec 6, item 7.
            when {
                anyOf {
                    branch 'main'
                    expression { return (env.DEV_GATEWAY_URL ?: '').trim() }
                }
            }
            environment {
                PERF_BASE_URL = "${env.DEV_GATEWAY_URL ?: 'http://api-gateway.dev.svc.cluster.local:8080'}"
                PERF_TENANT_ID = "${env.TEST_TENANT_ID ?: 'acme-telecom'}"
            }
            steps {
                sh '''
                  mkdir -p qa-automation/performance/results
                  k6 run \
                    --summary-export qa-automation/performance/results/k6-summary.json \
                    -e BASE_URL="$PERF_BASE_URL" \
                    -e TENANT_ID="$PERF_TENANT_ID" \
                    qa-automation/performance/gateway-load.js \
                    | tee qa-automation/performance/results/k6-console.log
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'qa-automation/performance/results/**', allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            // Doc 2 sec 3.10: release notes are generated, never hand-written.
            script {
                if (env.BRANCH_NAME == 'main') {
                    sh 'npx conventional-changelog -p conventionalcommits -i CHANGELOG.md -s || true'
                }
            }
        }
        failure {
            // Doc 2 sec 4: a failed pipeline is exactly the kind of thing that should be
            // visible immediately -- wire this to the same channel Sentry/Grafana alerts use.
            echo 'TODO: notify the on-call channel (Doc 2 sec 4) on pipeline failure.'
        }
    }
}
