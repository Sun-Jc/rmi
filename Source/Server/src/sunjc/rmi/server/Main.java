package sunjc.rmi.server;

import sunjc.rmi.shared.Service;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usages: -type of server (e for executor, d for distributor) -server name(s)");
            return;
        }

        if (null == System.getSecurityManager()) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            if (args[0].toLowerCase().equals("e")) {
                String[] serverNames = new String[args.length - 1];
                Executor[] executors = new Executor[args.length - 1];
                for (int i = 0; i < args.length - 1; i++) {
                    serverNames[i] = args[i + 1];

                    Executor executor = new Executor();
                    executors[i] = executor;
                    executor.setName(serverNames[i]);

                    Service stubInstance = (Service) UnicastRemoteObject.exportObject(executor, 0);
                    Registry reg = LocateRegistry.getRegistry();
                    reg.rebind(serverNames[i], stubInstance);

                    System.out.println("Manager: Executor Added - " + serverNames[i]);
                }

                while (true) {
                    Scanner in = new Scanner(System.in);
                    System.out.println("Manager: What are you going to do? 1. View Client History 2. View Jobs Loaded");
                    String input = in.next();
                    if (input.equals("1")) {
                        System.out.println("whose?");
                        String name = in.next().toLowerCase();
                        for (int i = 0; i < serverNames.length; i++) {
                            if (name.equals(serverNames[i].toLowerCase())) {
                                dispHistory(executors[i].getClientHistory(), "Clients who visited");
                                break;
                            }
                        }
                    } else if (input.equals("2")) {
                        System.out.println("whose?");
                        String name = in.next().toLowerCase();
                        for (int i = 0; i < serverNames.length; i++) {
                            if (name.equals(serverNames[i].toLowerCase())) {
                                dispHistory(executors[i].getJobHistory(), "Ever running jobs");
                                break;
                            }
                        }
                    }
                }

            } else if (args[0].toLowerCase().equals("d")) {
                Distributor center = DistributorWithSchedulerStrategy.getInstance();
                center.setName(args[1]);

                Service stubInstance = (Service) UnicastRemoteObject.exportObject(center, 0);
                Registry reg = LocateRegistry.getRegistry();
                reg.rebind(args[1], stubInstance);

                System.out.println("Manager: Distributor Running - " + args[1]);


                String[] names = {"Alpha", "Beta", "Gamma"};
                for (String s : names) {
                    Registry regForE = LocateRegistry.getRegistry("127.0.0.1");
                    Service executor = (Service) regForE.lookup(s);
                    center.addServer(executor, s);
                    System.out.println("Manager: node " + s + " added to " + center);
                }


                Scanner in = new Scanner(System.in);
                while (true) {
                    System.out.println("Manager: What are you going to do? 1. View Client History 2. Add new nodes");
                    String input = in.next();
                    if (input.equals("1")) {
                        dispHistory(center.getClientHistory(), "Clients who visited");
                    } else if (input.equals("2")) {

                        System.out.println("Manager: Ready to add new nodes for Distributor: -host name -service name");
                        String hostLocation = in.next();

                        String serverName = in.next();

                        System.out.println("Manager: Are you sure? (n/y)");
                        if (in.next().toLowerCase().equals("y")) {

                            Registry regForE = LocateRegistry.getRegistry(hostLocation);
                            Service executor = (Service) regForE.lookup(serverName);
                            center.addServer(executor, serverName);

                            System.out.println("Manager: node " + serverName + " added to " + center);
                        }
                    }
                }

            } else {
                System.out.println("no such kind");
            }
        } catch (Exception e) {
            System.err.println("Manager exception encountered:");
            e.printStackTrace();
        }
    }

    static void dispHistory(AbstractList<String> s, String historyName) {
        System.out.println(historyName + ":");
        for (int i = 0; i < s.size(); i++) {
            System.out.println(i + " : " + s.get(i));
        }

    }
}
