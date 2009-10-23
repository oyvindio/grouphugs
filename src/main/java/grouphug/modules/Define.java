package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Define implements TriggerListener {

    private static final String TRIGGER = "define";
    private static final String TRIGGER_HELP = "define";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Define(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Define: Use google to give a proper definition of a word.\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<keyword>");
        System.out.println("Define module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        String answer;
        try {
            answer = Define.search(message);
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("The intartubes seems to be clogged up (IOException).", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(answer == null)
            Grouphug.getInstance().sendMessage("No definition found for "+message.substring(TRIGGER.length())+".", false);
        else
            Grouphug.getInstance().sendMessage(Web.entitiesToChars(answer), false);
    }

    public static String search(String query) throws IOException {

        query = query.replace(' ', '+');
        System.out.print("Opening google connection... ");

        URLConnection urlConn;
        try {
            urlConn = new URL("http", "www.google.com", "/search?q=define:"+query+"").openConnection();
        } catch(MalformedURLException ex) {
            System.err.println("Define search error: MalformedURLException in partially dynamic URL in search()!");
            return null;
        }

        urlConn.setConnectTimeout(CONN_TIMEOUT);
        urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

        BufferedReader google = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        System.out.println("OK");

        // Search for some hardcoded stuff to try to parse a definition. this is fugly
        String line;
        int startIndex;
        String parseSearch = "<ul type=\"disc\" class=std><li>";
        while((line = google.readLine()) != null) {
            startIndex = line.indexOf(parseSearch);

            // if -1, then the phrase wasn't found
            if(startIndex == -1)
                continue;

            // If we reach this point, we found the wanted string, so find the end of the definition
            startIndex += parseSearch.length();
            int i = startIndex;
            for(; line.charAt(i) != '<'; i++) {
                if(i == line.length()) {
                    throw new IOException("Define search error: Couldn't find ending < in definition");
                }
            }
            return line.substring(startIndex, i);
        }

        // If we get here, we couldn't find the definition, or parsing went wrong - but we can't know which, for sure
        return null;
    }
}
