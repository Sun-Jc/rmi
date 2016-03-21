package sunjc.rmi.server;

import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by SunJc on Mar/21/16.
 */
public class FIFOScheduler implements Strategy {

    static final int TIMEOUT = 3 * 1000; // period of poll over when no node is available and jobs in need

    ConcurrentSkipListSet<Service> availableServers = new ConcurrentSkipListSet<>();

    @Override
    public void serverAddedAction(Service s) {
        availableServers.add(s);
    }

    @Override
    public AbstractCollection<Job> jobsCollectionFactory() {
        return new ConcurrentLinkedQueue<>(); // add safe FIFO queue
    }


    @Override
    public SchedulerServerFIFO.PickedServerAndJob pickServerAndJobIfJobNotExecuted(Hashtable<Service,String> servers, AbstractCollection jobs) {
        SchedulerServerFIFO.PickedServerAndJob res = new SchedulerServerFIFO.PickedServerAndJob();
        // Pick up one server if available
        if (!availableServers.isEmpty()) {
            Iterator<Service> it = availableServers.iterator();
            res.server = it.next();
            availableServers.remove(res.server);
            res.job = ((Queue<Job>) jobs).peek();
        } else {
        // Or refill the available servers, without returning an available server
            try {
                // poll over
                for (Service s : servers.keySet()) {
                    if (!s.isBusy()) {
                        availableServers.add(s);
                    }
                }

                // if polling over fails, wait for jobs to return, OR WAIT FOR A SECOND
                if (availableServers.isEmpty()) {
                    synchronized (availableServers) {
                        if (availableServers.isEmpty())
                            availableServers.wait(TIMEOUT);
                    }
                }

                res.job = null;
                res.server = null;
            } catch (Exception e) {
                System.out.println("FIFO Strategy: Exception encountered: ");
                e.printStackTrace();
            }
        }
        return res;
    }

    @Override
    public void addJobToCollection(AbstractCollection<Job> jobs, Job job){
        jobs.add(job);
    }

    @Override
    public void afterExecution(Service server, Job job) {
        // give back server to available server
        synchronized (availableServers) {
            availableServers.add(server);
            availableServers.notify();
        }
    }

    @Override
    public void strategyChangedToThis(Hashtable<Service,String> servers, AbstractCollection jobs) {
        try{
            for (Service s : servers.keySet()) {
                if (!s.isBusy()) {
                    availableServers.add(s);
                }
            }

            AbstractCollection newJobsCollection = jobsCollectionFactory();
            Job[] js = (Job[])jobs.toArray(new Job[jobs.size()]);
            for (Job j: js){
                newJobsCollection.add(j);
            }
            jobs = newJobsCollection;

        }catch (Exception e){
            System.out.println("FIFO strategy changed Exception encountered");
            e.printStackTrace();
        }
    }
}
