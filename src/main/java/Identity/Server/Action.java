package Identity.Server;

import Identity.Database.DatabaseManager;

import java.io.Serializable;

import static Identity.Server.Action.Type.*;

public class Action implements Serializable {
    public enum Type {CREATE,DELETE,UPDATE}
    private Type type;
    private User user;
    private String newName;
    int lamportStamp;

    /**
     * Creates a new Action
     *
     * Creating a new user: Action(time,CREATE,newUser)
     * Deleting a user: Action(time,DELETE,user)
     * Updating a user: Action(time,UPDATE,user,newName)
     * Users must contain login name and a passwordHash if needed
     * @param type
     * @param args
     */
    public Action(int lamportStamp, Type type, Object... args) {
        this.lamportStamp = lamportStamp;
        this.type = type;
        this.user = (User)args[0];
        if(type == UPDATE) {
            this.newName = (String)args[1];
        }
    }

    /**
     * Executes this action in/using a given database.
     * @param dm
     * @return success code of the action
     */
    public int execute(DatabaseManager dm) {
        if(this.type == CREATE) {
            this.user.setLstamp(this.lamportStamp);
            return dm.createUser(this.user);
        }
        if (this.type == DELETE) {
            this.user.setLstamp(this.lamportStamp);
            return dm.deleteUser(this.user.getName(),this.user.getPassHash(),this.lamportStamp);
        }
        if(this.type == UPDATE) {
            this.user.setLstamp(this.lamportStamp);
            return dm.changeUserName(this.user.getName(),this.newName,this.user.getPassHash(),this.user.getLstamp());
        }
        return -1;
    }

    public int getStamp() {
        return this.lamportStamp;
    }
}
