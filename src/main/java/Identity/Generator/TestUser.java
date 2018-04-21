package Identity.Generator;

import Identity.Server.User;

import java.net.InetAddress;

public class TestUser extends User {

    private String password;

    public TestUser(String uuid, String name, String realname, String password, String ipAddress) {
        super(uuid, name, realname, (password == null || password.equals("null")) ? null : Identity.Client.SHA2.trySHA(password), ipAddress);
        this.password = password;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
