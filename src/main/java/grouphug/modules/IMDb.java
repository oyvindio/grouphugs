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

public class IMDb implements TriggerListener {

    private static final String TRIGGER = "imdb";
    private static final String TRIGGER_HELP = "imdb";

    public IMDb(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "IMDb: Show IMDb info for a movie\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<movie name>");
        System.out.println("IMDb module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        URL imdbURL;
        try {
            imdbURL = Google.search(message+"+site:www.imdb.com");
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("But I don't want to. (IOException)", false);
            return;
        }
        if(imdbURL == null) {
            Grouphug.getInstance().sendMessage("Sorry, I didn't find "+message+" on IMDb.", false);
            return;
        }

        String title = "";
        double score = 0;
        String votes = "";
        String tagline = "";
        String plot = "";
        String commentTitle = "";

        URLConnection urlConn;
        try {
            urlConn = imdbURL.openConnection();
            urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

            BufferedReader imdb = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            String line;

            String titleString = "<title>";
            String scoreString = "<div class=\"meta\">";
            String votesString = "&nbsp;&nbsp;<a href=\"ratings\" class=\"tn15more\">";
            String taglineString = "<h5>Tagline:</h5>";
            String plotString = "<h5>Plot:</h5>";
            String commentString = "<h5>User Comments:</h5>";

            // A bit of copy-pasta and wtf's in here, enjoy :)
            while((line = imdb.readLine()) != null) {
                if(line.startsWith(titleString)) {
                    title = Web.entitiesToChars(line.substring(line.indexOf(">") + 1, line.substring(1).indexOf("<")+1));
                }
                if(line.trim().equals(scoreString)) {
                    line = imdb.readLine();
                    score = Double.parseDouble(line.substring(line.indexOf("<b>") + 3, line.indexOf("/")));
                }
                if(line.startsWith(votesString)) {
                    votes = line.substring(votesString.length()).substring(0, line.substring(votesString.length()).indexOf(" "));
                }
                if(line.startsWith(taglineString)) {
                    tagline = imdb.readLine().trim();
                    int ind = tagline.indexOf("<");
                    if(ind != -1) {
                        tagline = tagline.substring(0, ind).trim();
                    }
                    tagline = Web.entitiesToChars(" - "+tagline.replace("|", " "));
                }
                if(line.startsWith(plotString)) {
                    plot = imdb.readLine().trim();
                    int ind = plot.indexOf("<");
                    if(ind != -1) {
                        plot = plot.substring(0, ind).trim();
                    }
                    plot = Web.entitiesToChars(plot.replace("|", " "));
                }
                if(line.startsWith(commentString)) {
                    commentTitle = imdb.readLine().trim();
                    int ind = commentTitle.indexOf("<");
                    if(ind != -1) {
                        commentTitle = commentTitle.substring(0, ind).trim();
                    }
                    commentTitle = Web.entitiesToChars(commentTitle.replace("|", " "));
                }
            }

        } catch(StringIndexOutOfBoundsException ex) {
            Grouphug.getInstance().sendMessage("The IMDb site layout may have changed, I was unable to parse it.", false);
            return;
        } catch(NumberFormatException ex) {
            Grouphug.getInstance().sendMessage("The IMDb site layout may have changed, I was unable to parse it.", false);
            return;
        } catch(MalformedURLException ex) {
            Grouphug.getInstance().sendMessage("Wtf just happened? I caught a MalformedURLException.", false);
            return;
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seem to be clogged up.", false);
            return;
        }

        try {
            Grouphug.getInstance().sendMessage(title+tagline+"\n"+plot+"\n"+"Comment: "+commentTitle+"\n"+score+"/10 ("+votes+" votes) - "+imdbURL.toString(), false);
        } catch(NullPointerException ex) {
            Grouphug.getInstance().sendMessage("The IMDb site layout may have changed, I was unable to parse it.", false);
        }
    }
}
