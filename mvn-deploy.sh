#!/usr/bin/env bash

VERSION=1.0.3.RELEASE
URL=http://nexus.saas.hand-china.com/content/repositories/hcm-releases/
REPOSITORYID=hcm-releases
## URL=http://nexus.saas.hand-china.com/content/repositories/hdg-snapshots
## REPOSITORYID=hdg-snapshots

mvn clean package

cd target

mvn deploy:deploy-file -DgroupId=com.ljx.permission -DartifactId=data-permission-helper-starter -Dname=data-permission-helper-starter -Dversion=$VERSION -Dpackaging=jar -Dfile=data-permission-helper-starter.jar -Durl=$URL -DrepositoryId=$REPOSITORYID



