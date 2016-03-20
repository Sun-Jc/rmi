package sunjc.rmi.client;


import java.io.Serializable;

/**
 * Created by SunJc on Mar/20/16.
 */
public class Data implements Serializable {
    public String dataType = "default";
    public Object value = null;
    public Data(String s, Object v){
        dataType = s;
        value = v;
    }
}
