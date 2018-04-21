package Identity.Server;

import java.io.Serializable;
import java.util.Date;

/**
 * Class to store info about a user
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1281949658399440079L;
    private String uuid;
    private String name;
    private Date date;
    private transient String passHash;
    private transient String ipAddress;
    private String realname;

    /**
     * Default Constructor, does not initialize any values.
     * All values are null
     */
    public User() {

    }

    /**
     * Sets uuid, but all other values are null
     * @param uuid
     */
    public User(String uuid) {
        this.uuid = uuid;
        this.name = null;
        this.passHash = null;
        this.realname = null;
    }

    /**
     * Sets uuid name and realname. All other values are null
     * @param uuid
     * @param name
     * @param realname
     */
    public User(String uuid, String name, String realname) {
        this.uuid = uuid;
        this.name = name;
        this.passHash = null;
        this.realname = realname;
    }

    /**
     * Sets uuid, name, realname, passHash, and ipaddress.
     * @param uuid
     * @param name
     * @param realname
     * @param passHash
     * @param ipAddress
     */
    public User(String uuid, String name, String realname, String passHash, String ipAddress) {
        this.uuid = uuid;
        this.name = name;
        this.passHash = passHash;
        this.realname = realname;
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getPassHash() {
        return passHash;
    }

    public void setPassHash(String passHash) {
        this.passHash = passHash;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * String with username, UUID, and Hash
     * @return string describing user
     */
    public String toString() {
        String date;
        if(this.date == null) {
            date = "UNAVAILABLE";
        } else {
            date = this.date.toString();
        }
        return "[Username: " + this.name + " UUID: " + this.uuid + " Date: " + date + " Hash: " + passHash + "]";
    }

    /**
     * String with username and UUID
     * @return string describing user without passwordHash
     */
    public String publicString() {
        return "[Login name: " + this.name + " UUID: " + this.uuid + "]";

    }

}
