import fr.jobdsl.DefaultJob
import fr.jobdsl.Constants

def apiUrl = new URL("http://"+Constants.GITBLIT_SERVER+":8081/rpc/?req=LIST_REPOSITORIES")
def repos= new groovy.json.JsonSlurper().parse(apiUrl.newReader())

/**
{
  "https://192.168.99.101/r/projets/	.git": {
    "name": "projets/projet1.git",
    "description": "",
    "owners": [
      "admin"
    ],
    "lastChange": "2015-12-10T20:39:04Z",
    "lastChangeAuthor": "admin",
    "hasCommits": true,
    "showRemoteBranches": false,
    "useIncrementalPushTags": false,
    "accessRestriction": "PUSH",
    "authorizationControl": "AUTHENTICATED",
    "allowAuthenticated": false,
    "isFrozen": false,
    "federationStrategy": "FEDERATE_THIS",
    "federationSets": [],
    "isFederated": false,
    "skipSizeCalculation": false,
    "skipSummaryMetrics": false,
    "isBare": true,
    "isMirror": false,
    "HEAD": "refs/heads/master",
    "availableRefs": [
      "refs/heads/master"
      "refs/heads/v1.0"
    ],
    "indexedBranches": [],
    "size": "1,019 b",
    "preReceiveScripts": [],
    "postReceiveScripts": [],
    "mailingLists": [],
    "customFields": {},
    "projectPath": "projets",
    "allowForks": true,
    "verifyCommitter": false,
    "gcThreshold": "500k",
    "gcPeriod": 0,
    "maxActivityCommits": 0,
    "metricAuthorExclusions": [],
    "commitMessageRenderer": "PLAIN",
    "acceptNewPatchsets": true,
    "acceptNewTickets": true,
    "requireApproval": false,
    "mergeTo": "master",
    "lastGC": "1970-01-01T00:00:00Z"
  }
**/
def branchNameFromRef = {it.minus('refs/heads/')}
repos.collect{it.value}.findAll{it.projectPath=='projets'}.each{project -> 
    def projectName = project.name.minus('projets/').minus('.git');
    project.availableRefs.collect(branchNameFromRef).each { branchName ->  
        new DefaultJob (
            name : projectName+ '_' + branchName,
            gitRepository : project.name,
            scmBranch : branchName
        ).build(this)
        new DefaultJob (
            name : projectName+ '_' + branchName + '_sonar',
            description : 'Job sonar du projet ' + projectName,
            gitRepository : project.name,
            scmBranch : branchName,
            steps : {
                maven {
                    goals('clean sonar:sonar')
                    mavenOpts('-Xmx700m')
                    localRepository(javaposse.jobdsl.dsl.helpers.LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
                }
            }
        ).build(this)
    }
    listView(projectName) {
        jobs {
            regex(projectName + '.*')
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
}

