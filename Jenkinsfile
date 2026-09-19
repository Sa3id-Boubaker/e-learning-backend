pipeline {
    agent any

    environment {
        SERVICES = 'eureka-server api-gateway user-service course-service training-service forum-service notification-service'
        MONGO_DB_USER = credentials('mongo-db-user')
        MONGO_DB_PASSWORD = credentials('mongo-db-password')
        MAIL_USERNAME = credentials('mail-username')
        MAIL_PASSWORD = credentials('mail-password')
        JWT_SECRET = credentials('jwt-secret')
        CLOUDINARY_CLOUD_NAME = credentials('cloudinary-cloud-name')
        CLOUDINARY_API_KEY = credentials('cloudinary-api-key')
        CLOUDINARY_API_SECRET = credentials('cloudinary-api-secret')
        DOCKER_BUILDKIT = '1'
        REGISTRY = 'ghcr.io'
        REGISTRY_NAMESPACE = 'sa3id-boubaker'
        KUBE_NAMESPACE = 'omarise'
        DEPLOY_ORDER = 'eureka-server user-service course-service training-service forum-service notification-service api-gateway'
    }

    stages {
        stage('Verify Environment') {
            steps {
                sh 'java -version'
                script {
                    env.SERVICES.split(' ').each { svc ->
                        sh "test -f ${svc}/mvnw && echo '${svc}: mvnw OK' || (echo '${svc}: mvnw MISSING' && exit 1)"
                    }
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    env.SERVICES.split(' ').each { svc ->
                        dir(svc) {
                            sh 'chmod +x mvnw'
                            sh './mvnw clean compile -B -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    env.SERVICES.split(' ').each { svc ->
                        dir(svc) {
                            sh './mvnw test -B'
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis & Quality Gate') {
            steps {
                script {
                    env.SERVICES.split(' ').each { svc ->
                        dir(svc) {
                            withSonarQubeEnv('SonarQube-Local') {
                                sh "./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar -Dsonar.projectKey=omarise-${svc}"
                            }
                            timeout(time: 5, unit: 'MINUTES') {
                                waitForQualityGate abortPipeline: true
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker version'
                script {
                    env.SERVICES.split(' ').each { svc ->
                        dir(svc) {
                            sh "docker build -t omarise-${svc}:${env.BUILD_NUMBER} ."
                        }
                    }
                }
                sh "docker images --filter=reference='omarise-*'"
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'omarise-docker-registry', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_TOKEN')]) {
                    sh 'echo "$REGISTRY_TOKEN" | docker login "$REGISTRY" -u "$REGISTRY_USER" --password-stdin'
                }
            }
        }

        stage('Docker Push') {
            steps {
                script {
                    env.SERVICES.split(' ').each { svc ->
                        sh "docker tag omarise-${svc}:${env.BUILD_NUMBER} ${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/omarise-${svc}:${env.BUILD_NUMBER}"
                        sh "docker push ${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/omarise-${svc}:${env.BUILD_NUMBER}"
                    }
                }
                sh "docker images --filter=reference='${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/omarise-*'"
            }
        }

        stage('Cleanup Old GHCR Versions') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'omarise-docker-registry', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_TOKEN')]) {
                    script {
                        env.SERVICES.split(' ').each { svc ->
                            withEnv(["PACKAGE_NAME=omarise-${svc}"]) {
                                sh 'chmod +x scripts/cleanup-ghcr-package.sh && ./scripts/cleanup-ghcr-package.sh || true'
                            }
                        }
                    }
                }
            }
        }

        stage('Kubernetes Deploy') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-minikube', variable: 'KUBECONFIG')]) {
                    sh 'kubectl version --client'
                    sh "kubectl get namespace ${env.KUBE_NAMESPACE}"
                    sh "kubectl get secret ghcr-pull-secret -n ${env.KUBE_NAMESPACE}"
                    script {
                        // Deploy ONE service at a time and wait for its rollout before moving
                        // to the next. Updating all 7 Deployments at once was tried first and
                        // caused every new pod to crash-restart under liveness-probe timeouts —
                        // 7 simultaneous rolling updates (each briefly running old+new pod side
                        // by side) overloaded this single-node Minikube VM, the same class of
                        // CPU-contention issue seen earlier with RabbitMQ during manual Kubernetes
                        // setup. Going one service at a time keeps only one extra pod starting up
                        // at any given moment.
                        env.DEPLOY_ORDER.split(' ').each { svc ->
                            sh "kubectl set image deployment/${svc} ${svc}=${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/omarise-${svc}:${env.BUILD_NUMBER} -n ${env.KUBE_NAMESPACE}"
                            try {
                                sh "kubectl rollout status deployment/${svc} -n ${env.KUBE_NAMESPACE} --timeout=180s"
                                // Keep the k8s/ manifest in sync with what's actually running, so a
                                // future bootstrap of a fresh cluster (kubectl apply -f k8s/) never
                                // points at a tag that Cleanup Old GHCR Versions has since deleted
                                // (only the last KEEP_VERSIONS=3 tagged images are kept per service).
                                sh "sed -i \"s#image: ${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/omarise-${svc}:.*#image: ${env.REGISTRY}/${env.REGISTRY_NAMESPACE}/omarise-${svc}:${env.BUILD_NUMBER}#\" k8s/${svc}/deployment.yaml"
                            } catch (err) {
                                echo "Rollout failed for ${svc} — collecting diagnostics for this component only."
                                sh """
                                    set +e
                                    echo '--- kubectl get pods -n ${env.KUBE_NAMESPACE} ---'
                                    kubectl get pods -n ${env.KUBE_NAMESPACE}
                                    echo '--- kubectl describe deployment/${svc} -n ${env.KUBE_NAMESPACE} ---'
                                    kubectl describe deployment/${svc} -n ${env.KUBE_NAMESPACE}
                                    echo '--- kubectl describe pods -l app=${svc} -n ${env.KUBE_NAMESPACE} ---'
                                    kubectl describe pods -l app=${svc} -n ${env.KUBE_NAMESPACE}
                                    echo '--- kubectl logs deployment/${svc} -n ${env.KUBE_NAMESPACE} --tail=100 ---'
                                    kubectl logs deployment/${svc} -n ${env.KUBE_NAMESPACE} --tail=100
                                """
                                error("Kubernetes rollout failed for ${svc}")
                            }
                        }
                    }
                }
            }
        }

        stage('Kubernetes Rollout Verification') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-minikube', variable: 'KUBECONFIG')]) {
                    sh "kubectl get deployments -n ${env.KUBE_NAMESPACE}"
                    sh "kubectl get pods -n ${env.KUBE_NAMESPACE}"
                    script {
                        // Final cross-check: every Deployment this build touched must still show
                        // its Deployment-wide desired replica count as ready, confirming nothing
                        // regressed after the sequential deploy above.
                        env.DEPLOY_ORDER.split(' ').each { svc ->
                            def ready = sh(script: "kubectl get deployment/${svc} -n ${env.KUBE_NAMESPACE} -o jsonpath='{.status.readyReplicas}'", returnStdout: true).trim()
                            def desired = sh(script: "kubectl get deployment/${svc} -n ${env.KUBE_NAMESPACE} -o jsonpath='{.spec.replicas}'", returnStdout: true).trim()
                            if (ready == '' || ready != desired) {
                                error("Deployment ${svc} is not fully ready (ready=${ready}, desired=${desired})")
                            }
                        }
                    }
                }
            }
        }

        stage('Sync k8s Manifests to Git') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'github-ssh-omarise-backend', keyFileVariable: 'SSH_KEY')]) {
                    sh '''
                        set -e
                        export GIT_SSH_COMMAND="ssh -i $SSH_KEY -o StrictHostKeyChecking=accept-new"
                        git config user.email "jenkins-ci@omarise.local"
                        git config user.name "Jenkins CI"
                        if git diff --quiet -- k8s/; then
                            echo "k8s/ deja a jour, rien a committer."
                        else
                            git add k8s/
                            git commit -m "chore(k8s): sync image tags to build ${BUILD_NUMBER}"
                            git push origin HEAD:dev
                        fi
                    '''
                }
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            sh 'docker logout "$REGISTRY" || true'
        }
    }
}
