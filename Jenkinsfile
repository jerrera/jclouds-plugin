pipeline {
    agent {
      docker 'maven:3.2.5-jdk-8'
    }

    environment {
      MAVEN_OPTS = "-Xmx1024m -DsocksProxyHost=135.7.146.1 -DsocksProxyPort=8000"
    }

    stages {
        stage("Build") {
            steps {
              sh 'mvn -B -Dmaven.test.failure.ignore clean install'
            }
        }
    }

    post {
        success {
            archive "**/target/*.hpi"
            junit '**/target/surefire-reports/*.xml'
        }
    }

}
