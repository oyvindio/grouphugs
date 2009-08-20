package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.util.Web;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

/**
 * URLCatcher module
 *
 * This module catches http:// or https:// URLs in messages to the cahnnel, tries to look up the URL, then parses
 * whatever it finds at  the URL, looking for <title>somethingsomething</title>, and sends somethingsomething back
 * to the channel.
 */
public class URLCatcher implements GrouphugModule
{
    private static final String[] URI_SCHEMES = new String[] { "http://", "https://" }; 
    private static final Pattern TITLE_BEGIN = Pattern.compile("<title>|<TITLE>");
    private static final Pattern TITLE_END = Pattern.compile("</title>|</TITLE>");    
    private static final String HELP_TRIGGER = "urlcatcher";
    private static final int TITLE_MAX_LENGTH = 100;


    /**
     * This method is called by the bot when someone sends a chat line that starts with the trigger command.
     * It is then up to each module to parse the line and, if applicable, respond.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     */
    public void trigger(String channel, String sender, String login, String hostname, String message)
    {
        // not used
    }

    /**
     * This method is similar to the <code>trigger</code> method, but is called when the chat line contains
     * no specific trigger command. The module may still choose to parse this, e.g. the karma module fetching
     * up sentences ending with ++/--, but be careful as an important part of this bot is not to bother anyone
     * unless specifically requested.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     */
    public void specialTrigger(String channel, String sender, String login, String hostname, String message)
    {
        ArrayList<String> urls = findAllUrls(message);
        for (final String url : urls)
        {
            // look up urls in separate threads, to avoid blocking for other requests
            new Thread()
            {
                @Override
                public void run()
                {
                    String title = getHTMLTitle(url);
                    if (title != null) {
                        if (title.length() > TITLE_MAX_LENGTH)
                        {
                            title = title.substring(0, TITLE_MAX_LENGTH);
                            title = title.concat(" (...)");
                        }
                        Grouphug.getInstance().sendMessage("Title: " + Grouphug.entitiesToChars(title), false);
                    }
                }
            }.start();
        }
    }


    /**
     * Try to find the title of the html document that maybe is at url.
     * @param url the url that maybe points to a html document.
     * @return the title of the html document (if it's there, obviously), null if we run in to trouble somewhere.
     */
    private String getHTMLTitle(String url)
    {
        String html = Web.fetchHTML(url);

        // fetchHTML returns null if something fails
        if (html == null)
            return null;

        int titleBeginIndex = 0, titleEndIndex = 0;
        Matcher titleBegin = TITLE_BEGIN.matcher(html);
        Matcher titleEnd = TITLE_END.matcher(html);

        //  find the index at which <title> ends in html ( if it's there at all )
        if (titleBegin.find())
            titleBeginIndex = titleBegin.end();

        // find the index at which </title> starts in html ( if it's there at all )
        if (titleEnd.find())
            titleEndIndex = titleEnd.start();

        String title = html.substring(titleBeginIndex, titleEndIndex);
        return "".equalsIgnoreCase(title) ? null : title;
    }

    /**
     * Find all urls matching a URI scheme in URI_SCHEMES in string.
     * @param string the strings to look for urls in.
     * @return any urls found, in an arraylist.
     */
    private ArrayList<String> findAllUrls(String string)
    {
        ArrayList<String> urls = new ArrayList<String>();

        for (String s : URI_SCHEMES)
        {
            urls.addAll(Web.findURIs(s, string));
        }
        
        return urls;
    }


    /**
     * This is called when the user is believed to ask for general help about the bot.
     * All modules should return a small string stripped for whitespace, containing a lowercase-representation
     * of its name, that could be used with the special help trigger, to get more specific help of the module.
     * <p/>
     * Example: If the bot's name is SuperModule, the string could be "super", so that on "!help super", this
     * bot would reply with how the supermodule is used.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     * @return the name of the current module that will trigger a help-message on the special help trigger
     */
    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message)
    {
        return HELP_TRIGGER;
    }

    /**
     * This is called when the user is believed to ask for specific help of a module.
     * The module should parse the message, and if it includes the trigger that would be sent back in the
     * helpMainTrigger method, then this module should reply, with notices in pm to the sender,
     * all info about how this module is used, under the presumtion that this is the only module replying.
     * Example output:
     * <p/>
     * SuperModule 1.1 - Does Super Magic Upon Request
     * - Triggered by: !super
     * - Alternative trigger: !superduper
     * (More info..?)
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel, stripped for the triggers + 1 char for space
     * @return boolean - true if the module reacted to the message, false otherwise
     */
    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message)
    {
        if (message.equals(HELP_TRIGGER))
        {
            return "URLCatcher tries to catch http:// or https:// URLs in messages to the channel, tries" +
                                   " to look up the URL, then parses whatever it finds at  the URL, looking for " +
                                   "a html <title>, and sends the title back to the channel.";
        }
        return null;
    }
}
