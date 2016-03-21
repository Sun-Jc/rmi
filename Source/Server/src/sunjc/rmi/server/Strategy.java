package sunjc.rmi.server;

import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Hashtable;

/**
 * Created by SunJc on Mar/21/16.
 */
interface Strategy {

    void serverAddedAction(Service s); // single thread

    SchedulerServerFIFO.PickedServerAndJob pickServerAndJobIfJobNotExecuted(Hashtable<Service,String> servers, AbstractCollection jobs); // single thread, SchedulerThread

    Collection<Job> jobsCollectionFactory(); // single thread

    void addJobToCollection(AbstractCollection<Job> jobs, Job job); // multi-thread, execute()

    void afterExecution(Service server, Job job); // multi-thread, execute()

    void strategyChangedToThis(Hashtable<Service,String> servers, AbstractCollection jobs);
}
