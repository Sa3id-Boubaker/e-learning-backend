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
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        }
    }
}