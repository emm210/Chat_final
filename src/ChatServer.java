import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

public class ChatServer extends UnicastRemoteObject implements ChatServerInterface {
    private final ConcurrentHashMap<String, ChatClientInterface> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userStatuses = new ConcurrentHashMap<>();

    public ChatServer() throws RemoteException {
        super();
    }

    public synchronized void registerClient(ChatClientInterface client, String username) throws RemoteException {
        clients.put(username, client);
        userStatuses.put(username, "online");
        broadcastMessage(username + " has joined the chat!", "Server");
        updateAllUserLists();
    }

    public void broadcastMessage(String message, String sender) throws RemoteException {
        String formatted = formatMessage(sender, message);
        for (ChatClientInterface client : clients.values()) {
            client.receiveMessage(formatted);
        }
    }

    public void sendPrivateMessage(String message, String sender, String recipient) throws RemoteException {
        ChatClientInterface client = clients.get(recipient);
        if (client != null) {
            String formatted = formatMessage(sender, "(Private) " + message);
            client.receiveMessage(formatted);
        }
    }

    public void sendFile(byte[] fileData, String fileName, String sender, String recipient) throws RemoteException {
        ChatClientInterface client = clients.get(recipient);
        if (client != null) {
            client.receiveFile(fileData, fileName);
        }
    }


    public void disconnectClient(String username) throws RemoteException {
        ChatClientInterface client = clients.remove(username);
        userStatuses.remove(username);
        broadcastMessage(username + " has left the chat!", "Server");
        updateAllUserLists();

        if (client != null) {
            try {
                UnicastRemoteObject.unexportObject(client, true);
            } catch (NoSuchObjectException e) {
                System.err.println("Error unexporting client: " + e.getMessage());
            }
        }
    }

    public String[] getConnectedUsers() throws RemoteException {
        return clients.keySet().toArray(new String[0]);
    }

    private String formatMessage(String sender, String message) {
        return String.format("[%s] [%s]: %s",
                new SimpleDateFormat("HH:mm:ss").format(new Date()),
                sender,
                message
        );
    }

    private void updateAllUserLists() throws RemoteException {
        String[] users = getConnectedUsers();
        for (ChatClientInterface client : clients.values()) {
            client.updateUserList(users);
        }
    }


    public static void main(String[] args) {
        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
            Naming.rebind("ChatServer", new ChatServer());
            System.out.println("Server ready...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}