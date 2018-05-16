package Identity.Database;

import Identity.Server.User;

import java.util.*;

/**
 * Manages a database in memory, and periodically dumps the memory database
 * to a sql database. Makes use of lambda functions for dumping!
 */
public class DatabaseManager {
    private Hashtable<String,User> usersById;    //In memory database of users with user ID as key
    private Hashtable<String,User> usersByName;  //In memory database of users with user name as key
    private Database db;                         //Database on disk
    private List<Runnable> databaseChanges;      //list of in memory changes that need to be dumped to disk
    private Timer dumper;                        //A timer task that dumps databaseChanges to the disk periodically

    /**
     * Constructs a database manager. Initializes a sql database, reads in
     * users from the database into hashtables, and starts a timer that
     * periodically dumps changes to the database.
     * @param dbString
     */
    public DatabaseManager(String dbString) {
        usersById = new Hashtable<String,User>();
        usersByName = new Hashtable<String,User>();
        databaseChanges = Collections.synchronizedList(new ArrayList<Runnable>());
        db = new Database(dbString);

        //Putting any data stored in the database in memory
        ArrayList<User> users = db.getUsers();
        for(User u : users) {
            usersById.put(u.getUuid(),u);
            usersByName.put(u.getName(),u);
        }

        dumper = new Timer();
        dumper.scheduleAtFixedRate(new Dump(this),0,5000);
    }

    /**
     * Sets up the database (makes sure needed tables exist)
     */
    public void setUp() {
        db.setUp();
    }

    /**
     * Sets the servers ID
     * @param serverID
     * @return
     */
    public int setServerID(int serverID){
        return db.setServerID(serverID);
    }

    /**
     * Gets the servers ID
     * @return
     */
    public int getServerID(){
        return db.getServerID();
    }

    /**
     * Gets the current commit state of the server
     * @return
     */
    public int getCommitState() {
        return db.getCommitState();
    }

    /**
     * Sets the commit state of the server
     * @param state - The state to set the server
     * @return
     */
    public int setCommitState(int state) {
        return db.setCommitState(state);
    }

    /**
     * Gets the last stored logical stamp. Returns 0 if the stamp has never been updated.
     * @return
     */
    public int getLogicalStamp(){
        return db.getLogicalStamp();
    }


    /**
     * Creates a user in memory database, and adds change to be commited to database
     * @param user
     * @return
     */
    public synchronized int createUser(User user) {
        if(validNewUser(user)) {
            usersById.put(user.getUuid(),user);
            usersByName.put(user.getName(),user);
            Runnable command = () -> db.createNewUser(user); //Add database change to command list
            databaseChanges.add(command);
            return 1;
        }
        return -1;
    }

    /**
     * Deletes a user if user exists, and the correct password is provided. No password is required if the users password is null.
     *
     * Returns Success Code:
     * 1    The user was deleted
     * -1   No user by that login name existed
     * -2   The passwords did not match
     * @param loginName
     * @param passwordHash
     * @return
     */
    public synchronized int deleteUser(String loginName, String passwordHash,int lstamp) {
        User user = usersByName.get(loginName);
        if(user == null) {
            return -1;
        }
        if(user.getPassHash() == null || user.getPassHash().equals("null") || user.getPassHash().equals(passwordHash)) {
            User u = usersByName.get(loginName);
            u.setLstamp(lstamp);
            String uuid = u.getUuid();
            usersByName.remove(loginName);
            usersById.remove(uuid);
            Runnable command = () -> db.deleteUserByUUID(uuid,lstamp);
	        databaseChanges.add(command);
            return 1;
        } else {
            return -2; //Password did not match
        }
    }

    /**
     * Gets a user by name. Returns null if no user has that login name
     * @param name
     * @return User
     */
    public User getUserByName(String name) {
        return usersByName.get(name);
    }

    /**
     * Gets a user by UUID. Returns null if there is not user with that UUID
     * @param UUID
     * @return User
     */
    public User getUserByUUID(String UUID) {
        return usersById.get(UUID);
    }

    /**
     * Changes a users loginName
     *
     * returns success code:
     * 1    The user's login name was changed
     * -1   There was no user with that login name
     * -2   The passwords did not match (Invalid Password)
     * @param old
     * @param newName
     * @param passwordHash
     * @return
     */
    public synchronized int changeUserName(String old, String newName, String passwordHash, int lStamp) {
        User user = usersByName.get(old);
        if(user == null) return -1;
        if(user.getPassHash() == null || user.getPassHash().equals("null") || user.getPassHash().equals(passwordHash)) {
            user.setName(newName); //This should be reflected in both hashtables
            user.setLstamp(lStamp);
            usersByName.remove(old); //The old name is no longer mapped to a user, so remove the name,user pair
            usersByName.put(user.getName(),user); //Put the new name,user pair into the table
            String UUID = user.getUuid();
            Runnable command = () -> db.changeUserName(UUID,newName,lStamp);
            databaseChanges.add(command);
            return 1;
        } else {
            return -2;
        }
    }

    /**
     * Gets all users in the database
     * @return list of all users
     */
    public ArrayList<User> getUsers() {
        ArrayList<User> users = new ArrayList<>();
        for(User u : usersByName.values()) {
            users.add(u);
        }
        return users;
    }

    /**
     * Checks that a users login name and UUID(we don't have to but why not?) are not taken.
     * @param user
     * @return true if user is valid
     */
    public boolean validNewUser(User user) {
        for(User u : usersById.values()) {
            if(u.getUuid().equals(user.getUuid())) {
                return false;
            } else if (u.getName().equals(user.getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Makes all recent changes to database
     */
    public void updateDatabase() {
        while(databaseChanges.size() > 0) {
            databaseChanges.get(0).run();
            databaseChanges.remove(0);
        }
    }


    /**
     * Timer that periodically dumps changes to database
     */
    public class Dump extends TimerTask {

        public DatabaseManager dm;
        public Dump(DatabaseManager dm) {
            this.dm = dm;
        }

        @Override
        public void run() {
            this.dm.updateDatabase();
        }
    }


}
