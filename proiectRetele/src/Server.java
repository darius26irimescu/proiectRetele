import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final ConcurrentHashMap<String, Produs> objectDictionary = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> keysList = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Server is running...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream())) {

                output.writeObject(keysList);
                output.flush();

                while (!clientSocket.isClosed() && clientSocket.isConnected()) {
                    try {
                        String action = input.readUTF();
                        handleAction(action, input, output);
                    } catch (EOFException e) {
                        System.err.println("Client disconnected: " + e.getMessage());
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error handling client: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void handleAction(String action, ObjectInputStream input, ObjectOutputStream output) throws IOException, ClassNotFoundException {
            String key;
            Produs produs;
            switch (action) {
                case "publish":
                    key = input.readUTF();
                    produs = (Produs) input.readObject();
                    if (!objectDictionary.containsKey(key)) {
                        objectDictionary.put(key, produs);
                        keysList.add(key);
                        output.writeUTF("Produs publicat sub cheia: " + key);
                    } else {
                        output.writeUTF("Cheia exista deja!");
                    }
                    break;
                case "search":
                    key = input.readUTF();
                    produs = objectDictionary.get(key);
                    if (produs != null) {
                        output.writeObject(produs);
                    } else {
                        output.writeUTF("Produsul nu a fost gasit.");
                    }
                    break;
                case "delete":
                    key = input.readUTF();
                    if (objectDictionary.remove(key) != null) {
                        keysList.remove(key);
                        output.writeUTF("Cheia si produsul asociat au fost sterse: " + key);
                    } else {
                        output.writeUTF("Cheia nu exista, deci nu poate fi stearsa.");
                    }
                    break;
                case "showKeys":
                    output.writeObject(keysList);
                    break;
                default:
                    output.writeUTF("Actiune necunoscuta.");
                    break;
            }
            output.flush();
        }
    }
}
