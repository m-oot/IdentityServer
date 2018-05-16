package Identity.Server;

//Command line args
import Identity.Database.DatabaseManager;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.lang.Thread;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static Identity.Client.SHA2.trySHA;
import static Identity.Server.CommitState.State.*;
import static java.rmi.server.RemoteServer.getClientHost;
import static Identity.Server.Action.Type.*;

/**
 * Identity Server used to service Identity Clients
 * @author Mayson Green
 * @author Alex Mussell
 */
public class IdServer implements IdentityServerClusterInterface
{
    /**
     * Args4j arguments
     */
    @Option(name="--numport",usage="--numport <port number>")
    private int registryPort = 5156;
    @Option(name="--verbose",usage="--verbose")
    private boolean verbose;
    @Option(name="--killdelay",usage="--killdelay")
    private int killServerDelay = 0;
    @Option(name="--dbfile",usage="--dbfile")
    private String dbFileName = null;
    @Option(name="--dbsh",usage="--dbsh")
    private String debugServerHost = null;
    @Option(name="--dbsp",usage="--dbsp")
    private int debugServerPort = -1;

    @Argument     // receives other command line parameters than options
    private List<String> arguments = new ArrayList<String>();

    private int serverId;                                           //This servers ID
    private CommitState currentCommitState = new CommitState();
    private List<ServerInfo> liveServerInfo;                        //Server info for each server in the KnownServers.txt file
    private List<Action> actionHistory;                             //A list of recently committed actions
    private int lStamp;                                             //current lamport timestamp
    private int lastSynchronization = -1;                           //The last known synchronization with other servers
    private ServerInfo coordinator;                                 //Server Information for the coordinator
    private ServerInfo myInfo;                                      //Server Information for this server
    private boolean amCoordinator;                                  //Set true if this server is the coordinator, otherwise false
    private int nextID = 1;                                         //The next available server ID.
    private String databaseUrl;                                     //A file extention to be used to distinguish different databases
    private static String databaseUrlPrefix = "jdbc:sqlite:";
    private DatabaseManager dm;
    private Logger log;
    private String verboseChannel = "verbose";
    private String eventChannel = "event";

    private static int heartBeatDelay = 2000;                       //Amount of time in milliseconds between each heart beat
    private Action actionForCommit;                                 //The current action that is the candidate for commiting

    /**
     * main method
     * ==============================================================
     * ==============================================================
     */
    public static void main(String[] args) throws IOException {
        //Setting SSL properties
        System.setProperty("javax.net.ssl.keyStore", "Security/Server_Keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "test123");
        System.setProperty("javax.net.ssl.trustStore", "Security/Client_Truststore");
        System.setProperty("java.security.policy", "Security/mysecurity.policy");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1"); //Only use when testing on localhost
        try {
            IdServer server = new IdServer(args); //The constructor handles everything
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception occurred: " + e);
        }
    }


    /**
     * IdServer constructor
     */
    public IdServer(String[] args) {
        run(args); //Parses command line arguments

        //Setting up DataBase
        if(dbFileName == null) {
            databaseUrl = databaseUrlPrefix + "identity-" + this.registryPort + ".db";
        } else {
            databaseUrl = databaseUrlPrefix + dbFileName;
        }
        dm = new DatabaseManager(databaseUrl);
        if(dbFileName == null) dm.setUp(); //A null dbFileName indicates that the database has never been setup

        liveServerInfo = Collections.synchronizedList(new ArrayList<>());
        actionHistory = Collections.synchronizedList(new ArrayList<>());

        //Getting values from the database
        lastSynchronization = dm.getLogicalStamp(); //Get the last known synchronization from the database
        lStamp = lastSynchronization;
        serverId = dm.getServerID();
        currentCommitState.setCurrentState(dm.getCommitState());

        //Other set up
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());   //Shuts down server properly
        log = new Logger();
        log.addChannel(verboseChannel,null);
        log.addChannel(eventChannel,System.out);
        if(debugServerHost != null && debugServerPort != -1) log.addServerChannel(verboseChannel,debugServerHost,debugServerPort);
        log.setPrefix(verboseChannel,this.registryPort + ":");
        if(verbose) {
            log.addStream(verboseChannel,System.out);
            log.log(verboseChannel,"Verbose set");
        }

        getKnownServers();
    }

