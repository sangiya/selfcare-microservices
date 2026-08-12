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

    parameters {
        string(name: 'SERVICES', defaultValue: 'platform-common,config-tenant-service,api-gateway,loyalty-service,reports-service,notification-service,content-service',
               description: 'Comma-separated modules to build this run (CI normally computes this from changed paths).')
    }

    environment {
        REGISTRY = credentials('container-registry-url')
        SONAR_TOKEN = credentials('sonarqube-token')
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
                    sh "mvn -B -pl ${params.SERVICES} -am sonar:sonar"
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
                    steps { sh "mvn -B -pl ${params.SERVICES} -am org.owasp:dependency-check-maven:check" }
                }
                stage('Secrets scan (Gitleaks)') {
                    steps { sh 'gitleaks detect --source . --no-git -v --exit-code 1' }
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
            // the environment just deployed above. Placeholder invocation -- point this at your
            // Playwright/RestAssured/Detox suites once they exist alongside the FE repos.
            when { branch 'main' }
            steps {
                echo 'TODO: invoke Playwright (web/admin), RestAssured (API), Detox (RN) suites against the dev environment just deployed; publish an Allure report.'
            }
        }

        stage('Performance Test (Dev)') {
            // Doc 5 sec 6, item 7.
            when { branch 'main' }
            steps {
                echo 'TODO: run k6 against each changed service, compare to its legacy-PHP baseline (Doc 5 sec 6, item 7).'
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
