package no.kvikshaug.gh;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Grouphug
 *
 * A java-based IRC-bot created purely for entertainment purposes and as a personal excercise in design.
 * This bot extends the functionality of the well-designed PircBot, see http://www.jibble.org/
 *
 * The bot acts as a framework for modules that each serve their own purpose. With the help of SQL,
 * external web sites and clever imagination, the modules are used to entertain, insult, and/or inform the
 * users in its IRC channel.
 *
 * It is (supposed to be) easy to add a new module to the mix, and several utilities exist to make
 * writing a module as easy as possible.
 *
 * Some important concepts for the bot:
 * - It should never bother anyone unless it is clear that they want a response from it.
 * - It should never be unclear what a command or module does or intends to do. From a single !help trigger,
 *   a user should be able to dig down in detail and find out every interaction he/she is able to make to
 *   the bot, and what to be expected in return.
 *
 * The bot is currently maintained by most of the people hanging in #grouphugs @ efnet.
 * For more information, please join our channel or visit the web site: http://gh.kvikshaug.no/
 */

public class Grouphug extends PircBot {

    // The trigger characters (as Strings since startsWith takes String)
    public static final String MAIN_TRIGGER = "!";
    public static final String SPAM_TRIGGER = "@";
    public static final String HELP_TRIGGER = "help";

    // The number of characters upon which lines are splitted
    // Note that the 512 max limit includes the channel name, \r\n, and probably some other stuff.
    // maxing out on 440 seems to be a reasonable amount, both ways.
    private static final int MAX_LINE_CHARS = 440;

    // How many lines we can send to the channel in one go without needing spam-trigger
    private static final int MAX_SPAM_LINES = 5;

    // How often to try to reconnect to the server when disconnected, in ms
    private static final int RECONNECT_TIME = 15000;
    private static boolean spamOK = false;
    private static ModuleHandler moduleHandler;

    public static final String configFile = "props.xml";

    private static Grouphug bot;
    public static Grouphug getInstance() {
      return bot;
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        // Old reboot/reload functions - this is strictly not necessary, but maybe these
        // should be reimplemented properly sometime?
        if(message.equals("!reboot") || message.equals("!reload")) {
            bot.sendMessageChannel(channel, "Sorry, this functionality has been disabled. Patches are welcome though :)");
            return;
        }

        // Help trigger
        if(message.startsWith(MAIN_TRIGGER + HELP_TRIGGER)) {
            moduleHandler.onHelp(channel, message.substring(MAIN_TRIGGER.length() + HELP_TRIGGER.length()).trim());
        }

        // Normal/Spam-trigger
        if(message.startsWith(MAIN_TRIGGER) || message.startsWith(SPAM_TRIGGER)) {
            spamOK = message.startsWith(SPAM_TRIGGER);

            // But not for everyone
            if(spamOK && (sender.contains("icc") || login.contains("icc"))) {
                sendMessageChannel(channel, "icc, you are not allowed to use the spam trigger.");
                return;
            }
            moduleHandler.onTrigger(channel, sender, login, hostname, message.substring(1));
        }
        moduleHandler.onMessage(channel, sender, login, hostname, message);
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        moduleHandler.onJoin(channel, sender, login, hostname);
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        // Only accept help triggers in PM
        if(message.startsWith(MAIN_TRIGGER + HELP_TRIGGER)) {
            moduleHandler.onHelp(sender, message.substring(MAIN_TRIGGER.length() + HELP_TRIGGER.length()).trim());
        } else if(message.equals("!reparse")) {
            sendMessageChannel(sender, "Reparsing '" + Config.configFile() + "'...");
            Config.reparse();
            System.out.println(Config.nicks());
        }
    }

