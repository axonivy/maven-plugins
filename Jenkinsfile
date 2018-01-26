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
	      	configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
	        	sh 'mvn -s $MAVEN_SETTINGS clean deploy -Dmaven.test.failure.ignore=true'
	    	}
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
