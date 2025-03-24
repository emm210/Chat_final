// ChatServerInterface.java
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatServerInterface extends Remote {
    void registerClient(ChatClientInterface client, String username) throws RemoteException;
    void broadcastMessage(String message, String sender) throws RemoteException;
    void sendPrivateMessage(String message, String sender, String recipient) throws RemoteException;
    void sendFile(byte[] fileData, String fileName, String sender, String recipient) throws RemoteException;
    void disconnectClient(String username) throws RemoteException;
    String[] getConnectedUsers() throws RemoteException;
}
