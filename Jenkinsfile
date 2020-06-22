pipeline {
    agent {
      docker {
        image 'maven:3.6.3-jdk-8'
      }
    }
  
    options {
      buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '1'))
    }
  
    triggers {
      cron '@midnight'
    }
  
    stages {
      stage('build') {
        steps {
          script {
            def phase = env.BRANCH_NAME == 'release/7.0' ? 'deploy' : 'verify'
            maven cmd: "clean ${phase}"
          }
          junit testDataPublishers: [[$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
          archiveArtifacts '**/target/*.jar'
        }      
      }
    }
 }
