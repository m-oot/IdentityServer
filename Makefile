#This makefile depends on maven

default:
	mvn clean
	mvn compile
	mvn package

runserver:
	java -jar target/IdServer.jar

debug:
	java -jar target/IdServer.jar --verbose

runclient:
	java -jar target/IdClient.jar --get all

clean:
	mvn clean
