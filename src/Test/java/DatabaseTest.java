import Identity.Database.Database;
import Identity.Server.User;
import org.junit.Test;
import Identity.Generator.UserGenerator;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class DatabaseTest {

    @Test
    public void testInsertAndLookupByName() {
        Database db = new Database("jdbc:sqlite:identity-5156.db");
        db.setUp();
        UserGenerator.Initialize();
        int numUsers = 100;
        ArrayList<User> users = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            User user = UserGenerator.randomUser();
            users.add(user);
            int ret = db.createNewUser(user);
            assertEquals(1,ret);
        }
        for(int i = 0; i < numUsers; i++) {
            User user = db.getUserByName(users.get(i).getName());
            if(user == null) {
                System.err.println("NameTest: " + users.get(i));
            }
            assertEquals(users.get(i).getName(),user.getName());
        }
        db.deleteAllUsers();
        db.close();
    }

    @Test
    public void testInsertAndLookupByUUID() {
        Database db = new Database("jdbc:sqlite:identity-5156.db");
        UserGenerator.Initialize();
        int numUsers = 100;
        ArrayList<User> users = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            User user = UserGenerator.randomUser();
            users.add(user);
            int ret = db.createNewUser(user);
            assertEquals(1,ret);
        }
        for(int i = 0; i < numUsers; i++) {
            User user = db.getUserByUUID(users.get(i).getUuid());
            if(user == null) {
                System.err.println("UUIDTest: " + users.get(i));
            }
            assertEquals(users.get(i).getUuid(),user.getUuid());
        }
        db.deleteAllUsers();
        db.close();
    }

//    @Test
//    public void testTimeStampTable() {
//        Database db = new Database("jdbc:sqlite:identity.db");
//        db.setLogicalStamp(2);
//        assertEquals(2, db.getLogicalStamp().toString());
//        db.setLogicalStamp(5);
//        assertEquals(5, db.getLogicalStamp().toString());
//    }
//
//    @Test
//    public void testLogicalTimeStamp(){
//        Database db = new Database("jdbc:sqlite:identity.db");
//        db.setLogicalStamp(1);
//        assertEquals(1, db.getLogicalStamp());
//        db.setLogicalStamp(21);
//        assertEquals(21, db.getLogicalStamp());
//    }

}