    /**
     * ===================================================
     *Parses command line arguments using Args4j
     * ===================================================
     */
    private void run(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
        } catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            System.err.println("java IdServer [--numport <port#>] [--verbose] <query>");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.exit(1);
        }
    }

    /**
     * =======================================
     * RMI client methods
     * =======================================
     */
    @Override
    public synchronized User create(String loginName, String realName, String password) throws RemoteException, PartitionedException {
        log.log(verboseChannel,getTimeStamp() + "attempting to create new user: " + loginName);
        if(!amCoordinator) {
            log.log(verboseChannel,"Forwarding request to coordinator");
            return coordinator.getRemObj().create(loginName,realName,password);
        }
        if(currentCommitState.getCurrentState() == READY) throw new PartitionedException("The network is partitioned");
        String passwordHash = null;
        if(password != null){passwordHash = trySHA(password);} //Hash it twice, because our database could be compromised.
        String ip = "Could not be determined";
        try {
            ip = getClientHost();
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
        int lTimeStamp = nextLamportTime();
        User user = new User(UUID.randomUUID().toString(),loginName,realName,passwordHash,ip);
        user.setLstamp(lTimeStamp);
        Action action = new Action(lTimeStamp, CREATE,user);

        if(startTwoPhaseCommitPhaseOne(action) == -1){
            throw new PartitionedException("The system is partitioned. Unable to complete this request.");
        } //Unable to commit action
        User retUser = (dm.createUser(user) == 1) ? user : null;
        actionHistory.add(action);
        twoPhaseCommitPhaseTwo(action);
        return retUser;
    }

    @Override
    public User lookup(String loginName) throws RemoteException {
        log.log(verboseChannel,getTimeStamp() + " Looking up " + loginName);
        User user = dm.getUserByName(loginName);
        if(user == null){
            log.log(verboseChannel, "User not found");
        }
        return user;
    }

    @Override
    public User reverseLookup(String uuid) throws RemoteException {
        log.log(verboseChannel,getTimeStamp() +  " Looking up " + uuid);
        User user = dm.getUserByUUID(uuid);
        return user;
    }

    @Override
    public synchronized int modify(String oldLoginName, String newLoginName, String password) throws RemoteException, PartitionedException {
        log.log(verboseChannel,getTimeStamp() + " Modifying " + oldLoginName + " to " + newLoginName);
        if(!amCoordinator) {
            log.log(verboseChannel,"Forwarding request to coordinator");
            return coordinator.getRemObj().modify(oldLoginName,newLoginName,password);
        }
        String psswd = (password == null) ? null : trySHA(password);
        int lTimeStamp = nextLamportTime();
        User user = new User(null,oldLoginName,null,psswd,null);
        user.setLstamp(lTimeStamp);
        Action action = new Action(lTimeStamp,UPDATE,user,newLoginName);
        if(startTwoPhaseCommitPhaseOne(action) == -1){
            throw new PartitionedException("The system is partitioned. Unable to complete this request.");
        } //Unable to commit action
        int successCode = dm.changeUserName(oldLoginName,newLoginName,psswd,lTimeStamp);
        actionHistory.add(action);
        twoPhaseCommitPhaseTwo(action);
        return successCode;
    }

    @Override
    public synchronized int delete(String loginName, String password) throws RemoteException, PartitionedException {
        log.log(verboseChannel,getTimeStamp() + " Deleting " + loginName);
        if(!amCoordinator) {
            log.log(verboseChannel,"Forwarding request to coordinator");
            return coordinator.getRemObj().delete(loginName,password);
        }
        int lTimeStamp = nextLamportTime();
        String passwordHash = (password == null) ? null : trySHA(password);
        User user = new User(null,loginName,null,passwordHash,null);
        Action action = new Action(lTimeStamp,DELETE,user);
        if(startTwoPhaseCommitPhaseOne(action) == -1){
            throw new PartitionedException("The system is partitioned. Unable to complete this request.");
        } //Unable to commit action
        int successCode = dm.deleteUser(loginName,passwordHash,lTimeStamp);
        actionHistory.add(action);
        twoPhaseCommitPhaseTwo(action);
        return successCode;
    }

    @Override
    public List<String> get(String option) throws RemoteException {
        ArrayList<String> strings = new ArrayList<String>();
        ArrayList<User> users = dm.getUsers();
        if(option.equals("users")) {
            for(User u : users) {
                String name = u.getName();
                strings.add(name);
            }
        } else if(option.equals("uuids")) {
            for(User u : users) {
                String uuid = u.getUuid();
                strings.add(uuid);
            }
        } else if(option.equals("all")) {
            for(User u : users) {
                String info = u.publicString();
                strings.add(info);
            }
        } else {
            return null;
        }
        System.out.println(strings);
        for(String s : strings) {
            System.out.println(s);
        }
        return strings;
    }

    @Override
    public ServerInfo getCoordinatorInfo() throws RemoteException {
        return coordinator;
    }

    /**
     *====================================================================
     * Election RMI methods
     * ===================================================================
     */
    @Override
    public int startElection() throws RemoteException {
        class Election implements Runnable {
            @Override
            public void run() {
                runElection();
            }
        }
        new Thread(new Election()).start();
        return 1;
    }

    /**
     * Uses the bully algorithm to run an election. Instead of making the server with the highest id
     * the coordinator, it makes the server with the lowest id the coordinator
     * @return
     */
    public synchronized boolean runElection() {
        boolean amCoordinator = true; //Set true if this server is the coordinator
        for(ServerInfo server : new ArrayList<>(liveServerInfo)) { //Iterating over a copy of the list so that we can iterate and modify the old list at the same time
            try {
                if(server.getServerId() < this.serverId) {
                    log.log(verboseChannel,"ELECTION: Checking with: " + server);
                    server.getRemObj().startElection(); //Start elections on all servers with a lower id
                    amCoordinator = false; // We were able to connect to a superior server, so we must not be the coordinator
                }
            } catch (RemoteException e) {
                log.log(verboseChannel,"ELECTION: Could not connect with: " + server);
            }
        }
        this.amCoordinator = amCoordinator;
        if(this.amCoordinator) {
            log.log(verboseChannel,"I won the election!");
            coordinator = myInfo;
            for(ServerInfo server : liveServerInfo) {
                try {
                    server.getRemObj().announceVictory(this.serverId);
                } catch (RemoteException e) {
                }
            }
            //Make sure our data base is up to date
            for (ServerInfo server : liveServerInfo){
                try {
                    int currentServersLastSyncTime = server.getRemObj().getLastSynchronizedTime();
                    if (currentServersLastSyncTime > lastSynchronization){
                        synchronizeActions(server.getRemObj().getActions(lastSynchronization));
                    }
                } catch (RemoteException e) {
                }
            }
        }
        return amCoordinator;
    }

    @Override
    public void announceVictory(int serverId) throws RemoteException {
        log.log(verboseChannel,"ELECTION received election victory announcement for server: " + serverId);
        for(ServerInfo server : liveServerInfo) { //Loop through all current live servers to find coordinator remObj
            if(server.getServerId() == serverId) {
                coordinator = server;
                break;
            }
        }
        startHeartBeat();
    }

    @Override
    public boolean checkIfAlive() throws RemoteException {
        return true;
    }

    /**
     *=========================================================================
     * ========================================================================
     */
    @Override
    public ArrayList<ServerInfo> getLiveServers() throws RemoteException {
        ArrayList<ServerInfo> liveServerInfoCopy = new ArrayList<>(liveServerInfo);
        liveServerInfoCopy.add(myInfo);
        return liveServerInfoCopy;
    }

    @Override
    public int joinCluster(ServerInfo server, int serverId) throws RemoteException, NotCoordinatorException {
        if(!amCoordinator) throw new NotCoordinatorException("You must connect to the coordinator to get an ID");
        if(serverId == -1) {
            server.setServerId(nextID);
        } else {
            server.setServerId(serverId);
        }
        if(liveServerInfo.contains(server)){ //Keeping live server list current
            liveServerInfo.remove(server);
            System.out.println("Removed " + server.toString() + " from live servers");
        }
        try {
            server.setRemObj(getRemoteObject(server)); //Connect to new server and store its remote object reference
            for(ServerInfo liveServer : liveServerInfo) {
                try {
                    liveServer.getRemObj().announceNewServer(server); //Tell all live servers about the new server
                } catch (Exception e) {
                }
            }
            liveServerInfo.add(server);
        } catch (NotBoundException e) {
            return -1;
        }
        return serverId == -1 ? nextID++ : serverId;
    }

    @Override
    public int synchronizeActions(List<Action> actionHistory) throws RemoteException {
        if (actionHistory.isEmpty()) return 0;
        for(int i = 0; i < actionHistory.size(); i++) {
            if(actionHistory.get(i).getStamp() > lastSynchronization) actionHistory.get(i).execute(dm);
        }
        lastSynchronization = actionHistory.get(actionHistory.size() - 1).getStamp();
        return 0;
    }

    @Override
    public ArrayList<Action> getActions(int lastSyncTime) throws RemoteException {
        int lastSyncIndex = -1;
        for(Action action : actionHistory){
            if(action.getStamp() == lastSyncTime){
                lastSyncIndex = actionHistory.indexOf(action);
            }
        }
        if(lastSyncIndex == -1){
            throw new RemoteException();
        }
        int lastIndex = actionHistory.size() - 1;
        return new ArrayList<Action>(actionHistory.subList(lastSyncIndex, lastIndex));
    }

    @Override
    public int getLastSynchronizedTime() throws RemoteException {
        return lastSynchronization;
    }

    @Override
    public void announceNewServer(ServerInfo server) throws RemoteException {
        try {
            log.log(verboseChannel,"NEW SERVER: trying to connect with " + server);
            server.setRemObj(getRemoteObject(server));
            if(liveServerInfo.contains(server)) liveServerInfo.remove(server); //Removing old remote object
            liveServerInfo.add(server);
        } catch (NotBoundException e) {
            log.log(verboseChannel,"Could not connect to new server: " + server.toString());
        }
    }

    /**
     *================================================================================
     * Two Phase Commit methods
     * ===============================================================================
     */

    @Override
    public CommitState.State getCommitState() throws RemoteException {
        return currentCommitState.getCurrentState();
    }

    @Override
    public int voteRequest(Action actionForCommit) throws RemoteException {
        if(amCoordinator){
            System.out.println("Non coordinator called vote request on the coordinator.");
            runElection();
        }
        log.log(verboseChannel, "Vote request received for action: " + actionForCommit.getStamp());

        this.actionForCommit = actionForCommit;
        currentCommitState.setCurrentState(READY);
        dm.setCommitState(CommitState.stateToInt(READY));
        abortIfNoResponse(actionForCommit.getStamp());
        return currentCommitState.getCurrentState() == READY ? 1 : -1;
    }

    @Override
    public int abort() throws RemoteException {
        log.log(verboseChannel, "Abort message recieved");
        currentCommitState.setCurrentState(ABORT);
        dm.setCommitState(CommitState.stateToInt(ABORT));
        actionForCommit = null;
        return 1;
    }

    @Override
    public void commit() throws RemoteException {
        log.log(verboseChannel, "Commiting action: " + actionForCommit.getStamp());
        actionHistory.add(actionForCommit);
        actionForCommit.execute(dm);
        lStamp = actionForCommit.getStamp();
        lastSynchronization = lStamp;
        actionForCommit = null;
        currentCommitState.setCurrentState(COMMIT);
        dm.setCommitState(CommitState.stateToInt(COMMIT));
    }

    /**
     * Initiates a two phase commit
     * @param currentActionRequest - The action that is currently a candidate for being committed
     * @return 1 if successful, -1 not successful
     */
    private int startTwoPhaseCommitPhaseOne(Action currentActionRequest){
        currentCommitState.setCurrentState(INIT);
        dm.setCommitState(CommitState.stateToInt(INIT));
        try {
            log.log(verboseChannel, "Two Phase Commit initiated for action: " + currentActionRequest.getStamp());
            //Setting all servers to READY
            for(ServerInfo server : liveServerInfo){
                if(server.getRemObj().voteRequest(currentActionRequest) == -1){
                    log.log(verboseChannel, "Bad state received from " + server.toString());
                    currentCommitState.setCurrentState(ABORT);
                    dm.setCommitState(CommitState.stateToInt(ABORT));
                    for (ServerInfo s: liveServerInfo) {
                        try {
                            s.getRemObj().abort();
                        } catch (RemoteException e1) {
                            log.log(verboseChannel, "Problem sending abort message.");
                        }
                        return -1; //Abort message sent
                    }
                }
            }
        } catch (RemoteException e) {
            log.log(verboseChannel, "Not everyone is ready. Aborting vote request.");
            currentCommitState.setCurrentState(ABORT);
            dm.setCommitState(CommitState.stateToInt(ABORT));
            for (ServerInfo server : liveServerInfo){
                System.out.println(server.toString());
            }
            for (ServerInfo server: liveServerInfo) {
                try {
                    server.getRemObj().abort();
                } catch (RemoteException e1) {
                    log.log(verboseChannel, "Problem sending abort message.");
                }
                return -1; //Abort message sent
            }
        }

        currentCommitState.setCurrentState(COMMIT);
        dm.setCommitState(CommitState.stateToInt(COMMIT));
        return 1;
    }

    /**
     *Commits the action that was voted on during twoPhaseCommitPhaseOne
     * @param currentActionRequest - The action that needs to be committed
     */
    private void twoPhaseCommitPhaseTwo(Action currentActionRequest) {
        //Sending commit to all servers
        log.log(verboseChannel, "Commiting action: " + currentActionRequest.getStamp());
        try {
            for (ServerInfo server : liveServerInfo){
                log.log(verboseChannel, "Sending commit for action: " + currentActionRequest.getStamp());
                server.getRemObj().commit();
            }
        } catch (RemoteException e) {
            log.log(verboseChannel, "Problem with committing action to backup servers.");
        }
        lStamp = currentActionRequest.getStamp();
        log.log(verboseChannel, "Commit made for action: " + currentActionRequest.getStamp());
    }

    /**
     * A thread to run and make a decision if no commit message is received from the coordinator
     * after receiving a vote request
     * @param actionlStamp - The logical time stamp of the request
     */
    private void abortIfNoResponse(int actionlStamp) {
        Timer timer = new Timer();
        class CommitListener extends TimerTask {

            @Override
            public void run() {
                    while(currentCommitState.getCurrentState() == READY){
                        log.log(verboseChannel, "No commit message recieved for action: " + actionlStamp);
                        log.log(verboseChannel, "Asking other servers for state information.");

                        for(int i = 0; i < liveServerInfo.size(); i++) {
                            ServerInfo server = liveServerInfo.get(i);
                            try{
                                CommitState.State serverState = server.getRemObj().getCommitState();
                                System.out.println(serverState);
                                if(serverState == CommitState.State.COMMIT){
                                    log.log(verboseChannel, "Found another server in the commit state for action: " + actionlStamp);
                                    log.log(verboseChannel, "Executing action: " + actionlStamp);
                                    currentCommitState.setCurrentState(INIT);
                                    dm.setCommitState(CommitState.stateToInt(INIT));
                                    actionHistory.add(actionForCommit);
                                    actionForCommit.execute(dm);
                                    setLamportTime(actionlStamp);
                                    return;
                                } else if(serverState == CommitState.State.ABORT | serverState == INIT){
                                    log.log(verboseChannel, "Found server in bad state for action: " + actionlStamp);
                                    log.log(verboseChannel, "Aborting action: " + actionlStamp);
                                    currentCommitState.setCurrentState(ABORT);
                                    dm.setCommitState(CommitState.stateToInt(ABORT));
                                    return;
                                }
                            } catch (RemoteException e) {
                            }
                        }
                    }
            }
        }
        CommitListener cl = new CommitListener();
        timer.schedule(cl, 5000); //Schedules timer.run() after a scheduled amount of time
    }

    /**
     *==============================================================================================
     * Server set up methods
     * =============================================================================================
     */

    /**
     * Returns the current time in a printable format
     * @return
     */
    private String getTimeStamp(){
        return "[" + new SimpleDateFormat("MM/dd: HH.mm.ss").format(new Date()) + "] ";
    }

    /**
     * Used to create thread safe access to lstamp
     * @return
     */
    private synchronized int nextLamportTime() {
        lStamp++;
        return lStamp;
    }

    /**
     * updates the lamport time stamp
     */
    private synchronized int setLamportTime(int lStamp){
        return this.lStamp = lStamp;
    }

    /**
     * A heart beat thread that will check for a response from the coordinator every heartBeatDelay milliseconds
     */
    private void startHeartBeat() {
        Timer timer = new Timer();
        class HeartBeat extends TimerTask {

            @Override
            public void run() {
                try {
                    coordinator.getRemObj().checkIfAlive();
                } catch (RemoteException e) {
                    try {
                        timer.cancel(); //Stop the heart beat checks while we run the election
                        startElection();
                        log.log(verboseChannel, "Election started.");
                    } catch (RemoteException e1) {
                    }
                }
            }
        }
        HeartBeat hb = new HeartBeat();
        timer.scheduleAtFixedRate(hb, 0, heartBeatDelay); //Schedules timer.run() to execute periodically
    }

    /**
     * Gets a remote object from a server
     * @param server The server info where the remote object will be
     * @return
     * @throws RemoteException
     * @throws NotBoundException
     */
    private IdentityServerClusterInterface getRemoteObject(ServerInfo server) throws RemoteException, NotBoundException {
        log.log(verboseChannel, "Trying to connect with " + server.toString());
        Registry registry = LocateRegistry.getRegistry(server.getHostIpAddress(), server.getRegistryPort());
        IdentityServerClusterInterface remObj = (IdentityServerClusterInterface)registry.lookup("IdServer");
        server.setRemObj(remObj);
        return  remObj;
    }

    /**
     * Sets rmi socket timeout
     * @param timeoutMilliseconds
     */
    private void setRmiTimeout(int timeoutMilliseconds) {
        try {
            RMISocketFactory.setSocketFactory(new RMISocketFactory()
            {
                public Socket createSocket(String host, int port ) throws IOException {
                    Socket socket = new Socket();
                    socket.setSoTimeout(timeoutMilliseconds);
                    socket.setSoLinger( false, 0 );
                    socket.connect( new InetSocketAddress( host, port ), timeoutMilliseconds );
                    return socket;
                }
                public ServerSocket createServerSocket(int port ) throws IOException {
                    return new ServerSocket( port );
                }
            } );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the information for this server
     */
    private void setMyInfo(){
        //Setting Server info
        String myIpaddress = "";
        try {
            myIpaddress = InetAddress.getLocalHost().getHostAddress(); //If an exception is thrown here, our systems not going to work, we could fix it, but we haven't
            myIpaddress = "127.0.0.1";
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        myInfo = new ServerInfo(myIpaddress,registryPort);
    }

    /**
     * Finds the current coordinator
     * @return the ServerInfo for the current coordinator
     * @throws RemoteException
     */
    private ServerInfo findCoordinator() throws RemoteException {
        //Getting list of servers from file
        ServerAddressParser fileParser = new ServerAddressParser("KnownServers.txt");
        liveServerInfo = fileParser.getServerInfo();
        liveServerInfo.remove(myInfo);
        //Looping through known servers trying to make a connection
        IdentityServerClusterInterface remObj = null;
        boolean connected = false;
        for(ServerInfo server : liveServerInfo){
            try {
                remObj = getRemoteObject(server);
                connected = true;
                break;
            } catch (NotBoundException e) {
            } catch (RemoteException e) {
            }
        }
        if(!connected) return null;
        return remObj.getCoordinatorInfo();
    }

    /**
     * Gets the remote objects from the servers listed in KnownServers.txt
     */
    public void getKnownServers(){

        setRmiTimeout(1000);

        setMyInfo();

        try{
            if ((coordinator = findCoordinator()) == null && serverId == -1) { //You are the coordinator
                bind();
                //You are the only known server
                dm.setServerID(1);
                serverId = 1;
                myInfo.setServerId(serverId);
                amCoordinator = true;
                coordinator = myInfo;
                log.log(verboseChannel,"No live servers found. My server info: " + myInfo);
                nextID++;
            } else if (serverId == -1){ //You have never joined and need to connect to the coordinator
                coordinator.setRemObj(getRemoteObject(coordinator));
                liveServerInfo = coordinator.getRemObj().getLiveServers();

                if(liveServerInfo.contains(myInfo)){ liveServerInfo.remove(myInfo);}

                //Getting all remote objects from live servers
                log.log(verboseChannel,"Connecting to all live servers");
                for (ServerInfo server : liveServerInfo) {
                    try {
                        server.setRemObj(getRemoteObject(server));
                    } catch (RemoteException | NotBoundException e) {
                        log.log(verboseChannel, "Could not connect with " + server.getHostIpAddress());
                    }
                }

                bind();
                serverId = coordinator.getRemObj().joinCluster(myInfo,serverId);
                dm.setServerID(serverId);
                myInfo.setServerId(serverId);
                log.log(verboseChannel,"My server info " + myInfo);
                startHeartBeat();
            } else{ //You have lost connection/died and need to reconnect
                log.log(verboseChannel, "Rejoining cluster");
                bind();
                if(coordinator == null){ //You are the coordinator
                    coordinator = myInfo;
                    amCoordinator = true;
                } else{ //You are not the coordinator
                    coordinator.setRemObj(getRemoteObject(coordinator));
                    liveServerInfo = coordinator.getRemObj().getLiveServers();
                    liveServerInfo.remove(myInfo);
                    serverId = coordinator.getRemObj().joinCluster(myInfo,serverId);

                    startHeartBeat();

                    //Making sure remote objects are valid
                    for(ServerInfo server : liveServerInfo){
                        server.setRemObj(getRemoteObject(server));
                    }

                    //Make sure we are up to date on actions
                    ArrayList<Action> neededActions = coordinator.getRemObj().getActions(lStamp);
                    for (Action action : neededActions){
                        action.execute(dm);
                        lStamp = action.getStamp();
                    }
                }
            }
        }catch (NotCoordinatorException e) {
            System.err.println("I tried to connect to coordinator, but it wasn't the coordinator...");
            e.printStackTrace();
            System.exit(0);
        } catch (RemoteException e) {
        } catch (NotBoundException e) {
        }
    }

    /**
     * Binds the server to the registry
     */
    public void bind() {
        try {
            log.log(verboseChannel,"Trying to bind server on port " + registryPort);
            RMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            RMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
            IdentityServerInterface server = (IdentityServerInterface) UnicastRemoteObject.exportObject(this, 0, csf,
                    ssf);

            Registry registry = LocateRegistry.createRegistry(registryPort);

            registry.rebind("IdServer", server);
            log.log(verboseChannel,"IdServer bound on port " + registry);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception occurred: " + e);
        }
    }

    /**
     *Catches events such as Ctrl + C and shuts down gracefully.
     * Gets passed into Runtime.getRuntime.addShutdownhook(Runnable r), and
     * the method it is passed into catches the ctrl-c signal and runs the runnable
     * before shutting down.
     */
    private class ShutdownHook extends Thread{
        public ShutdownHook(){};
        public void run() {
            log.log(verboseChannel,"Server is shutting down");
            shutDownGracefully("Server is shutting down");
        }
    }

    /**
     * Shuts the server down gracefully and informs any active users
     */
    public void shutDownGracefully(String goodByeMessage) {
        System.err.println(goodByeMessage);
        dm.updateDatabase();
    }

}

