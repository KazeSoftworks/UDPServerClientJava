
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Spellkaze
 */
public class Client extends Application
{
    ClientFXMLController clientController = new ClientFXMLController();
    DatagramSocket datagramSocket = null;
    public static String userActive = "";

    @Override
    public void start(Stage stage) throws Exception
    {
        Parent root = FXMLLoader.load(getClass().getResource("ClientFXMLDocument.fxml"));

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
        
    }

    @Override
    public void stop() throws SocketException
    {
        System.out.println("Cerrando todo");
        TerminateClient();

    }
    
    public void TerminateClient()
    {
        System.out.println("Sending");
        try
        {
            datagramSocket = new DatagramSocket();
            InetAddress address = InetAddress.getLocalHost();
            //SEND
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));

            objectOutput.writeObject("TERMINATE_CLIENT" + " " + userActive);
            objectOutput.flush();
            byte[] buffer = byteStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5000);
            datagramSocket.send(packet);
            System.out.println("Sending");
            
                        Thread.sleep(500);

        } catch (SocketException | SocketTimeoutException ex)
        {
            System.out.println("No hay conexion con el servidor");
            //Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex)
        {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }

}
