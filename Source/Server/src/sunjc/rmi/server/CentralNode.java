package sunjc.rmi.server;
import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;

import java.util.AbstractCollection;
import java.util.Hashtable;
import java.util.Stack;

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

    protected Stack<String> clientHistory = new Stack<>();

    public Stack<String> getClientHistory(){
        return clientHistory;
    }

    protected Hashtable<Service,String> nodes = nodesFactory();
    protected AbstractCollection<Job> jobs = jobsFactory();

    protected Hashtable nodesFactory(){
        return new Hashtable<Service,String>();
    }
    protected abstract AbstractCollection<Job> jobsFactory();

    public void addServer(Service s, String serverName){
        nodes.put(s,serverName);
    }
}
