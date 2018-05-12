package Identity.Server;

import Identity.DebugServer.DebugServer;
import Identity.DebugServer.DebugServerInterface;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Hashtable;

public class Logger {
    Hashtable<String,ArrayList<PrintWriter>> channels;
    Hashtable<String,String> prefixes;
    DebugServerInterface server = null;
    String serverChannel = "";
    int serverId = 0;

    public Logger() {
        channels = new Hashtable<>();
        prefixes = new Hashtable<>();
    }

    public void addChannel(String channel, OutputStream stream) {
        PrintWriter writer = null;
        if(stream != null) writer = new PrintWriter(stream);
        ArrayList<PrintWriter> list = new ArrayList();
        list.add(writer);
        channels.put(channel,list);
    }

    public int addServerChannel(String channel,String host,int registryPort) {
        if(server != null) return -1;
        try {
            server = (DebugServerInterface) getRemoteObject(host,registryPort,"DebugServer");
            this.serverId = server.connect();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        } catch (NotBoundException e) {
            e.printStackTrace();
            return -1;
        }
        this.serverChannel = channel;
        return 1;
    }
    public void setServerId(int id) {
        this.serverId = id;
    }

    public void addStream(String channel, OutputStream stream) {
        channels.get(channel).add(new PrintWriter(stream));
    }

    public void setPrefix(String channel, String prefix) {
        prefixes.put(channel,prefix);
    }

    public void logClient(String serverChannel,String info) {
        if(serverChannel.equals(this.serverChannel)) {
            try {
                server.logClient("CLIENT: " + info);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(info);
        }
    }

    public void log(String channel,String info) {
        if(channel.equals(serverChannel)) {
            try {
                server.log(serverId,info);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        ArrayList<PrintWriter> writers = channels.get(channel);
        String prefix = prefixes.get(channel);
        if(prefix == null) prefix = "";
        if(writers == null) {
            System.out.println(prefix + info);
            return;
        }
        for(PrintWriter writer : writers) {
            if(writer != null) {
                writer.println(prefix + info);
                writer.flush();
            }
        }
    }

    /**
     * Gets a remote object from a server
     * @return
     * @throws RemoteException
     * @throws NotBoundException
     */
    private DebugServerInterface getRemoteObject(String host, int registryPort, String name) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, registryPort);
        DebugServerInterface remObj = (DebugServerInterface)registry.lookup(name);
        return  remObj;
    }
}
