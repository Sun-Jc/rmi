package sunjc.rmi.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by SunJc on Mar/21/16.
 */
public interface NotifyReceiver extends Remote {
    void onRemoteEvent(String msg) throws RemoteException;
}
