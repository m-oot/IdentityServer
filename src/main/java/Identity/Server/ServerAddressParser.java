package Identity.Server;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class ServerAddressParser {

    private Scanner scanner;

    public ServerAddressParser(String filename) {
        try{
            File file = new File(filename);
            scanner = new Scanner(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ServerInfo> getServerInfo() {
        if(scanner == null) return null;
        ArrayList<ServerInfo> servers = new ArrayList<>();
        while(scanner.hasNextLine()) {
            String nextLine = scanner.nextLine();
            if(!nextLine.trim().isEmpty()) {
                Scanner lineScanner = new Scanner(nextLine);
                String ipAddress = lineScanner.next();
                if(ipAddress.toLowerCase().equals("localhost")){
                    try {
                        ipAddress =InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
                int port = Integer.parseInt(lineScanner.next());
                servers.add(new ServerInfo(ipAddress, port));
            }
        }
        return servers;
    }
}
