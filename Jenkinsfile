pipeline {
  agent {
    docker {
      image 'maven:3.5.0-jdk-8'
    }
    
  }
  stages {
    stage('build') {
      steps {
        sh 'mvn -Dmaven.test.failure.ignore=true install'
      }
	  post {
        success {
          junit '**/target/surefire-reports/**/*.xml' 
        }
      }
    }
  }
}