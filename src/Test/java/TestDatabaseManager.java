import Identity.Generator.TestUser;
import Identity.Generator.UserGenerator;
import Identity.Database.Database;
import Identity.Database.DatabaseManager;
import Identity.Server.User;
import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.TestCase.assertEquals;

public class TestDatabaseManager {

    String dbURL = "jdbc:sqlite:identity.db";

    @Test
    public void testCreateLookupName() {
        UserGenerator.Initialize();
        DatabaseManager dm = new DatabaseManager(dbURL);
        ArrayList<TestUser> users = new ArrayList<>();
        int numUsers = 100;
        for(int i = 0; i < 100; i++) {
            TestUser user = UserGenerator.randomUser();
            users.add(user);
            int ret = dm.createUser(user);
            assertEquals(1,ret);
        }

        for(TestUser u : users) {
            User retUser = dm.getUserByName(u.getName());
            assertEquals(u.getName(),retUser.getName());
            assertEquals(u.getUuid(),retUser.getUuid());
            assertEquals(u.getRealname(),retUser.getRealname());
        }

        Database db = new Database(dbURL);
        db.deleteAllUsers();
        db.close();
    }

    @Test
    public void testCreateLookupUuid() {
        UserGenerator.Initialize();
        DatabaseManager dm = new DatabaseManager(dbURL);
        ArrayList<TestUser> users = new ArrayList<>();
        int numUsers = 100;
        for(int i = 0; i < 100; i++) {
            TestUser user = UserGenerator.randomUser();
            users.add(user);
            int ret = dm.createUser(user);
            assertEquals(1,ret);
        }

        for(TestUser u : users) {
            User retUser = dm.getUserByUUID(u.getUuid());
            assertEquals(u.getName(),retUser.getName());
            assertEquals(u.getUuid(),retUser.getUuid());
            assertEquals(u.getRealname(),retUser.getRealname());
        }

        Database db = new Database(dbURL);
        db.deleteAllUsers();
        db.close();
    }

    @Test
    public void testCreateModifyLookupName() {
        UserGenerator.Initialize();
        DatabaseManager dm = new DatabaseManager(dbURL);
        ArrayList<TestUser> users = new ArrayList<>();
        int numUsers = 100;
        for(int i = 0; i < 100; i++) {
            TestUser user = UserGenerator.randomUser();
            users.add(user);
            int ret = dm.createUser(user);
            assertEquals(1,ret);
        }

        ArrayList<String> oldNames = new ArrayList<>();

        //Modify all names
        for(User u : users) {
            oldNames.add(u.getName());
            String newName = UserGenerator.uniqueRandomName();
            int ret = dm.changeUserName(u.getName(),newName,u.getPassHash());
            u.setName(newName);
            assertEquals(1,ret);
        }

        for(TestUser u : users) {
            User retUser = dm.getUserByName(u.getName());
            assertEquals(u.getName(),retUser.getName());
            assertEquals(u.getUuid(),retUser.getUuid());
            assertEquals(u.getRealname(),retUser.getRealname());
        }

        for(String name : oldNames) {
            User retUser = dm.getUserByName(name);
            assertEquals(null,retUser);
        }


        Database db = new Database(dbURL);
        db.deleteAllUsers();
        db.close();
    }
}
