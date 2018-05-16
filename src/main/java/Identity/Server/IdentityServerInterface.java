package Identity.Server;

//import Identity.Client.SHA2;
//import com.sun.org.apache.regexp.internal.RE;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface for a IdentityServer
 */
public interface IdentityServerInterface extends Remote {

    /**
     * -create <loginname> [<real name>] [--password <password>]
     * With this option,
     *the client contacts the server and attempts to create the new login name. The client optionally
     *provides the real user name and password along with the request.  In Java, we
     *can merely pass the user.name property as the user’s real name. Use System.getProperty("user.name")
     *to obtain this information.  If successful, the client prints an appropriate message with the generated
     *UUID for that account. Otherwise it returns an appropriate error message.
     *
     * @param loginName
     * @return
     * @throws RemoteException
     */
    User create(String loginName, String realName, String password) throws RemoteException, PartitionedException;

    /**
     * --lookup <loginname>
     *With this option, the client connects with the server and looks
     *up the loginname and displays all information found associated with the login name
     *(except for the encrypted password).
     *
     * @return
     * @throws RemoteException
     */
     User lookup(String loginName) throws RemoteException;

    /**
     * -reverse-lookup <UUID>
     *With this option, the client connects with the server and looks up the UUID
     *and displays all information found associated with the UUID (except for the encrypted password).
     *
     * @return
     * @throws RemoteException
     */
     User reverseLookup(String uuid) throws RemoteException;

    /**
     *--modify <oldloginname> <newloginname> [--password <password>]
     *The client contacts the server and requests a loginname change. If the new login name is available,
     *the server changes the name (note that the UUID does not ever change, once it has been
     *assigned). If the new login name is taken, then the server returns an error.
     *
     * @return
     * @throws RemoteException
     */
    int modify(String oldLoginName, String newLoginName, String password) throws RemoteException, PartitionedException;

    /**
     *--delete <loginname> [--password <password>]
     *The client contacts the server and requests to delete their loginname.
     *The client must supply the correct password for this operation to succeed.
     *
     * @return
     * @throws RemoteException
     */
    int delete(String userToDelete, String password) throws RemoteException, PartitionedException;

    /**
     * --get users|uuids|all
     *The client contacts the server and obtains either a list all login
     *names,  list of all UUIDs or a list of user,  UUID and string description all accounts
     *(don’t show encrypted passwords).
     *
     * @return
     * @throws RemoteException
     */
     List<String> get(String option) throws RemoteException;

    /**
     * This method returns the coordinators connection info (ipaddress, port). It can be used
     * if a client wants to talk directly to the coordinator to limit communication delay.
     *
     * @return ServerInfo object containing coordinator connection info
     * @throws RemoteException
     */
     ServerInfo getCoordinatorInfo() throws RemoteException;

     void announceNewServer(ServerInfo server) throws RemoteException;
}
