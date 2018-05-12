package Identity.Generator;

import Identity.Server.User;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class UserGenerator {
    private static ArrayList<String> nouns;
    private static ArrayList<String> adjectives;
    private static Hashtable<String,Boolean> takenNames;
    private static boolean initialized = false;

    public static void Initialize() {
        takenNames = new Hashtable<>();
        File nounFile = new File("GeneratorWords/nouns.txt");
        File adjetivesFile = new File("GeneratorWords/adjectives.txt");
        nouns = new ArrayList<String>();
        adjectives = new ArrayList<String>();
        try {
            Scanner sc = new Scanner(nounFile);
            while(sc.hasNext()) {
                nouns.add(sc.next());
            }
            sc.close();
            sc = new Scanner(adjetivesFile);

            while(sc.hasNext()) {
                adjectives.add(sc.next());
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String randomName() {
        Random rand = new Random();
        int index = rand.nextInt(nouns.size());
        String noun = nouns.get(index);
        index = rand.nextInt(adjectives.size());
        String adjective = adjectives.get(index);
        return adjective + " " + noun;
    }

    public static String uniqueRandomName() {
        String name;
        while(takenNames.contains((name = randomName())));
        return name;
    }

    public static String randomPassword() {
        Random rand = new Random();
        int index = rand.nextInt(nouns.size());
        String noun = nouns.get(index);
        index = rand.nextInt(adjectives.size());
        String adjective = adjectives.get(index);
        return adjective + "_" + (index % 100) + noun ;
    }

    public static TestUser randomUser() {
        Random rand = new Random();
        String loginName = uniqueRandomName();
        String realName = (rand.nextInt(3) == 1) ? uniqueRandomName() : null;
        String password = (rand.nextInt(3) == 1) ? randomPassword() : null;
        String uuid = UUID.randomUUID().toString();
        String ip = "UNKOWN";
        try {
            ip = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        TestUser user = new TestUser(uuid,loginName,realName,password,ip);
        return user;
    }

    public static String[] userToArgsCreate(TestUser user) {
        ArrayList<String> args = new ArrayList<>();
        args.add("--create");
        if(user.getName() == null || user.getName().equals("null")) return null;
        args.add(user.getName());
        if(user.getRealname() != null && !user.getRealname().equals("null")) args.add(user.getRealname());
        if(user.getPassHash() != null && !user.getPassHash().equals("null")) {
            args.add("--password");
            args.add(user.getPassword());
        }

        args.add("--testing");

        return stringListToArray(args);
    }

    private static String[] stringListToArray(ArrayList<String> arr) {
        String[] retArr = new String[arr.size()];
        for(int i = 0; i < arr.size(); i++) {
            retArr[i] = arr.get(i);
        }
        return retArr;
    }

    public static String userToCreateString(TestUser user) {
        return stringArrayToString(userToArgsCreate(user));
    }

    public static String[] userToArgsLookup(User user) {
        ArrayList<String> args = new ArrayList<>();
        args.add("--lookup");
        if(user.getName() == null || user.getName().equals("null")) return null;
        args.add(user.getName());
        args.add("--testing");
        return stringListToArray(args);
    }

    public static String[] userToArgsModify(TestUser user, String newName) {
        ArrayList<String> args = new ArrayList<>();
        args.add("--modify");

        if(user.getName() == null || user.getName().equals("null")) return null;
        if(newName == null || newName.equals("null")) return null;

        args.add(user.getName());
        args.add(newName);

        if(user.getPassword() != null && !user.getPassword().equals("null")) {
            args.add("--password");
            args.add(user.getPassword());
        }
        args.add("--testing");
        return stringListToArray(args);
    }

    public static String[] userToArgsReverseLookup(User user) {
        ArrayList<String> args = new ArrayList<>();
        args.add("--reverse-lookup");
        if(user.getUuid() == null || user.getUuid().equals("null")) return null;
        args.add(user.getUuid());
        args.add("--testing");
        return stringListToArray(args);
    }

    public static String[] userToArgsDelete(TestUser user) {
        ArrayList<String> args = new ArrayList<>();
        args.add("--delete");

        if(user.getName() == null || user.getName().equals("null")) return null;

        args.add(user.getName());

        if(user.getPassword() != null && !user.getPassword().equals("null")) {
            args.add("--password");
            args.add(user.getPassword());
        }
        args.add("--testing");
        return stringListToArray(args);
    }

    public static String stringArrayToString(String[] arr) {
        String ret = "";
        for(int i = 0; i < arr.length; i++) {
            ret += "[" + arr[i] + "] ";
        }
        return ret;
    }

}
