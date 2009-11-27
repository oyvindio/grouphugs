package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.BufferedReader;
import java.io.IOException;

public class Define implements TriggerListener {

    private static final String TRIGGER = "define";
    private static final String TRIGGER_HELP = "define";

    public Define(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Define: Use google to give a proper definition of a word.\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<keyword>");
        System.out.println("Define module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String answer;
        try {
            answer = Define.search(message);
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("The intartubes seems to be clogged up (IOException).");
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(answer == null)
            Grouphug.getInstance().sendMessage("No definition found for "+message+".");
        else
            Grouphug.getInstance().sendMessage(Web.entitiesToChars(answer));
    }

    public static String search(String query) throws IOException {
        BufferedReader google = Web.prepareBufferedReader("http://www.google.com/search?q=define:"+query.replace(' ', '+'));

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
