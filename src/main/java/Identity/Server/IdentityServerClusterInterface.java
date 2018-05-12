package Identity.Server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface IdentityServerClusterInterface extends IdentityServerInterface {

    /**
     * Used for one server to let another server know that it is starting an election
     *
     */
    int startElection() throws RemoteException;

    /**
     * Used for one server to let another server know that it is the new coordinator.
     * When the method is called it should change the coordinator reference for that server.
     *
     * @param serverId used to make it convenient for a server to know which server is calling the method
     */
    void announceVictory(int serverId) throws RemoteException;

    /**
     * Used to check if another server is alive.
     *
     * Should throw remote exception if the server is dead
     * @return always true to indicate that server is alive
     */
    boolean checkIfAlive() throws RemoteException;

    /**
     * Gets a list of live servers from the coordinator
     * @return An ArrayList of known live servers.
     * @throws RemoteException
     */
    ArrayList<ServerInfo> getLiveServers() throws RemoteException;

    /**
     * Used when a server is first connecting to the system. Must be called on the coordinator.
     * The coordinator will let all current servers know that a new server is joining, by sending
     * out the new servers info.
     * @return A server ID
     * @throws RemoteException
     */
    int joinCluster(ServerInfo server, int serverId) throws RemoteException, NotCoordinatorException;

    /**
     * Sends a list of Actions to the backup servers to keep them synchronized. This method should only be called
     * by the coordinator.
     * @return 1 if successful, -1 if not successful
     * @throws RemoteException
     */
    int synchronizeActions(List<Action> actionHistory) throws RemoteException;

    /**
     * Gets Actions that have not been recorded by a server
     * @param lastSyncTime - The last known sync time of the server calling the method
     * @return An ArrayList of actions that the callee needs to be up to date
     * @throws RemoteException
     */
    ArrayList<Action> getActions(int lastSyncTime) throws RemoteException;

    /**
     * Returns the last known synchronized time
     * @return
     * @throws RemoteException
     */
    int getLastSynchronizedTime() throws RemoteException;

    /**
     * TWo phase commit methods
     */
    CommitState.State getCommitState() throws RemoteException;
    int voteRequest(Action actionToCommit) throws RemoteException;
    int abort() throws RemoteException;
    void commit() throws RemoteException;

}
