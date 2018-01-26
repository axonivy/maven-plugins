pipeline {
  agent {
    docker {
      image 'maven:3.5.0-jdk-8'
    }
    
  }
  stages {
    stage('build and deploy') {
      steps {
        sh 'mvn clean deploy -Dmaven.test.failure.ignore=true '
      }
	  post {
        success {
          junit '**/target/surefire-reports/**/*.xml' 
        }
      }
    }
  }
}
