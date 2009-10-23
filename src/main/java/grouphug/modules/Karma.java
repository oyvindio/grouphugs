package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.MessageListener;
import grouphug.listeners.TriggerListener;
import grouphug.util.SQLHandler;

import java.sql.SQLException;
import java.util.ArrayList;

public class Karma implements TriggerListener, MessageListener {

    private static final String TRIGGER_HELP = "karma";
    private static final String TRIGGER = "karma";
    private static final String TRIGGER_TOP = "karmatop";
    private static final String TRIGGER_BOTTOM = "karmabottom";
    private static final String TRIGGER_RESET = "karmareset";
    //private static final boolean CAN_RESET = false;

    private static final int LIMIT = 5; // how many items to show in karmatop/karmabottom

    private static final String KARMA_DB = "karma";

    private SQLHandler sqlHandler;

    public Karma(ModuleHandler moduleHandler) {
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Karma: Increase, decrease, or show an objects karma.\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<object>\n" +
                    "  <object>++\n" +
                    "  <object>--\n" +
                    "  " + Grouphug.MAIN_TRIGGER + " " + TRIGGER_TOP+"\n" +
                    "  " + Grouphug.MAIN_TRIGGER + " " + TRIGGER_BOTTOM+"\n" +
                    "  " + Grouphug.MAIN_TRIGGER + " " + TRIGGER_RESET + " <object>" + " if resetting is enabled");
        } catch(ClassNotFoundException ex) {
            System.err.println("Karma startup error: SQL unavailable!");
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if(message.endsWith("++") || message.endsWith("++;")) {
            add(sender, message.substring(0, message.length()-2), 1);
        } else if(message.endsWith("--") || message.endsWith("--;")) {
            add(sender, message.substring(0, message.length()-2), -1);
        } else if(message.equals(Grouphug.MAIN_TRIGGER + TRIGGER_TOP)) {
            showScore(true);
        } else if(message.equals(Grouphug.MAIN_TRIGGER + TRIGGER_BOTTOM)) {
            showScore(false);
//        } else if(message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_RESET) && CAN_RESET) {
//            add(sender, message.substring(11, message.length()), 0);
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        print(message);
    }

    private String htmlEntitiesToNorwegianChars(String str) {
        str = str.replace("&aelig;", "æ");
        str = str.replace("&oslash;", "ø");
        str = str.replace("&aring;", "å");
        str = str.replace("&AElig;", "Æ");
        str = str.replace("&Oslash;", "Ø");
        str = str.replace("&Aring;", "Å");
        return str;
    }

    private String norwegianCharsToHtmlEntities(String str) {
        str = str.replace("æ", "&aelig;");
        str = str.replace("ø", "&oslash;");
        str = str.replace("å", "&aring;");
        str = str.replace("Æ", "&AElig;");
        str = str.replace("Ø", "&Oslash;");
        str = str.replace("Å", "&Aring;");
        return str;
    }

    private void print(String name) {
        String sqlName = norwegianCharsToHtmlEntities(name);
        KarmaItem ki;
        try {
            ki = find(sqlName);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage(name+" has probably bad karma, because an SQL error occured.", false);
            return;
        }
        if(ki == null)
            Grouphug.getInstance().sendMessage(name+" has neutral karma.", false);
        else
            Grouphug.getInstance().sendMessage(ki.getName()+" has "+ki.getKarma()+" karma.", false);
    }

    private void add(String sender, String name, int karma) {
        if(name.equals(sender)) {
            Grouphug.getInstance().sendMessage(sender+", self karma is bad karma.", false);
            return;
        }

        String sqlName = norwegianCharsToHtmlEntities(name);

        try {
            // we sleep for a little while, in case the user is very fast - to avoid duplicate inserts
            try {
                Thread.sleep(600);
            } catch(InterruptedException e) {
                // interrupted, ok, just continue
            }
            KarmaItem ki = find(sqlName);
            if(ki == null) {
                sqlHandler.insert("INSERT INTO "+KARMA_DB+" (name, value) VALUES ('"+sqlName+"', '"+karma+"');");
            } else if(karma == 0) {
                sqlHandler.update("UPDATE "+KARMA_DB+" SET value='"+karma+"' WHERE id='"+ki.getID()+"';");
            } else {
                sqlHandler.update("UPDATE "+KARMA_DB+" SET value='"+(ki.getKarma() + karma)+"' WHERE id='"+ki.getID()+"';");
            }
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, unable to change karma value; an SQL error occurred.", false);
        }
    }

    /**
     * Finds a karma-item in the DB based on its name. Returns null if no item is found.
     * @param karma The karma string to search for in the DB
     * @return a KarmaItem-object of the item found in the DB, or null if no item was found
     * @throws SQLException - if an SQL error occured
     */
    private KarmaItem find(String karma) throws SQLException {
        Object[] row = sqlHandler.selectSingle("SELECT id, name, value FROM "+KARMA_DB+" WHERE name='"+karma+"';");
        if(row == null) {
            return null;
        }
        return new KarmaItem((Integer)row[0], htmlEntitiesToNorwegianChars((String)row[1]), (Integer)row[2]);
    }

    private void showScore(boolean top) {
        String reply;
        if(top) {
            reply = "Top five karma winners:\n";
        } else {
            reply = "Bottom five karma losers:\n";
        }
        try {
            String query = "SELECT name, value FROM "+KARMA_DB+" ORDER BY value ";
            if(top) {
                query += "DESC ";
            }
            query += "LIMIT "+LIMIT+";";
            ArrayList<Object[]> rows = sqlHandler.select(query);
            int place = 1;
            for(Object[] row : rows) {
                reply += (place++)+". "+htmlEntitiesToNorwegianChars((String)row[0])+" ("+row[1]+")\n";
            }
            if(top) {
                reply += "May their lives be filled with sunlight and pink stuff.";
            } else {
                reply += "May they burn forever in the pits of "+ Grouphug.CHANNEL+".";
            }
            Grouphug.getInstance().sendMessage(reply, false);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, unable to gather karma records; an SQL error occured.", false);
        }
    }

    private static class KarmaItem {

        private int ID;
        private String name;
        private int karma;

        public int getID() {
            return ID;
        }

        public String getName() {
            return name;
        }

        public int getKarma() {
            return karma;
        }

        public KarmaItem(int ID, String name, int karma) {
            this.ID = ID;
            this.name = name;
            this.karma = karma;
        }

        public String toString() {
            if(karma == 0)
                return "neutral";
            else
                return ""+karma;
        }
    }

}