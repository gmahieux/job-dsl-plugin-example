def gitblitServer = '192.168.99.101'

def apiUrl = new URL("http://"+gitblitServer+":8081/rpc/?req=LIST_REPOSITORIES")
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
    project.availableRefs.collect(branchNameFromRef).each { branchName ->  
        def projectName = project.name.minus('projets/').minus('.git')+ '_' + branchName;
        job(projectName) {
            description 'Job de build du projet ' + projectName
            triggers {
                scm {
                    git {
                        remote {
                            url('git://'+gitblitServer + '/' + project.name )
                            refspec('+refs/heads/*:refs/remotes/origin/*')
                        }
                        branch(branchName)
                    }
                }
            }
            steps {
                maven {
                    goals('clean install')
                    mavenOpts('-Xmx700m')
                    localRepository(javaposse.jobdsl.dsl.helpers.LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
                }
            }
        }
        job(projectName+'_sonar') {
            description 'Job sonar du projet ' + projectName
            triggers {
                scm {
                    git {
                        remote {
                            url('git://'+gitblitServer + '/' + project.name )
                            refspec('+refs/heads/*:refs/remotes/origin/*')
                        }
                        branch(branchName)
                        localBranch('master')
                    }
                }
            }
            steps {
                maven {
                    goals('clean sonar:sonar')
                    mavenOpts('-Xmx700m')
                    localRepository(javaposse.jobdsl.dsl.helpers.LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
                }
            }
        }
    }
}

