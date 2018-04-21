package Identity.Database;

import Identity.Server.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

/**
 *Sqlite database Class
 */
public class Database {

    Connection conn = null;
    String url = "";
    Logger log;

    public Database(String url) {
        this.url = url;
        log = Logger.getLogger("logger");
        log.setUseParentHandlers(false);
        connect();
    }

    /**
     * Connect to a sample database
     */
    public void connect() {
        try {
            // create a connection to the database
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            log.severe("Error in connect:\n" + e.getStackTrace());
        } catch (ClassNotFoundException e) {
            log.severe("Error in connect:\n" + e.getStackTrace());
        }
    }

    /**
     * Closes database handler
     */
    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.severe("Error in close:\n" + e.getStackTrace());
        }
    }

    /**
     * Return a list of all users in the database
     * @return list of all users
     */
    public ArrayList<User> getUsers() {
        ArrayList<User> users = new ArrayList<User>();
        try {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("select * from users;");
            while(rs.next()) {
                User u = new User(rs.getString("uuid"),rs.getString("name"),rs.getString("realName"),rs.getString("passHash"),rs.getString("ipAddress"));
                u.setDate(rs.getTimestamp("date"));
                users.add(u);

            }
        } catch (SQLException e) {
            log.severe("Error in getUsers:\n" + e.getStackTrace());
        }
        return users;
    }

    /**
     * Gets a user with the specified uuid. or null if there isn't a user with the uuid
     * @param uuid
     * @return User
     */
    public User getUserByUUID(String uuid) {
        User user = null;
        try {
            PreparedStatement stmt = conn.prepareStatement("select * from users where uuid = ? ;");
            stmt.setString(1,uuid);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                user = new User(rs.getString("uuid"),rs.getString("name"),rs.getString("realName"));
                user.setDate(rs.getTimestamp("date"));
            }
        } catch (SQLException e) {
            log.severe("Error in getUserByUUID:\n" + e.getStackTrace());
        }
        return user;
    }

    /**
     * Gets a user with the specified login name or null
     * @param name
     * @return User
     */
    public User getUserByName(String name) {
        User user = null;
        try {
            PreparedStatement stmt = conn.prepareStatement("select * from users where name = ? ;");
            stmt.setString(1,name);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                user = new User(rs.getString("uuid"),rs.getString("name"),rs.getString("realName"));
                user.setDate(rs.getTimestamp("date"));
            }
        } catch (SQLException e) {
            log.severe("Error in getUserByName:\n" + e.getStackTrace());
        }
        return user;
    }

    /**
     * Creates a new user returns 1 if the user was successfully created
     *
     * success codes:
     * 1    the user was added successfully
     * -1   unknown error in query
     * -2   username is not unique, the user was not created
     * @param user
     * @return successCode
     */
    public synchronized int createNewUser(User user) {
        PreparedStatement stmt = null;
        try {
            Date date = new Date();
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());

            stmt = conn.prepareStatement("select * from users where name = ? ;");
            stmt.setString(1,user.getName());

            ResultSet rs = stmt.executeQuery();//statement.executeQuery("select * from users where name = '" + username + "';");
            if (rs.next()) {
                log.severe("Error in createNewUser: loginname is already taken\n");
                return -2; //The query should be empty and rs.next() should return false
            }
            stmt = conn.prepareStatement("insert into users values (?,?,?,?,?,?);");
            stmt.setString(1,user.getUuid());
            stmt.setString(2,user.getName());
            stmt.setDate(3,sqlDate);
            stmt.setString(4,user.getPassHash());
            stmt.setString(5,user.getRealname());
            stmt.setString(6,user.getIpAddress());
            stmt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            log.severe("Error in createNewUser:\n" + e.toString());
            return -1;
        }
        return 1;
    }

    /**
     * Deletes a user using their uuid
     *
     * success code:
     * 1    the user was successfully deleted
     * -1   their was an unknown error, the user was probably not deleted
     *
     * @param user
     * @return success code
     */
    public synchronized int deleteUser(User user) {
        try {
            String uuid = user.getUuid();
            PreparedStatement stmt = conn.prepareStatement("delete from users where uuid = ?;");
            stmt.setString(1,uuid);
            stmt.execute();
        } catch (SQLException e) {
            log.severe("Error in deleteUser:\n" + e.getStackTrace());
            return -1;
        }
        return 1;
    }

    /**
     * Deletes a user using their uuid
     * @param uuid
     * @return success code
     */
    public int deleteUserByUUID(String uuid) {
        User user = new User(uuid);
        return deleteUser(user);
    }

    /**
     * Modifies a users login name.
     * @param UUID
     * @param newName
     * @return 1 if successful, -1 if sql error
     */
    public int changeUserName(String UUID, String newName) {
        try {
            PreparedStatement stmt = conn.prepareStatement("update users set name = ? where uuid = ? ;");
            stmt.setString(1,newName);
            stmt.setString(2,UUID);
            stmt.execute();
        } catch (SQLException e) {
            log.severe("Error in changeUserName:\n" + e.getStackTrace());
            return -1;
        }
        return 1;
    }

    /**
     * Deletes all users in the database
     */
    public void deleteAllUsers() {
        try {
            Statement statement = conn.createStatement();
            statement.executeUpdate("delete * from users;");
        } catch (SQLException e) {
            log.severe("Error in deleteAllUsers:\n" + e.getStackTrace());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Database db = null;
        try {
            db = new Database("jdbc:sqlite:identity.db");
            ArrayList<User> users = db.getUsers();
            System.out.println(UUID.randomUUID());
            for(User u : users) {
                System.out.println(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}
