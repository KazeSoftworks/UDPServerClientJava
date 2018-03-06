
import java.io.Serializable;
import java.net.InetAddress;

/**
 *
 * @author Spellkaze
 */
public class User implements Serializable
{

    private String userName;
    private boolean isConnected = false;
    private InetAddress address = null;
    private int port = 0;

    User(String userName)
    {
        this.userName = userName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public void ConnectionEstablished()
    {
        isConnected = true;
    }

    public void ConnectionLost()
    {
        isConnected = false;
    }

    public boolean ConnectionStatus()
    {
        return isConnected;
    }

    public void setAddress(InetAddress address)
    {
        this.address = address;
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public String toString()
    {
        return userName;
    }
}
