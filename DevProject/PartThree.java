package com.mycompany.devproject;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.*;

public class PartThree {
    private static List<User> registeredUsers = new ArrayList<>();
    private static List<Message> sentMessages = new ArrayList<>();
    private static List<Message> disregardedMessages = new ArrayList<>();
    private static List<Message> storedMessages = new ArrayList<>();
    private static List<String> messageHashes = new ArrayList<>();
    private static List<String> messageIds = new ArrayList<>();
    private static User currentUser = null;
    private static int totalMessagesSent = 0;

    public static void main(String[] args) {
        loadStoredMessages(); // Load stored messages from JSON at startup
        showWelcomeMessage();
        mainMenu();
    }

    private static void loadStoredMessages() {
        JSONParser parser = new JSONParser();
        try {
            File file = new File("stored_messages.json");
            if (file.exists()) {
                JSONArray messagesArray = (JSONArray) parser.parse(new FileReader(file));
                for (Object obj : messagesArray) {
                    JSONObject messageJson = (JSONObject) obj;
                    Message message = new Message(
                        (String) messageJson.get("recipientNumber"),
                        (String) messageJson.get("messageContent")
                    );
                    message.messageId = (String) messageJson.get("messageId");
                    message.messageHash = (String) messageJson.get("messageHash");
                    storedMessages.add(message);
                    messageHashes.add(message.messageHash);
                    messageIds.add(message.messageId);
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    // ... [Previous methods remain the same until sendMessages()]

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
                
                // Update arrays based on message status
                if (result.equals("Message successfully sent.")) {
                    sentMessages.add(message);
                    messageHashes.add(message.messageHash);
                    messageIds.add(message.messageId);
                } else if (result.equals("Press 0 to delete message.")) {
                    disregardedMessages.add(message);
                }
            }
            JOptionPane.showMessageDialog(null, 
                "Total messages sent in this session: " + Message.returnTotalMessages());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Please enter a valid number");
        }
    }

    // New methods for Part 3 functionality
    private static void displayMessageArrays() {
        while (true) {
            String input = JOptionPane.showInputDialog(
                "Message Array Operations:\n" +
                "1) Display sender and recipients of all sent messages\n" +
                "2) Display longest sent message\n" +
                "3) Search for message by ID\n" +
                "4) Search messages by recipient\n" +
                "5) Delete message by hash\n" +
                "6) Display full report\n" +
                "7) Back to main menu"
            );

            if (input == null) {
                return;
            }

            try {
                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 1:
                        displaySendersAndRecipients();
                        break;
                    case 2:
                        displayLongestMessage();
                        break;
                    case 3:
                        searchMessageById();
                        break;
                    case 4:
                        searchMessagesByRecipient();
                        break;
                    case 5:
                        deleteMessageByHash();
                        break;
                    case 6:
                        displayFullReport();
                        break;
                    case 7:
                        return;
                    default:
                        JOptionPane.showMessageDialog(null, "Invalid option");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter a valid number");
            }
        }
    }

    private static void displaySendersAndRecipients() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sender and Recipients of Sent Messages:\n");
        for (Message msg : sentMessages) {
            sb.append("From: ").append(currentUser.getFirstName()).append(" ")
              .append(currentUser.getLastName()).append("\n")
              .append("To: ").append(msg.recipientNumber).append("\n")
              .append("-----------------\n");
        }
        JOptionPane.showMessageDialog(null, sb.toString());
    }

    private static void displayLongestMessage() {
        if (sentMessages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No sent messages available");
            return;
        }

        Message longest = sentMessages.get(0);
        for (Message msg : sentMessages) {
            if (msg.messageContent.length() > longest.messageContent.length()) {
                longest = msg;
            }
        }
        JOptionPane.showMessageDialog(null, 
            "Longest Message:\n" + longest.messageContent);
    }

    private static void searchMessageById() {
        String id = JOptionPane.showInputDialog("Enter message ID to search:");
        for (Message msg : sentMessages) {
            if (msg.messageId.equals(id)) {
                JOptionPane.showMessageDialog(null, 
                    "Message Found:\n" +
                    "Recipient: " + msg.recipientNumber + "\n" +
                    "Message: " + msg.messageContent);
                return;
            }
        }
        JOptionPane.showMessageDialog(null, "Message ID not found");
    }

    private static void searchMessagesByRecipient() {
        String recipient = JOptionPane.showInputDialog("Enter recipient number to search:");
        StringBuilder sb = new StringBuilder();
        sb.append("Messages for ").append(recipient).append(":\n");
        
        boolean found = false;
        for (Message msg : sentMessages) {
            if (msg.recipientNumber.equals(recipient)) {
                sb.append(msg.messageContent).append("\n-----------------\n");
                found = true;
            }
        }
        
        if (!found) {
            sb.append("No messages found for this recipient");
        }
        JOptionPane.showMessageDialog(null, sb.toString());
    }

    private static void deleteMessageByHash() {
        String hash = JOptionPane.showInputDialog("Enter message hash to delete:");
        for (int i = 0; i < sentMessages.size(); i++) {
            if (sentMessages.get(i).messageHash.equals(hash)) {
                String content = sentMessages.get(i).messageContent;
                sentMessages.remove(i);
                messageHashes.remove(i);
                messageIds.remove(i);
                JOptionPane.showMessageDialog(null, 
                    "Message \"" + content + "\" successfully deleted.");
                return;
            }
        }
        JOptionPane.showMessageDialog(null, "Message hash not found");
    }

    private static void displayFullReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("FULL MESSAGE REPORT\n");
        sb.append("===================\n");
        
        for (Message msg : sentMessages) {
            sb.append("Message ID: ").append(msg.messageId).append("\n")
              .append("Message Hash: ").append(msg.messageHash).append("\n")
              .append("Recipient: ").append(msg.recipientNumber).append("\n")
              .append("Message: ").append(msg.messageContent).append("\n")
              .append("-----------------\n");
        }
        
        sb.append("\nTOTAL MESSAGES SENT: ").append(sentMessages.size());
        JOptionPane.showMessageDialog(null, sb.toString());
    }

    // Update main menu to include the new functionality
    private static void mainMenu() {
        while (true) {
            String input = JOptionPane.showInputDialog(
                "Please select an option:\n" +
                "1) Register\n" +
                "2) Login\n" +
                "3) Send Messages\n" +
                "4) Show recently sent messages\n" +
                "5) Message Array Operations\n" +
                "6) Quit"
            );

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
                        if (currentUser != null) {
                            displayMessageArrays();
                        } else {
                            JOptionPane.showMessageDialog(null, "Please login first");
                        }
                        break;
                    case 6:
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

    // ... [Rest of the previous classes (User, Login, Message) remain the same]

    private static class User {

        public User() {
        }
    }

    private static class Message {

        public Message() {
        }
    }
}
