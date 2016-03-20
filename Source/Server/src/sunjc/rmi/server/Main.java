package sunjc.rmi.server;

import sunjc.rmi.shared.Service;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2){
            System.out.println("Usages: -type of server (e or c) -server name(s)");
            return;
        }

        if(null == System.getSecurityManager()) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            System.out.println(args[0]);
            if (args[0].toLowerCase().equals("e")) {
                String[] serverNames = new String[args.length-1];
                for (int i = 0; i < args.length - 1; i++) {
                    serverNames[i] = args[i+1];

                    Executor executor = new Executor();
                    executor.setName(serverNames[i]);

                    Service stubInstance = (Service) UnicastRemoteObject.exportObject(executor,0);
                    Registry reg = LocateRegistry.getRegistry();
                    reg.rebind(serverNames[i], stubInstance);

                    System.out.println("Manager: Executor Added - " + serverNames[i]);
                }

            } else if (args[0].toLowerCase().equals("c")) {
                CentralNode center = SchedulerServer.getInstance();
                center.setName(args[1]);

                Service stubInstance = (Service) UnicastRemoteObject.exportObject(center,0);
                Registry reg = LocateRegistry.getRegistry();
                reg.rebind(args[1], stubInstance);

                System.out.println("Manager: Central Node Added - " + args[1]);

            } else {
                System.out.println("no such server");
            }
        }catch (Exception e){
            System.err.println("Manager exception encountered:");
            e.printStackTrace();
        }
    }
}
