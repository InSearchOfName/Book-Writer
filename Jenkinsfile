@Library("InSearchOfName's-Library") _

pipeline {
	agent any

	options {
		buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
	}

	stages {

		stage('Checkout') {
			steps {
				checkout scm
			}
		}

		stage('Build') {
			steps {
				script {
					java.buildGradle()
				}
			}
		}
    }

	post {
		always {
			cleanWs()
		}
	}
}