*Author Mayson Green, Alex Mussel

## Overview:
 This project contains an identity server and client. The client can connect to the server and submit
 account requests. The set of commands that are supported can be seen below. 
```
 --create <loginname> [<real name>] [--password <password>]
 --lookup <loginname>
 --reverse-lookup <UUID>
 --modify <oldloginname> <newloginname> [--password <password>]
 --delete <loginname> [--password <password>]
 --get users|uuids|all
```
## Manifest
```
    #Structure
    ├── .idea                 # intelliJ setup
    ├── src                   # Source files
    ├── target                # Generated output files
    ├── Makefile              # Makefile to compile program
    ├── pom.xml               # Maven pom.xml
    └── README.md             # This file
    #
    #Src files
    ├── ...
    ├── src                   
    │   ├── main 
    │   │    └── Java                   # All Java Source files
    │   │         └── Identity
    │   │               └── Client      #All Java Source files for IdClient
    │   │               └── Generator   #All Java Source files for Generating random users (Used in testing)
    │   │               └── Server      #All Java Source files for IdServer
    │   │               └── Database    #All Java Source files for the Database
    │   └── test
    │        └── Java                   # All Test written in Java
    └── ...
    #
    #
    #target [this will only be visible if the project has been compiled]
     ├── ...
     ├── target                   
     │   ├── classes         # All generated .class files
     │   ├── generated-sources
     │   ├── generated-test-sources
     │   ├── maven-status
     │   ├── surefire-reports
     │   └── test-classes
     └── ...

     #Source file descriptions
     FILE                            TYPE                                Description

     IdServer.java                   DRIVER (server)                     Manages messaging groups
     IdClient.java                   DRIVER (client)                     Driver for client
     SHA2.java                       SOURCE                              Helps set up the GUI
     TestUser.java                   SOURCE (testing)                    Server for clients to connect to
     UserGenerator.java              SOURCE (testing)                    Object used for Client --> Server communication
     Database.java                   SOURCE                              Client used for testing the server
     DatabaseManager.java            SOURCE                              Represents a client
     IdentityServerInterface.java    Interface
     User.java                       Source
```
## Building the project
 This project uses Maven for compiling and running.

 To compile this project go to project root and run
```
 make
```
 After the .java files have been compiled you can start the server by running (This will default to run on port 5156)
```
 make runserver
```
 To run the server in verbose mode run
```
 make debug
```
 If you would like to specify a port rather than 5156 run the server with the following command
```
 java -jar target/IdServer.jar --numport <numport>
```
 The client can be run using the following command from the project root directory
 (Defaults to localhost:5156 if no --server or --numport options are specified)
```
 java -jar target/IdClient.jar <args>
```

## Testing

    We built a User Generator that generates random usernames, passwords, and realnames. We also built a user object to
    args translator that turns our randomly generated users into formatted command line arguments for our chat client.
    We can generate arguments to create, delete, modify, or lookup a user. We automated tests to generate 100s of users
    then create the command line arguments to create those users. We generate arguments to test all the functions of the
    server, and verify their results. We also did manual testing for the shutdown hook, testing that our server saves
    its state. We also manually tested the server across onyx.

    Our name generator is pretty fun. Try using --get all to see some of the random names that were generated. Names are
    generated as Adjective Noun pairs


## Known Bugs
    No Known Bugs in Source Code

    Testing Code (minor bug) :
        Running the tests will not actually delete any users from the database. It tries to delete all the users
        that were created in the test, but because the Server has to be running in the background, the server owns
        the handle to the database. The database is locked, and the test cannot delete the users.
            Note: I do not mean that the we are not able to delete users through the server interface when testing.
                    We are not able to use the database directly to delete users in our tests.
