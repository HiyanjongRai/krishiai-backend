@echo off
set "JAVA_HOME=C:\Users\raihe\.jdks\openjdk-26.0.2.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=C:\Users\raihe\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd"
echo Using JAVA_HOME: %JAVA_HOME%
java -version
echo Using Maven: %MVN%
call "%MVN%" clean compile -f pom.xml
