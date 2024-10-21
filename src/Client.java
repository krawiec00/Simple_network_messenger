import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 4321;
    private static final int PORT_RANGE_START = 40000;
    private static final int PORT_RANGE_END = 50000;

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    public void start() {
        int clientPort = findAvailablePort(); // Szukamy wolnego portu

        if (clientPort == -1) {
            System.out.println("No available port found in the range.");
            return; // Jeśli nie ma wolnych portów, kończymy program
        }

        try {
            // Tworzenie gniazda klienta na wybranym porcie
            Socket socket = new Socket();
            socket.setSoLinger(true, 0); // Ustawienie SO_LINGER na natychmiastowe zamknięcie
            socket.bind(new InetSocketAddress(SERVER_ADDRESS, clientPort));
            socket.connect(new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT));

            System.out.println("Connected to the server on port " + SERVER_PORT + " from client port " + clientPort);

            // Strumienie wejściowe i wyjściowe
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            // Odczytanie komendy WHO od serwera
            String serverMessage = input.readLine();
            if ("WHO".equals(serverMessage)) {
                System.out.println("Server: " + serverMessage);

                // Prośba o ID od użytkownika
                String clientID = getUserID();

                // Wysłanie odpowiedzi do serwera
                output.println("NAME " + clientID);

                // Odbieranie odpowiedzi serwera
                String response = input.readLine();
                System.out.println("Server response: " + response);
                if (response.startsWith("ERROR")) {
                    socket.close();
                    return;
                }
            }

            // Uruchamiamy osobny wątek do nasłuchiwania odpowiedzi serwera
            new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = input.readLine()) != null) {
                        if(serverResponse.startsWith("Message from")){
                            System.out.println("\nServer: " + serverResponse);
                        }
                        else {
                        System.out.println("Server: " + serverResponse);}
                        // Po otrzymaniu wiadomości wyświetlamy ponownie zachętę do wpisania komendy
                        System.out.print("Enter command (LIST, MESG <ID> <MESSAGE>, QUIT): ");
                    }
                } catch (IOException e) {
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            }).start();

            // Pętla komunikacji z serwerem
            Scanner scanner = new Scanner(System.in);
            String userMessage;

            // Pierwsze wyświetlenie zachęty do wpisania komendy
            System.out.print("Enter command (LIST, MESG <ID> <MESSAGE>, QUIT): ");
            while (true) {
                userMessage = scanner.nextLine();

                if (userMessage.equalsIgnoreCase("QUIT")) {
                    output.println("QUIT");
                    System.out.println("Disconnected");
                    break;
                }

                if (userMessage.equalsIgnoreCase("LIST")) {
                    output.println("LIST");
                } else if (userMessage.startsWith("MESG ")) {
                    output.println(userMessage);
                } else {
                    System.out.println("Unknown command, enter a correct one");
                    System.out.print("Enter command (LIST, MESG <ID> <MESSAGE>, QUIT): ");
                }
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    // Funkcja do sekwencyjnego szukania wolnego portu
    private static int findAvailablePort() {
        for (int port = PORT_RANGE_START; port <= PORT_RANGE_END; port++) {
            try {
                ServerSocket testSocket = new ServerSocket(port);
                testSocket.close(); // Zamykamy, bo jest wolny
                return port; // Zwracamy wolny port
            } catch (IOException e) {
                // Port jest zajęty, próbujemy kolejny
            }
        }
        return -1; // Brak wolnych portów
    }

    // Funkcja do pobrania ID od użytkownika
    private static String getUserID() {
        Scanner scanner = new Scanner(System.in);
        String clientID = "";
        while (clientID.length() != 8) {
            System.out.print("Enter your 8-character ASCII ID: ");
            clientID = scanner.nextLine();
            if (clientID.length() != 8) {
                System.out.println("ID must be exactly 8 characters long. Please try again.");
            }
        }
        return clientID;
    }
}
