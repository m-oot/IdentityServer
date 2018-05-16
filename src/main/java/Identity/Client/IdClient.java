package Identity.Client;

import Identity.Server.*;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.Remote;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 *A client that can execute RMI commands with the Identity Server
 *
 * java IdClient [--server <serverhost>] [--numport <port#>] <query>
 *
 * @author Mayson Green
 * @author Alex Mussell
 */
public class IdClient{
    /**
     * Arguments
     */
    @Option(name="--testing")
    private boolean testing;
    @Option(name="--server", aliases="-s", handler = StringArrayOptionHandler.class)
    private List<String> serverList = new ArrayList<String>();
    @Option(name="--numport", aliases ="-n")
    private int registryPort = 5156;
    @Option(name="--password",aliases ="-p",usage="[--password <password>]")
    private String password = null;
    @Option(name="--create",aliases ="-c",usage="--create <loginname> [<real name>] [--password <password>]")
    private boolean create;
    @Option(name="--lookup",aliases="-l",usage="--lookup <loginname>")
    private boolean lookup;
    @Option(name="--reverse-lookup",aliases="-r",usage="-reverse-lookup <UUID>")
    private boolean reverseLookup;
    @Option(name="--modify",aliases="-m",usage="-modify <oldloginname> <newloginname> [--password <password>")
    private boolean modify;
    @Option(name="--delete",aliases="-d",usage="--delete <loginname> [--password <password>]")
    private boolean delete;
    @Option(name="--get",aliases="-g",usage="--get users|uuids|all")
    private boolean get;
    @Option(name="--dbsh",usage="--dbsh")
    private String debugServerHost = null;
    @Option(name="--dbsp",usage="--dbsp")
    private int debugServerPort = -1;
    @Argument
    private List<String> arguments = new ArrayList<String>();    // receives other command line parameters than options

    IdentityServerInterface remObj;    //A reference to the remote object on the IdServer

    Logger log;
    String debugServerChannel = "dbserverchannel";

    public static void main(String[] args) throws IOException{
        IdClient client = new IdClient(args);
        client.connectToIdentityServer();
    }

    /**
     *IdClient constructor
     */
    public IdClient(String[] args) throws IOException {
        System.setProperty("javax.net.ssl.trustStore", "Security/Client_Truststore");
        System.setProperty("java.security.policy", "Security/mysecurity.policy");
        log = new Logger();
        run(args);
        if(debugServerHost != null && debugServerPort != -1) log.addServerChannel(debugServerChannel,debugServerHost,debugServerPort);
    }

    /**
     * Parses command line arguments and connects to an IdServer
     */
    public int run(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
            if( arguments.isEmpty()) throw new CmdLineException(parser,"No argument is given");
        } catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            System.err.println("java IdClient <serverhost> [--port <port#>] <query>");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("   Example: java IdClient --create Crankiest Lancelet");

            return -1;
        }
        return 1;
    }

    /**
     * Gets the remote object from the IdServer
     */
    public void connectToIdentityServer(){
        boolean connected = false;
        setRmiTimeout(1000);

        ArrayList<ServerInfo> knownServers = getKnownServers(); //Getting list of servers from file and command line arguments

        //Looping through known servers trying to make a connection
        for(ServerInfo server : knownServers){
            try {
                //Creating executor to connect to server
                ExecutorService executor2 = Executors.newSingleThreadExecutor();
                Callable<Object> task2 = new Callable<Object>() {
                    public Object call() throws IOException, NotBoundException {
                        return (remObj = (IdentityServerInterface) getRMIRemoteConnection("IdServer",server));
                    }
                };
                Future<Object> future2 = executor2.submit(task2);
                remObj = (IdentityServerInterface) future2.get(5, TimeUnit.SECONDS);
                connected = true;

                //Creating executor to execute request
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Callable<Object> task = new Callable<Object>() {
                    public Object call() throws IOException {
                        return execute();
                    }
                };
                Future<Object> future = executor.submit(task);
                Object result = future.get(5, TimeUnit.SECONDS);
                System.exit(1); //The execution has not completed within the specified amount of time at this point
            }
            catch (InterruptedException e) {
            } catch (ExecutionException e) {
            } catch (TimeoutException e) {
            }
        }

        //Shuts the client down if no connection has been made
        System.out.println("Timeout error.");
        System.exit(0);
    }

    /**
     * Executes the specified client action on the remote object
     * @return
     * @throws IOException
     */
    private int execute() throws IOException {
        if(create) return create();
        if(lookup) return lookup();
        if(reverseLookup)  return reverseLookup();
        if(modify) return modify();
        if(delete) return delete();
        if(get) return get();

        return -1; //A command was not executed so return -1
    }

    /**
     * Sets a timeout on RMI so we don't have to wait forever if there are no available servers
     * @param timeoutMilliseconds
     */
    private void setRmiTimeout(int timeoutMilliseconds) {
        try {
            RMISocketFactory.setSocketFactory(new RMISocketFactory()
            {
                public Socket createSocket(String host, int port ) throws IOException {
                    Socket socket = new Socket();
                    socket.setSoTimeout( timeoutMilliseconds );
                    socket.setSoLinger( false, 0 );
                    socket.connect( new InetSocketAddress( host, port ), timeoutMilliseconds );
                    return socket;
                }
                public ServerSocket createServerSocket(int port ) throws IOException {
                    return new ServerSocket( port );
                }
            } );
        } catch (IOException e) {
        }
    }

    /**
     * Gets the known servers from the command line and KnownServers.txt
     * @return - A list of the known servers
     */
    public ArrayList<ServerInfo> getKnownServers() {
        ArrayList<ServerInfo> knownServers = new ArrayList<>();
        for(String server : serverList){
            knownServers.add(0,new ServerInfo(server, registryPort));
        }
        ServerAddressParser fileParser = new ServerAddressParser("KnownServers.txt");
        knownServers.addAll(fileParser.getServerInfo());
        return knownServers;
    }

    /**
     * Tries to get a Remote Object from a known server
     * @param serviceName
     * @param server
     * @return
     * @throws NotBoundException
     * @throws RemoteException
     */
    public Remote getRMIRemoteConnection(String serviceName, ServerInfo server) throws NotBoundException, RemoteException {
        Registry registry = LocateRegistry.getRegistry(server.getHostIpAddress(), server.getRegistryPort());
        Remote remObj = registry.lookup(serviceName);
        return remObj;
    }

    /**
     * Creates a new user
     * @return success code
     * @throws RemoteException
     */
    public int create() throws RemoteException {
        String realName = System.getProperty("user.name");
        String loginName = arguments.get(0);

        if (arguments.size() == 2){ realName = arguments.get(1);}
        if (password != null){ password = SHA2.trySHA(password);}
        User u = null;
        try {
            log.logClient(debugServerChannel,"Trying to create " + loginName);
            u = remObj.create(loginName, realName, password);
        } catch (PartitionedException e) {
            log.logClient(debugServerChannel,"System is partitioned. Unable to complete request.");
            System.exit(1);
        }

        int successCode = (u == null) ? -1 : 1;
        if (!testing){
            if(successCode == 1){
                log.logClient(debugServerChannel,"Successful login created: " + u.getUuid());
            } else{
                log.logClient(debugServerChannel,"User cannot be created with that information.");
            }
        }
        return successCode;
    }

    /**
     * Looks up user by login name
     * @return success code
     * @throws RemoteException
     */
    public int lookup() throws RemoteException {
        if (!testing)log.logClient(debugServerChannel,"Looking up " + arguments.get(0));
        User u = null;
        try {
            u = remObj.lookup(arguments.get(0));
        } catch (RemoteException e){
            System.out.println("Unable to fulfill request at this moment.");
        }

        int successCode = (u == null) ? -1 : 1;
        if (!testing) {
            String result = (u == null) ? "No user with that login name" : u.publicString();
            log.logClient(debugServerChannel,"Returned result: " + result);
        }
        return successCode;
    }

    /**
     * Looks up user by uuid
     * @return success code
     * @throws RemoteException
     */
    public int reverseLookup() throws RemoteException {
        User u = null;
        try {
            log.logClient(debugServerChannel,"Looking up " + arguments.get(0));
            u = remObj.reverseLookup(arguments.get(0));
        } catch (RemoteException e){
            System.out.println("Unable to fulfill request at this moment.");
        }

        String result = (u == null) ? "No user with that uuid" : u.publicString();
        int successCode = (u == null) ? -1 : 1;
        if (!testing)log.logClient(debugServerChannel,result);
        return successCode;
    }

    /**
     * Modifies a users name
     * @return
     * @throws RemoteException
     */
    public int modify() throws RemoteException {
        String oldLoginName = arguments.get(0);
        String newLoginName = arguments.get(1);

        if (password != null) password = SHA2.trySHA(password);

        int r = -1;
        try {
            log.logClient(debugServerChannel,"Trying to change " + oldLoginName + " to " + newLoginName);
            r = remObj.modify(oldLoginName, newLoginName, password);
        } catch (PartitionedException e) {
            log.logClient(debugServerChannel,"System is partitioned. Unable to complete request.");
            System.exit(1);
        }

        if (!testing){
            if(r == -2){
                log.logClient(debugServerChannel,"Thats not the right password...");
            } else if (r == -1){
                log.logClient(debugServerChannel,"There was no user with that login name");
            }else{
                log.logClient(debugServerChannel,oldLoginName + " has been changed to  " + newLoginName);
            }
        }
        return r;
    }

    /**
     * Deletes a user
     * @return success code
     * @throws RemoteException
     */
    public int delete() throws RemoteException {
        String userToDelete = arguments.get(0);

        if (password != null){ password = SHA2.trySHA(password);}

        int r = -1;
        try {
            log.logClient(debugServerChannel,"Deleting " + arguments.get(0));
            r = remObj.delete(userToDelete, password);
        } catch (PartitionedException e) {
            log.logClient(debugServerChannel,"System is partitioned. Unable to complete request.");
            System.exit(1);
        }

        if (!testing){
            if (r ==1 ){
                log.logClient(debugServerChannel,"Successfully deleted user.");
            } else if (r  == - 2){
                log.logClient(debugServerChannel,"Oops. That's not the right password. Are you sure this is your login...?");
            } else{
                log.logClient(debugServerChannel,"Are you sure you know what you are doing?");
            }
        }
        return r;
    }

    /**
     * Obtains either a list all login names,  list of all UUIDs
     * or a list of user,  UUID and string description all accounts
     * @return success code
     * @throws RemoteException
     */
    public int get() throws RemoteException {
        System.out.println(arguments.get(0));
        List<String> result = null;
        try{
            result = remObj.get(arguments.get(0));
        } catch(RemoteException e){
            System.out.println("Unable to fulfill request at this moment.");
        }

        if (!testing){
            if( result != null){
                log.logClient(debugServerChannel,"Results:");
                for(String res : result)
                    log.logClient(debugServerChannel,res);
            } else{
                log.logClient(debugServerChannel,"Oops. What are you trying to do? Did you read the instructions?");
                return -1;
            }
        }
        return result.size();
    }

    /**
     * Used for testing the client
     * @param args - The arguments to pass to the client
     * @return 1 if test passed, -1 if failed
     */
    public static int test(String[] args){
        try {
            IdClient client = new IdClient(args);
            return client.run(args); //This is broken!! DELETE IT
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
