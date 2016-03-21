package sunjc.rmi.server;

import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 *  TBD: 1. Client interface, interact double directionly
 *       2. Multi-level scheduler
 */

/**
 * ****************************************************************************
 *  Critical Resources:
 *      Mutual and solved by java thread safe type:
 *              clientHistory, apply(add, x) - apply(add, x)
 *              availableServers, addServer(1, add) - SchedulerThread(1, check empty, add, remove) - execute(x, add )
 *                   add in outer thread doesn't affect check empty or remove logic in SchedulerThread
 *              jobs, SchedulerThread(remove, check empty, 1) - execute(add, x, lock already)
 *                   add in outer thread doesn't affect SchedulerThread check or remove logic
 *      Mutual and needn't be java thread safe type:
 *
 *              nodes, addServer(add, 1) - SchedulerThread(search, 1) - execute(fetch, x)
 *                         no removing, add in outer thread doesn't affect searching
 *      Mutual and can't be solved by java thread safe type:
 *              appliedKeys; appliedServers, SchedulerThread(1, add)- execute(x, remove)
 *                          pair, java thread safe version doesn't help mutual case
 *     Sync:
 *              2.3.4.
 ******************************************************************************************************************
 *  Sync block:
 *  1. Singleton: unique instance, mutual, getInstance()-getInstance()
 *
 *  Queuing Agent
 *  2. Job block itself until server applied, sync, execute()-SchedulingTread
 *
 *  Consumer - Producer
 *  3. Scheduler block itself until job comes in, sync, execute()-SchedulingTread
 *  4. Scheduler block itself if job waiting until other job comes out, sync, execute()-SchedulingTread
 *
 *  5. Server picker of job, mutual pairs
 *
 *
 *
 *
 *
 */

/**
 * Created by SunJc on Mar/20/16.
 */
public class SchedulerServerFIFO extends CentralNode {
    /**
     * Fixed Params
     **/
    static final int SUC = 0;
    static final int FAILED = -1;
    /** End: Fixed Params**/


    /**
     * Region: Singleton
     **/
    private volatile static SchedulerServerFIFO uniqueInstance;

    public static SchedulerServerFIFO getInstance() {
        if (uniqueInstance == null) {
            synchronized (Executor.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new SchedulerServerFIFO();
                    uniqueInstance.beginSchedulerThread();
                }
            }
        }
        return uniqueInstance;
    }

    private SchedulerServerFIFO() {
    }
    /** End of Region: Singleton **/


    /**
     * Region: Prompts
     **/
    private void someoneComesPrompt(String client) {
        System.out.println(name + ": " + client + " comes");
    }

    private void jobWaitsForExecutorPrompt(String job) {
        System.out.println(name + ": " + job + " queuing...");
    }

    private void beforeExecutePrompt(String server, String job) {
        System.out.println(name + ": " + server + " is executing " + job);
    }

    private void afterExecutePrompt(String server, String job) {
        System.out.println(name + ": " + server + " done with " + job);
    }
    /** End of Region: Prompts **/


    /**
     * Region: Properties
     **/
    private volatile ArrayList<Service> appliedServers = new ArrayList<>();
    private volatile ArrayList<Integer> appliedKeys = new ArrayList<>();
    /** End of Region: Properties **/


    /**
     * Region: Strategy
     **/

    @Override
    protected AbstractCollection jobsFactory() {
        return new ArrayList<>();
    }

    ConcurrentSkipListSet<Service> availableServers = new ConcurrentSkipListSet<>();

    @Override
    public void addServer(Service s, String name) {
        super.addServer(s, name);
        availableServers.add(s);
    }

    public static class PickedServerAndJob{
        public Job job;
        public Service server;
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
                                    synchronized (appliedServers) {
                                        appliedServers.add(pickedServer);
                                        appliedKeys.add(key);
                                    }
                                    synchronized (pickedJob) {
                                        pickedJob.notify();
                                    }
                                    Thread.yield();
                                }
                            } else {
                                // poll over, very busy
                                for (Service s : nodes.keySet()) {
                                    if (!s.isBusy()) {
                                        availableServers.add(s);
                                    }
                                }
                                if (availableServers.isEmpty()){
                                    synchronized (availableServers){
                                        if (availableServers.isEmpty())
                                            availableServers.wait();
                                    }
                                }
                            }
                        }else {
                            synchronized (jobs){
                                if (jobs.isEmpty()){
                                    jobs.wait();
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
        someoneComesPrompt(s);
        clientHistory.add(s);
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
            synchronized (jobs){
                jobs.add(job);
                jobs.notify();
            }
            try {
                jobWaitsForExecutorPrompt(job.getName());
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

        // Executing
        T res = null;
        try {
            beforeExecutePrompt(nodes.get(pickedServer), job.getName());
            res = pickedServer.execute(job, password);
            afterExecutePrompt(nodes.get(pickedServer), job.getName());
            // return
            synchronized (availableServers) {
                availableServers.add(pickedServer);
                availableServers.notify();
            }
        } catch (Exception e) {
            System.err.println(name + ": Job passing exception encountered:");
            e.printStackTrace();
        }

        return res;
    }
    /** End of Region: Strategy**/
}
