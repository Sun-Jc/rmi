package sunjc.rmi.server;

import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.rmi.RemoteException;
import java.util.AbstractCollection;
import java.util.ArrayList;

/**
 * TBD: 1. Client interface, interact double directionly
 * 2. Multi-level scheduler
 * <p>
 * ****************************************************************************
 * Critical Resources:
 *
 * Mutual and solved by java thread safe type:
 *
 * clientHistory, apply(add, x) - apply(add, x)
 *
 * jobs, SchedulerThread(remove, check empty, 1) - execute(add, x, lock already)
 * add in outer thread doesn't affect SchedulerThread check or remove logic
 *
 *
 * Mutual and needn't be java thread safe type:
 *
 * availableServers, addServer(1, add) - SchedulerThread(1, check empty, add, remove) - execute(x, add )
 * add in outer thread doesn't affect check empty or remove logic in SchedulerThread
 *
 *
 * nodes, addServer(add, 1) - SchedulerThread(search, 1) - execute(fetch, x)
 * no removing, add in outer thread doesn't affect searching
 *
 * Mutual and can't be solved by java thread safe type:
 *
 * appliedKeys; appliedServers, SchedulerThread(1, add)- execute(x, remove)
 * pair, java thread safe version doesn't help mutual case
 * Sync:
 * 2.3.4.
 * *****************************************************************************************************************
 * Sync block:
 * 1. Singleton: unique instance, mutual, getInstance()-getInstance()
 * <p>
 * Queuing Agent
 * 2. Job block itself until server applied, sync, execute()-SchedulingTread
 * <p>
 * Consumer - Producer
 * 3. Scheduler block itself until job comes in, sync, execute()-SchedulingTread
 * 4. Scheduler block itself if job waiting until other job comes out, sync, execute()-SchedulingTread
 * <p>
 * 5. Server picker of job, mutual pairs
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
        jobs = jobsFactory();
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



    /** Region: without strategy methods */
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
    /** End of Region: without strategy methods */



    /**
     * Region: Strategy
     **/

    static Strategy DEAFULT_STRATEGY = new FIFOScheduler();

    Strategy strategy = DEAFULT_STRATEGY;

    // rarely used
    public void changeStrategy() {
        while (!jobs.isEmpty()) ;
        strategy.strategyChangedToThis(nodes);
    }

    @Override
    protected AbstractCollection jobsFactory() {
        return strategy.jobsCollectionFactory();
    }

    @Override
    public void addServer(Service s, String name) {
        super.addServer(s, name);
        strategy.serverAddedAction(s);
    }

    public static class PickedServerAndJob {
        public Job job;
        public Service server;
    }

    private Thread scheduling = new Thread() {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    if (!jobs.isEmpty()) {

                        PickedServerAndJob picked = strategy.pickServerAndJobIfJobNotExecuted(nodes, jobs);
                        if (picked.server != null) {
                            int key = picked.server.apply(name);
                            if (key != FAILED) {
                                jobs.remove(picked.job);

                                synchronized (appliedServers) {
                                    appliedServers.add(picked.server);
                                    appliedKeys.add(key);
                                }

                                synchronized (picked.job) {
                                    picked.job.notify();
                                }

                                Thread.yield();
                            }
                        }
                    } else {
                        synchronized (jobs) {
                            if (jobs.isEmpty()) {
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

    private void beginSchedulerThread() {
        scheduling.start();
    }

    @Override
    public <T> T execute(Job<T> job, int key) throws RemoteException {
        if (SUC != key) {
            throw new IllegalStateException();
        }

        // wait, watch this order
        synchronized (job) {
            synchronized (jobs) {
                strategy.addJobToCollection(jobs,job);
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
            strategy.afterExecution(pickedServer,job);
        } catch (Exception e) {
            System.err.println(name + ": Job passing exception encountered:");
            e.printStackTrace();
        }

        return res;
    }
    /** End of Region: Strategy**/
}
