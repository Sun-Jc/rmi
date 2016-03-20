package sunjc.rmi.server;

import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.rmi.RemoteException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by SunJc on Mar/20/16.
 */
public class SchedulerServer extends CentralNode {
    /**
     * Fixed Params
     **/
    static final int SUC = 0;
    static final int FAILED = -1;
    /** End: Fixed Params**/


    /**
     * Region: Singleton
     **/
    private volatile static SchedulerServer uniqueInstance;

    public static SchedulerServer getInstance() {
        if (uniqueInstance == null) {
            synchronized (Executor.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new SchedulerServer();
                    uniqueInstance.beginSchedulerThread();
                }
            }
        }
        return uniqueInstance;
    }

    private SchedulerServer() {
    }
    /** End of Region: Singleton **/


    /**
     * Region: Factory
     **/
    @Override
    protected AbstractCollection jobsFactory() {
        return new ConcurrentLinkedQueue<Job>();
    }
    /** End of Region: Factory **/


    /**
     * Region: Prompts
     **/
    private void someoneComes(String client) {
        System.out.println(name + ":" + client + " comes");
    }

    private void jobWaitsForExecutor(String job) {
        System.out.println(name + ": " + job + " queuing...");
    }

    private void beforeExecutePrompt(String server, String job) {
        System.out.println(name + ": " + server + " is executing " + job);
    }

    private void afterExecutePrompt(String server, String job) {
        System.out.println(name + ": " + server + " done with " + job);
    }
    /** End of Region: hooks **/


    /**
     * Region: Properties
     **/
    private volatile ArrayList<Service> appliedServers = new ArrayList<>();
    private volatile ArrayList<Integer> appliedKeys = new ArrayList<>();
    /** End of Region: Properties **/


    /**
     * Region: Strategy
     **/

    ConcurrentLinkedQueue<Service> availableServers = new ConcurrentLinkedQueue<>();

    @Override
    public void addServer(Service s, String name) {
        super.addServer(s, name);
        availableServers.add(s);
    }

    private void beginSchedulerThread() {
        Thread scheduling = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while (true) {
                        if (!jobs.isEmpty()) {

                            // pick a server to the head of list
                            // acquiring a chance of executing
                            // unique thread handling scheduling, synchronized not needed
                            if (!availableServers.isEmpty()) {
                                Service pickedServer = null;
                                Job pickedJob;
                                int key = FAILED;

                                Iterator<Service> it = availableServers.iterator();
                                pickedServer = it.next();
                                availableServers.remove(pickedServer);
                                key = pickedServer.apply(name);

                                if (key != FAILED) {
                                    pickedJob = ((Queue<Job>) jobs).poll();
                                    appliedServers.add(pickedServer);
                                    appliedKeys.add(key);
                                    synchronized (pickedJob) {
                                        pickedJob.notify();
                                    }
                                }
                            } else {
                                for (Service s : nodes.keySet()) {
                                    if (!s.isBusy()) {
                                        availableServers.add(s);
                                    }
                                }
                            }

                        }
                    }
                } catch (Exception e) {
                    System.err.println("Scheduler exception encountered:");
                    e.printStackTrace();
                }

            }
        };
        scheduling.start();
    }

    @Override
    public int apply(String s) throws RemoteException {
        someoneComes(s);
        clientHistory.push(s);
        return SUC;
    }

    @Override
    public boolean isBusy() throws RemoteException {
        return false;
    }

    @Override
    public <T> T execute(Job<T> job, int key) throws RemoteException {
        if (SUC != key) {
            throw new IllegalStateException();
        }

        // wait, watch this order
        synchronized (job) {
            jobs.add(job);
            try {
                jobWaitsForExecutor(job.getName());
                job.wait();
            } catch (Exception e) {
                System.err.println(name + ": Job waiting exception encountered:");
                e.printStackTrace();
            }
        }

        // thread-safe pick-out
        Service pickedServer;
        int password;
        synchronized (appliedServers) {
            if (appliedServers.size() < 1) {
                System.out.println("#########: should be impossible, let us reconsider the concurrent design");
            }
            pickedServer = appliedServers.remove(0);
            password = appliedKeys.remove(0);
        }

        T res = null;
        try {
            beforeExecutePrompt(nodes.get(pickedServer), job.getName());
            res = pickedServer.execute(job, password);
            afterExecutePrompt(nodes.get(pickedServer), job.getName());
        } catch (Exception e) {
            System.err.println(name + ": Job passing exception encountered:");
            e.printStackTrace();
        }

        return res;
    }
    /** End of Region: Strategy**/
}
