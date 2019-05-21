pipeline {
  agent {
    docker {
      image 'maven:3.5.2-jdk-8'
    }
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '5'))
  }
  triggers {
    pollSCM '@hourly'
    cron '@midnight'
  }
  stages {
    stage('build and deploy') {
      steps {
        script {
          maven cmd: 'deploy -Dmaven.test.failure.ignore=true'
        }
        junit '**/target/surefire-reports/**/*.xml' 
        archiveArtifacts '**/target/*.jar'
      }      
    }
  }
}
