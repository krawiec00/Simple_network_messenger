import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 4321;
    private static Map<String, ClientHandler> activeClients = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);  // Ustawienie REUSEADDR na serwerze
            serverSocket.bind(new InetSocketAddress(PORT));

            System.out.println("Server is listening on port " + PORT);

            while (true) {
                // Serwer akceptuje połączenie od nowego klienta
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client attempt connection");

                // Każdy klient jest obsługiwany w nowym wątku
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
        }
    }

    public static synchronized boolean addClient(String clientID, ClientHandler handler) {
        if (!activeClients.containsKey(clientID)) {
            activeClients.put(clientID, handler);
            return true;
        }
        return false;
    }

    public static synchronized void removeClient(String clientID) {
        activeClients.remove(clientID);
    }

    public static synchronized String getClientList() {
        return String.join(", ", activeClients.keySet());
    }

    public static synchronized boolean sendMessageToClient(String targetID, String message, String fromID) {
        ClientHandler targetClient = activeClients.get(targetID);
        if (targetClient != null) {
            targetClient.sendMessage("Message from " + fromID + ": " + message);
            return true;
        }
        return false;
    }

}

class ClientHandler extends Thread {
    private Socket clientSocket;
    private String clientID;
    private PrintWriter output;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Strumienie wejściowe i wyjściowe
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);

            // Wysłanie komendy WHO do klienta
            output.println("WHO");

            // Odczytanie odpowiedzi od klienta
            String clientResponse = input.readLine();
            if (clientResponse != null && clientResponse.startsWith("NAME ")) {
                clientID = clientResponse.substring(5); // Pobieranie ID
                if (Server.addClient(clientID, this)) {
                    System.out.println("Client connected with ID: " + clientID);
                    output.println("ID accepted " + clientID);
                } else {
                    System.out.println("Client didn't connect");
                    output.println("ERROR: ID already in use");
                    clientSocket.close();
                    Thread.currentThread().join();
                    return;
                }
            } else {
                System.out.println("Incorrect response from client");
            }

            // Pętla komunikacji - serwer może wysyłać i odbierać wiadomości
            String messageFromClient;
            while ((messageFromClient = input.readLine()) != null) {
                String[] parts = messageFromClient.split(" ", 3);
                String command = parts[0].toUpperCase();  // Komenda w postaci wielkich liter

                switch (command) {
                    case "LIST":
                        output.println("Active clients: " + Server.getClientList());
                        break;

                    case "MESG":
                        if (parts.length < 3) {
                            output.println("ERROR: Incorrect message format. Use MESG <ID> <MESSAGE>");
                        } else {
                            String target = parts[1];
                            String message = parts[2];
                            if (!Server.sendMessageToClient(target, message, clientID)) {
                                output.println("ERROR: Client not found");
                            } else {
                                output.println("Message sent");
                            }
                        }
                        break;

                    case "QUIT":
                        System.out.println("Client [" + clientID + "] disconnected");
                        Server.removeClient(clientID);
                        return;  // Kończymy wątek po rozłączeniu klienta

                    default:
                        output.println("ERROR: Unknown command");
                        break;
                }
            }
        } catch (IOException | InterruptedException e) {
            if ("Connection reset".equals(e.getMessage())) {
                System.out.println("Client [" + clientID + "] disconnected unexpectedly.");
            } else {
                System.out.println("Error handling client: " + e.getMessage());
            }
        }  finally {
            Server.removeClient(clientID);
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        output.println(message);
    }
}


