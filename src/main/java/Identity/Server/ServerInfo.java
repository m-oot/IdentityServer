package Identity.Server;

import org.apache.maven.settings.Server;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * contains the information on how to connect to a server.
 */
public class ServerInfo implements Serializable {

    private String hostIpAddress;
    private int registryPort;
    private int serverId;
    private transient IdentityServerClusterInterface remObj;

    public ServerInfo(String hostIpAddress, int registryPort) {
        this.hostIpAddress = hostIpAddress;
        this.registryPort = registryPort;
        serverId = -1;
    }

    public ServerInfo(String hostIpAddress, int registryPort, IdentityServerClusterInterface remObj) {
        this.hostIpAddress = hostIpAddress;
        this.registryPort = registryPort;
        this.remObj = remObj;

    }

    public String toString() {
        return "IP Address: " + hostIpAddress + " Port: " + registryPort + " Server ID: " + serverId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    public void setHostIpAddress(String hostIpAddress) {
        this.hostIpAddress = hostIpAddress;
    }

    public int getRegistryPort() {
        return registryPort;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    public IdentityServerClusterInterface getRemObj() throws RemoteException{
        if(remObj == null){throw new RemoteException();}
        return remObj;
    }

    public void setRemObj(IdentityServerClusterInterface remObj) {
        this.remObj = remObj;
    }

    @Override
    public boolean equals(Object o){
        ServerInfo serverToCompare = (ServerInfo) o;
        return this.getHostIpAddress().equals(serverToCompare.getHostIpAddress()) & (this.getRegistryPort() == serverToCompare.getRegistryPort());
    }
}
