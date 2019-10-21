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
          if (env.BRANCH_NAME == 'master')
          {
	          maven cmd: 'deploy sonar:sonar -Dsonar.host.url=http://zugprosonar '
	      }
          else
          {
		      maven cmd: 'verify'
          }
        }

        recordIssues tools: [eclipse()], unstableTotalAll: 1
        junit testDataPublishers: [[$class: 'StabilityTestDataPublisher']], testResults: '**/target/surefire-reports/**/*.xml'
        archiveArtifacts '**/target/*.jar'
      }      
    }
  }
}
