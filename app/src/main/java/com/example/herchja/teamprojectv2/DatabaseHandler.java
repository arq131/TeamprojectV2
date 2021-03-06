package com.example.herchja.teamprojectv2;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.SecureRandom;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.io.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/**
 * Created by Danny Nguyen on 4/5/2017.
 */

public class DatabaseHandler {

    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private Connection connection;

    /**
     * Domain name for our project:
     *      jdbc:mysql://" + "se-team4-project.cyl2fljhshrg.us-west-2.rds.amazonaws.com:3306/se4_mydb
     *
     * @param domain        domain name. Hardcoded this into the code.
     * @param username      username. Only private users should know this
     * @param password      password. Only private users should know this.
     */
    private DatabaseHandler(String domain, String username, String password) {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://" + "se-team4-project.cyl2fljhshrg.us-west-2.rds.amazonaws.com:3306/se4_mydb",
                    username, password);
        } catch (Exception e) { System.out.println("Connection Failed!:\n" + e.getMessage()); }
    }


    /**
     * this will close the connection of the database handler.
     */
    public void close() {
        try {
            this.connection.close();
        } catch (Exception e) { System.out.println("Unable to close connection"); }
    }



    /**
     * connectJDBCToAWSec2 will check to see if the connection to the database is working
     * correctly, and print out a statement saying successful connection or error.
     */
    public void connectJDBCToAWSEC2() {

        System.out.println("----MySQL JDBC Connection Testing-------");

        /* Locate class for mysql jdbc driver */
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your MySQL JDBC Driver?");
            e.printStackTrace();
            return;
        }

        System.out.println("MySQL JDBC Driver Registered!");
        /* Print out a message depending if the connection worked. */
        if (this.connection != null) {
            System.out.println("Able to connect to database!");
        } else {
            System.out.println("FAILURE! Failed to make connection!");
        }
    }

    /**
     * This will get all of the usernames within the database and return an arraylist of all of them.
     * @return  Arraylist of usernames in the database.
     */
    public ArrayList<String> getUsers() {
        ArrayList<String> users = new ArrayList<>();
        String query = "Select username from Users"; // select all users from username
        try {
            Statement stmt = this.connection.createStatement(); // connect to database
            stmt.executeQuery(query);                           // execute query
            ResultSet rs = stmt.getResultSet();                 // get results
            while (rs.next()) {  // while there are things to read
                users.add(rs.getString("usernames")); // add to arraylist
            }
            stmt.close();   // close connections
            rs.close();
        } catch (Exception e) {
            System.out.println("Error executing statement!");
        }
        return users;
    }

    /**
     * getNextSalt will create a random salt
     * ****** Danny's Notes *******
     * We need to store this somewhere, and use this same salt for passwords. If we don't, we can't check
     * directly with the database if the password is correct.
     * @return a 16-byte randomly generated salt
     */
    public static byte[] getNextSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Create a hash off of the password and randomly generated salt.
     *
     * @param password  User password input.
     * @param salt      Salt generated from getNextSalt()
     * @return          Hash value of password input and the salt.
     */
    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new Error("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * This will check a password with its expected hash, and return true or false if the hash and the
     * input password matches.
     *
     * @param password          User password that is typed in
     * @param salt              Salt that we have stored somewhere
     * @param expectedHash      Hash that we are expecting, we retrieve this from the database.
     * @return                  True if password hash matches the database one, false otherwise.
     */
    public static boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) {
        byte[] pwdHash = hash(password, salt);
        Arrays.fill(password, Character.MIN_VALUE);
        if (pwdHash.length != expectedHash.length) return false;
        for (int i = 0; i < pwdHash.length; i++) {
            if (pwdHash[i] != expectedHash[i]) return false;
        }
        return true;
    }
}
