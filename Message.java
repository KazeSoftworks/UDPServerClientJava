

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Spellkaze
 */
public class Message implements Serializable
{
    private String sender;
    private String receiver;
    private String message;
    private boolean receivedNotification = false;
    private String date;
    private static final String COMMAND = "message";
    
    public void setMessage(String sender, String receiver, String message)
    {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        Date dateObj = new Date();
        date = df.format(dateObj);
    }
    
    
    //SET
    public void setSender (String sender)
    {
        this.sender = sender;
    }
    
    public void setReceiver(String receiver)
    {
        this.receiver = receiver;
    }
    
    private void setMessage(String message)
    {
        this.message = message;
    }
    
    private void setReceivedNotification()
    {
        receivedNotification = true;
    }
    
    //GET
    public String getSender()
    {
        return sender;
    }
    
    public String getReceiver()
    {
        return receiver;
    }
    
    
    public String getMessage()
    {
        return message;
    }
    
    public boolean getReceivedNotification()
    {
        return receivedNotification;
    }
    
    public String getDate()
    {
        return date;
    }

    
    @Override
    public String toString()
    {
        String output = sender + " -> " + receiver + ": " + message + " / " + date;
        return output;
    }
    
}
