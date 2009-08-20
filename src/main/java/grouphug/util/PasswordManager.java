package grouphug.util;

import grouphug.Grouphug;

import java.io.*;

/**
 * PasswordManager handles the MySQL passwords that will be loaded from files so they
 * are not uploaded in cleartext to the publicly accessible svn repository
 */
public class PasswordManager {

    private static final String FILE_HINUX_PW = Grouphug.ROOT_DIR+"pw/hinux";
    private static final String FILE_GRIMSTUX_PW = Grouphug.ROOT_DIR+"pw/grimstux";

    private static String hinuxPass;
    private static String grimstuxPass;

    /**
     * Returns the password for the HiNux MySQL DB account read from file, or null if it wasn't correctly read
     * @return the password for the HiNux MySQL DB account read from file, or null if it wasn't correctly read
     */
    public static String getHinuxPass() {
        return hinuxPass;
    }

    /**
     * Returns the password for the Grimstux MySQL DB account read from file, or null if it wasn't correctly read
     * @return the password for the Grimstux MySQL DB account read from file, or null if it wasn't correctly read
     */
    public static String getGrimstuxPass() {
        return grimstuxPass;
    }

    /**
     * Tries to load all specified passwords from file into memory
     *
     * @return true if all passwords were loaded successfully, false if one or more passwords failed to load
     */
    public static boolean loadPasswords() {
        boolean readOK = true;

        if(Debugger.DEBUG) {
            if(Debugger.HINUX_PASSWORD != null) {
                hinuxPass = Debugger.HINUX_PASSWORD;
            } else {
                System.err.println("Debug-mode: MySQL password for HiNux not set.");
                readOK = false;
            }
            if(Debugger.GRIMSTUX_PASSWORD != null) {
                grimstuxPass = Debugger.GRIMSTUX_PASSWORD;
            } else {
                System.err.println("Debug-mode: MySQL password for Grimstux not set.");
                readOK = false;
            }
        } else {
            // The read attempts are in separate try/catch blocks, so that if the first fails, it still tries the others
            try {
                hinuxPass = loadPassword(FILE_HINUX_PW);
            } catch(IOException ex) {
                System.err.println("Failed to load hinux password: "+ex.getMessage());
                readOK = false;
            }
            try {
                grimstuxPass = loadPassword(FILE_GRIMSTUX_PW);
            } catch(IOException ex) {
                System.err.println("Failed to load grimstux password: "+ex.getMessage());
                readOK = false;
            }
        }
        return readOK;
    }

    /**
     * Load up the SQL password from the first line of the specified textfile
     *
     * @param file the filename to retrieve
     * @return true if the password was successfully fetched and saved, false otherwise
     * @throws java.io.IOException if an IOException occurs :)
     */
    private static String loadPassword(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
        String pass = reader.readLine();
        reader.close();
        if(pass == null || pass.equals(""))
            throw new FileNotFoundException("No data extracted from MySQL password file!");
        return pass;
    }
}
