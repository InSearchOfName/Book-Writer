pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {

        stage('Check Skip CI') {
            steps {
                script {
                    def commitMsg = sh(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    def commitMsgLower = commitMsg.toLowerCase()

                    echo "Last commit message: ${commitMsg}"

                    if (commitMsgLower.contains("[skip ci]") || commitMsgLower.contains("[ci skip]")) {
                        echo "Skipping build due to commit message"
                        currentBuild.result = 'NOT_BUILT'
                    }
                }
            }
        }

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
                    expression { env.BRANCH_NAME ==~ /^\d+\.\d+(\.\d+)?$/ }
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
                    expression { env.BRANCH_NAME ==~ /^\d+\.\d+(\.\d+)?$/ }
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
                            git push https://${GIT_USER}:${GIT_PASS}@github.com/InSearchOfName/Book-Writer.git "$TAG_NAME"

                            RELEASE_PAYLOAD=$(jq -n \
                                --arg tag_name "$TAG_NAME" \
                                --arg name "Release $TAG_NAME" \
                                '{tag_name: $tag_name, name: $name, generate_release_notes: true}')

                            RELEASE_RESPONSE=$(curl -sS -X POST \
                            -H "Accept: application/vnd.github.v3+json" \
                            -H "Authorization: token ${GIT_PASS}" \
                            https://api.github.com/repos/InSearchOfName/Book-Writer/releases \
                            -d "$RELEASE_PAYLOAD")

                            UPLOAD_URL=$(printf '%s' "$RELEASE_RESPONSE" | jq -r '.upload_url' | sed 's/{.*}//')
                            CHANGELOG=$(printf '%s' "$RELEASE_RESPONSE" | jq -r '.body')

                            CHANGELOG_B64=$(printf '%s' "$CHANGELOG" | base64 | tr -d '\n')
                            echo "CHANGELOG_B64=$CHANGELOG_B64" >> release.env

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
                    expression { env.BRANCH_NAME ==~ /^\d+\.\d+(\.\d+)?$/ }
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

                            if [ -n "${CHANGELOG_B64:-}" ]; then
                                CHANGELOG=$(printf '%s' "$CHANGELOG_B64" | base64 -d)
                            else
                                CHANGELOG=""
                            fi

                            MODRINTH_DATA=$(jq -n \
                                --arg name "Version $APP_VERSION" \
                                --arg version_number "$APP_VERSION" \
                                --arg changelog "$CHANGELOG" \
                                --arg game_version "$BRANCH_NAME" \
                                '{
                                    name: $name,
                                    version_number: $version_number,
                                    changelog: $changelog,
                                    dependencies: [
                                        {project_id: "P7dR8mSH", dependency_type: "required"},
                                        {project_id: "ccKDOlHs", dependency_type: "required"}
                                    ],
                                    game_versions: [$game_version],
                                    version_type: "release",
                                    loaders: ["fabric"],
                                    featured: true,
                                    status: "listed",
                                    requested_status: null,
                                    project_id: "yn4qgdpm",
                                    file_parts: ["main", "sources"],
                                    primary_file: "main"
                                }')

                            curl --fail --location 'https://api.modrinth.com/v2/version' \
                            --header "Authorization: ${MODRINTH_TOKEN}" \
                            --form "data=$MODRINTH_DATA" \
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