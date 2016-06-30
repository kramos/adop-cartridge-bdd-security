// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def projectNameKey = projectFolderName.toLowerCase().replace("/", "-")
def referenceAppgitRepo = "bdd-security"
def referenceAppGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + referenceAppgitRepo

// Jobs
def buildAppJob = freeStyleJob(projectFolderName + "/BDD_Security_Demo")

// Views
def pipelineView = buildPipelineView(projectFolderName + "/Java_Reference_Application")

pipelineView.with {
    title('BDD Security Demo Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/Java_Reference_Application")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

buildAppJob.with {
    description("This job runs a deliberately vunerable application and then tests it using BDD Security")
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent("adop-jenkins-master")
    }
    scm {
        git {
            remote {
                url(referenceAppGitUrl)
                credentials("adop-jenkins-master")
            }
            branch("*/master")
        }
    }
    environmentVariables {
        env('WORKSPACE_NAME', workspaceFolderName)
        env('PROJECT_NAME', projectFolderName)
    }
    label("java8")
    triggers {
        gerrit {
            events {
                refUpdated()
            }
            project(projectFolderName + '/' + referenceAppgitRepo, 'plain:master')
            configure { node ->
                node / serverName("ADOP Gerrit")
            }
        }
    }
    steps {
        shell('''set +x
            |curl -O https://github.com/continuumsecurity/RopeyTasks/blob/master/ropeytasks.jar
            |java -jar ropeytasks.jar &
            |./gradlew -Dcucumber.options="--tags @authentication --tags ~@skip" test
            |set -x'''.stripMargin()
        )
    }
    publishers {
        archiveArtifacts("**/*")
    }
}