    @Override
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) {
            joinChannel(channel);
            sendMessageChannel(channel, "sry :(");
        }
    }

    @Override
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
        moduleHandler.onNickChange(oldNick, login, hostname, newNick);
    }

    /**
     * Sends a message to the specified channel.
     *
     * The message will NOT be protected against spam.
     *
     * The messages are splitted by maximum line number characters and by the newline character (\n), then
     * each line is sent to the pircbot sendMessage function, which adds the lines to the outgoing message queue
     * and sends them at the earliest possible opportunity.
     *
     * @param channel - The channel where the message should be sent
     * @param message - The message to send
     */
    public void sendMessageChannel(String channel,String message) {
        sendMessageChannel(channel, message, false);
    }

    /**
     * Sends a message to the specified channel.
     *
     * If verifySpam is true, the message will not be sent if it is longer than Grouphug.MAX_SPAM_LINES,
     * but instead replaced with a message telling the user to use the spam trigger (@) instead.
     *
     * verifySpam should not be used if the output is random, because then using the spam trigger obviously
     * won't resend the message that was too long.
     *
     * The messages are splitted by maximum line number characters and by the newline character (\n), then
     * each line is sent to the pircbot sendMessage function, which adds the lines to the outgoing message queue
     * and sends them at the earliest possible opportunity.
     *
     * @param receiver - Either the channel to send to, or a nick to send a PM to
     * @param message - The message to send
     * @param verifySpam - true if verifying that spamming is ok before sending large messages
     */
    public void sendMessageChannel(String receiver, String message, boolean verifySpam) {

        // First create a list of the lines we will send separately.
        List<String> lines = new ArrayList<String>();

        // This will be used for searching.
        int index;

        // Remove all carriage returns.
        for(index = message.indexOf('\r'); index != -1; index = message.indexOf('\r'))
            message = message.substring(0, index) + message.substring(index + 1);

        // Split all \n into different lines
        for(index = message.indexOf('\n'); index != -1; index = message.indexOf('\n')) {
            lines.add(message.substring(0, index).trim());
            message = message.substring(index + 1);
        }
        lines.add(message.trim());

        // If the message is longer than max line chars, separate them
        for(int i = 0; i<lines.size(); i++) {
            while(lines.get(i).length() > Grouphug.MAX_LINE_CHARS) {
                String line = lines.get(i);
                lines.remove(i);
                lines.add(i, line.substring(0, Grouphug.MAX_LINE_CHARS).trim());
                lines.add(i+1, line.substring(Grouphug.MAX_LINE_CHARS).trim());
            }
        }

        // Remove all empty lines
        for(int i = 0; i<lines.size(); i++) {
            if(lines.get(i).equals(""))
                lines.remove(i);
        }

        // Now check if we are spamming the channel, and stop if the spam-trigger isn't used
        if(verifySpam && !spamOK && lines.size() > MAX_SPAM_LINES) {
            sendMessage(receiver, "This would spam the channel with "+lines.size()+" lines, replace "+MAIN_TRIGGER+" with "+SPAM_TRIGGER+" if you really want that.");
            return;
        }

        // Finally send all the lines to the channel
        for(String line : lines) {
            this.sendMessage(receiver, line);
        }
    }

    /**
     * The main method, starting the bot, connecting to the server and joining its main channel.
     *
     * @param args Command-line arguments, unused
     * @throws UnsupportedEncodingException very rarely since the encoding is almost never changed
     */
    public static void main(String[] args) throws UnsupportedEncodingException {
        // Fire her up
        Grouphug.bot = new Grouphug();
        bot.setVerbose(true);
        bot.setEncoding("UTF-8");
        moduleHandler = new ModuleHandler(bot);
        connect(bot);
        for (String channel : Config.channels()) {
            bot.joinChannel(channel);
        }
        NickPoller.load(bot);
    }

    private static boolean connect(Grouphug bot) {
        final String prefix = "[connection] ";

        for(String server : Config.servers()) {
            try {
                System.out.print("\n" + prefix + "Connecting to " + server + "...");
                for(String nick : Config.nicks()) {
                    bot.setName(nick);
                    System.out.println("\n" + prefix + " -> Trying nick '" + nick + "'...");
                    try {
                        bot.connect(server);
                        System.out.println("\n" + prefix + "Connected as '" + nick + "'!");
                        break;
                    } catch(NickAlreadyInUseException ignored) {}
                }
                if(!bot.isConnected()) {
                    System.out.println("\n" + prefix + " -> None of our nicks are available, letting pircbot choose...");
                    bot.setAutoNickChange(true);
                    bot.connect(server);
                    System.out.println("\n" + prefix + "Connected as '" + bot.getNick() + "'!");
                }
                if(bot.isConnected()) {
                    return true;
                }
            } catch(IrcException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return bot.isConnected();
    }

    @Override
    protected void onDisconnect() {
        final String prefix = "[reconnecter] ";
        System.out.println("\n" + prefix + "Whoops, I was disconnected! Retrying connection...");
        while(!Grouphug.connect(this)) {
            System.out.println("\n" + prefix + "Noes! Couldn't connect to any of the specified servers!");
            System.out.println(prefix + "Trying again in " + (RECONNECT_TIME / 1000) + " seconds...");
            try { Thread.sleep(RECONNECT_TIME); } catch(InterruptedException ignored) { }
        }
        for (String channel : Config.channels()) {
            this.joinChannel(channel);
        }
        NickPoller.load(bot);
    }

}
