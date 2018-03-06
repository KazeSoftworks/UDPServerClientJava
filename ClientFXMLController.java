

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author Spellkaze
 */
public class ClientFXMLController implements Initializable
{
    @FXML
    private TextField userField;
    @FXML
    private TextField SendToField;
    @FXML
    private TextArea MessageField;
    @FXML
    private Button loginButton;

    User userActive;
    DatagramSocket datagramSocket;
    @FXML
    private Button SendButton;

    Service<Void> backgroundThread;
    Message message = new Message();
    ArrayList<Message> messageList;
    ArrayList<String> sendToList;
    InetAddress address;

    private ObservableList<Message> dataMessage;
    @FXML
    private TableView<Message> table;
    @FXML
    private TableColumn<Message, String> ColumnRemitente;
    @FXML
    private TableColumn<Message, String> ColumnMensaje;
    @FXML
    private TableColumn<Message, String> ColumnFecha;
    @FXML
    private TableColumn<Message, String> ColumnReceptor;
    @FXML
    private Button unloginButton;
    @FXML
    private TextArea MessageTextArea;
    @FXML
    private Label senderAndReceiverLabel;
    @FXML
    private Label DateLabel;
    @FXML
    private AnchorPane root;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            address = InetAddress.getLocalHost();
            datagramSocket = new DatagramSocket();
        } catch (SocketException ex)
        {
            System.out.println("Socket timeout on initialize");
            Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex)
        {
            Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setTableMessagesAll()
    {
        dataMessage = FXCollections.observableArrayList();
        dataMessage.addAll(messageList);

        ColumnRemitente.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSender()));
        ColumnReceptor.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getReceiver()));
        ColumnMensaje.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMessage()));
        ColumnFecha.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDate()));

        table.setItems(dataMessage);
    }

    @FXML
    private void LoginAction(ActionEvent event)
    {
        String username = userField.getText();
        userActive = new User(username);
        SendAndConfirmPacket();
    }

    private void ActiveMenu()
    {
        System.out.println("Active Menu");
    }

    private void SendAndConfirmPacket()
    {
        try
        {
            //SEND
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));

            objectOutput.writeObject(userActive);
            objectOutput.flush();
            byte[] buffer = byteStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5000);
            datagramSocket.send(packet);

            //RECIEVE
            byte[] buffer2 = new byte[100];
            packet = new DatagramPacket(buffer2, buffer2.length);
            datagramSocket.setSoTimeout(10000);
            datagramSocket.receive(packet);
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer2);
            ObjectInputStream objectInput = new ObjectInputStream(new BufferedInputStream(inputByteStream));
            Object obj = objectInput.readObject();

            if (obj.toString().equals("GRANTED"))
            {
                Granted();
            } else
            {
                Denied();
            }

        } catch (SocketException | SocketTimeoutException ex)
        {
            DisplayAlert(AlertType.ERROR, "Conexion al Servidor", "No hay conexion al servidor");
            //System.out.println("No hay conexion con el servidor");
            //Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ClassNotFoundException ex)
        {
            Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } finally
        {
            try
            {
                datagramSocket.setSoTimeout(0);
            } catch (SocketException ex)
            {
                Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void Denied()
    {
        System.out.println("Access Denied");
        DisplayAlert(AlertType.ERROR, "Error en Usuario", "Ha ingresado un usuario incorrecto o inexistente");
    }

    private void Granted()
    {
        System.out.println("Access granted");
        Client.userActive = userActive.getUserName();
        ActivateMessengerMode();
        AwaitingPackets();

        if (!backgroundThread.isRunning())
        {
            AwaitingPackets();
        }

    }

    private void ActivateMessengerMode()
    {
        //userField.clear();
        userField.setDisable(true);
        loginButton.setDisable(true);

        unloginButton.setDisable(false);
        SendToField.setDisable(false);
        MessageField.setDisable(false);
        SendButton.setDisable(false);
    }

    private void DisableMessengerMode()
    {
        userField.clear();
        userField.setDisable(false);
        loginButton.setDisable(false);
        unloginButton.setDisable(true);

        SendToField.clear();
        MessageField.clear();

        dataMessage = FXCollections.observableArrayList();
        dataMessage.clear();

        ColumnRemitente.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSender()));
        ColumnReceptor.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getReceiver()));
        ColumnMensaje.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMessage()));
        ColumnFecha.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDate()));

        table.setItems(dataMessage);

        SendToField.setDisable(true);
        MessageField.setDisable(true);
        SendButton.setDisable(true);
    }

    @FXML
    private void SendAndConfirmMessagePacket()
    {
        if (!backgroundThread.isRunning())
        {
            AwaitingPackets();
        }

        try
        {
            ReadSenders();
            for (int i = 0; i < sendToList.size(); i++)
            {
                address = InetAddress.getLocalHost();
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
                ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));
                message.setMessage(userActive.getUserName(), sendToList.get(i), getMessageToSend());

                objectOutput.writeObject(message);
                objectOutput.flush();
                byte[] buffer = byteStream.toByteArray();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5000);
                System.out.println("Sending message to server... ");
                datagramSocket.send(packet);
                System.out.println("Message was sent ");

                Thread.sleep(1000);
            }

        } catch (SocketException | SocketTimeoutException ex)
        {
            System.out.println("Message has NOT been received Client");
            //Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex)
        {
            Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void ReadSenders()
    {
        String inputText = SendToField.getText();
        sendToList = new ArrayList<>(Arrays.asList(inputText.split("\\s*,\\s*")));
        System.out.println(sendToList);
    }

    private String getMessageToSend()
    {
        String inputText = MessageField.getText();
        return inputText;
    }

    private void AwaitingPackets() //RECEPTOR DE TODO PACKET
    {
        backgroundThread = new Service<Void>()
        {
            @Override
            protected Task<Void> createTask()
            {
                return new Task<Void>()
                {
                    @Override
                    protected Void call() throws Exception
                    {
                        try
                        {

                            while (true)
                            {
                                byte[] buffer = new byte[10000];
                                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                                datagramSocket.receive(packet);
                                System.out.println("Have received something");
                                ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
                                ObjectInputStream objectInput = new ObjectInputStream(new BufferedInputStream(inputByteStream));
                                Object obj = objectInput.readObject();

                                if (obj instanceof Message)
                                {
                                    System.out.println("Message received"); //PARA CONFIRMAR MENSAJE
                                    System.out.println(obj.toString());
                                    //Platform.runLater(() ->
                                    //{
                                    ConfirmUserPacket(datagramSocket, packet, "RECEIVED");
                                    //});

                                } else if (obj instanceof String)
                                {
                                    if (obj.toString().equals("RECEIVED")) //RECIBIR CONFIRMACION DE MENSAJE
                                    {
                                        System.out.println("Message has been received");
                                        Platform.runLater(() ->
                                        {
                                            DisplayAlert(AlertType.CONFIRMATION, "Mensaje recibido", "Mensaje ha sido recibido");
                                        });
                                    } else if (obj.toString().equals("NONEXISTANT")) // NO EXISTE O DESCONECTADO
                                    {
                                        Platform.runLater(() ->
                                        {
                                            DisplayAlert(AlertType.ERROR, "Mensaje no Enviado", "El mensaje no se ha recibido, Usuario inexistente/Desconectado");
                                        });
                                        System.out.println("Sender does not exist");
                                    } else if (obj.toString().equals("NOTRECEIVED")) //NO RECIBIDO
                                    {
                                        Platform.runLater(() ->
                                        {
                                            DisplayAlert(AlertType.ERROR, "Mensaje no Enviado", "El mensaje no se ha recibido, Usuario inexistente/Desconectado");
                                        });
                                        System.out.println("Message has NOT been received");
                                    } else if (obj.toString().equals("ALLCLEAR")) //NO RECIBIDO
                                    {
                                        Platform.runLater(() ->
                                        {
                                            clearSelectedMessage();
                                            ClearTable();
                                        });
                                        System.out.println("Clear table");
                                    } else if (obj.toString().equals("TERMINATE")) //TERMINAR PROGRAMA
                                    {
                                        System.out.println("Client has been terminated from server");
                                        Platform.runLater(() ->
                                        {
                                            DisplayAlert(AlertType.WARNING, "Usuario terminado", "El administrador ha terminado su usuario");
                                            try
                                            {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException ex)
                                            {
                                                Logger.getLogger(ClientFXMLController.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            Platform.exit();
                                        });
                                    } else
                                    {
                                        System.out.println("Rare message" + obj.toString());
                                    }
                                } else if (obj instanceof ArrayList<?>)
                                {
                                    if (((ArrayList<?>) obj).get(0) instanceof Message)
                                    {
                                        System.out.println("Message list got");
                                        messageList = (ArrayList<Message>) obj;
                                        Platform.runLater(() ->
                                        {
                                            clearSelectedMessage();
                                            setTableMessagesAll();
                                        });
                                    } else
                                    {
                                        System.out.println("No messages retrievable");
                                    }
                                } else
                                {
                                    System.out.println("Getting unknown object");
                                    System.out.println(obj.toString());
                                }

                            }
                        } catch (SocketException ex)
                        {
                            System.out.println("Socket timeoutr");
                        } catch (IOException ex)
                        {
                            System.out.println("IO error" + ex);
                        } catch (ClassNotFoundException ex)
                        {
                            System.out.println("Class error" + ex);
                        } finally
                        {
                            AwaitingPackets();
                        }
                        return null;
                    }
                };
            }
        };

        backgroundThread.restart();

    }

    private void ConfirmUserPacket(DatagramSocket socket, DatagramPacket packet, String confirm)
    {
        try
        {
            InetAddress addressOrigin = packet.getAddress();
            int portOrigin = packet.getPort();
            //SEND
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));

            objectOutput.writeObject(confirm);
            objectOutput.flush();
            byte[] buffer = byteStream.toByteArray();
            packet = new DatagramPacket(buffer, buffer.length, addressOrigin, portOrigin);
            socket.send(packet);
            System.out.println("Sending confirmation to: " + addressOrigin + " " + portOrigin);

            AwaitingPackets();

        } catch (SocketException ex)
        {
            Logger.getLogger(ClientFXMLController.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (IOException ex)
        {
            Logger.getLogger(ClientFXMLController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Terminate client when it closes
    @FXML
    public void TerminateClient()
    {
        System.out.println("Sending Termination command to server");
        try
        {
            address = InetAddress.getLocalHost();
            //SEND
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));

            objectOutput.writeObject("TERMINATE_CLIENT" + " " + userActive.getUserName());
            objectOutput.flush();
            byte[] buffer = byteStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5000);
            datagramSocket.send(packet);

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
        DisableMessengerMode();
        backgroundThread.cancel();
    }
    

    //ALERT FOR ERRORS
    private void DisplayAlert(Alert.AlertType type, String title, String message)
    {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private void ClearTable()
    {
        dataMessage = FXCollections.observableArrayList();
        dataMessage.clear();
        table.setItems(dataMessage);
    }

    @FXML
    private void SelectedMessageList(MouseEvent event)
    {
        Message messageSelected = table.getSelectionModel().getSelectedItem();
        String senderReceiverText = messageSelected.getSender() + "  ===>  " + messageSelected.getReceiver();
        String date = messageSelected.getDate();
        String message = messageSelected.getMessage();
        
        senderAndReceiverLabel.setText(senderReceiverText);
        DateLabel.setText(date);
        MessageTextArea.setText(message);
    }
    
    private void clearSelectedMessage()
    {
        senderAndReceiverLabel.setText("");
        DateLabel.setText("");
        MessageTextArea.setText("");
    }

}
