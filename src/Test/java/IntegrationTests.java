import Identity.Client.IdClient;
import Identity.Generator.TestUser;
import Identity.Generator.UserGenerator;
import Identity.Database.Database;
import Identity.Server.IdServer;
import Identity.Server.User;
import org.junit.Test;

import static org.junit.Assert.*;


import java.io.IOException;
import java.util.ArrayList;

public class IntegrationTests {

    private static String dbURL = "jdbc:sqlite:identity.db";

    @Test
    public void testClientCreateLookup() {
        UserGenerator.Initialize();
        int numUsers = 100;

        ArrayList<TestUser> users = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            TestUser user = UserGenerator.randomUser();
            users.add(user);
            String[] userCreateArgs = UserGenerator.userToArgsCreate(user);
            assertEquals(1,IdClient.test(userCreateArgs));
        }
        for(User u : users) {
            String[] userLookupArgs = UserGenerator.userToArgsLookup(u);
            assertEquals(1,IdClient.test(userLookupArgs));
        }

    }

    @Test
    public void testClientLookupNegative() {
        UserGenerator.Initialize();
        int numUsers = 100;

        ArrayList<TestUser> users = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            TestUser user = UserGenerator.randomUser();
            users.add(user);
        }
        for(User u : users) {
            String[] userLookupArgs = UserGenerator.userToArgsLookup(u);
            assertEquals(-1, IdClient.test(userLookupArgs));
        }

    }

    @Test
    public void testReverseLookup() {
        UserGenerator.Initialize();
        int numUsers = 100;

        ArrayList<TestUser> users = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            TestUser user = UserGenerator.randomUser();
            users.add(user);
            String[] userCreateArgs = UserGenerator.userToArgsCreate(user);
            IdClient.test(userCreateArgs);
        }
        for(User u : users) {
            String[] userLookupArgs = UserGenerator.userToArgsReverseLookup(u);
            try {
                IdClient.main(userLookupArgs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Testing for negative results
        ArrayList<TestUser> nonExistingUsers = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            TestUser user = UserGenerator.randomUser();
            nonExistingUsers.add(user);
        }
        for(User u : users) {
            String[] userReverseLookupArgs = UserGenerator.userToArgsReverseLookup(u);
            assertEquals(-1,IdClient.test(userReverseLookupArgs));
        }

    }

    @Test
    public void testModify() {
        UserGenerator.Initialize();
        int numUsers = 100;

        //Generating users and adding them to the DB
        ArrayList<TestUser> users = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            TestUser user = UserGenerator.randomUser();
            users.add(user);
            String[] userCreateArgs = UserGenerator.userToArgsCreate(user);
            IdClient.test(userCreateArgs);
        }
        //Modifying users
        for(TestUser u : users) {
            String newName = UserGenerator.uniqueRandomName();
            String[] userModifyArgs = UserGenerator.userToArgsModify(u, newName);
            int retModify = IdClient.test(userModifyArgs);
            assertEquals(1,retModify);
            assertEquals(-1,IdClient.test(UserGenerator.userToArgsLookup(u)));
        }

    }

    @Test
    public void testClientCreateDeleteLookup() {
        UserGenerator.Initialize();
        int numUsers = 1;

        ArrayList<TestUser> usersToDelete = new ArrayList<>();
        ArrayList<TestUser> usersNotToDelete = new ArrayList<>();
        for(int i = 0; i < numUsers; i++) {
            TestUser user = UserGenerator.randomUser();
            //We will delete every other user generated
            if ((i % 2) == 0) {
                usersToDelete.add(user);
            } else {
                usersNotToDelete.add(user);
            }
            String[] userCreateArgs = UserGenerator.userToArgsCreate(user);
            IdClient.test(userCreateArgs);
        }
        for(TestUser u : usersToDelete) {
            String[] userDeleteArgs = UserGenerator.userToArgsDelete(u);
            IdClient.test(userDeleteArgs);
        }
        for(User u : usersToDelete) {
            String[] userLookupArgs = UserGenerator.userToArgsLookup(u);
            int res = IdClient.test(userLookupArgs);
            assertEquals(-1,res);
        }
        for(User u : usersNotToDelete) {
            String[] userLookupArgs = UserGenerator.userToArgsLookup(u);
            int res = IdClient.test(userLookupArgs);
            assertEquals(1,res);
        }
    }

}
