pipeline {
  agent {
    docker {
      image 'maven:3.8.6-eclipse-temurin-17'
    }
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '5'))
  }

  triggers {
    cron '@midnight'
  }

  stages {
    stage('build') {
      steps {
        script {
          def phase = env.BRANCH_NAME == 'master' || env.BRANCH_NAME.startsWith("release/") ? 'deploy' : 'verify'
          maven cmd: phase
        }
        recordIssues tools: [eclipse()], unstableTotalAll: 1
        recordIssues tools: [mavenConsole()]
        junit testDataPublishers: [[$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
        archiveArtifacts '**/target/*.jar'
      }
    }
  }
}
