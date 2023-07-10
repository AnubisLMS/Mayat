package com.outlandr.irc.client.ui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import com.outlandr.irc.client.Channel;
import com.outlandr.irc.client.ClientState;
import com.outlandr.irc.client.IRCClient;
import com.outlandr.irc.client.IRCListener;
import com.outlandr.irc.client.Room;
import com.outlandr.irc.client.events.ConnectedEvent;
import com.outlandr.irc.client.events.Event;
import com.outlandr.irc.client.events.NameReplyEvent;
import com.outlandr.irc.client.events.UpdateChatEvent;
import com.outlandr.irc.server.User;

public class IRCClientUI implements IRCListener {

    private JFrame frmIrcChatClient;

    private JTextField textField;

    private JList membersList;

    private JList channelList;

    private JTree tree;

    private JSplitPane splitPane;

    private JSplitPane splitPane_1;

    Room selectedChannel;

    JTextArea chatArea;

    private IRCClient client;

    /**
	 * Launch the application.
	 */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    IRCClientUI window = new IRCClientUI();
                    window.frmIrcChatClient.setVisible(true);
                    window.setSplitterDivier();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
	 * Create the application.
	 */
    public IRCClientUI() {
        initialize();
    }

    public void setSplitterDivier() {
        splitPane.setDividerLocation(0.25);
        splitPane_1.setDividerLocation(0.75);
    }

    private void createClient(String host, String nick) {
        String SERVER;
        SERVER = host;
        int port = 6667;
        client = new IRCClient(SERVER, port, nick, "ar", "AR");
        client.addListener(this);
        client.start();
    }

