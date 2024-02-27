pipeline {
  agent {
    docker {
      image 'maven:3.6.3-jdk-11'
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
          if (env.BRANCH_NAME == 'release/8.0')
          {
	          maven cmd: 'deploy'
	        }
          else
          {
		        maven cmd: 'verify'
          }
        }

        recordIssues tools: [eclipse()], qualityGates: [[threshold: 1, type: 'TOTAL']]
        junit testDataPublishers: [[$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
        archiveArtifacts '**/target/*.jar'
      }      
    }
  }
}
