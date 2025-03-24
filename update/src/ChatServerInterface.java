import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// Interface pour le serveur
public interface ChatServerInterface extends Remote {
    void registerClient(ChatClientInterface client) throws RemoteException;
    void broadcastMessage(String message, ChatClientInterface sender) throws RemoteException;
    void sendPrivateMessage(String message, ChatClientInterface sender, String recipient) throws RemoteException;
    void sendFile(byte[] fileData, String fileName, ChatClientInterface sender, String recipient) throws RemoteException;
    List<String> getConnectedUsers() throws RemoteException;
}