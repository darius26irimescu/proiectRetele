import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientGUI extends JFrame {
    private JTextArea textArea;
    private JTextField keyField, nameField, priceField, searchKeyField, deleteKeyField, nameInputField;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private List<String> keysList;
    private String clientName;

    public ClientGUI() {
        showLoginScreen();
    }

    private void showLoginScreen() {
        JFrame loginFrame = new JFrame("Client Login");
        loginFrame.setSize(300, 150);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel nameLabel = new JLabel("Nume:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginFrame.add(nameLabel, gbc);

        nameInputField = new JTextField(15);
        gbc.gridx = 1;
        gbc.gridy = 0;
        loginFrame.add(nameInputField, gbc);

        JButton loginButton = new JButton("Conecteaza-te");
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginFrame.add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clientName = nameInputField.getText();
                if (!clientName.isEmpty()) {
                    loginFrame.dispose();
                    initializeGUI();
                    connectToServer();
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Numele nu poate fi gol!", "Eroare", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        loginFrame.setVisible(true);
    }

    private void initializeGUI() {
        setTitle("Client GUI");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        // Text area for output
        textArea = new JTextArea(10, 50);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        add(scrollPane, gbc);

        // Labels and text fields
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addLabelAndField("Cheie:", keyField = new JTextField(20), gbc, 1);
        addLabelAndField("Nume:", nameField = new JTextField(20), gbc, 2);
        addLabelAndField("Pret:", priceField = new JTextField(20), gbc, 3);

        // Add button
        JButton addButton = new JButton("Adauga Produs");
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        add(addButton, gbc);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addProduct();
            }
        });

        // Search section
        addLabelAndField("Cheie pentru cautare:", searchKeyField = new JTextField(20), gbc, 5);
        JButton searchButton = new JButton("Cauta Produs");
        gbc.gridx = 1;
        gbc.gridy = 6;
        add(searchButton, gbc);
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchProduct();
            }
        });

        // Delete section
        addLabelAndField("Cheie pentru stergere:", deleteKeyField = new JTextField(20), gbc, 7);
        JButton deleteButton = new JButton("Sterge Produs");
        gbc.gridx = 1;
        gbc.gridy = 8;
        add(deleteButton, gbc);
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteProduct();
            }
        });

        setVisible(true);
    }

    private void addLabelAndField(String labelText, JTextField textField, GridBagConstraints gbc, int yPos) {
        gbc.gridx = 0;
        gbc.gridy = yPos;
        add(new JLabel(labelText), gbc);
        gbc.gridx = 1;
        gbc.gridy = yPos;
        add(textField, gbc);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 1234);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // Send client name to server
            output.writeUTF(clientName);
            output.flush();

            keysList = (List<String>) input.readObject();
            textArea.append("Conectat la server ca: " + clientName + ". Chei primite: " + keysList + "\n");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            textArea.append("Eroare la conectarea la server: " + e.getMessage() + "\n");
        }
    }

    private void addProduct() {
        try {
            String key = keyField.getText();
            String name = nameField.getText();
            double price = Double.parseDouble(priceField.getText());
            output.writeUTF("publish");
            output.writeUTF(key);
            output.writeObject(new Produs(name, price));
            output.flush();
            String response = input.readUTF();
            textArea.append(response + "\n");
            if (response.startsWith("Produs publicat sub cheia: ")) {
                keysList.add(key);
            }
            // Afișează cheile după adăugare
            displayKeys();
        } catch (IOException e) {
            e.printStackTrace();
            textArea.append("Eroare la adaugarea produsului: " + e.getMessage() + "\n");
        }
    }

    private void searchProduct() {
        try {
            String key = searchKeyField.getText();
            output.writeUTF("search");
            output.writeUTF(key);
            output.flush();
            Object response = input.readObject();
            if (response instanceof Produs) {
                textArea.append("Produs gasit: " + response + "\n");
            } else if (response instanceof String) {
                textArea.append("Raspuns de la server: " + response + "\n");
            } else {
                textArea.append("Raspuns necunoscut de la server.\n");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            textArea.append("Eroare la cautarea produsului: " + e.getMessage() + "\n");
        }
    }

    private void deleteProduct() {
        try {
            String key = deleteKeyField.getText();
            output.writeUTF("delete");
            output.writeUTF(key);
            output.flush();
            String response = input.readUTF();
            textArea.append(response + "\n");
            keysList.remove(key);
            // Afișează cheile după ștergere
            displayKeys();
        } catch (IOException e) {
            e.printStackTrace();
            textArea.append("Eroare la stergerea produsului: " + e.getMessage() + "\n");
        }
    }

    private void displayKeys() {
        textArea.append("Cheile disponibile: " + keysList + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ClientGUI().setVisible(true);
            }
        });
    }
}
