@echo off

mvn -f ..\pom.xml -DoutputDirectory=bin -Dmdep.stripVersion=true -DincludeScope=compile dependency:copy-dependencies
