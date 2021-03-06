package sunjc.rmi.client;

import sunjc.rmi.shared.Job;


/**
 * Created by SunJc on Mar/20/16.
 */
public class MrJobLovesSleeping extends Job<Data> {

    private int time;

    public MrJobLovesSleeping(String nameOfMrJob, int sleepTime) {
        super(nameOfMrJob);
        time = sleepTime * 1000;
    }

    @Override
    public Data doIt() {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            System.out.println("Mr.Job has trouble sleeping");
            e.printStackTrace();
        }
        return new Data("INTEGER", time);
    }

}
