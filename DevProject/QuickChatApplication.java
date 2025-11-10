
package com.mycompany.devproject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane;

/**
 *
 * @author chway
 */
public class QuickChatApplication {
    private static final List<User> registeredUsers = new ArrayList<>();
    private static final List<Message> sentMessages = new ArrayList<>();
    private static User currentUser = null;
    private static int totalMessagesSent = 0;

    public static void main(String[] args) {
        showWelcomeMessage();
        mainMenu();
    }

    private static void showWelcomeMessage() {
        JOptionPane.showMessageDialog(null, "Welcome to QuickChat");
    }

    private static void mainMenu() {
        while (true) {
            String input = JOptionPane.showInputDialog("""
                                                       Please select an option:
                                                       1) Register
                                                       2) Login
                                                       3) Send Messages
                                                       4) Show recently sent messages
                                                       5) Quit""");

            if (input == null) {
                System.exit(0);
            }

            try {
                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 1:
                        registerUser();
                        break;
                    case 2:
                        loginUser();
                        break;
                    case 3:
                        if (currentUser != null) {
                            sendMessages();
                        } else {
                            JOptionPane.showMessageDialog(null, "Please login first");
                        }
                        break;
                    case 4:
                        JOptionPane.showMessageDialog(null, "Coming Soon");
                        break;
                    case 5:
                        System.exit(0);
                        break;
                    default:
                        JOptionPane.showMessageDialog(null, "Invalid option");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter a valid number");
            }
        }
    }

    // Registration and Login Classes
    static class User {
        private String username;
        private String password;
        private String firstName;
        private String lastName;
        private String cellNumber;

        public User(String username, String password, String firstName, String lastName, String cellNumber) {
            this.username = username;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
            this.cellNumber = cellNumber;
        }

        // Getters
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getCellNumber() { return cellNumber; }
    }

    static class Login {
        public static boolean checkUserName(String username) {
            return username.contains("_") && username.length() <= 5;
        }

        public static boolean checkPasswordComplexity(String password) {
            String regex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
            return password.matches(regex);
        }

        public static boolean checkCellPhoneNumber(String cellNumber) {
            String regex = "^\\+27\\d{9}$";
            return cellNumber.matches(regex);
        }

        public static String registerUser(String username, String password, String firstName, String lastName, String cellNumber) {
            if (!checkUserName(username)) {
                return "Username is not correctly formatted, please ensure that your username contains an underscore and is no more than five characters in length.";
            }
            if (!checkPasswordComplexity(password)) {
                return "Password is not correctly formatted, please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.";
            }
            if (!checkCellPhoneNumber(cellNumber)) {
                return "Cell phone number incorrectly formatted or does not contain international code.";
            }
            
            User newUser = new User(username, password, firstName, lastName, cellNumber);
            registeredUsers.add(newUser);
            return "User registered successfully.";
        }

        public static boolean loginUser(String username, String password) {
            for (User user : registeredUsers) {
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                    currentUser = user;
                    return true;
                }
            }
            return false;
        }

