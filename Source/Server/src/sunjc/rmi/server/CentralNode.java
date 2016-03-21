package sunjc.rmi.server;
import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by SunJc on Mar/20/16.
 */
public abstract class CentralNode implements Service {

    protected String name;
    public void setName(String name){
        this.name = name;
    }
    @Override
    public String toString() {
        return name;
    }

    protected ConcurrentLinkedQueue<String> clientHistory = new ConcurrentLinkedQueue<>();

    public Stack<String> getClientHistory(){
        Stack<String> stack = new Stack<>();
        for (String s: clientHistory){
            stack.push(s);
        }
        return stack;
    }

    protected Hashtable<Service,String> nodes = nodesFactory();
    protected AbstractCollection<Job> jobs = jobsFactory();

    protected Hashtable nodesFactory(){
        return new Hashtable<Service,String>();
    }
    protected abstract AbstractCollection<Job> jobsFactory();

    // only invoked by single thread manager
    public void addServer(Service s, String serverName){
        nodes.put(s,serverName);
    }
}
