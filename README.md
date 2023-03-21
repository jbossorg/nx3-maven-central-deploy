# nx3-maven-central-deploy
Plugin for deployment of artifacts from Sonatype Nexus 3 repositories to [Maven Central](https://central.sonatype.com). The plugin allows using **Sonatype Nexus 3** with all its powerful features as a base for publishing artifacts, especially combining the deployment with **tagging** and **staging**.

The advantage of using this plugin instead of directly deploying artifacts with Maven is the ability to test your artifact first - your developer may deploy his work to your NXRM3 repository, QA can test the release and if everything is OK, this plugin will transfer the tested bits directly. Running the Maven command to build and deploy from the same source code usually does not result in the same binaries (timestamps do not match) and there is also a room for some human error.

Nx3-maven-central-deploy also makes sure, that before you try to push your artifacts into Maven Central, you fulfill the [requirements](https://central.sonatype.org/publish/requirements/) Sonatype has for the published content. You can even extend the checking features by adding your own validation rules. You are not required to use any CI/CD pipelines such as Jenkins, though you still can if you want to.


# Features

Nx3-maven-central-deploy is currently in very early stage so most of the "must have features" are not yet completed. See the roadmap.

* push released artifacts into Maven Central
* filter components in the repository to be selected for the deployment
* validate the artifacts before pushing them to Maven Central
* combine your validation with nx3-maven-central-deploy pre-prepared tests by adding simple custom modules
* tag artifacts successfully deployed to Maven Central (NXRM3Pro only)
* tag artifacts where errors were found making them not suitable for publishing (NXRM3Pro only)
* report validation errors in customizable text file format
* report validation errors using NXRM3 logging environment
* integrate validation reporting with Jira
* allow adding your own integrations with other bug reporting tools by adding custom modules


# Compatibility

At the current state of development the plugin works with Sonatype Nexus version 3 Professional. I plan to make it work with the community version later, but it will not support NXRM3 Professional features, such as tagging artifacts. The current build is working with NXRM3 version 3.42.0-01, later it will follow Nexus public releases. 

# Roadmap

The features here are ordered based on the current development priority - the top ones should be finished first, although in some cases they are being worked on simultaneously.  

## Completed
- filter components in the repository to be selected for the deployment
- validate the artifacts before pushing them to Maven Central
- tag artifacts successfully deployed to Maven Central
- tag artifacts, where errors were found making them not suitable for publishing

## In Development
- report validation errors in customizable text file format

## Coming Soon
- push released artifacts into Maven Central
- combine your validation with nx3-maven-central-deploy pre-prepared tests by adding simple custom modules
  - example validation plugin
- integrate validation reporting with Jira
- report validation errors using NXRM3 logging environment
- allow adding your own integrations with other bug reporting tools by adding custom modules
  - example reporting plugin


# Installation

You will find the installation packages of the current versions of Nexus in [GitHub releases](https://github.com/jbossorg/nx3-maven-central-deploy/releases).

Download the version suitable for your version of Nexus and copy it to the *{nexusInstallDir}/deploy/*. When doing Nexus upgrade, you must also upgrade nx3-maven-central-deploy, so it runs the same version as your Nexus. 

# Capabilities and Tasks in Detail

## Deployment Task

In order to use nx3-maven-central-deploy, you must enable **Schedule** capability first. The deployments are scheduled as tasks under *System-->Tasks* tab.    

Go to *System-->Tasks* tab and click on *+ Create Task* button. It will open list of possible tasks you can create and if you installed nx3-maven-central-deploy properly, you will be able to find **Maven Central Deployment**.

Mandatory fields are **name of the task**, make sure to find a good name, because the name will appear in reports. Then you must select **source repositories** - only hosted release repositories are allowed. The other repositories do not appear here. The last mandatory field is **Task frequency**. 

When preparing a synchronization task for a project, that is already in Maven Central you need to prepare your task, so it does not try to re-publish artifacts, that were already published in the past, so set it to "Manual". We will cover this case later. If you are trying to prepare deployment of a project, that is not in Maven Central yet, select some reasonable period here, that is not too long, otherwise you will need to wait for the artifacts to appear in Maven Central for too long, but also do not set it to a too short interval, otherwise you may be spammed by failing reports. Reasonable time is IMO once a day, but it is up to you.        

| Field                | Explantion                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Dry-Run              | If checked, the task will do the validations, but it will not try to publish the artifacts into Maven Central. It is useful in initial testing of your project or when setting up the task.                                                                                                                                                                                                                                                                                            |
| Mark Completed       | Use this field with Dry-Run checked before you do your first push to Maven Central if you already published your artifacts there. <br/>If checked, after the successful validation of your artifacts the task will mark the validated artifacts as published and next time this task will run the marked artifacts will be ignored by the task. <br/>If you have Dry-Run checkbox unchecked also, the task will only mark the artifacts as deployed if the actual deployment succeeds. |
| Filter               | This field allows you to limit the deployment of artifacts to a specific subset of artifacts. The syntax of the filter is explained later.                                                                                                                                                                                                                                                                                                                                             |
| Task Variables       | nx3-maven-central-deploy uses Velocity templating technology in many places, especially during the reporting of the errors. Here you can set your own variables to be added to the template processing by this  task. Example is the name of Jira project, that should be used to report validation failures to.<br/><br/>The syntax is {variable}={value} delimited by a newline character (a standard property file without sections).                                               |

While most of the [requirements](https://central.sonatype.org/publish/requirements/) are mandatory, you might have a reason for disabling some of them. For example even though the JavaDoc is mandatory you have it deployed elsewhere. Or you just want to make the initial "mark all artifacts as published using Dry-Run" even though your old artifacts do not comply with the Sonatype requirements.

| Check               | Descritpion                                                                                                                                                                                                                                           |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Project Name        | Ensures you have the name of the project in your pom.xml.                                                                                                                                                                                             |
| Project Description | Ensures you have the description of your project in pom.xml.                                                                                                                                                                                          |
| URL                 | Your pom.xml should have the URL with link to your project web page.                                                                                                                                                                                  |
| SCM                 | SCM stands for "source code management". Now it will be most likely a git repository, SVN or similar.                                                                                                                                                 |
| Developer Info      | Information about the developer of the plugin (author, company).                                                                                                                                                                                      |
| License             | Your project must be open-source if you deploy it to MVN Central. You must say under what conditions it can be used.                                                                                                                                  |
| Snapshot Version    | Snapshot versions are not allowed in Maven Central. This validation also makes sure you did not forget a snapshot version in one of the dependencies. The check only validates your pom.xml, it does not check the dependencies of your dependencies. |
| Source Code         | Maven allows downloading source code of your dependencies during the development. [See here](https://maven.apache.org/plugins/maven-source-plugin/).                                                                                                  |
| JavaDoc             | JavaDoc file is also required when you publish to Maven Central. More information [see here](https://maven.apache.org/plugins/maven-javadoc-plugin/).                                                                                                 |
| MD5 Checksums       | The artifacts should have checksum files so Maven can validate the downloaded files during build. md5 and sha1 are a must in Maven Central. sha256, sha512 and asc (GPG) are welcome but not required.                                                |
| SHA1 Checksums      | See MD5 Checksums                                                                                                                                                                                                                                     |
| Project             | Your pom.xml must have project as the root element. If not, something is very wrong with your pom.xml file. Probably some error in your CI/CD pipeline damaged the file. Maven itself would refuse to build your project.                             |
| Group               | Maven uses group, artifact and version to identify your artifact. Group is mandatory, however it can be skipped if Maven can take it from parent.                                                                                                     |
| Artifact            | Maven uses group, artifact and version to identify your artifact. Artifact is mandatory and your artifact must have it.                                                                                                                               |
| Version             | Maven uses group, artifact and version to identify your artifact. Version is mandatory, however it can be skipped if Maven can take it from parent.                                                                                                   |

### Filtering Artifacts
You may have a big repository with released content and want to deploy only part of it to Maven Central. Or you want to split your deployment to several smaller deployments, so that possible validation errors are reported in Jira projects of the specific responsible teams. Whatever is your intention **Filter** field in the task configuration is your friend. 

You can filter by group, artifact, version, tag name and tag attribute. You can also search without specifying the search field, and in that case the plugin will search for artifacts, that have this text in group, artifact or version. 

| Field    | Operators       | Example             | Description                                                                                                                                                                |
|----------|-----------------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| {none}   | {none}          | kie-api             | Searches for kie-api in fields group, artifact and version                                                                                                                 |
| group    | =               | group=org.jboss.as  | Searches for artifacts with org.jboss.as group                                                                                                                             |
| artifact | =               | artifact=kie-api    | Searches for artifact named kie-api                                                                                                                                        |
| name     | =               | name=kie-api        | Nexus uses "name" instead of "artifact" in the classes due to the support of other than Maven repository types. This expression has identical effect to *artifact=kie-api* |
| version  | = < > <= >= !=  | version>4.5.0       | Check, if version is equal, less than, smaller then etc. than the expression. The version is compared by string comparison, so version 10.0 is smaller than 4.0.           |
| tag      | = < > <= >= !=  | tag!=Deployed       | Check if one tags assigned to an artifact corresponds to the operation. In this example tag must not be *Deployed*. You can only use this feature in NXRM3 Professional.   |
| tagAttr  | = < > <= >= !=  | tagAttr=os!=Windows | Tags in Nexus may have attributes. In this case search for any tag. If it has attribute *os* with value not equal to *Windows* the artifact would be a match.              |

You can combine several of these tests together and divide them by & character (in that case all these conditions must be fulfilled). Each condition must be used only once though.

**Example:** *group=org.jboss.as&version>=7.4.0* returns all artifacts with group org.jboss.as with version 7.4.0 and higher.

*group=org.jboss.as&group=org.hibernate* will fail, because group can only be present once in the filter.  

## Tag Setup Capability
In order to help to navigate in the synchronized repository, nx3-maven-central-deploy can be customized to tag the deployed artifacts. In order to 



## Plain-text Reporting Capability

## Using Velocity in nx3-maven-central-deploy 

# Extending nx3-maven-central-deploy by Your Tests and Reports
