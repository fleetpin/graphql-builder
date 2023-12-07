#!/usr/bin/env bash
set -e

# avoid the release loop by checking if the latest commit is a release commit
readonly local last_release_commit_hash=$(git log --author="$GIT_RELEASE_BOT_NAME" --pretty=format:"%H" -1)
echo "Last $GIT_RELEASE_BOT_NAME commit: ${last_release_commit_hash}"
echo "Current commit: ${GITHUB_SHA}"
if [[ "${last_release_commit_hash}" = "${GITHUB_SHA}" ]]; then
  echo "Skipping for $GIT_RELEASE_BOT_NAME commit. Release failed as no changes since last release."
  exit 1
fi

# This script will do a release of the artifact according to http://maven.apache.org/maven-release/maven-release-plugin/
echo "Setup git user name to '$GIT_RELEASE_BOT_NAME'"
git config --global user.name "$GIT_RELEASE_BOT_NAME";
echo "Setup git user email to '$GIT_RELEASE_BOT_EMAIL'"
git config --global user.email "$GIT_RELEASE_BOT_EMAIL";

# Setup next version
MAVEN_OPTION=$(
  case "$RELEASE_TYPE" in
    ("minor") echo "$MAVEN_OPTION \
      -DdevelopmentVersion=\${parsedVersion.majorVersion}.\${parsedVersion.nextMinorVersion}.1-SNAPSHOT \
      -DreleaseVersion=\${parsedVersion.majorVersion}.\${parsedVersion.nextMinorVersion}.0" ;;
    ("major") echo "$MAVEN_OPTION
      -DdevelopmentVersion=\${parsedVersion.nextMajorVersion}.0.1-SNAPSHOT \
      -DreleaseVersion=\${parsedVersion.nextMajorVersion}.0.0" ;;
    (*) echo "$MAVEN_OPTION" ;;
  esac
)
echo "Performing a $RELEASE_TYPE release of branch $RELEASE_BRANCH_NAME"

if [[ -n "$GITREPO_ACCESS_TOKEN" && -z "${SSH_PRIVATE_KEY}" ]]; then
    echo "Git repo access token defined and no SSH setup. We then use the git repo access token via maven release to commit in the repo."
    MAVEN_OPTION="$MAVEN_OPTION -Dusername=$GITREPO_ACCESS_TOKEN"
else
  echo "Not using access token authentication, as no access token (via env GITREPO_ACCESS_TOKEN) defined or SSH key setup (via env SSH_PRIVATE_KEY)"
fi

# Prepare the release
echo "Do mvn release:prepare with options $MAVEN_OPTION and arguments $MAVEN_ARGS"
mvn $MAVEN_OPTION $MAVEN_REPO_LOCAL build-helper:parse-version release:prepare -B -Darguments="$MAVEN_ARGS"

# Do release if prepare did not fail
if [[ ("$?" -eq 0) ]]; then
  echo "Do mvn release:perform with options $MAVEN_OPTION and arguments $MAVEN_ARGS"
  mvn $MAVEN_OPTION $MAVEN_PERFORM_OPTION $MAVEN_REPO_LOCAL build-helper:parse-version release:perform -B -Darguments="$MAVEN_ARGS -Dmaven.test.skip=true"
fi

# rollback release if prepare failed
if [[ "$?" -ne 0 ]] ; then
  echo "Rolling back release after failure"
  mvn $MAVEN_OPTION $MAVEN_REPO_LOCAL release:rollback -B -Darguments="$MAVEN_ARGS"
fi