    /**
	 * Initialize the contents of the frame.
	 */
    private void initialize() {
        frmIrcChatClient = new JFrame();
        frmIrcChatClient.setMinimumSize(new Dimension(700, 500));
        frmIrcChatClient.setTitle("IRC Chat client");
        frmIrcChatClient.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                if (client != null) {
                    client.quit();
                }
                System.exit(0);
            }
        });
        frmIrcChatClient.addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent arg0) {
                setSplitterDivier();
            }
        });
        {
            JMenuBar menuBar = new JMenuBar();
            frmIrcChatClient.setJMenuBar(menuBar);
            {
                JMenu mnChat = new JMenu("Chat");
                menuBar.add(mnChat);
                {
                    JMenuItem mntmConnect = new JMenuItem("Connect");
                    mntmConnect.addMouseListener(new MouseAdapter() {

                        @Override
                        public void mouseReleased(MouseEvent arg0) {
                            JTextField host = new JTextField("192.168.192.128");
                            JTextField nickName = new JTextField("ar");
                            Object[] msg = { "IRC Server:", host, "Nick Name:", nickName };
                            JOptionPane op = new JOptionPane(msg, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, null);
                            JDialog dialog = op.createDialog(frmIrcChatClient, "Enter IRC info...");
                            dialog.setVisible(true);
                            int result = JOptionPane.OK_OPTION;
                            try {
                                result = ((Integer) op.getValue()).intValue();
                            } catch (Exception uninitializedValue) {
                            }
                            if (result == JOptionPane.OK_OPTION) {
                                if (client == null) {
                                    createClient(host.getText(), nickName.getText());
                                } else {
                                    JOptionPane.showMessageDialog(frmIrcChatClient, "Already connected");
                                }
                            } else {
                                System.out.println("Canceled");
                            }
                        }
                    });
                    mnChat.add(mntmConnect);
                }
                {
                    JMenuItem mntmDisconnect = new JMenuItem("Leave Channel");
                    mntmDisconnect.addMouseListener(new MouseAdapter() {

                        @Override
                        public void mouseReleased(MouseEvent arg0) {
                            if (client != null && selectedChannel != null && selectedChannel instanceof Channel) {
                                client.leave((Channel) selectedChannel);
                                Room[] channels = client.getClientState().getChannels();
                                channelList.setListData(channels);
                            }
                        }
                    });
                    mnChat.add(mntmDisconnect);
                }
                {
                    JMenuItem mntmExit = new JMenuItem("Exit");
                    mntmExit.addMouseListener(new MouseAdapter() {

                        @Override
                        public void mouseReleased(MouseEvent arg0) {
                            if (client != null) {
                                client.quit();
                            }
                            frmIrcChatClient.dispose();
                            System.exit(0);
                        }
                    });
                    mnChat.add(mntmExit);
                }
            }
        }
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0, 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0 };
        gridBagLayout.columnWeights = new double[] { 1.0, 1.0E-4 };
        gridBagLayout.rowWeights = new double[] { 1.0, 1.0E-4 };
        frmIrcChatClient.getContentPane().setLayout(gridBagLayout);
        {
            splitPane = new JSplitPane();
            splitPane.addComponentListener(new ComponentAdapter() {

                public void componentResized(ComponentEvent arg0) {
                    setSplitterDivier();
                }
            });
            splitPane.setDividerLocation(0.25);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = 0;
            frmIrcChatClient.getContentPane().add(splitPane, gbc);
            {
                channelList = new JList();
                channelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                channelList.addListSelectionListener(new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent event) {
                        JList source = (JList) event.getSource();
                        selectedChannel = (Room) source.getSelectedValue();
                        if (selectedChannel != null) {
                            String buffer = selectedChannel.getBuffer();
                            chatArea.setText(buffer);
                        }
                    }
                });
                splitPane.setLeftComponent(channelList);
            }
            {
                JPanel panel = new JPanel();
                GridBagLayout gridBagLayout_1 = new GridBagLayout();
                gridBagLayout_1.columnWidths = new int[] { 0, 0 };
                gridBagLayout_1.rowHeights = new int[] { 0, 0, 0 };
                gridBagLayout_1.columnWeights = new double[] { 1.0, 1.0E-4 };
                gridBagLayout_1.rowWeights = new double[] { 1.0, 0.0, 1.0E-4 };
                panel.setLayout(gridBagLayout_1);
                splitPane.setRightComponent(panel);
                {
                    splitPane_1 = new JSplitPane();
                    splitPane_1.setDividerLocation(0.75);
                    GridBagConstraints gbc_1 = new GridBagConstraints();
                    gbc_1.insets = new Insets(0, 0, 5, 0);
                    gbc_1.fill = GridBagConstraints.BOTH;
                    gbc_1.gridx = 0;
                    gbc_1.gridy = 0;
                    panel.add(splitPane_1, gbc_1);
                    {
                        membersList = new JList();
                        splitPane_1.setRightComponent(membersList);
                    }
                    {
                        JScrollPane scrollPane = new JScrollPane();
                        splitPane_1.setLeftComponent(scrollPane);
                        {
                            chatArea = new JTextArea();
                            scrollPane.setViewportView(chatArea);
                        }
                    }
                }
                {
                    textField = new JTextField();
                    textField.addKeyListener(new KeyAdapter() {

                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                                String channelName = null;
                                if (selectedChannel != null && selectedChannel instanceof Channel) channelName = ((Channel) selectedChannel).getName();
                                String text = textField.getText();
                                client.queueUserMessage(channelName, text);
                                if (text.charAt(0) != '/' && selectedChannel != null) {
                                    String nick = client.getNick();
                                    String str = String.format("%s: %s", nick, text);
                                    chatArea.append(str + '\n');
                                    selectedChannel.updateText(str);
                                }
                                textField.setText("");
                            }
                        }
                    });
                    GridBagConstraints gbc_1 = new GridBagConstraints();
                    gbc_1.fill = GridBagConstraints.HORIZONTAL;
                    gbc_1.gridx = 0;
                    gbc_1.gridy = 1;
                    panel.add(textField, gbc_1);
                    textField.setColumns(10);
                }
            }
        }
        frmIrcChatClient.setBounds(100, 100, 450, 300);
        frmIrcChatClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void handleEvent(final Event event) {
        try {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    Room[] channels = client.getClientState().getChannels();
                    if (event instanceof ConnectedEvent) {
                        ConnectedEvent connected = (ConnectedEvent) event;
                        channelList.setListData(channels);
                        String text = connected.getText();
                        chatArea.append(text + "\n");
                        channelList.setSelectedValue(channels[0], true);
                    } else if (event instanceof UpdateChatEvent) {
                        UpdateChatEvent update = (UpdateChatEvent) event;
                        String text = update.getText();
                        chatArea.append(text + "\n");
                    } else if (event instanceof NameReplyEvent) {
                        NameReplyEvent nameRply = (NameReplyEvent) event;
                        membersList.setListData(nameRply.getChannelMembers());
                        channelList.setListData(channels);
                        String channelName = nameRply.getChannel();
                        Room channel = client.getClientState().getChannel(channelName);
                        channelList.setSelectedValue(channel, true);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("Exception when Updateing chat window: " + e);
            System.exit(1);
        }
    }
}
