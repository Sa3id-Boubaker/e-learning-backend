pipeline {
    agent any

    triggers {
        // Auto-start this pipeline whenever GitHub sends a push webhook for
        // this repo (relayed to Jenkins via the smee.io channel + local
        // smee-client container, since this Jenkins is not publicly
        // reachable). Requires the GitHub plugin's /github-webhook/
        // endpoint, already part of Jenkins' suggested plugin set.
        githubPush()
    }

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
        // Phase 8 — monitoring verification. Names confirmed during the Phase 7.2/7.3
        // audit (k8s/monitoring/grafana-values.yaml header) — not assumed.
        MONITORING_NAMESPACE = 'monitoring'
        PROMETHEUS_SVC = 'prometheus-server'
        PROMETHEUS_SVC_PORT = '80'
        GRAFANA_SVC = 'grafana'
        DASHBOARD_CONFIGMAP = 'omarise-dashboard'
    }

    stages {
        stage('Pipeline') {
            when {
                // Skip entirely when the triggering commit is this pipeline's
                // own automated k8s-manifest-sync push (see 'Sync k8s Manifests
                // to Git' below) - otherwise that push would itself fire the
                // githubPush() trigger above and start another build, forever
                // (each build produces a new tag, deploys it, and pushes another
                // sync commit that would trigger the next one).
                not {
                    expression {
                        return sh(
                            script: "git log -1 --pretty=%B | grep -qF '[skip ci]'",
                            returnStatus: true
                        ) == 0
                    }
                }
            }
            stages {
            stage('CI') {
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
            }
            }

            stage('CD') {
            stages {
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

            stage('Monitoring Verification') {
                steps {
                    withCredentials([file(credentialsId: 'kubeconfig-minikube', variable: 'KUBECONFIG')]) {
                        timeout(time: 3, unit: 'MINUTES') {
                            sh '''
                                set -u
                                echo "Monitoring Verification"
                                echo "-----------------------"

                                FAIL=0

                                # 1. Kubernetes cluster access (same kubeconfig already used above).
                                if ! kubectl get namespace "$MONITORING_NAMESPACE" >/dev/null 2>&1; then
                                    echo "Kubernetes cluster access: FAILED (namespace $MONITORING_NAMESPACE unreachable)"
                                    exit 1
                                fi

                                # 2. Prometheus Service exists (name/namespace/port confirmed during the
                                # Phase 7.2/7.3 audit, see k8s/monitoring/grafana-values.yaml header).
                                if ! kubectl get svc "$PROMETHEUS_SVC" -n "$MONITORING_NAMESPACE" >/dev/null 2>&1; then
                                    echo "Prometheus service '$PROMETHEUS_SVC' not found in namespace '$MONITORING_NAMESPACE'"
                                    exit 1
                                fi

                                # Helper: is a Deployment's ready replica count equal to its desired count?
                                # Same pattern already used above in Kubernetes Rollout Verification.
                                deployment_ready() {
                                    d_ready=$(kubectl get deployment "$1" -n "$2" -o jsonpath='{.status.readyReplicas}' 2>/dev/null)
                                    d_desired=$(kubectl get deployment "$1" -n "$2" -o jsonpath='{.spec.replicas}' 2>/dev/null)
                                    [ -n "$d_ready" ] && [ "$d_ready" = "$d_desired" ]
                                }

                                # 3. Prometheus Deployment Ready, short retry window for a transient
                                # scheduling delay right after this build's own deploy.
                                PROM_READY="false"
                                i=1
                                while [ "$i" -le 3 ]; do
                                    if deployment_ready "$PROMETHEUS_SVC" "$MONITORING_NAMESPACE"; then
                                        PROM_READY="true"
                                        break
                                    fi
                                    echo "Prometheus deployment not ready yet (attempt $i/3), retrying in 10s..."
                                    i=$((i+1))
                                    sleep 10
                                done
                                if [ "$PROM_READY" = "true" ]; then
                                    echo "Prometheus: READY"
                                else
                                    echo "Prometheus: NOT READY"
                                    FAIL=1
                                fi

                                # 4/5. Prometheus reachable + the 7 expected OMARISE targets UP.
                                # Queried through the Kubernetes API server's Service proxy subresource
                                # (kubectl get --raw .../proxy/...) rather than a direct HTTP call, so
                                # this only needs the same API-server access every other kubectl command
                                # in this pipeline already uses — no direct network route from the
                                # Jenkins agent into the cluster pod network is required.
                                #
                                # Target label names (job=kubernetes-service-endpoints, app=<service>,
                                # namespace=omarise) were inspected directly in Prometheus/Grafana
                                # Explore during Phase 7.2/7.3 — not assumed here.
                                UP_COUNT=0
                                UP_LIST=""
                                MISSING_LIST=""
                                ATTEMPTS=6
                                SLEEP_SECONDS=15
                                attempt=1
                                while [ "$attempt" -le "$ATTEMPTS" ]; do
                                    RAW=$(kubectl get --raw "/api/v1/namespaces/$MONITORING_NAMESPACE/services/$PROMETHEUS_SVC:$PROMETHEUS_SVC_PORT/proxy/api/v1/targets?state=active" 2>/dev/null || true)

                                    UP_COUNT=0
                                    UP_LIST=""
                                    MISSING_LIST=""
                                    if [ -n "$RAW" ]; then
                                        # Dependency-free split: strip quotes, then one target object per
                                        # line. Prometheus only emits a "},{ " sequence at activeTargets
                                        # array boundaries in this response shape — nested objects like
                                        # "labels":{...} are always followed by ,"<nextkey>", never by
                                        # another top-level target object, so this split is safe here.
                                        FLAT=$(printf '%s' "$RAW" | tr -d '"' | sed 's/},{/}\\n{/g')
                                        for svc in $SERVICES; do
                                            LINE=$(printf '%s\\n' "$FLAT" | grep -E "app:$svc[,}]" | grep -E "namespace:$KUBE_NAMESPACE[,}]" || true)
                                            if [ -n "$LINE" ] && printf '%s' "$LINE" | grep -q "health:up"; then
                                                UP_COUNT=$((UP_COUNT+1))
                                                UP_LIST="$UP_LIST $svc"
                                            else
                                                MISSING_LIST="$MISSING_LIST $svc"
                                            fi
                                        done
                                    fi

                                    if [ "$UP_COUNT" -eq 7 ]; then
                                        break
                                    fi
                                    echo "OMARISE targets: $UP_COUNT/7 UP (attempt $attempt/$ATTEMPTS) - retrying in ${SLEEP_SECONDS}s..."
                                    attempt=$((attempt+1))
                                    sleep "$SLEEP_SECONDS"
                                done

                                echo "OMARISE targets: $UP_COUNT/7 UP"
                                if [ "$UP_COUNT" -ne 7 ]; then
                                    echo "Missing/DOWN OMARISE targets:$MISSING_LIST"
                                    FAIL=1
                                fi

                                # 6. Grafana Deployment Ready, same short retry window.
                                GRAF_READY="false"
                                i=1
                                while [ "$i" -le 3 ]; do
                                    if deployment_ready "$GRAFANA_SVC" "$MONITORING_NAMESPACE"; then
                                        GRAF_READY="true"
                                        break
                                    fi
                                    echo "Grafana deployment not ready yet (attempt $i/3), retrying in 10s..."
                                    i=$((i+1))
                                    sleep 10
                                done
                                if [ "$GRAF_READY" = "true" ]; then
                                    echo "Grafana: READY"
                                else
                                    echo "Grafana: NOT READY"
                                    FAIL=1
                                fi

                                # 7. Grafana Service exists.
                                if ! kubectl get svc "$GRAFANA_SVC" -n "$MONITORING_NAMESPACE" >/dev/null 2>&1; then
                                    echo "Grafana service missing"
                                    FAIL=1
                                fi

                                # 7 (cont). Datasource provisioning file still mounted in the running
                                # Grafana container (provisioned from k8s/monitoring/grafana-values.yaml's
                                # "datasources:" block via the chart's default provisioning mechanism).
                                # This only checks the file is PRESENT - it never reads/prints its
                                # contents, so no datasource URL or credential is ever logged.
                                if kubectl exec -n "$MONITORING_NAMESPACE" "deploy/$GRAFANA_SVC" -c grafana -- test -f /etc/grafana/provisioning/datasources/datasources.yaml >/dev/null 2>&1; then
                                    echo "Grafana datasource provisioning: PRESENT"
                                else
                                    echo "Grafana datasource provisioning: MISSING"
                                    FAIL=1
                                fi

                                # 8. Existing dashboard provisioning ConfigMap (sidecar mechanism from
                                # Phase 7.3) still present with its expected label. Does not create or
                                # modify any dashboard.
                                # NOTE: "kubectl get <type> <name> -l <selector>" is rejected by
                                # kubectl ("name cannot be provided when a selector is specified") -
                                # confirmed empirically against the real cluster in build #42.
                                # Existence-by-name and the label value are therefore checked as two
                                # separate, independent kubectl calls.
                                if kubectl get configmap "$DASHBOARD_CONFIGMAP" -n "$MONITORING_NAMESPACE" >/dev/null 2>&1; then
                                    DASHBOARD_LABEL=$(kubectl get configmap "$DASHBOARD_CONFIGMAP" -n "$MONITORING_NAMESPACE" -o jsonpath='{.metadata.labels.grafana_dashboard}' 2>/dev/null)
                                    if [ "$DASHBOARD_LABEL" = "1" ]; then
                                        echo "Dashboard provisioning ConfigMap ($DASHBOARD_CONFIGMAP): PRESENT"
                                    else
                                        echo "Dashboard provisioning ConfigMap ($DASHBOARD_CONFIGMAP): PRESENT but missing/wrong grafana_dashboard label (found: '$DASHBOARD_LABEL')"
                                        FAIL=1
                                    fi
                                else
                                    echo "Dashboard provisioning ConfigMap ($DASHBOARD_CONFIGMAP): MISSING"
                                    FAIL=1
                                fi

                                # 8 (cont). The dashboard JSON file in this repo still exists and is
                                # valid JSON. Repo-content check only - does not touch Grafana itself.
                                DASHBOARD_FILE="k8s/monitoring/omarise-dashboard.json"
                                if [ ! -f "$DASHBOARD_FILE" ]; then
                                    echo "Dashboard file ($DASHBOARD_FILE): MISSING"
                                    FAIL=1
                                elif command -v jq >/dev/null 2>&1; then
                                    if jq empty "$DASHBOARD_FILE" >/dev/null 2>&1; then
                                        echo "Dashboard file ($DASHBOARD_FILE): VALID JSON"
                                    else
                                        echo "Dashboard file ($DASHBOARD_FILE): INVALID JSON"
                                        FAIL=1
                                    fi
                                elif command -v python3 >/dev/null 2>&1; then
                                    if python3 -c "import json;json.load(open('$DASHBOARD_FILE'))" >/dev/null 2>&1; then
                                        echo "Dashboard file ($DASHBOARD_FILE): VALID JSON"
                                    else
                                        echo "Dashboard file ($DASHBOARD_FILE): INVALID JSON"
                                        FAIL=1
                                    fi
                                else
                                    echo "Dashboard file ($DASHBOARD_FILE): present, JSON validity not verified (no jq/python3 on this agent)"
                                fi

                                echo "-----------------------"
                                if [ "$FAIL" -eq 0 ]; then
                                    echo "Monitoring verification: SUCCESS"
                                else
                                    echo "Monitoring verification: FAILED"
                                    exit 1
                                fi
                            '''
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
                                git commit -m "chore(k8s): sync image tags to build ${BUILD_NUMBER} [skip ci]"

                                # dev can move while Kubernetes Deploy above was running (several
                                # minutes, sequential rollouts) - e.g. another build's own sync
                                # commit landing first. Retry the push a few times, rebasing this
                                # one sync commit onto the latest dev each time, instead of failing
                                # an otherwise-successful deployment over a losing race.
                                ATTEMPT=1
                                MAX_ATTEMPTS=5
                                until git push origin HEAD:dev; do
                                    if [ "$ATTEMPT" -ge "$MAX_ATTEMPTS" ]; then
                                        echo "git push failed after $MAX_ATTEMPTS attempts - giving up."
                                        exit 1
                                    fi
                                    echo "Push rejected (dev moved), rebasing and retrying (attempt $ATTEMPT/$MAX_ATTEMPTS)..."
                                    git fetch origin dev
                                    if ! git rebase origin/dev; then
                                        git rebase --abort
                                        echo "Rebase conflict while syncing k8s/ onto dev - manual resolution needed."
                                        exit 1
                                    fi
                                    ATTEMPT=$((ATTEMPT+1))
                                    sleep 3
                                done
                            fi
                        '''
                    }
                }
            }
            }
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
