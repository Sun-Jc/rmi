package sunjc.rmi.shared;

import java.io.Serializable;

/**
 * Created by SunJc on Mar/20/16.
 */
public abstract class Job<T> implements Serializable {
    String name = "default job name";

    public String getName(){
        return name;
    }

    public Job(String name){
        this.name = name;
    }


    @Override
    public String toString() {
        return name;
    }

    public abstract T doIt();
}
