package fr.jobdsl

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.Context
import javaposse.jobdsl.dsl.DslContext
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.jobs.FreeStyleJob
import fr.jobdsl.Constants

class DefaultJob {

	String description
	String name
	String gitRepository
	String scmBranch = 'master'

	Closure steps
	Closure additionalConf

	Job build(DslFactory dslFactory) {
		def job1 = dslFactory.freeStyleJob(name){
			description ((this.description==null)?this.getDefaultDescription():this.description)
			if (gitRepository != null) {
				this.gitConfig(delegate,gitRepository, scmBranch)
			}
			steps {
			  if(this.steps) {
				  delegate.with(this.steps)
				}
				else {
					maven {
						goals('clean install')
						mavenOpts('-Xmx700m')
						localRepository(javaposse.jobdsl.dsl.helpers.LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
					}
				}
			}
		}
		if (this.additionalConf != null) {
			job1.with(this.additionalConf)
		}
		return job1
	}

	protected String getDefaultDescription() {
		"Job de construction du projet " + name
	}

	protected void gitConfig(context, repoName, branchName = 'master') {
		context.with {
			scm {
				git {
                    remote {
                        url('git://'+ Constants.GITBLIT_SERVER + '/'+ repoName)
                        refspec('+refs/heads/*:refs/remotes/origin/*')
                    }
                    branch(branchName)
                    localBranch('master')
                }
			}
		}
	}
}
