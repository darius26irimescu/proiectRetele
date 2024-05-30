import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 1234);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

            List<String> keysList = (List<String>) input.readObject();
            System.out.println("Conectat la server. Chei primite: " + keysList);

            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            while (running) {
                System.out.println("\nMeniu:");
                System.out.println("1 - Afiseaza cheile obiectelor");
                System.out.println("2 - Cauta dupa cheie");
                System.out.println("3 - Sterge obiect dupa cheie");
                System.out.println("4 - Adauga un nou produs");
                System.out.println("5 - Iesire");
                System.out.print("Alege o optiune: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        System.out.println("Cheile disponibile: " + keysList);
                        break;
                    case 2:
                        handleSearch(scanner, output, input);
                        break;
                    case 3:
                        handleDelete(scanner, output, input);
                        break;
                    case 4:
                        handlePublish(scanner, output, input, keysList);
                        break;
                    case 5:
                        running = false;
                        break;
                    default:
                        System.out.println("Optiune invalida. Te rog sa alegi din nou.");
                        break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Eroare la conectarea la server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleSearch(Scanner scanner, ObjectOutputStream output, ObjectInputStream input) throws IOException, ClassNotFoundException {
        System.out.print("Introdu cheia pentru cautare: ");
        String key = scanner.nextLine();
        output.writeUTF("search");
        output.writeUTF(key);
        output.flush();

        try {
            Object response = input.readObject();
            if (response instanceof Produs) {
                System.out.println("Produs gasit: " + response);
            } else if (response instanceof String) {
                System.out.println("Raspuns de la server: " + response);
            } else {
                System.out.println("Raspuns necunoscut de la server.");
            }
        } catch (OptionalDataException e) {
            System.err.println("Eroare la conectarea la server: Cheia nu exista.");
        } catch (EOFException e) {
            System.err.println("Serverul a inchis conexiunea.");
        }
    }

    private static void handleDelete(Scanner scanner, ObjectOutputStream output, ObjectInputStream input) throws IOException, ClassNotFoundException {
        System.out.print("Introdu cheia pentru stergere: ");
        String key = scanner.nextLine();
        output.writeUTF("delete");
        output.writeUTF(key);
        output.flush();
        System.out.println("Raspuns de la server: " + input.readUTF());
    }

    private static void handlePublish(Scanner scanner, ObjectOutputStream output, ObjectInputStream input, List<String> keysList) throws IOException, ClassNotFoundException {
        System.out.print("Introdu numele produsului: ");
        String name = scanner.nextLine();
        System.out.print("Introdu pretul produsului: ");
        double price = scanner.nextDouble();
        scanner.nextLine();
        System.out.print("Introdu cheia pentru noul produs: ");
        String key = scanner.nextLine();
        output.writeUTF("publish");
        output.writeUTF(key);
        output.writeObject(new Produs(name, price));
        output.flush();

        String response = input.readUTF();
        System.out.println("Raspuns de la server: " + response);
        if (response.startsWith("Produs publicat sub cheia: ")) {
            keysList.add(key);
        }
    }
}
