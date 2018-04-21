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
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static Identity.Client.SHA2.trySHA;
import static java.rmi.server.RemoteServer.getClientHost;
import static org.kohsuke.args4j.ExampleMode.ALL;

/**
 * Identity Server used to service Identity Clients
 * @author Mayson Green
 * @author Alex Mussell
 */
public class IdServer implements IdentityServerInterface
{

    private DatabaseManager dm = new DatabaseManager("jdbc:sqlite:identity.db");

    /**
     * Default constructor
     */
    public IdServer() {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());   //Shuts down server properly
    }

    /**
     * ==================================================
     * Arguments
     * ==================================================
     */
    @Option(name="--numport",usage="--numport <port number>")
    public int registryPort = 5156;

    @Option(name="--verbose",usage="--verbose")
    private boolean verbose;

    // receives other command line parameters than options
    @Argument
    private List<String> arguments = new ArrayList<String>();

    /**
     * ===================================================
     *Runs the server
     * ===================================================
     */
    private void run(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            // parse the arguments.
            parser.parseArgument(args);

        } catch( CmdLineException e ) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java IdServer [--numport <port#>] [--verbose] <query>");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("   Example: java IdServer"+parser.printExample(ALL));
        }

        if(verbose) {
            System.out.println("Verbose set");
            System.out.println("Trying to run server on port " + registryPort);

        }

        bind();

    }

    /**
     * =======================================
     * RMI methods
     * =======================================
     */

    @Override
    public User create(String loginName, String realName, String password) throws RemoteException {
        if (verbose) System.out.println(getTimeStamp() + "attempting to create new user: " + loginName);
        String passwordHash = null;
        if(password != null){passwordHash = trySHA(password);} //Hash it twice, cause our database could be compromised.
        String ip = "Could not be determined";
        try {
            ip = getClientHost();
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
        User user = new User(UUID.randomUUID().toString(),loginName,realName,passwordHash,ip);
        User retUser = (dm.createUser(user) == 1) ? user : null;
        return retUser;
    }

    @Override
    public User lookup(String loginName) throws RemoteException {
        if (verbose) System.out.println(getTimeStamp() + " Looking up " + loginName);
        User user = dm.getUserByName(loginName);
        if(user == null){
            if (verbose) System.out.println("User not found");
        }
        return user;
    }

    @Override
    public User reverseLookup(String uuid) throws RemoteException {
        if (verbose) System.out.println(getTimeStamp() +  " Looking up " + uuid);
        User user = dm.getUserByUUID(uuid);
        return user;
    }

    @Override
    public int modify(String oldLoginName, String newLoginName, String password) throws RemoteException {
        if (verbose) System.out.println(getTimeStamp() + " Modifying " + oldLoginName + " to " + newLoginName);
        String psswd = (password == null) ? null : trySHA(password);
        return dm.changeUserName(oldLoginName,newLoginName,psswd);
    }

    @Override
    public int delete(String loginName, String password) throws RemoteException {
        if (verbose) System.out.println(getTimeStamp() + " Deleting " + loginName);
        String passwordHash = (password == null) ? null : trySHA(password);
        return dm.deleteUser(loginName,passwordHash);
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

    //Gets a time stamp with the current time
    private String getTimeStamp(){
        return "[" + new SimpleDateFormat("MM/dd: HH.mm.ss").format(new Date()) + "] ";
    }

    /**
     * Binds the server to the registry
     */
    public void bind() {
        try {
            RMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            RMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
            IdentityServerInterface server = (IdentityServerInterface) UnicastRemoteObject.exportObject(this, 0, csf,
                    ssf);

            Registry registry = LocateRegistry.createRegistry(registryPort);

            registry.rebind("IdServer", server);
            if (verbose){ System.out.println("IdServer bound on port " + registry);}
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


    /**
     * Starts the server
     * ==============================================================
     * ==============================================================
     */
    public static void main(String[] args) throws IOException {
        System.setProperty("javax.net.ssl.keyStore", "../Server_Keystore");
        // Warning: change to match your password! Also the password should be
        // stored encrypted in a file outside the program.
        System.setProperty("javax.net.ssl.keyStorePassword", "test123");
        System.setProperty("java.security.policy", "mysecurity.policy");
        try {
            IdServer server = new IdServer();
            server.run(args); //Parses arguments
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println("Exception occurred: " + th);
        }
    }
}

