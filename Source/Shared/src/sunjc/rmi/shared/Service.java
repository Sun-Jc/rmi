package sunjc.rmi.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Service extends Remote{
    int apply(String clientName) throws RemoteException;
    boolean isBusy() throws RemoteException;
    <T> T execute(Job<T> j, int key) throws RemoteException;
}
