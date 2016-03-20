package sunjc.rmi.server;
import sunjc.rmi.shared.Job;
import sunjc.rmi.shared.Service;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Random;
import java.util.Stack;

/**
 * Created by SunJc on Mar/20/16.
 */
public class Executor implements Service {

    /** Fixed Params **/
    static final long maxWaitingTime = 60 * 1000;
    static final int FAILED = -1;
    /** End: Fixed Params**/

    /** Region: Singleton **/
    /*private volatile static Executor uniqueInstance;

    public static Executor getInstance(){
        if(uniqueInstance == null){
            synchronized (Executor.class){
                if(uniqueInstance == null){
                    uniqueInstance = new Executor();
                }
            }
        }
        return uniqueInstance;
    }

    private Executor(){}*/
    /** End of Region: Singleton **/


    /** Region: property **/
    private String name;

    public void setName(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    Stack<Job> jobHistory = new Stack<Job>();
    public Stack<String> getJobHistory(){
        Stack<String> str = new Stack<>();
        for (Job j: jobHistory){
            str.add(j.getName());
        }
        return str;
    }
    Stack<String> clientHistory = new Stack<>();
    public Stack<String> getClientHistory(){
        return clientHistory;
    }
    Job jobPlaceHolder = new Job("job place holder") {
        @Override
        public Object doIt() {
            return null;
        }
    };
    /** End of Region: property **/


    /** Region: hooks **/
    private void someoneAsksPrompt(String client){
        System.out.println(name + ": " + client + " applies");
    }

    private void someoneAsksQueuePrompt(String client){
        System.out.println(name + ": " + client + "         and queues");
    }

    private void someoneAsksFailedPrompt(String client){
        System.out.println(name + ": " + client + "                     and fails");
    }

    private void someoneAsksSuccessPrompt(String client){
        System.out.println(name + ": " + client + "                    and successes");
    }

    private void beforeExecutePrompt(String job){
        System.out.println(name + ": " + "executing " + job);
    }

    private void afterExecutePrompt(String job){
        System.out.println(name + ": " + job + " done.");
    }
    /** End of Region: hooks **/


    /** Region: States **/
    private Service availableState = new Service() {
        @Override
        public int apply(String s) throws RemoteException {
            someoneAsksPrompt(s);
            if (currentState != availableState) {
                someoneAsksFailedPrompt(s);
                return FAILED;
            }
            someoneAsksQueuePrompt(s);
            synchronized (Executor.this) {
                if (currentState != availableState) {
                    someoneAsksFailedPrompt(s);
                    return FAILED;
                } else {
                    clientHistory.push(s);
                    jobHistory.push(jobPlaceHolder);

                    currentState = readyToExecuteState;
                    Random random = new Random();
                    int key = random.nextInt();
                    readyToExecuteState.beginWaiting(key);

                    someoneAsksSuccessPrompt(s);
                    return key;
                }
            }
        }

        @Override
        public boolean isBusy() throws RemoteException {
            return false;
        }

        @Override
        public <T> T execute(Job<T> job, int key) throws RemoteException {
            throw new IllegalStateException();
        }
    };


    private ReadyToExecuteState readyToExecuteState = new ReadyToExecuteState();

    class ReadyToExecuteState implements Service {
        private Date startTime;
        private int key;
        public void beginWaiting(int k){
            startTime = new Date();
            key = k;
        }

        @Override
        public int apply(String s) throws RemoteException {
            Date now = new Date();
            if(now.getTime()-startTime.getTime() > maxWaitingTime){
                currentState = availableState;
                return this.apply(s);
            }else {
                return FAILED;
            }
        }

        @Override
        public boolean isBusy() throws RemoteException {
            Date now = new Date();
            if(now.getTime()-startTime.getTime() > maxWaitingTime){
                currentState = availableState;
                return false;
            }else {
                return true;
            }
        }

        @Override
        public <T> T execute(Job<T> job, int key) throws RemoteException {
            // need client access checking
            if (key == this.key) {
                currentState = executingState;
                executingState.setKey(key);
                return this.execute(job, key);
            }else {
                throw new IllegalStateException();
            }

        }
    }

    private ExecutingState executingState = new ExecutingState();
    class ExecutingState implements Service {
        int key;
        void setKey(int k){ key = k;}
        @Override
        public int apply(String s) throws RemoteException {
            return FAILED;
        }

        @Override
        public boolean isBusy() throws RemoteException {
            return true;
        }

        @Override
        public <T> T execute(Job<T> job, int k) throws RemoteException {
            if (key != k)
                throw new IllegalStateException();
            jobHistory.pop();
            jobHistory.push(job);
            beforeExecutePrompt(job.getName());
            T res = job.doIt();
            afterExecutePrompt(job.getName());
            currentState = availableState;
            return res;
        }
    };

    private volatile Service currentState = availableState;
    /** End of Region: States **/


    /** Region: apply for resource and execute **/
    @Override
    public int apply(String clientName) throws RemoteException {
        return currentState.apply(clientName);
    }

    @Override
    public boolean isBusy() throws RemoteException {
        return currentState.isBusy();
    }

    @Override
    public <T> T execute(Job<T> job, int key) throws RemoteException {
        return currentState.execute(job,key);
    }
    /** End of Region: apply for resource and execute **/
}
