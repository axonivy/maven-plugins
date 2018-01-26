@Library('ivy-jenkins-shared-libraries') _

pipeline {
  agent {
    docker {
      image 'maven:3.5.2-jdk-8'
    }
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