        public static String returnLoginStatus(boolean isLoggedIn, User user) {
            if (isLoggedIn) {
                return "Welcome " + user.getFirstName() + "," + user.getLastName() + " it is great to see you again.";
            } else {
                return "Username or password incorrect, please try again.";
            }
        }
    }

    // Message Class
    static class Message {
        private String messageId;
        private String recipientNumber;
        private String messageContent;
        private String messageHash;
        private boolean sent;

        public Message(String recipientNumber, String messageContent) {
            this.messageId = generateMessageId();
            this.recipientNumber = recipientNumber;
            this.messageContent = messageContent;
            this.messageHash = createMessageHash();
            this.sent = false;
        }

        private String generateMessageId() {
            Random random = new Random();
            return String.format("%010d", random.nextInt(1000000000));
        }

        public boolean checkMessageID() {
            return messageId.length() == 10;
        }

        public boolean checkRecipientCell() {
            return Login.checkCellPhoneNumber(recipientNumber);
        }

        public String createMessageHash() {
            String[] words = messageContent.split(" ");
            String firstWord = words.length > 0 ? words[0] : "";
            String lastWord = words.length > 0 ? words[words.length - 1] : "";
            return (messageId.substring(0, 2) + ":" + 
                   messageId.substring(0, 1) + ":" + 
                   firstWord.toUpperCase() + lastWord.toUpperCase());
        }

        public String sentMessage() {
            String[] options = {"Send Message", "Disregard Message", "Store Message to send later"};
            int choice = JOptionPane.showOptionDialog(null, 
                "Message details:\n" +
                "ID: " + messageId + "\n" +
                "Recipient: " + recipientNumber + "\n" +
                "Message: " + messageContent + "\n" +
                "Hash: " + messageHash,
                "Message Options",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

            switch (choice) {
                case 0: // Send
                    sent = true;
                    totalMessagesSent++;
                    return "Message successfully sent.";
                case 1: // Disregard
                    return "Press 0 to delete message.";
                case 2: // Store
                    storeMessage();
                    return "Message successfully stored.";
                default:
                    return "Message action cancelled.";
            }
        }

        @SuppressWarnings("unchecked")
        private void storeMessage() {
            JSONObject messageJson = new JSONObject();
            messageJson.put("messageId", messageId);
            messageJson.put("recipientNumber", recipientNumber);
            messageJson.put("messageContent", messageContent);
            messageJson.put("messageHash", messageHash);

            JSONArray messagesArray;
            JSONParser parser = new JSONParser();

            try {
                File file = new File("stored_messages.json");
                if (file.exists()) {
                    messagesArray = (JSONArray) parser.parse(new FileReader(file));
                } else {
                    messagesArray = new JSONArray();
                }

                messagesArray.add(messageJson);

                FileWriter writer = new FileWriter(file);
                writer.write(messagesArray.toJSONString());
                writer.flush();
                writer.close();
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        public static String printMessages() {
            StringBuilder sb = new StringBuilder();
            for (Message msg : sentMessages) {
                sb.append("ID: ").append(msg.messageId).append("\n")
                  .append("Recipient: ").append(msg.recipientNumber).append("\n")
                  .append("Message: ").append(msg.messageContent).append("\n")
                  .append("Hash: ").append(msg.messageHash).append("\n\n");
            }
            return sb.toString();
        }

        public static int returnTotalMessages() {
            return totalMessagesSent;
        }
    }

    // Helper Methods
    private static void registerUser() {
        String firstName = JOptionPane.showInputDialog("Enter your first name:");
        String lastName = JOptionPane.showInputDialog("Enter your last name:");
        String username = JOptionPane.showInputDialog("Enter username (must contain _ and be ≤5 chars):");
        String password = JOptionPane.showInputDialog("Enter password (8+ chars, capital, number, special char):");
        String cellNumber = JOptionPane.showInputDialog("Enter SA cell number (e.g., +27831234567):");

        String result = Login.registerUser(username, password, firstName, lastName, cellNumber);
        JOptionPane.showMessageDialog(null, result);
    }

    private static void loginUser() {
        String username = JOptionPane.showInputDialog("Enter username:");
        String password = JOptionPane.showInputDialog("Enter password:");

        boolean isLoggedIn = Login.loginUser(username, password);
        String message = Login.returnLoginStatus(isLoggedIn, currentUser);
        JOptionPane.showMessageDialog(null, message);
    }

    private static void sendMessages() {
        String input = JOptionPane.showInputDialog("How many messages would you like to send?");
        try {
            int numMessages = Integer.parseInt(input);
            for (int i = 0; i < numMessages; i++) {
                String recipient = JOptionPane.showInputDialog("Enter recipient cell number (+27 format):");
                String content = JOptionPane.showInputDialog("Enter message (max 250 chars):");
                
                if (content.length() > 250) {
                    JOptionPane.showMessageDialog(null, 
                        "Message exceeds 250 characters by " + (content.length() - 250) + 
                        ", please reduce size.");
                    i--; // Retry this message
                    continue;
                }

                Message message = new Message(recipient, content);
                if (!message.checkRecipientCell()) {
                    JOptionPane.showMessageDialog(null, 
                        "Cell phone number is incorrectly formatted or does not contain an international code.");
                    i--; // Retry this message
                    continue;
                }

                String result = message.sentMessage();
                JOptionPane.showMessageDialog(null, result);
                
                if (result.equals("Message successfully sent.")) {
                    sentMessages.add(message);
                }
            }
            JOptionPane.showMessageDialog(null, 
                "Total messages sent in this session: " + Message.returnTotalMessages());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Please enter a valid number");
        }
    }

    private static class JSONArray {

        public JSONArray() {
        }
    }

    private static class JSONParser {

        public JSONParser() {
        }
    }

    private static class File {

        public File() {
        }

        private File(String stored_messagesjson) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        private boolean exists() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    private static class FileReader {

        public FileReader(File file) {
        }
    }
}
