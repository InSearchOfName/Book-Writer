pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build'
            }
        }

        stage('Prepare Release Assets') {
            when {
                allOf {
                    expression { env.BRANCH_NAME ==~ /^\d+\.\d+\.\d+$/ }
                    expression { !env.CHANGE_ID }
                }
            }

            steps {
                script {
                    sh '''
                        set -e

                        APP_VERSION=$(grep '^mod_version=' gradle.properties | cut -d= -f2 | tr -d ' ')
                        if [ -z "$APP_VERSION" ]; then
                            echo "mod_version not found"
                            exit 1
                        fi

                        TAG_NAME="${BRANCH_NAME}-v${APP_VERSION}"

                        MAIN_JAR=$(ls build/libs/*-${APP_VERSION}.jar | grep -v sources)
                        SOURCES_JAR=$(ls build/libs/*-${APP_VERSION}-sources.jar)

                        echo "APP_VERSION=$APP_VERSION" > release.env
                        echo "TAG_NAME=$TAG_NAME" >> release.env
                        echo "MAIN_JAR=$MAIN_JAR" >> release.env
                        echo "SOURCES_JAR=$SOURCES_JAR" >> release.env

                        echo "Prepared release assets:"
                        cat release.env
                    '''
                }
            }
        }

        stage('GitHub Release') {
            when {
                allOf {
                    expression { env.BRANCH_NAME ==~ /^\d+\.\d+\.\d+$/ }
                    expression { fileExists('release.env') }
                    expression { !env.CHANGE_ID }
                }
            }

            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'github',
                            usernameVariable: 'GIT_USER',
                            passwordVariable: 'GIT_PASS'
                        )
                    ]) {
                        sh '''
                            set -e
                            . "$WORKSPACE/release.env"

                            git config user.name "${GIT_USER}"

                            git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
                            git push https://${GIT_USER}:${GIT_PASS}@github.com/InSearchOfName/book-writer.git "$TAG_NAME"

                            RELEASE_RESPONSE=$(curl -s -X POST \
                            -H "Accept: application/vnd.github.v3+json" \
                            -H "Authorization: token ${GIT_PASS}" \
                            https://api.github.com/repos/InSearchOfName/book-writer/releases \
                            -d "{
                                \\"tag_name\\": \\"${TAG_NAME}\\",
                                \\"name\\": \\"Release ${TAG_NAME}\\",
                                \\"generate_release_notes\\": true
                            }")

                            UPLOAD_URL=$(echo "$RELEASE_RESPONSE" | jq -r '.upload_url' | sed 's/{.*}//')
                            CHANGELOG=$(echo "$RELEASE_RESPONSE" | jq -r '.body')

                            echo "CHANGELOG<<EOF" >> release.env
                            echo "$CHANGELOG" >> release.env
                            echo "EOF" >> release.env

                            for FILE in "$MAIN_JAR" "$SOURCES_JAR"; do
                                NAME=$(basename "$FILE")
                                curl --fail -X POST \
                                    -H "Authorization: token ${GIT_PASS}" \
                                    -H "Content-Type: application/java-archive" \
                                    --data-binary @"$FILE" \
                                    "${UPLOAD_URL}?name=${NAME}"
                            done
                        '''
                    }
                }
            }
        }

        stage('Modrinth Release') {
            when {
                allOf {
                    expression { env.BRANCH_NAME ==~ /^\d+\.\d+\.\d+$/ }
                    expression { fileExists('release.env') }
                    expression { !env.CHANGE_ID }
                }
            }

            steps {
                script {
                    withCredentials([
                        string(
                            credentialsId: 'modrinth-token',
                            variable: 'MODRINTH_TOKEN'
                        )
                    ]) {
                        sh '''
                            set -e
                            . "$WORKSPACE/release.env"

                            curl --fail --location 'https://api.modrinth.com/v2/version' \
                            --header "Authorization: ${MODRINTH_TOKEN}" \
                            --form 'data="{
                                \\"name\\": \\"Version '"${APP_VERSION}"'\\",
                                \\"version_number\\": \\"'"${APP_VERSION}"'\\",
                                \\"changelog\\": \\"'"${CHANGELOG}"'\\",
                                \\"dependencies\\": [
                                    {\\"project_id\\":\\"P7dR8mSH\\",\\"dependency_type\\":\\"required\\"},
                                    {\\"project_id\\":\\"ccKDOlHs\\",\\"dependency_type\\":\\"required\\"}
                                ],
                                \\"game_versions\\": [\\"'"${BRANCH_NAME}"'\\"],
                                \\"version_type\\": \\"release\\",
                                \\"loaders\\": [\\"fabric\\"],
                                \\"featured\\": true,
                                \\"status\\": \\"listed\\",
                                \\"requested_status\\": null,
                                \\"project_id\\": \\"yn4qgdpm\\",
                                \\"file_parts\\": [\\"main\\",\\"sources\\"],
                                \\"primary_file\\": \\"main\\"
                            }"' \
                            --form "main=@${MAIN_JAR}" \
                            --form "sources=@${SOURCES_JAR}"
                        '''
                    }
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