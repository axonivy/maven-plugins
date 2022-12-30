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
    stage('build and deploy') {
      steps {
        script {
          if (env.BRANCH_NAME == 'master') {
            maven cmd: 'deploy sonar:sonar -Dsonar.host.url=https://sonar.ivyteam.io -Dsonar.projectKey=maven-plugins -Dsonar.projectName=maven-plugins'
          } else {
            maven cmd: 'verify'
          }
        }

        recordIssues tools: [eclipse()], unstableTotalAll: 1
        recordIssues tools: [mavenConsole()]
        junit testDataPublishers: [[$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
        archiveArtifacts '**/target/*.jar'
      }
    }
  }
}
