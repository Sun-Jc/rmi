package sunjc.rmi.client;

import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    /**
     * Fixed Params
     **/
    static final int SUC = 0;
    static final int FAILED = -1;

    /**
     * End: Fixed Params
     **/

    public static void main(String[] args) {
        String clientName;
        String hostLocation;
        String serverName;
        String jobName;
        int sleepTime;
        try {
            clientName = args[0];
            hostLocation = args[1];
            serverName = args[2];
            jobName = args[3];
            sleepTime = Integer.parseInt(args[4]);
        } catch (Exception e) {
            System.out.println("Usages: -client name -host name -server name -job name -sleep time");
            return;
        }

        if (null == System.getSecurityManager()) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            Job<Data> MrJob = new MrJobLovesSleeping(jobName, sleepTime);
            Registry reg = LocateRegistry.getRegistry(hostLocation);
            Service s = (Service) reg.lookup(serverName);

            System.out.println("sending Mr Job");

            int key = s.apply(clientName);

            if (key != FAILED) {
                Data y = s.execute(MrJob, key);
                System.out.println("Mr Job returned, with " + y.dataType + y.value);
            }

        } catch (Exception e) {
            System.err.println("Client exception encountered:");
            e.printStackTrace();
        }
    }

}
