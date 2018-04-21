package Identity.Client;

import Identity.Server.IdServer;

import static org.kohsuke.args4j.ExampleMode.ALL; //Command line parsing

import Identity.Server.IdentityServerInterface;
import Identity.Server.User;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.util.List;

/**
 *A client that can execute RMI commands with the Identity Server
 *
 * java IdClient --server <serverhost> [--numport <port#>] <query>
 *
 * @author Mayson Green
 * @author Alex Mussell
 */
public class IdClient{

    IdentityServerInterface remObj;

    /**
     * Default constructor
     */
    public IdClient() {
        System.setProperty("javax.net.ssl.trustStore", "../Client_Truststore");
        System.setProperty("java.security.policy", "mysecurity.policy");
        /* System.setSecurityManager(new RMISecurityManager()); */
    }

    /**
     * Gets the remote object
     */
    public void getRemoteObject(){
        try {
            Registry registry = LocateRegistry.getRegistry(host, registryPort);
            remObj = (IdentityServerInterface) registry.lookup("IdServer");

        } catch (java.rmi.NotBoundException e) {
            System.err.println("RMI endpoint not bound: " + e);
            System.exit(2);
        } catch (java.rmi.RemoteException e) {
            System.err.println("RMI RemoteException: " + e);
            System.exit(2);
        }
    }

    /**
     * ==================================================
     * Arguments
     * ==================================================
     */
    @Option(name="--testing")
    private boolean testing;

    @Option(name="--server", aliases="-s")
    private String host = "localhost";

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

    // receives other command line parameters than options
    @Argument
    private List<String> arguments = new ArrayList<String>();

    /**
     * ===================================================
     *Runs client
     * ===================================================
     */
    public int run(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            // parse the arguments.
            parser.parseArgument(args);

            if( arguments.isEmpty() )
                throw new CmdLineException(parser,"No argument is given");

        } catch( CmdLineException e ) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java IdClient <serverhost> [--port <port#>] <query>");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("   Example: java IdClient --create Crankiest Lancelet");

            return -1;
        }

        //Now we can get the remote object after parsing the arguments
        getRemoteObject();

        /**
         * Creates a user
         */
        if(create) {
            return create();

        }

        /**
         * Looks up a user based off login name
         */
        if(lookup) {
            return lookup();
        }


        /**
         * Looks up a user based off a uuid
         */
        if(reverseLookup) {
            return reverseLookup();
        }

        /**
         * Modifies the current login name to the newly specified login name.
         */
        if(modify) {
            return modify();
        }

        /**
         * Deletes a user based off of a user name
         */
        if(delete) {
            return delete();
        }

        /**
         * Obtains either a list all login names,  list of all UUIDs
         * or a list of user,  UUID and string description all accounts
         */
        if(get) {
            return get();
        }

        return -1;

    }

    public int create() throws RemoteException {
        String realName = System.getProperty("user.name");
        String loginName = arguments.get(0);

        if (arguments.size() == 2){ realName = arguments.get(1);}
        if (password != null){ password = SHA2.trySHA(password);}

        User u = remObj.create(loginName, realName, password);
        int successCode = (u == null) ? -1 : 1;
        if (!testing){
            if(successCode == 1){
                System.out.println("Successful login created: " + u.getUuid());
            } else{
                System.out.println("User cannot be created with that information.");
            }
        }
        return successCode;
    }

    public int lookup() throws RemoteException {
        if (!testing)System.out.println("Looking up " + arguments.get(0));

        User u = remObj.lookup(arguments.get(0));
        int successCode = (u == null) ? -1 : 1;
        if (!testing) {
            String result = (u == null) ? "No user with that login name" : u.publicString();
            System.out.println("Returned result: " + result);
        }
        return successCode;
    }

    public int reverseLookup() throws RemoteException {
        User u = remObj.reverseLookup(arguments.get(0));
        String result = (u == null) ? "No user with that uuid" : u.publicString();
        int successCode = (u == null) ? -1 : 1;
        if (!testing)System.out.println(result);
        return successCode;
    }

    public int modify() throws RemoteException {
        String oldLoginName = arguments.get(0);
        String newLoginName = arguments.get(1);

        if (password != null) password = SHA2.trySHA(password);

        int r = remObj.modify(oldLoginName, newLoginName, password);

        if (!testing){
            if(r == -2){
                System.out.println("Thats not the right password...");
            } else if (r == -1){
                System.out.println("There was no user with that login name");
            }else{
                System.out.println(oldLoginName + " has been changed to  " + newLoginName);
            }
        }
        return r;
    }

    public int delete() throws RemoteException {
        String userToDelete = arguments.get(0);

        if (password != null){ password = SHA2.trySHA(password);}

        int r = remObj.delete(userToDelete, password);

        if (!testing){
            if (r ==1 ){
                System.out.println("Successfully deleted user.");
            } else if (r  == - 2){
                System.out.println("Oops. That's not the right password. Are you sure this is your login...?");
            } else{
                System.out.println("Are you sure you know what you are doing?");
            }
        }
        return r;
    }

    public int get() throws RemoteException {
        List<String> result = remObj.get(arguments.get(0));
        if (!testing){
            if( result != null){
                System.out.println("Results:");
                for(String res : result)
                System.out.println(res);
            } else{
                System.out.println("Oops. What are you trying to do? Did you read the instructions?");
                return -1;
            }
        }
        return result.size();
    }

    //Used for testing
    public String toString(){
        return "host: " + host + " port: " + registryPort;
    }

    /**
     * Starts the IdentityClient
     * ===================================================
     * ===================================================
     */
    public static void main(String[] args) throws IOException{
        IdClient client = new IdClient();
        client.run(args);
    }

    /**
     * Used for testing the client
     * @param args - The arguments to pass to the client
     * @return 1 if test passed, -1 if failed
     */
    public static int test(String[] args){
        IdClient client = new IdClient();
        try {
            return client.run(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
