pipeline {
  agent {
    docker {
      image 'maven:3.5.4-jdk-8'
    }
  }
  triggers {
    pollSCM '@hourly'
    cron '@midnight'
  }
  stages {
    stage('build and deploy') {
      steps {
        script {
          maven cmd: 'clean deploy -Dmaven.test.failure.ignore=true'
        }
      }
      post {
        success {
          junit '**/target/surefire-reports/**/*.xml' 
        }
      }
    }
  }
}
