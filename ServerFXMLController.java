
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
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
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

/**
 *
 * @author Spellkaze
 */
public class ServerFXMLController implements Initializable
{

    ArrayList<Message> messageList = new ArrayList<>();
    ArrayList<User> userList = new ArrayList<>();
    Service<Void> backgroundThread;
    Message selectedMessage;

    ArrayList<Message> outMessageList;

    @FXML
    private TableColumn<Message, String> ColumnRemitente;
    @FXML
    private TableColumn<Message, String> ColumnReceptor;
    @FXML
    private TableColumn<Message, String> ColumnMensaje;
    @FXML
    private TableColumn<Message, String> ColumnFecha;
    @FXML
    private TableColumn<Message, String> ColumnConfirmation;

    private ObservableList<Message> dataMessage;
    private ObservableList<User> dataUser;

    @FXML
    private TableView<Message> table;
    DatagramSocket datagramSocket;

    boolean abortedSending = false;
    @FXML
    private ListView<User> UserListTable;
    @FXML
    private TextField userField;
    @FXML
    private TextField FilterField;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            datagramSocket = new DatagramSocket(5000);
        } catch (SocketException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
        MessageListRead();
        UserListRead();
        setTableMessagesAll();
        //SetUserList();
        setUserTableAll();
        AwaitingPackets();
        UserListWrite();
        MessageListWrite();
    }

    public void setTableMessagesAll()
    {
        dataMessage = FXCollections.observableArrayList();
        dataMessage.addAll(messageList);

        ColumnRemitente.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSender()));
        ColumnReceptor.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getReceiver()));
        ColumnMensaje.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMessage()));
        ColumnFecha.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDate()));
        ColumnConfirmation.setCellValueFactory(p ->
        {
            if (!p.getValue().getReceivedNotification())
            {
                return new SimpleStringProperty("✓"); //Simbolo positivo
            } else
            {
                return new SimpleStringProperty("✗"); //Simbolo negativo
            }
        });

        table.setItems(dataMessage);
    }

    @FXML
    public void SelectedUserList()
    {
        userField.setText(UserListTable.getSelectionModel().getSelectedItem().getUserName());
    }

    public void setUserTableAll()
    {
        dataUser = FXCollections.observableArrayList();
        dataUser.addAll(userList);

        UserListTable.setItems(dataUser);
    }

    public void UserListRead()
    {
        try
        {
            ObjectInputStream archivoIngreso = new ObjectInputStream(new FileInputStream("./user.dat"));
            userList = (ArrayList<User>) archivoIngreso.readObject();
        } catch (FileNotFoundException ex)
        {
            System.out.println("No file to read");
        } catch (IOException | ClassNotFoundException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void UserListWrite()
    {
        try
        {
            ObjectOutputStream ArchivoSalida = new ObjectOutputStream(new FileOutputStream("./user.dat", false));
            ArchivoSalida.writeObject(userList);

        } catch (IOException ex)
        {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void MessageListRead()
    {
        try
        {
            ObjectInputStream archivoIngreso = new ObjectInputStream(new FileInputStream("./messages.dat"));
            messageList = (ArrayList<Message>) archivoIngreso.readObject();

        } catch (FileNotFoundException ex)
        {
            System.out.println("No file to read");
        } catch (IOException | ClassNotFoundException ex)
        {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void MessageListWrite()
    {
        try
        {
            ObjectOutputStream ArchivoSalida = new ObjectOutputStream(new FileOutputStream("./messages.dat", false));
            ArchivoSalida.writeObject(messageList);

        } catch (IOException ex)
        {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void ShowAllMesages()
    {
        System.out.println(messageList);
    }

    private void AwaitingPackets()
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
                                byte[] buffer = new byte[5000];
                                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                                datagramSocket.receive(packet);
                                ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
                                ObjectInputStream objectInput = new ObjectInputStream(new BufferedInputStream(inputByteStream));
                                Object obj = objectInput.readObject();

                                System.out.println(obj.toString());

                                if (obj instanceof User)
                                {
                                    System.out.println("is user request");
                                    User dataObtained = (User) obj;
                                    if (SearchListUser(dataObtained))
                                    {
                                        Platform.runLater(() ->
                                        {
                                            ConfirmUserPacket(dataObtained, datagramSocket, packet, "GRANTED");
                                            sendMessageListToUser(dataObtained, datagramSocket, packet);
                                        });
                                    } else
                                    {
                                        Platform.runLater(() ->
                                        {
                                            ConfirmUserPacket(dataObtained, datagramSocket, packet, "REJECTED");
                                        });
                                    }

                                } else if (obj instanceof Message)
                                {
                                    Message dataObtained = (Message) obj;
                                    System.out.println("is message request");
                                    ConfirmMessagePacket(dataObtained, datagramSocket, packet);

                                    Platform.runLater(() ->
                                    {
                                        sendMessageListToSender(dataObtained.getSender(), datagramSocket, packet);
                                        if (!abortedSending)
                                        {
                                            try
                                            {
                                                Thread.sleep(100);
                                                sendMessageListToReceiver(dataObtained.getReceiver(), datagramSocket, packet);
                                            } catch (InterruptedException ex)
                                            {
                                                Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                        MessageListWrite();
                                        setTableMessagesAll();
                                    });
                                } else if (obj instanceof String)
                                {
                                    String dataObtained = (String) obj;
                                    if (dataObtained.substring(0, 16).equals("TERMINATE_CLIENT"))
                                    {
                                        String[] parted = dataObtained.split(" ");
                                        System.out.println("Terminating client " + parted[1]);
                                        ResetUserInfo(parted[1]);
                                    } else
                                    {
                                        System.out.println("Data string obtained incorrectly: " + dataObtained);
                                    }
                                } else
                                {
                                    System.out.println("Getting unknown object");
                                    System.out.println(obj.toString());
                                }
                            }
                        } catch (SocketException | SocketTimeoutException ex)
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
                            datagramSocket.setSoTimeout(0);
                        }
                        return null;
                    }
                };
            }
        };

        backgroundThread.restart();

    }

    private void ConfirmUserPacket(User user, DatagramSocket socket, DatagramPacket packet, String confirm)
    {
        try
        {
            InetAddress address = packet.getAddress();
            int port = packet.getPort();

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));

            objectOutput.writeObject(confirm);
            objectOutput.flush();
            byte[] buffer2 = byteStream.toByteArray();

            packet = new DatagramPacket(buffer2, buffer2.length, address, port);
            socket.send(packet);
            System.out.println("Sending confirmation to: " + address + " " + port + " " + confirm);

            setUserAddressPort(user, address, port);

        } catch (SocketException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void ConfirmMessagePacket(Message message, DatagramSocket socket, DatagramPacket packet)
    {
        abortedSending = false;
        String sender = message.getSender();
        String receiver = message.getReceiver();
        String messageGot = message.getMessage();
        String date = message.getDate();

        InetAddress addressOrigin = packet.getAddress();
        int portOrigin = packet.getPort();
        try
        {

            InetAddress address = null;
            int port = 0;
            for (User userOnList : userList)
            {
                if (receiver.equals(userOnList.getUserName()))
                {
                    address = userOnList.getAddress();
                    port = userOnList.getPort();
                }
            }
            if (address == null) //SECURITY CHECK PARA IMPOSIBLE DE ENVIAR
            {
                System.out.println("Abortar Envio");
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
                ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));

                objectOutput.writeObject("NONEXISTANT");
                objectOutput.flush();
                byte[] buffer2 = byteStream.toByteArray();

                packet = new DatagramPacket(buffer2, buffer2.length, addressOrigin, portOrigin);
                socket.send(packet);
                System.out.println("Sending counter to: " + addressOrigin + " " + portOrigin);
                abortedSending = true;
                return;
            }

            //Send message to receiver
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));

            objectOutput.writeObject(message);
            objectOutput.flush();
            byte[] buffer2 = byteStream.toByteArray();

            packet = new DatagramPacket(buffer2, buffer2.length, address, port);
            socket.send(packet);
            System.out.println("Sending message to: " + address + " " + port);

            //Awaiting confirmation from receiver
            byte[] buffer = new byte[100];
            packet = new DatagramPacket(buffer, buffer.length);
            datagramSocket.setSoTimeout(4000);
            datagramSocket.receive(packet);
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
            ObjectInputStream objectInput = new ObjectInputStream(new BufferedInputStream(inputByteStream));
            Object obj = objectInput.readObject();

            System.out.println("Recieved confirmation from: " + packet.getAddress() + " " + packet.getPort());
            System.out.println(obj.toString());

            if (obj.toString().equals("RECEIVED"))
            {
                messageList.add(message);
                System.out.println("Message has been received");
                //Send message to sender
                ByteArrayOutputStream byteStream2 = new ByteArrayOutputStream(5000);
                ObjectOutputStream objectOutput2 = new ObjectOutputStream(new BufferedOutputStream(byteStream2));

                objectOutput2.writeObject("RECEIVED");
                objectOutput2.flush();
                byte[] buffer3 = byteStream2.toByteArray();

                packet = new DatagramPacket(buffer3, buffer3.length, addressOrigin, portOrigin);
                socket.send(packet);
                System.out.println("Sending confirmation to: " + addressOrigin + " " + portOrigin);
                Thread.sleep(100);

            } else
            {
                System.out.println("Message has NOT been received");
            }
        } catch (SocketException | SocketTimeoutException ex)
        {
            System.out.println("Message has Not been received");
            //Send message to sender
            try
            {
                datagramSocket.setSoTimeout(0);
                ByteArrayOutputStream byteStream2 = new ByteArrayOutputStream(5000);
                ObjectOutputStream objectOutput2 = new ObjectOutputStream(new BufferedOutputStream(byteStream2));
                objectOutput2.writeObject("NOTRECEIVED");
                objectOutput2.flush();
                byte[] buffer3 = byteStream2.toByteArray();

                packet = new DatagramPacket(buffer3, buffer3.length, addressOrigin, portOrigin);
                socket.send(packet);
                System.out.println("Sending confirmation of not received to: " + addressOrigin + " " + portOrigin);
            } catch (IOException ex1)
            {
                Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex1);
            }

            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } finally
        {
            try
            {
                datagramSocket.setSoTimeout(0);
            } catch (SocketException ex)
            {
                //Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    //#Searching list by username
    private boolean SearchListUser(User user)
    {
        for (User userOnList : userList)
        {
            if (user.getUserName().equals(userOnList.getUserName()))
            {
                return true;
            }
        }
        return false;
    }

    //#Searching list by username
    private boolean SearchListUser(String user)
    {
        for (User userOnList : userList)
        {
            if (user.equals(userOnList.getUserName()))
            {
                return true;
            }
        }
        return false;
    }

    //Asigns adddres and port to user list
    private void setUserAddressPort(User user, InetAddress address, int port)
    {
        for (User userOnList : userList)
        {
            if (user.getUserName().equals(userOnList.getUserName()))
            {
                userOnList.setAddress(address);
                userOnList.setPort(port);
                userOnList.ConnectionEstablished();
                System.out.println("Guardando info de cliente: " + userOnList.getUserName() + " " + userOnList.getAddress() + " " + userOnList.getPort());
            }
        }
    }

    //Send message list to the respective client
    private void sendMessageListToUser(User user, DatagramSocket socket, DatagramPacket packet)
    {
        outMessageList = new ArrayList<>();
        InetAddress address = user.getAddress();
        int port = user.getPort();

        for (User userOnList : userList)
        {
            if (user.getUserName().equals(userOnList.getUserName()))
            {
                address = userOnList.getAddress();
                port = userOnList.getPort();
            }
        }

        for (Message messageOnList : messageList)
        {
            if (user.getUserName().equals(messageOnList.getSender()) || user.getUserName().equals(messageOnList.getReceiver()))
            {
                outMessageList.add(messageOnList);
            }
        }

        try
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));
            objectOutput.writeObject(outMessageList);
            objectOutput.flush();
            byte[] buffer2 = byteStream.toByteArray();

            packet = new DatagramPacket(buffer2, buffer2.length, address, port);
            socket.send(packet);
            System.out.println("Sending message List to: " + address + " " + port);
        } catch (IOException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Send message list, update sender and receiver message list;
    private void sendMessageListToSender(String sender, DatagramSocket socket, DatagramPacket packet)
    {
        outMessageList = new ArrayList<>();
        InetAddress address = null;
        int port = 0;

        //UPDATE SENDER
        for (User userOnList : userList)
        {
            if (sender.equals(userOnList.getUserName()))
            {
                address = userOnList.getAddress();
                port = userOnList.getPort();
            }
        }

        for (Message messageOnList : messageList)
        {
            if (sender.equals(messageOnList.getSender()) || sender.equals(messageOnList.getReceiver()))
            {
                outMessageList.add(messageOnList);
            }
        }

        try
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));
            objectOutput.writeObject(outMessageList);
            objectOutput.flush();
            byte[] buffer2 = byteStream.toByteArray();

            packet = new DatagramPacket(buffer2, buffer2.length, address, port);
            socket.send(packet);
            System.out.println("Sending message Sender List to: " + address + " " + port);
        } catch (IOException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Send message list, update sender and receiver message list;
    private void sendMessageListToReceiver(String receiver, DatagramSocket socket, DatagramPacket packet)
    {
        outMessageList = new ArrayList<>();
        InetAddress address = null;
        int port = 0;

        //UPDATE SENDER
        for (User userOnList : userList)
        {
            if (receiver.equals(userOnList.getUserName()))
            {
                address = userOnList.getAddress();
                port = userOnList.getPort();
            }
        }

        for (Message messageOnList : messageList)
        {
            if (receiver.equals(messageOnList.getSender()) || receiver.equals(messageOnList.getReceiver()))
            {
                outMessageList.add(messageOnList);
            }
        }

        try
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));
            objectOutput.writeObject(outMessageList);
            objectOutput.flush();
            byte[] buffer2 = byteStream.toByteArray();

            packet = new DatagramPacket(buffer2, buffer2.length, address, port);
            socket.send(packet);
            System.out.println("Sending message Receiver List to: " + address + " " + port);
        } catch (IOException ex)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void ResetUserInfo(String user)
    {

        for (User userOnList : userList)
        {
            if (user.equals(userOnList.getUserName()))
            {
                userOnList.setAddress(null);
                userOnList.setPort(0);
                System.out.println("Terminated " + userOnList.getUserName());
                return;
            }
        }
        UserListWrite();

    }

    //#DEBUG
    private void SetUserList()
    {
        userList.add(new User("Waluigi"));
        userList.add(new User("Espageti"));
        userList.add(new User("Wario"));
    }

    @FXML
    private void AddUser(ActionEvent event)
    {
        String user = userField.getText();
        if (!SearchListUser(user) && user.length() > 0)
        {
            userList.add(new User(user));
        } else
        {
            System.out.println("Already added");
        }
        setUserTableAll();
        UserListWrite();
    }

    @FXML
    private void RemoveUser(ActionEvent event)
    {
        String user = userField.getText();
        if (SearchListUser(user) && user.length() > 0)
        {
            for (int i = 0; i < userList.size(); i++)
            {
                if (user.equals(userList.get(i).getUserName()))
                {
                    TerminateClientbyUser(userList.get(i).getUserName());
                    userList.remove(i);
                    for (int j = 0; j < messageList.size(); j++)
                    {
                        if (messageList.get(j).getSender().equals(user))
                        {
                            messageList.get(j).setSender("-ELIMINADO-");
                        }
                        if (messageList.get(j).getReceiver().equals(user))
                        {
                            messageList.get(j).setReceiver("-ELIMINADO-");
                        }
                    }
                }
            }

        } else
        {
            System.out.println("No user like that");
        }
        System.out.println(messageList);
        setUserTableAll();
        dataMessage.clear();
        setTableMessagesAll();
        UpdateMessageListToAllConnected();
        UserListWrite();
    }

    @FXML
    private void SelectedMessageList(MouseEvent event)
    {
        selectedMessage = table.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void DeleteMessage(ActionEvent event)
    {
        for (int i = 0; i < messageList.size(); i++)
        {
            if (messageList.get(i).equals(selectedMessage))
            {
                System.out.println("Found message to get deleted");
                messageList.remove(i);
                UpdateMessageListToAllConnected();
            }
        }
        setTableMessagesAll();
        MessageListWrite();

    }

    @FXML
    private void FilterMessageListSender(ActionEvent event)
    {
        String searchString = FilterField.getText();
        ArrayList<Message> filteredMessageList = new ArrayList<>();
        ObservableList<Message> dataFilteredMessage;
        dataFilteredMessage = FXCollections.observableArrayList();

        if (searchString.length() > 0)
        {
            for (Message messageOnList : messageList)
            {
                if (searchString.equals(messageOnList.getSender()))
                {
                    filteredMessageList.add(messageOnList);
                }
            }
            System.out.println(filteredMessageList);

            dataFilteredMessage.addAll(filteredMessageList);

            ColumnRemitente.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSender()));
            ColumnReceptor.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getReceiver()));
            ColumnMensaje.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMessage()));
            ColumnFecha.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDate()));
            ColumnConfirmation.setCellValueFactory(p ->
            {
                if (!p.getValue().getReceivedNotification())
                {
                    return new SimpleStringProperty("✓"); //Simbolo positivo
                } else
                {
                    return new SimpleStringProperty("✗"); //Simbolo negativo
                }
            });

            table.setItems(dataFilteredMessage);
        } else
        {
            setTableMessagesAll();
        }
    }

    @FXML
    private void FilterMessageListReceiver(ActionEvent event)
    {
        String searchString = FilterField.getText();
        ArrayList<Message> filteredMessageList = new ArrayList<>();
        ObservableList<Message> dataFilteredMessage;
        dataFilteredMessage = FXCollections.observableArrayList();

        if (searchString.length() > 0)
        {
            for (Message messageOnList : messageList)
            {
                if (searchString.equals(messageOnList.getReceiver()))
                {
                    filteredMessageList.add(messageOnList);
                }
            }
            System.out.println(filteredMessageList);

            dataFilteredMessage.addAll(filteredMessageList);

            ColumnRemitente.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSender()));
            ColumnReceptor.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getReceiver()));
            ColumnMensaje.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMessage()));
            ColumnFecha.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDate()));
            ColumnConfirmation.setCellValueFactory(p ->
            {
                if (!p.getValue().getReceivedNotification())
                {
                    return new SimpleStringProperty("✓"); //Simbolo positivo
                } else
                {
                    return new SimpleStringProperty("✗"); //Simbolo negativo
                }
            });

            table.setItems(dataFilteredMessage);
        } else
        {
            setTableMessagesAll();
        }
    }

    private void TerminateClientbyUser(String user)
    {
        InetAddress address = null;
        int port = 0;

        //UPDATE SENDER
        for (User userOnList : userList)
        {
            if (user.equals(userOnList.getUserName()))
            {
                address = userOnList.getAddress();
                port = userOnList.getPort();
                System.out.println("Found address");
            }
        }

        if (address == null)
        {
            System.out.println(user + " already terminated");
            return;
        }

        try
        {
            ByteArrayOutputStream byteStream2 = new ByteArrayOutputStream(5000);
            ObjectOutputStream objectOutput2 = new ObjectOutputStream(new BufferedOutputStream(byteStream2));
            objectOutput2.writeObject("TERMINATE");
            objectOutput2.flush();
            byte[] buffer3 = byteStream2.toByteArray();

            DatagramPacket packet = new DatagramPacket(buffer3, buffer3.length, address, port);
            datagramSocket.send(packet);
            System.out.println("Terminating client remotely:  " + address + " " + port);
        } catch (IOException ex1)
        {
            Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex1);
        }
    }

    private void UpdateMessageListToAllConnected()
    {
        for (User userOnList : userList)
        {
            outMessageList = new ArrayList<>();
            InetAddress address;
            int port;
            boolean canSend = true;
            address = userOnList.getAddress();
            port = userOnList.getPort();

            if (address == null)
            {
                canSend = false;
            }
            System.out.println(userOnList.getUserName() + " " + userOnList.getAddress() + " " + userOnList.getPort() + " " + canSend);

            if (canSend)
            {
                for (Message messageOnList : messageList)
                {
                    if (userOnList.getUserName().equals(messageOnList.getSender()) || userOnList.getUserName().equals(messageOnList.getReceiver()))
                    {
                        outMessageList.add(messageOnList);
                    }
                }

                try
                {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
                    ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(byteStream));
                    if (outMessageList.size() > 0)
                    {
                        objectOutput.writeObject(outMessageList);
                        objectOutput.flush();
                    } else
                    {
                        objectOutput.writeObject("ALLCLEAR");
                        objectOutput.flush();
                    }
                    byte[] buffer2 = byteStream.toByteArray();

                    DatagramPacket packet = new DatagramPacket(buffer2, buffer2.length, address, port);
                    datagramSocket.send(packet);
                    System.out.println("Updating Message List to: " + userOnList.getUserName() + " " + address + " " + port);
                } catch (IOException ex)
                {
                    Logger.getLogger(ServerFXMLController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @FXML
    private void DeleteMessageFromUser(ActionEvent event)
    {
        ArrayList<Message> toDeleteList = new ArrayList<>();
        String user = userField.getText();
        for (Message messageOnList : messageList)
        {
            if (messageOnList.getReceiver().equals(user) || messageOnList.getSender().equals(user))
            {
                toDeleteList.add(messageOnList);
            }
        }
        messageList.removeAll(toDeleteList);

        setTableMessagesAll();
        MessageListWrite();
        UpdateMessageListToAllConnected();
    }

    @FXML
    private void DeleteMessageFromDeletedUser(ActionEvent event)
    {
        ArrayList<Message> toDeleteList = new ArrayList<>();
        String user = "-ELIMINADO-";
        for (Message messageOnList : messageList)
        {
            if (messageOnList.getReceiver().equals(user) || messageOnList.getSender().equals(user))
            {
                toDeleteList.add(messageOnList);
            }
        }
        messageList.removeAll(toDeleteList);

        setTableMessagesAll();
        MessageListWrite();
        UpdateMessageListToAllConnected();
    }

    @FXML
    private void FilterUser(ActionEvent event)
    {
        String searchString = userField.getText();
        ArrayList<User> filteredUserList = new ArrayList<>();
        ObservableList<User> dataFilteredUser;

        if (!(searchString.length() > 0))
        {
            setUserTableAll();
            return;
        }

        for (User userOnList : userList)
        {
            if (userOnList.getUserName().startsWith(searchString))
            {
                filteredUserList.add(userOnList);
            }
        }

        dataFilteredUser = FXCollections.observableArrayList();
        dataFilteredUser.addAll(filteredUserList);

        UserListTable.setItems(dataFilteredUser);
    }
}
