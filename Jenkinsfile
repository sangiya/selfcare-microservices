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
        SONAR_TOKEN = credentials('sonarqube-token')
        NVD_API_KEY = credentials('nvd-api-key')
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build & Unit Test') {
            // Doc 5 sec 6, item 1: unit testing, coverage-gated (jacoco-check in the parent
            // pom fails the build below the threshold -- this stage's failure IS the gate).
            steps {
                sh "mvn -B -pl ${params.SERVICES} -am test"
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco execPattern: '**/target/jacoco.exec'
                }
            }
        }

        stage('Code Quality') {
            // Doc 5 sec 6, item 5.
            steps {
                withSonarQubeEnv('sonarqube') {
                    // Call the Sonar Maven plugin by full coordinates so CI does not depend on
                    // Maven prefix discovery for the non-Apache `sonar` plugin group.
                    sh "mvn -B -pl ${params.SERVICES} -am org.sonarsource.scanner.maven:sonar-maven-plugin:sonar"
                }
            }
        }

        stage('Quality Gate') {
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
                              if [ -z "\${NVD_API_KEY:-}" ]; then
                                echo 'SKIPPED: NVD_API_KEY is not configured; set it to enable OWASP Dependency-Check with live NVD data.'
                                exit 0
                              fi
                              echo "NVD_API_KEY length: \${#NVD_API_KEY}"
                              echo "NVD_API_KEY sha256 prefix: \$(printf '%s' \"\$NVD_API_KEY\" | sha256sum | cut -c1-12)"
                              mvn -B -pl ${params.SERVICES} -am \
                                -DnvdApiKey="\$NVD_API_KEY" \
                                org.owasp:dependency-check-maven:check
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
                          docker run --rm --volumes-from "$HOSTNAME" -w "$PWD" bridgecrew/checkov:latest \
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
                    junit allowEmptyResults: true, testResults: 'qa-automation/checkov/checkov-report.xml'
                    archiveArtifacts artifacts: 'qa-automation/checkov/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package & Scan Images') {
            steps {
                script {
                    params.SERVICES.split(',').each { svc ->
                        if (fileExists("${svc}/Dockerfile")) {
                            sh "docker build -f ${svc}/Dockerfile -t ${REGISTRY}/${svc}:${env.GIT_COMMIT} ."
                            // Doc 5 sec 6, item 6 (continued): container image scanning.
                            sh "trivy image --exit-code 1 --severity HIGH,CRITICAL ${REGISTRY}/${svc}:${env.GIT_COMMIT}"
                        }
                    }
                }
            }
        }

        stage('Push Images') {
            when { branch 'main' }
            steps {
                script {
                    params.SERVICES.split(',').each { svc ->
                        if (fileExists("${svc}/Dockerfile")) {
                            sh "docker push ${REGISTRY}/${svc}:${env.GIT_COMMIT}"
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
                    params.SERVICES.split(',').each { svc ->
                        if (fileExists("deploy/helm/values/${svc}-values.yaml")) {
                            sh """
                              helm upgrade --install ${svc} deploy/helm/microservice-chart \
                                -f deploy/helm/values/${svc}-values.yaml \
                                --set image.tag=${env.GIT_COMMIT} \
                                --namespace dev --kube-context dev-cluster
                            """
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
                WEB_BASE_URL = "${env.DEV_WEB_BASE_URL ?: 'http://web-app.dev.svc.cluster.local:3000'}"
            }
            steps {
                script {
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
                                sh '''
                                  if [ -f package-lock.json ]; then
                                    npm ci
                                  else
                                    npm install
                                  fi
                                  npx playwright install --with-deps chromium firefox webkit
                                  npm test
                                '''
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
                always {
                    junit allowEmptyResults: true, testResults: 'qa-automation/**/target/surefire-reports/*.xml'
                    sh '''
                      npm install -g allure-commandline --silent || true
                      allure generate qa-automation/api/target/allure-results qa-automation/web/allure-results \
                        -o qa-automation/allure-report --clean || true
                    '''
                    archiveArtifacts artifacts: 'qa-automation/web/playwright-report/**, qa-automation/allure-report/**, qa-automation/pact/target/pacts/**', allowEmptyArchive: true
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
                sh '''
                  mkdir -p qa-automation/zap
                  docker run --rm --network host -v "$PWD/qa-automation/zap":/zap/wrk/:rw ghcr.io/zaproxy/zaproxy:stable \
                    zap-baseline.py -t "$DAST_BASE_URL" -c /zap/wrk/rules.tsv \
                    -J zap-report.json -r zap-report.html -w zap-report.md -m 5
                '''
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
