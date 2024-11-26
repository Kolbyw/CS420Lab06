package Lab06;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class Server {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            TokenManager tokenManager = new TokenManager();
            Naming.rebind("TokenManager", tokenManager);
            System.out.println("TokenManager is ready.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

interface ProcessInterface extends Remote {
    public void requestCriticalSection() throws RemoteException;

    public void releaseCriticalSection() throws RemoteException;

    public int getSequenceNumber() throws RemoteException;

    public void grantAccess() throws RemoteException;
}

class Process extends UnicastRemoteObject implements ProcessInterface {
    public TokenManagerInterface tokenManager;
    private int processId;
    private int sequenceNumber;
    private boolean criticalSectionStatus;

    public Process(TokenManagerInterface manager, int processId, int sequenceNumber) throws RemoteException {
        super();
        this.tokenManager = manager;
        this.processId = processId;
        this.sequenceNumber = sequenceNumber;
        this.criticalSectionStatus = false;
    }

    @Override
    public void requestCriticalSection() throws RemoteException {
        System.out.println("Process " + processId + " has requested entry");
        this.sequenceNumber++;
        tokenManager.requestEntry(processId, sequenceNumber);
    }

    @Override
    public void releaseCriticalSection() throws RemoteException {
        System.out.println("Process " + this.processId + " releasing critical section.");
        criticalSectionStatus = false;
        tokenManager.releaseToken(processId, sequenceNumber);
    }

    @Override
    public int getSequenceNumber() throws RemoteException {
        return this.sequenceNumber;
    }

    public void grantAccess() throws RemoteException {
        System.out.println("Process " + this.processId + " has entered critical section.");
        criticalSectionStatus = true;
    }
}

interface TokenManagerInterface extends Remote {
    public void requestEntry(int processId, int sequenceNumber) throws RemoteException;

    public void releaseToken(int processId, int sequenceNumber) throws RemoteException;
}

class TokenManager extends UnicastRemoteObject implements TokenManagerInterface {
    private int tokenHolderId;
    private PriorityQueue<Request> requestQueue;

    public TokenManager() throws RemoteException {
        super();
        this.tokenHolderId = -1;
        this.requestQueue = new PriorityQueue<>(
                Comparator.comparingInt((Request r) -> r.sequenceNumber).thenComparingInt(r -> r.processId));
    }

    @Override
    public void requestEntry(int processId, int sequenceNumber) throws RemoteException {
        requestQueue.offer(new Request(processId, sequenceNumber));
        checkQueue();
    }

    @Override
    public void releaseToken(int processId, int sequenceNumber) throws RemoteException {
        tokenHolderId = -1;
        requestQueue.removeIf(request -> request.processId == processId && request.sequenceNumber == sequenceNumber);
        checkQueue();
    }

    public void checkQueue() throws RemoteException {
        if (!requestQueue.isEmpty() && tokenHolderId == -1) {
            Request next = requestQueue.poll();
            tokenHolderId = next.processId;
            // Notify the process that it can enter the critical section
            ProcessInterface process;
            try {
                process = (ProcessInterface) java.rmi.Naming
                        .lookup("rmi://localhost/Process" + tokenHolderId);
                process.grantAccess();
            } catch (MalformedURLException | RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }
}

class Request {
    int processId;
    int sequenceNumber;

    Request(int processId, int sequenceNumber) {
        this.processId = processId;
        this.sequenceNumber = sequenceNumber;
    }
}