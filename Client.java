package Lab06;

import java.rmi.Naming;

public class Client {
    public static void main(String[] args) {
        try {
            TokenManagerInterface tokenManager = (TokenManagerInterface) Naming.lookup("rmi://localhost/TokenManager");
            Process process1 = new Process(tokenManager, 1, 0);
            Process process2 = new Process(tokenManager, 2, 0);
            Process process3 = new Process(tokenManager, 3, 0);

            Naming.rebind("Process" + 1, process1);
            Naming.rebind("Process" + 2, process2);
            Naming.rebind("Process" + 3, process3);

            process1.requestCriticalSection();
            Thread.sleep(1000);
            process2.requestCriticalSection();
            Thread.sleep(1000);
            process3.requestCriticalSection();
            Thread.sleep(1000);

            process1.releaseCriticalSection();
            process2.releaseCriticalSection();
            process3.releaseCriticalSection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}