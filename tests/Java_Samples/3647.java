package uk.ac.kingston.aqurate.author_UI;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.*;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import src.Previewer;
import gqtiv2.AssessmentHandler;
import gqtiv2.ItemBodyHandler;
import uk.ac.kingston.aqurate.api.AqurateAssessmentItem;
import uk.ac.kingston.aqurate.api.AqurateException;
import uk.ac.kingston.aqurate.api.AqurateObjectFactory;
import uk.ac.kingston.aqurate.author_controllers.AssociateInteractionController;
import uk.ac.kingston.aqurate.author_controllers.ChoiceInteractionController;
import uk.ac.kingston.aqurate.author_controllers.DefaultController;
import uk.ac.kingston.aqurate.author_controllers.GraphicOrderInteractionController;
import uk.ac.kingston.aqurate.author_controllers.HotspotInteractionController;
import uk.ac.kingston.aqurate.author_controllers.InlineChoiceInteractionController;
import uk.ac.kingston.aqurate.author_controllers.OrderInteractionController;
import uk.ac.kingston.aqurate.author_controllers.SliderInteractionController;
import uk.ac.kingston.aqurate.author_controllers.TextEntryInteractionController;
import uk.ac.kingston.aqurate.author_documents.AssessmentItemDoc;
import uk.ac.kingston.aqurate.author_documents.AssociateInteractionDoc;
import uk.ac.kingston.aqurate.author_documents.ChoiceInteractionDoc;
import uk.ac.kingston.aqurate.author_documents.GraphicOrderInteractionDoc;
import uk.ac.kingston.aqurate.author_documents.HotspotInteractionDoc;
import uk.ac.kingston.aqurate.author_documents.InlineChoiceInteractionDoc;
import uk.ac.kingston.aqurate.author_documents.OrderInteractionDoc;
import uk.ac.kingston.aqurate.author_documents.SliderInteractionDoc;
import uk.ac.kingston.aqurate.author_documents.TextEntryInteractionDoc;
import uk.ac.kingston.aqurate.util.ContentPackageBuilder;
import uk.ac.kingston.aqurate.util.XMLFileFilter;

public class AqurateFramework extends JFrame implements FocusListener, ActionListener, MouseListener {

    static class initAPI extends Thread {

        private AqurateObjectFactory aqurateObjectFactory;

        private String sProgress = "";

        AqurateFramework owner = null;

        initAPI(AqurateObjectFactory aqurateObjectFactory, AqurateFramework owner) {
            this.aqurateObjectFactory = aqurateObjectFactory;
            this.sProgress = "";
            this.owner = owner;
        }

        public String getProgress() {
            return sProgress;
        }

        @Override
        public void run() {
            sProgress = "Creating QTI Schema Factory.";
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                aqurateObjectFactory.createQTISchemaFactory();
            }
            sProgress = "Creating QTI Object Factory...";
            aqurateObjectFactory.createQTIObjectFactory();
            sProgress = "Creating QTI Marshaler...";
            aqurateObjectFactory.createQTIMarshaller();
            sProgress = "Creating QTI Unmarshaller...";
            aqurateObjectFactory.createQTIUnmarshaller();
            sProgress = "Setting QTI Unmarshaller to the schemas.";
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                aqurateObjectFactory.setSchemaQTIUnmarshaller();
            }
        }
    }

    public static int currentViewIndex;

    private static final int iIMAGES = 2;

    public static JTable jTableQuestionsPool = null;

    public static String newaiType = "";

    private static final long serialVersionUID = 1L;

    public static DefaultTableModel tableModel = null;

    private GraphicsConfiguration gc = null;

    private int filecount = 0;

    private TableColumn tc = null;

    private TableColumnModel tcm = null;

    public void init() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                Rectangle screenRect = ge.getMaximumWindowBounds();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                gc = gd.getDefaultConfiguration();
                aqurateObjectFactory = new AqurateObjectFactory();
                setBounds(screenRect);
                if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                    displaySplash(screenRect);
                } else {
                    Thread t = new initAPI(aqurateObjectFactory, owner);
                    t.start();
                    int iDelay = 200;
                    while (t.isAlive()) {
                        try {
                            if (iDelay > 100) {
                                iDelay -= 1;
                            }
                            Thread.sleep(iDelay);
                            initAPI initapi = (initAPI) t;
                            String s = initapi.getProgress();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                documentList = new ArrayList<AssessmentItemDoc>();
                controllerList = new ArrayList<DefaultController>();
                viewPanelList = new ArrayList<AbstractViewPanel>();
                xmlPanelList = new ArrayList<XMLViewPanel>();
                currentViewIndex = 0;
                setJMenuBar(getJJMenuBar());
                setContentPane(getJContentPane());
                jTabbedPane = new JTabbedPane();
                JPanel emptyPanel = new JPanel();
                jScrollEditorView = new JScrollPane(emptyPanel);
                jTabbedPane.addTab("Editor View", jScrollEditorView);
                jScrollXmlView = new JScrollPane(emptyPanel);
                jTabbedPane.addTab("XML View", jScrollXmlView);
                jTabbedPane.setOpaque(true);
                getContentPane().add(jTabbedPane, BorderLayout.CENTER);
                disableMenuOptions();
                setVisible(true);
            }
        });
    }

    void displaySplash(Rectangle screenRect) {
        Splash window = null;
        try {
            BufferedImage[] image = new BufferedImage[iIMAGES];
            for (int i = 0; i < (iIMAGES * 0.5); i++) {
                image[i] = ImageIO.read(new File("images/a" + i + ".jpg"));
            }
            int count = 0;
            int half = (int) (iIMAGES * 0.5);
            for (int i = half; i > 0; i--) {
                if (super.getClass().getSimpleName().equals("owner")) image[half + count] = ImageIO.read(new File("images/a" + (i - 1) + ".jpg"));
                count++;
            }
            window = new Splash(image, screenRect, gc);
            window.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int iCount = 0;
        Thread t = new initAPI(aqurateObjectFactory, owner);
        t.start();
        int iDelay = 200;
        java.awt.Graphics g = null;
        BufferStrategy myStrategy;
        window.createBufferStrategy(2);
        myStrategy = window.getBufferStrategy();
        while (t.isAlive()) {
            try {
                if (iDelay > 100) {
                    iDelay -= 1;
                }
                Thread.sleep(iDelay);
                g = myStrategy.getDrawGraphics();
                initAPI initapi = (initAPI) t;
                String s = initapi.getProgress();
                window.setProgress(s);
                window.setIndex(iCount++);
                if (iCount == (iIMAGES * 0.5)) iCount = 0;
                window.update(g);
                myStrategy.show();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (window != null) window.dispose();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set look and feel, using java default");
        }
        AqurateFramework framework = new AqurateFramework();
        framework.init();
    }

    public static void setNewQuestion(String ptype) {
        newaiType = ptype;
    }

    public static void updateQuestionTitle(String newTitle) {
        int currentQuestion = jTableQuestionsPool.getSelectedRow();
        tableModel.setValueAt(newTitle, currentQuestion, 0);
    }

    protected Hashtable<Object, String> actions = null;

    private AqurateObjectFactory aqurateObjectFactory;

    protected List<DefaultController> controllerList = null;

    protected List<AssessmentItemDoc> documentList = null;

    protected int iFocus;

    protected JComboBox jComboBoxStyle;

    private JPanel jContentPane = null;

    protected JEditorPane jEditorPaneItemBody = null;

    protected JEditorPane jEditorPanePrompt = null;

    private JFileChooser jFileChooser = null;

    private JMenuBar jJMenuBar = null;

    protected JMenu jMenu = null;

    private JMenu jMenuFile = null;

    private JMenu jMenuHelp = null;

    private JMenu jMenuPreview = null;

    private JMenu jMenuImport = null;

    private JMenu jMenuExport = null;

    private JMenuItem jMenuItemNew = null;

    private JMenuItem jMenuItemNewQuiz = null;

    private JMenuItem jMenuItemOpen = null;

    private JMenuItem jMenuItemSave = null;

    private JMenuItem jMenuItemSavePool = null;

    private JMenuItem jMenuItemClose = null;

    private JMenuItem jMenuItemImport = null;

    private JMenuItem jMenuItemExport = null;

    private JMenuItem jMenuItemDownload = null;

    private JMenuItem jMenuItemUpload = null;

    private JMenuItem jMenuItemExit = null;

    private JMenuItem jMenuItemPreview = null;

    protected JMenuItem jMenuItemUserGuide = null;

    protected JMenuItem jMenuItemContactAqurate = null;

    protected JMenuItem jMenuItemLicense = null;

    protected JMenuItem jMenuItemImage = null;

    protected JMenuItem jMenuItemCode = null;

    private JMenuItem jMenuItemPUSaveAs = null;

    private JMenuItem jMenuItemPUClose = null;

    private JMenuItem jMenuItemPUUp = null;

    private JMenuItem jMenuItemPUDown = null;

    protected JMenuItem jMenuItemSpan = null;

    protected JScrollPane jScrollEditorView = null;

    private JScrollPane jScrollPaneQuestionsPool = null;

    protected JScrollPane jScrollXmlView = null;

    protected JTabbedPane jTabbedPane = null;

    private AqurateFramework owner;

    private String sTicket;

    protected List<AbstractViewPanel> viewPanelList = null;

    protected List<XMLViewPanel> xmlPanelList = null;

    /**
	 * This is the default constructor
	 */
    public AqurateFramework() {
        owner = this;
        sTicket = "";
    }

    public void createAssessmentItem(AssessmentItemDoc aid, DefaultController dc, AbstractViewPanel avp, XMLViewPanel xmlvp) {
        this.jTabbedPane.removeAll();
        jScrollEditorView = new JScrollPane(avp);
        this.jTabbedPane.addTab("Editor View", jScrollEditorView);
        jScrollXmlView = new JScrollPane(xmlvp);
        this.jTabbedPane.addTab("XML View", jScrollXmlView);
        this.jTabbedPane.setOpaque(true);
        this.getContentPane().add(jTabbedPane, BorderLayout.CENTER);
        documentList.add(aid);
        controllerList.add(dc);
        viewPanelList.add(avp);
        xmlPanelList.add(xmlvp);
        this.enableMenuOptions();
        newaiType = "";
    }

    private void createAssociateInteraction() {
        AssociateInteractionDoc asidoc = new AssociateInteractionDoc(aqurateObjectFactory);
        AssociateInteractionController asicontroller = new AssociateInteractionController();
        AssociateInteractionViewPanel asiview = new AssociateInteractionViewPanel(owner, asicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, asicontroller);
        asicontroller.addModel(asidoc);
        asicontroller.addView(asiview);
        asicontroller.addView(xmlViewPanel);
        asidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(asidoc, asicontroller, asiview, xmlViewPanel);
    }

    private void createChoiceInteraction() {
        ChoiceInteractionDoc cidoc = new ChoiceInteractionDoc(aqurateObjectFactory);
        ChoiceInteractionController cicontroller = new ChoiceInteractionController();
        ChoiceInteractionViewPanel ciview = new ChoiceInteractionViewPanel(owner, cicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, cicontroller);
        cicontroller.addModel(cidoc);
        cicontroller.addView(ciview);
        cicontroller.addView(xmlViewPanel);
        cidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(cidoc, cicontroller, ciview, xmlViewPanel);
    }

    public void createGraphicOrderInteraction() {
        GraphicOrderInteractionDoc goidoc = new GraphicOrderInteractionDoc(aqurateObjectFactory);
        GraphicOrderInteractionController goicontroller = new GraphicOrderInteractionController();
        GraphicOrderInteractionViewPanel goiview = new GraphicOrderInteractionViewPanel(owner, goicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, goicontroller);
        goicontroller.addModel(goidoc);
        goicontroller.addView(goiview);
        goicontroller.addView(xmlViewPanel);
        goidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(goidoc, goicontroller, goiview, xmlViewPanel);
    }

    private void createHotspotInteraction() {
        HotspotInteractionDoc hsidoc = new HotspotInteractionDoc(aqurateObjectFactory);
        HotspotInteractionController hsicontroller = new HotspotInteractionController();
        HotspotInteractionViewPanel hsiview = new HotspotInteractionViewPanel(owner, hsicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, hsicontroller);
        hsicontroller.addModel(hsidoc);
        hsicontroller.addView(hsiview);
        hsicontroller.addView(xmlViewPanel);
        hsidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(hsidoc, hsicontroller, hsiview, xmlViewPanel);
    }

    private void createInlineInteraction() {
        InlineChoiceInteractionDoc icidoc = new InlineChoiceInteractionDoc(aqurateObjectFactory);
        InlineChoiceInteractionController icicontroller = new InlineChoiceInteractionController();
        InlineChoiceInteractionViewPanel iciview = new InlineChoiceInteractionViewPanel(owner, icicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, icicontroller);
        icicontroller.addModel(icidoc);
        icicontroller.addView(iciview);
        icicontroller.addView(xmlViewPanel);
        icidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(icidoc, icicontroller, iciview, xmlViewPanel);
    }

    private void createOrderInteraction() {
        OrderInteractionDoc oidoc = new OrderInteractionDoc(aqurateObjectFactory);
        OrderInteractionController oicontroller = new OrderInteractionController();
        OrderInteractionViewPanel oiview = new OrderInteractionViewPanel(owner, oicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, oicontroller);
        oicontroller.addModel(oidoc);
        oicontroller.addView(oiview);
        oicontroller.addView(xmlViewPanel);
        oidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(oidoc, oicontroller, oiview, xmlViewPanel);
    }

    public void createSliderInteraction() {
        SliderInteractionDoc sidoc = new SliderInteractionDoc(aqurateObjectFactory);
        SliderInteractionController sicontroller = new SliderInteractionController();
        SliderInteractionViewPanel siview = new SliderInteractionViewPanel(owner, sicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, sicontroller);
        sicontroller.addModel(sidoc);
        sicontroller.addView(siview);
        sicontroller.addView(xmlViewPanel);
        sidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(sidoc, sicontroller, siview, xmlViewPanel);
    }

    private void createTextEntryInteraction() {
        TextEntryInteractionDoc teidoc = new TextEntryInteractionDoc(aqurateObjectFactory);
        TextEntryInteractionController teicontroller = new TextEntryInteractionController();
        TextEntryInteractionViewPanel teiview = new TextEntryInteractionViewPanel(owner, teicontroller);
        XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, teicontroller);
        teicontroller.addModel(teidoc);
        teicontroller.addView(teiview);
        teicontroller.addView(xmlViewPanel);
        teidoc.init();
        xmlViewPanel.initialize();
        this.createAssessmentItem(teidoc, teicontroller, teiview, xmlViewPanel);
    }

    public int export() {
        int ticket = 0;
        return ticket;
    }

    private void FocusAIFrame(int row) {
        jTabbedPane.removeAll();
        jScrollEditorView = new JScrollPane(viewPanelList.get(row));
        jTabbedPane.addTab("Editor View", jScrollEditorView);
        jScrollXmlView = new JScrollPane(xmlPanelList.get(row));
        jTabbedPane.addTab("XML View", jScrollXmlView);
        jTabbedPane.setOpaque(true);
        getContentPane().add(jTabbedPane, BorderLayout.CENTER);
        currentViewIndex = row;
    }

    public void focusGained(FocusEvent fe) {
    }

    public void focusLost(FocusEvent fe) {
    }

    public AqurateObjectFactory getAqurateObjectFactory() {
        return this.aqurateObjectFactory;
    }

    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getJScrollPanePool(), BorderLayout.WEST);
        }
        return jContentPane;
    }

    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getJMenuFile());
            jJMenuBar.add(getJMenuPreview());
            jJMenuBar.add(getJMenuHelp());
        }
        return jJMenuBar;
    }

    private JMenu getJMenuFile() {
        if (jMenuFile == null) {
            jMenuFile = new JMenu("File");
            jMenuFile.setMnemonic('F');
        }
        jMenuFile.add(getJMenuItemNew());
        jMenuFile.add(getJMenuItemNewQuiz());
        jMenuFile.add(getJMenuItemOpen());
        jMenuFile.add(getJMenuItemSave());
        jMenuFile.add(getJMenuItemSavePool());
        jMenuFile.add(getJMenuItemClose());
        jMenuFile.addSeparator();
        jMenuFile.add(getJMenuImport());
        jMenuFile.add(getJMenuExport());
        jMenuFile.addSeparator();
        jMenuFile.add(getJMenuItemExit());
        return jMenuFile;
    }

    private JMenuItem getJMenuItemNew() {
        if (jMenuItemNew == null) {
            jMenuItemNew = new JMenuItem("New Question");
            jMenuItemNew.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.Event.CTRL_MASK));
        }
        jMenuItemNew.setActionCommand("new");
        jMenuItemNew.addActionListener(this);
        return jMenuItemNew;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("new")) {
            JDialogNewQuestion dialog = new JDialogNewQuestion(owner);
            dialog.setVisible(true);
            if (newaiType != null) {
                if (newaiType.equals("choiceinteraction")) {
                    this.createChoiceInteraction();
                } else if (newaiType.equals("orderinteraction")) {
                    this.createOrderInteraction();
                } else if (newaiType.equals("associateinteraction")) {
                    this.createAssociateInteraction();
                } else if (newaiType.equals("inlineinteraction")) {
                    this.createInlineInteraction();
                } else if (newaiType.equals("textentryinteraction")) {
                    this.createTextEntryInteraction();
                } else if (newaiType.equals("hotspotinteraction")) {
                    this.createHotspotInteraction();
                } else if (newaiType.equals("graphicorderinteraction")) {
                    this.createGraphicOrderInteraction();
                } else if (newaiType.equals("sliderinteraction")) {
                    this.createSliderInteraction();
                }
            }
        }
    }

    private JMenuItem getJMenuItemNewQuiz() {
        if (jMenuItemNewQuiz == null) {
            jMenuItemNewQuiz = new JMenuItem("Generate Quiz");
            jMenuItemNewQuiz.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.Event.CTRL_MASK));
        }
        jMenuItemNewQuiz.setActionCommand("newQuiz");
        jMenuItemNewQuiz.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                JDialogNewQuiz dialogQuiz = new JDialogNewQuiz(owner, jTableQuestionsPool, documentList);
                dialogQuiz.setVisible(true);
            }
        });
        return jMenuItemNewQuiz;
    }

    private JMenu getJMenuImport() {
        if (jMenuImport == null) {
            jMenuImport = new JMenu("Import");
            jMenuImport.setMnemonic('I');
        }
        jMenuImport.add(this.getJMenuItemImport());
        jMenuImport.addSeparator();
        jMenuImport.add(this.getJMenuItemDownload());
        return jMenuImport;
    }

    private JMenuItem getJMenuItemImport() {
        if (jMenuItemImport == null) {
            jMenuItemImport = new JMenuItem("Import XML");
            jMenuItemImport.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.Event.CTRL_MASK));
        }
        jMenuItemImport.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                JFileChooser jFileChooserOpen = new JFileChooser();
                jFileChooserOpen.setFileFilter(new XMLFileFilter());
                int returnVal = jFileChooserOpen.showOpenDialog(owner);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = jFileChooserOpen.getSelectedFile();
                    String sPath = file.getParent();
                    Load(file, sPath, "");
                }
            }
        });
        return jMenuItemImport;
    }

    private JMenu getJMenuExport() {
        if (jMenuExport == null) {
            jMenuExport = new JMenu("Export");
            jMenuExport.setMnemonic('E');
        }
        jMenuExport.add(this.getJMenuItemExport());
        jMenuExport.addSeparator();
        jMenuExport.add(this.getJMenuItemUpload());
        return jMenuExport;
    }

    private JMenu getJMenuPreview() {
        if (jMenuPreview == null) {
            jMenuPreview = new JMenu("Preview");
        }
        jMenuPreview.add(getJMenuItemPreviewQuestion());
        return jMenuPreview;
    }

    private JMenuItem getJMenuItemPreviewQuestion() {
        if (jMenuItemPreview == null) {
            jMenuItemPreview = new JMenuItem("Preview Question");
            jMenuItemPreview.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.Event.CTRL_MASK));
        }
        jMenuItemPreview.setActionCommand("preview-question");
        jMenuItemPreview.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                AssessmentHandler.previewQuestion = true;
                ItemBodyHandler.previewQuestion = true;
                ItemBodyHandler.previewQuestionCB = true;
                ItemBodyHandler.previewQuestionResponse = true;
                String defaultFileName = "PACKAGEDirectory" + File.separator + "preview.zip";
                File fdir = new File(defaultFileName);
                File absDir = new File(fdir.getAbsolutePath());
                Document dXML = documentList.get(currentViewIndex).getDomXML();
                String sXML = documentList.get(currentViewIndex).getXML();
                String sticket = documentList.get(currentViewIndex).getTicket();
                String sauthor = documentList.get(currentViewIndex).getAuthor();
                String slang = documentList.get(currentViewIndex).getLanguage();
                String sdesc = documentList.get(currentViewIndex).getResourceDescription();
                String sitype = documentList.get(currentViewIndex).getInteractionType();
                String sftype = documentList.get(currentViewIndex).getFeedbackType();
                String stimedep = "" + documentList.get(currentViewIndex).isTimeDependent();
                ContentPackageBuilder cpBuilder = new ContentPackageBuilder(owner, dXML, sXML, sticket, absDir.getParent(), "preview", sauthor, slang, sdesc, sitype, sftype, stimedep);
                cpBuilder.buildPackage(1);
                String outfile = "PACKAGEDirectory" + File.separator + "htm" + File.separator + "preview.html";
                Previewer myPreviewer = new Previewer(absDir.getAbsolutePath(), outfile);
                myPreviewer.run();
            }
        });
        return jMenuItemPreview;
    }

    private JMenu getJMenuHelp() {
        if (jMenuHelp == null) {
            jMenuHelp = new JMenu("Help");
            jMenuHelp.setMnemonic('H');
        }
        jMenuHelp.add(this.getJMenuItemUserGuide());
        jMenuHelp.addSeparator();
        jMenuHelp.add(this.getJMenuItemContactAqurate());
        jMenuHelp.addSeparator();
        jMenuHelp.add(this.getJMenuItemLicense());
        return jMenuHelp;
    }

    private JMenuItem getJMenuItemUserGuide() {
        if (jMenuItemUserGuide == null) {
            jMenuItemUserGuide = new JMenuItem("User Guide");
        }
        return jMenuItemUserGuide;
    }

    private JMenuItem getJMenuItemContactAqurate() {
        if (jMenuItemContactAqurate == null) {
            jMenuItemContactAqurate = new JMenuItem("Contact Aqurate!");
        }
        return jMenuItemContactAqurate;
    }

    private JMenuItem getJMenuItemLicense() {
        if (jMenuItemLicense == null) {
            jMenuItemLicense = new JMenuItem("About Aqurate...");
        }
        return jMenuItemLicense;
    }

    private JMenuItem getJMenuItemClose() {
        if (jMenuItemClose == null) {
            jMenuItemClose = new JMenuItem("Close");
            jMenuItemClose.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.Event.CTRL_MASK));
        }
        jMenuItemClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int[] rowsSelected = jTableQuestionsPool.getSelectedRows();
                int numberSelected = rowsSelected.length;
                for (int r = 0; r < numberSelected; r++) {
                    jScrollEditorView.removeAll();
                    jScrollEditorView.updateUI();
                    jScrollXmlView.removeAll();
                    jScrollXmlView.updateUI();
                    documentList.remove(rowsSelected[r] - r);
                    controllerList.remove(rowsSelected[r] - r);
                    viewPanelList.remove(rowsSelected[r] - r);
                    xmlPanelList.remove(rowsSelected[r] - r);
                    tableModel.removeRow(rowsSelected[r] - r);
                }
                int nextSelected = rowsSelected[0];
                if ((tableModel.getRowCount() != 0) && (nextSelected < tableModel.getRowCount())) {
                    jTableQuestionsPool.setRowSelectionInterval(nextSelected, nextSelected);
                    FocusAIFrame(nextSelected);
                } else if (nextSelected > 0) {
                    jTableQuestionsPool.setRowSelectionInterval(nextSelected - 1, nextSelected - 1);
                    FocusAIFrame(nextSelected - 1);
                }
                if (documentList.isEmpty()) disableMenuOptions();
            }
        });
        return jMenuItemClose;
    }

    private JMenuItem getJMenuItemExit() {
        if (jMenuItemExit == null) {
            jMenuItemExit = new JMenuItem("Exit");
        }
        jMenuItemExit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                System.exit(0);
            }
        });
        return jMenuItemExit;
    }

    private JMenuItem getJMenuItemExport() {
        if (jMenuItemExport == null) {
            jMenuItemExport = new JMenuItem("Export to XML");
            jMenuItemExport.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.Event.CTRL_MASK));
        }
        jMenuItemExport.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    JFileChooser jFileChooserExport = new JFileChooser();
                    int returnVal = jFileChooserExport.showSaveDialog(owner);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = jFileChooserExport.getSelectedFile();
                        String oldPath = file.getAbsolutePath();
                        int indexLastSlash = oldPath.lastIndexOf(File.separator);
                        String fileName = oldPath.substring(indexLastSlash, oldPath.length());
                        File fdir = new File(oldPath);
                        fdir.mkdir();
                        try {
                            Document doc = documentList.get(currentViewIndex).getDomXML();
                            String sXml = controllerList.get(currentViewIndex).getXML();
                            String newPath = fdir.getAbsolutePath() + File.separator;
                            sXml = copyImages(doc, sXml, newPath, "img", "src");
                            sXml = copyImages(doc, sXml, newPath, "object", "data");
                            FileWriter fw = new FileWriter(fdir.getAbsolutePath() + File.separator + fileName + ".xml");
                            fw.write(sXml);
                            fw.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });
        return jMenuItemExport;
    }

    public String copyImages(Document doc, String sXML, String newPath, String tagName, String itemName) {
        NodeList nl = null;
        Node n = null;
        NamedNodeMap nnp = null;
        Node nsrc = null;
        URL url = null;
        String sFilename = "";
        String sNewPath = "";
        int index;
        String sOldPath = "";
        try {
            nl = doc.getElementsByTagName(tagName);
            for (int i = 0; i < nl.getLength(); i++) {
                n = nl.item(i);
                nnp = n.getAttributes();
                nsrc = nnp.getNamedItem(itemName);
                String sTemp = nsrc.getTextContent();
                url = new URL("file", "localhost", sTemp);
                sOldPath = url.getPath();
                sOldPath = sOldPath.replace('/', File.separatorChar);
                int indexFirstSlash = sOldPath.indexOf(File.separatorChar);
                String sSourcePath;
                if (itemName.equals("data")) sSourcePath = sOldPath; else sSourcePath = sOldPath.substring(indexFirstSlash + 1);
                index = sOldPath.lastIndexOf(File.separatorChar);
                sFilename = sOldPath.substring(index + 1);
                sNewPath = newPath + sFilename;
                FileChannel in = null;
                FileChannel out = null;
                try {
                    in = new FileInputStream(sSourcePath).getChannel();
                    out = new FileOutputStream(sNewPath).getChannel();
                    long size = in.size();
                    MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                    out.write(buf);
                } finally {
                    if (in != null) in.close();
                    if (out != null) out.close();
                }
                sXML = sXML.replace(nsrc.getTextContent(), sFilename);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sXML;
    }

    private JMenuItem getJMenuItemDownload() {
        if (jMenuItemDownload == null) {
            jMenuItemDownload = new JMenuItem("Download Content Package");
            jMenuItemDownload.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.Event.CTRL_MASK));
        }
        jMenuItemDownload.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                DownloadViewPanel downloadPanel = new DownloadViewPanel(owner);
            }
        });
        return jMenuItemDownload;
    }

    private JMenuItem getJMenuItemUpload() {
        if (jMenuItemUpload == null) {
            jMenuItemUpload = new JMenuItem("Upload Content Package");
            jMenuItemUpload.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.Event.CTRL_MASK));
        }
        jMenuItemUpload.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Document dXML = documentList.get(currentViewIndex).getDomXML();
                String sXML = documentList.get(currentViewIndex).getXML();
                String sOldPath = "";
                String sticket = documentList.get(currentViewIndex).getTicket();
                String sauthor = documentList.get(currentViewIndex).getAuthor();
                String slang = documentList.get(currentViewIndex).getLanguage();
                String sdesc = documentList.get(currentViewIndex).getResourceDescription();
                String stitle = documentList.get(currentViewIndex).getTitle();
                String sitype = documentList.get(currentViewIndex).getInteractionType();
                String sftype = documentList.get(currentViewIndex).getFeedbackType();
                String stimedep = "" + documentList.get(currentViewIndex).isTimeDependent();
                ContentPackageBuilder cpBuilder = new ContentPackageBuilder(owner, dXML, sXML, sticket, sOldPath, stitle, sauthor, slang, sdesc, sitype, sftype, stimedep);
                UploadViewPanel uploadPanel = new UploadViewPanel(owner, cpBuilder);
                documentList.get(currentViewIndex).setTicket(owner.getTicket());
            }
        });
        return jMenuItemUpload;
    }

    public static boolean unpackZip(File zipFile, File targetFolder) throws IOException {
        targetFolder.mkdirs();
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        ZipInputStream zIn = null;
        ZipEntry zipEntry;
        int bytesRead;
        final int bufSize = 512;
        byte buf[] = new byte[bufSize];
        in = new BufferedInputStream(new FileInputStream(zipFile), bufSize);
        zIn = new ZipInputStream(in);
        try {
            while ((zipEntry = zIn.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    File outFile = new File(targetFolder, zipEntry.getName());
                    if (!outFile.getParentFile().exists()) {
                        outFile.getParentFile().mkdirs();
                    }
                    out = new BufferedOutputStream(new FileOutputStream(outFile), bufSize);
                    int sleep_count = 0;
                    while ((bytesRead = zIn.read(buf)) != -1) {
                        out.write(buf, 0, bytesRead);
                        if (sleep_count >= 40) {
                            try {
                                Thread.sleep(2);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            sleep_count = 0;
                        }
                        sleep_count++;
                    }
                    outFile.setLastModified(zipEntry.getTime());
                    out.flush();
                    out.close();
                }
                zIn.closeEntry();
            }
            zIn.close();
        } catch (IOException ex) {
            zIn.close();
            if (out != null) {
                out.flush();
                out.close();
            }
            throw ex;
        }
        return true;
    }

    private JMenuItem getJMenuItemOpen() {
        if (jMenuItemOpen == null) {
            jMenuItemOpen = new JMenuItem("Open");
            jMenuItemOpen.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Event.CTRL_MASK));
        }
        jMenuItemOpen.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                JFileChooser jFileChooserOpen = new JFileChooser();
                int returnVal = jFileChooserOpen.showOpenDialog(owner);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = jFileChooserOpen.getSelectedFile();
                    if (file.getName().contains(".xml")) {
                        String sPath = file.getParent();
                        Load(file, sPath, "");
                    } else {
                        try {
                            FileInputStream fis = new FileInputStream(file.getPath());
                            ZipInputStream zf = new ZipInputStream(new BufferedInputStream(fis));
                            File newtemp = new File("temp");
                            newtemp.mkdir();
                            boolean containsQTIFile = false;
                            final int BUFFER = 2048;
                            BufferedOutputStream dest = null;
                            File tempdir = new File("temp\\" + file.getName());
                            tempdir.mkdir();
                            System.out.println("dir::::: " + tempdir.getAbsolutePath());
                            ZipEntry entry;
                            while ((entry = zf.getNextEntry()) != null) {
                                System.out.println("Extracting: " + entry);
                                if (entry.isDirectory()) {
                                    File newdir = new File(tempdir.getAbsolutePath() + "/" + entry.getName());
                                    System.out.print("Creating directory " + newdir + "..");
                                    newdir.mkdir();
                                    System.out.println("Done!");
                                } else {
                                    int count;
                                    byte data[] = new byte[BUFFER];
                                    FileOutputStream fos = new FileOutputStream(tempdir.getAbsolutePath() + "/" + entry.getName());
                                    dest = new BufferedOutputStream(fos, BUFFER);
                                    while ((count = zf.read(data, 0, BUFFER)) != -1) {
                                        dest.write(data, 0, count);
                                    }
                                    dest.flush();
                                    dest.close();
                                    String newfile = tempdir.getAbsolutePath() + "/" + entry.getName();
                                    if (newfile.contains(".xml") && !newfile.contains("imsmanifest.xml")) {
                                        File QTIItem = new File(tempdir.getAbsolutePath() + "/" + entry.getName());
                                        Load(QTIItem, tempdir.getAbsolutePath(), "");
                                        containsQTIFile = true;
                                    }
                                }
                            }
                            zf.close();
                            if (!containsQTIFile) {
                                JOptionPane.showMessageDialog(null, "The zip file you are trying to open contains no valid QTI file!", "File input error", JOptionPane.ERROR_MESSAGE);
                                System.err.println("Zip file does not contain QTI files");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Not a valid IMS content package file!");
                            JOptionPane.showMessageDialog(null, "The file you are trying to open is not a valid IMS content package file!", "File format error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
        return jMenuItemOpen;
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    private JMenuItem getJMenuItemSave() {
        if (jMenuItemSave == null) {
            jMenuItemSave = new JMenuItem("Save Question");
            jMenuItemSave.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK));
        }
        jMenuItemSave.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                String patternStr = "\\W";
                String replacementStr = "";
                String filename = "";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(documentList.get(currentViewIndex).getTitle());
                filename = matcher.replaceAll(replacementStr);
                if (filename.equals("")) filename = "qtidoc" + filecount;
                jFileChooser = new JFileChooser();
                jFileChooser.setSelectedFile(new File(filename + ".zip"));
                try {
                    int returnVal = jFileChooser.showSaveDialog(owner);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File fdirectory = jFileChooser.getCurrentDirectory();
                        String oldPath = fdirectory.getAbsolutePath() + File.separatorChar;
                        String defaultFileName = jFileChooser.getName(jFileChooser.getSelectedFile());
                        if (!defaultFileName.contains(".")) {
                            defaultFileName = defaultFileName + ".zip";
                        }
                        int posDot = defaultFileName.lastIndexOf(".");
                        String fileName = defaultFileName.substring(0, posDot);
                        Document dXML = documentList.get(currentViewIndex).getDomXML();
                        String sXML = documentList.get(currentViewIndex).getXML();
                        String sticket = documentList.get(currentViewIndex).getTicket();
                        String sauthor = documentList.get(currentViewIndex).getAuthor();
                        String slang = documentList.get(currentViewIndex).getLanguage();
                        String sdesc = documentList.get(currentViewIndex).getResourceDescription();
                        String sitype = documentList.get(currentViewIndex).getInteractionType();
                        String sftype = documentList.get(currentViewIndex).getFeedbackType();
                        String stimedep = "" + documentList.get(currentViewIndex).isTimeDependent();
                        ContentPackageBuilder cpBuilder = new ContentPackageBuilder(owner, dXML, sXML, sticket, oldPath, fileName, sauthor, slang, sdesc, sitype, sftype, stimedep);
                        cpBuilder.buildPackage(1);
                        filecount++;
                    } else {
                    }
                } catch (RuntimeException re) {
                    re.printStackTrace();
                }
            }
        });
        return jMenuItemSave;
    }

    private JMenuItem getJMenuItemSavePool() {
        if (jMenuItemSavePool == null) {
            jMenuItemSavePool = new JMenuItem("Save Pool");
            jMenuItemSavePool.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.Event.CTRL_MASK));
        }
        jMenuItemSavePool.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                String patternStr = "\\W";
                String replacementStr = "";
                String filename = "";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(documentList.get(currentViewIndex).getTitle());
                filename = matcher.replaceAll(replacementStr);
                if (filename.equals("")) filename = "qtidoc" + filecount;
                jFileChooser = new JFileChooser();
                jFileChooser.setSelectedFile(new File(filename + ".zip"));
                try {
                    int returnVal = jFileChooser.showSaveDialog(owner);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File fdirectory = jFileChooser.getCurrentDirectory();
                        String oldPath = fdirectory.getAbsolutePath() + File.separatorChar;
                        String defaultFileName = jFileChooser.getName(jFileChooser.getSelectedFile());
                        if (!defaultFileName.contains(".")) {
                            defaultFileName = defaultFileName + ".zip";
                        }
                        int posDot = defaultFileName.lastIndexOf(".");
                        String fileName = defaultFileName.substring(0, posDot);
                        int numberOfQuestions = jTableQuestionsPool.getRowCount();
                        int[] selectedQuestions = new int[numberOfQuestions];
                        int row = 0;
                        for (int rows = 0; rows < numberOfQuestions; rows++) {
                            selectedQuestions[row] = rows;
                            row++;
                        }
                        String[] Questions = new String[row];
                        for (int j = 0; j < row; j++) {
                            Questions[j] = "Question_" + j;
                        }
                        ContentPackageBuilder cpBuilder = new ContentPackageBuilder(owner, documentList, fdirectory.getPath(), fileName, selectedQuestions, Questions, row);
                        cpBuilder.buildPackage(row);
                    } else {
                    }
                } catch (RuntimeException re) {
                    re.printStackTrace();
                }
            }
        });
        return jMenuItemSavePool;
    }

    public JPopupMenu getJPopupMenu() {
        JPopupMenu jPopupMenu = null;
        if (jPopupMenu == null) {
            jPopupMenu = new JPopupMenu();
        }
        jMenuItemPUClose = new JMenuItem("Close");
        jMenuItemPUUp = new JMenuItem("Up");
        jMenuItemPUDown = new JMenuItem("Down");
        jMenuItemPUUp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                int rowSelected = jTableQuestionsPool.getSelectedRow();
                jScrollEditorView.removeAll();
                jScrollEditorView.updateUI();
                jScrollXmlView.removeAll();
                jScrollXmlView.updateUI();
                AssessmentItemDoc TempDocList = documentList.get(rowSelected - 1);
                DefaultController TempController = controllerList.get(rowSelected - 1);
                AbstractViewPanel TempviewPanelList = viewPanelList.get(rowSelected - 1);
                XMLViewPanel TempxmlPanelList = xmlPanelList.get(rowSelected - 1);
                documentList.set(rowSelected - 1, documentList.get(rowSelected));
                controllerList.set(rowSelected - 1, controllerList.get(rowSelected));
                viewPanelList.set(rowSelected - 1, viewPanelList.get(rowSelected));
                xmlPanelList.set(rowSelected - 1, xmlPanelList.get(rowSelected));
                documentList.set(rowSelected, TempDocList);
                controllerList.set(rowSelected, TempController);
                viewPanelList.set(rowSelected, TempviewPanelList);
                xmlPanelList.set(rowSelected, TempxmlPanelList);
                tableModel.moveRow(rowSelected, rowSelected, rowSelected - 1);
                FocusAIFrame(rowSelected - 1);
                ListSelectionModel a = jTableQuestionsPool.getSelectionModel();
                a.setSelectionInterval(rowSelected - 1, rowSelected - 1);
            }
        });
        jMenuItemPUDown.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                int rowSelected = jTableQuestionsPool.getSelectedRow();
                jScrollEditorView.removeAll();
                jScrollEditorView.updateUI();
                jScrollXmlView.removeAll();
                jScrollXmlView.updateUI();
                AssessmentItemDoc TempDocList = documentList.get(rowSelected + 1);
                DefaultController TempController = controllerList.get(rowSelected + 1);
                AbstractViewPanel TempviewPanelList = viewPanelList.get(rowSelected + 1);
                XMLViewPanel TempxmlPanelList = xmlPanelList.get(rowSelected + 1);
                documentList.set(rowSelected + 1, documentList.get(rowSelected));
                controllerList.set(rowSelected + 1, controllerList.get(rowSelected));
                viewPanelList.set(rowSelected + 1, viewPanelList.get(rowSelected));
                xmlPanelList.set(rowSelected + 1, xmlPanelList.get(rowSelected));
                documentList.set(rowSelected, TempDocList);
                controllerList.set(rowSelected, TempController);
                viewPanelList.set(rowSelected, TempviewPanelList);
                xmlPanelList.set(rowSelected, TempxmlPanelList);
                tableModel.moveRow(rowSelected, rowSelected, rowSelected + 1);
                FocusAIFrame(rowSelected + 1);
                ListSelectionModel a = jTableQuestionsPool.getSelectionModel();
                a.setSelectionInterval(rowSelected + 1, rowSelected + 1);
            }
        });
        jMenuItemPUClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                int[] rowsSelected = jTableQuestionsPool.getSelectedRows();
                int numberSelected = rowsSelected.length;
                for (int r = 0; r < numberSelected; r++) {
                    jScrollEditorView.removeAll();
                    jScrollEditorView.updateUI();
                    jScrollXmlView.removeAll();
                    jScrollXmlView.updateUI();
                    documentList.remove(rowsSelected[r] - r);
                    controllerList.remove(rowsSelected[r] - r);
                    viewPanelList.remove(rowsSelected[r] - r);
                    xmlPanelList.remove(rowsSelected[r] - r);
                    tableModel.removeRow(rowsSelected[r] - r);
                }
                int nextSelected = rowsSelected[0];
                if ((tableModel.getRowCount() != 0) && (nextSelected < tableModel.getRowCount())) {
                    jTableQuestionsPool.setRowSelectionInterval(nextSelected, nextSelected);
                    FocusAIFrame(nextSelected);
                } else {
                    if (nextSelected != 0) {
                        jTableQuestionsPool.setRowSelectionInterval(nextSelected - 1, nextSelected - 1);
                        FocusAIFrame(nextSelected - 1);
                    }
                }
                if (documentList.isEmpty()) disableMenuOptions();
            }
        });
        jPopupMenu.add(jMenuItemPUUp);
        jPopupMenu.add(jMenuItemPUDown);
        jPopupMenu.add(jMenuItemPUClose);
        jPopupMenu.setVisible(true);
        return jPopupMenu;
    }

    private JScrollPane getJScrollPanePool() {
        if (jScrollPaneQuestionsPool == null) {
            jScrollPaneQuestionsPool = new JScrollPane();
            jScrollPaneQuestionsPool.setViewportView(getJTableQuestionsPool());
        }
        jScrollPaneQuestionsPool.setPreferredSize(new Dimension(170, 700));
        return jScrollPaneQuestionsPool;
    }

    private JTable getJTableQuestionsPool() {
        if (jTableQuestionsPool == null) {
            jTableQuestionsPool = new JTable(tableModel);
            jTableQuestionsPool.setRowHeight(50);
        }
        String[] columns = { "Question Pool", "X" };
        tableModel = new DefaultTableModel(columns, 0) {

            private static final long serialVersionUID = 1L;

            public Class getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                } else {
                    return super.getColumnClass(columnIndex);
                }
            }

            ;
        };
        DefaultListSelectionModel mylsmodel = new DefaultListSelectionModel();
        mylsmodel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTableQuestionsPool.setSelectionModel(mylsmodel);
        jTableQuestionsPool.setModel(tableModel);
        jTableQuestionsPool.getColumn("X").setMaxWidth(26);
        tcm = jTableQuestionsPool.getColumnModel();
        tc = tcm.getColumn(1);
        tcm.removeColumn(tc);
        jTableQuestionsPool.addMouseListener(this);
        return jTableQuestionsPool;
    }

    public String getTicket() {
        return sTicket;
    }

    void Load(File file, String sPath, String sTicket) {
        AqurateAssessmentItem ai = null;
        try {
            ai = aqurateObjectFactory.Load(file);
        } catch (javax.xml.bind.UnmarshalException ue) {
            JOptionPane.showMessageDialog(null, "Your input file is not valid QTI v. 2.1", "Invalid content", JOptionPane.ERROR_MESSAGE);
        }
        String sAIType = "";
        int iNInteractions = ai.getNInteractions();
        for (int i = 0; i < iNInteractions; i++) {
            try {
                sAIType = ai.getAssessmentItemQ(i);
            } catch (AqurateException e) {
                e.printStackTrace();
            }
            if (sAIType.equals("ChoiceInteraction")) {
                ChoiceInteractionDoc cidoc = new ChoiceInteractionDoc(aqurateObjectFactory);
                ChoiceInteractionController cicontroller = new ChoiceInteractionController();
                ChoiceInteractionViewPanel ciview = new ChoiceInteractionViewPanel(owner, cicontroller);
                XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, cicontroller);
                cicontroller.addModel(cidoc);
                cicontroller.addView(ciview);
                cicontroller.addView(xmlViewPanel);
                cidoc.load(i, ai, sPath);
                xmlViewPanel.initialize();
                cidoc.setTicket(sTicket);
                createAssessmentItem(cidoc, cicontroller, ciview, xmlViewPanel);
            } else if (sAIType.equals("OrderInteraction")) {
                OrderInteractionDoc oidoc = new OrderInteractionDoc(aqurateObjectFactory);
                OrderInteractionController oicontroller = new OrderInteractionController();
                OrderInteractionViewPanel oiview = new OrderInteractionViewPanel(owner, oicontroller);
                XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, oicontroller);
                oicontroller.addModel(oidoc);
                oicontroller.addView(oiview);
                oicontroller.addView(xmlViewPanel);
                oidoc.load(i, ai, sPath);
                xmlViewPanel.initialize();
                oidoc.setTicket(sTicket);
                createAssessmentItem(oidoc, oicontroller, oiview, xmlViewPanel);
            } else if (sAIType.equals("AssociateInteraction")) {
                AssociateInteractionDoc asidoc = new AssociateInteractionDoc(aqurateObjectFactory);
                AssociateInteractionController asicontroller = new AssociateInteractionController();
                AssociateInteractionViewPanel asiview = new AssociateInteractionViewPanel(owner, asicontroller);
                XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, asicontroller);
                asicontroller.addModel(asidoc);
                asicontroller.addView(asiview);
                asicontroller.addView(xmlViewPanel);
                asidoc.load(i, ai, sPath);
                xmlViewPanel.initialize();
                asidoc.setTicket(sTicket);
                createAssessmentItem(asidoc, asicontroller, asiview, xmlViewPanel);
            } else if (sAIType.equals("HotspotInteraction")) {
                HotspotInteractionDoc hsidoc = new HotspotInteractionDoc(aqurateObjectFactory);
                HotspotInteractionController hsicontroller = new HotspotInteractionController();
                HotspotInteractionViewPanel hsiview = new HotspotInteractionViewPanel(owner, hsicontroller);
                XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, hsicontroller);
                hsicontroller.addModel(hsidoc);
                hsicontroller.addView(hsiview);
                hsicontroller.addView(xmlViewPanel);
                hsidoc.load(i, ai, sPath);
                xmlViewPanel.initialize();
                hsidoc.setTicket(sTicket);
                createAssessmentItem(hsidoc, hsicontroller, hsiview, xmlViewPanel);
            } else if (sAIType.equals("InlineChoiceInteraction")) {
                InlineChoiceInteractionDoc ilcdoc = new InlineChoiceInteractionDoc(aqurateObjectFactory);
                InlineChoiceInteractionController ilccontroller = new InlineChoiceInteractionController();
                InlineChoiceInteractionViewPanel ilcview = new InlineChoiceInteractionViewPanel(owner, ilccontroller);
                XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, ilccontroller);
                ilccontroller.addModel(ilcdoc);
                ilccontroller.addView(ilcview);
                ilccontroller.addView(xmlViewPanel);
                ilcdoc.load(i, ai, sPath);
                xmlViewPanel.initialize();
                ilcdoc.setTicket(sTicket);
                createAssessmentItem(ilcdoc, ilccontroller, ilcview, xmlViewPanel);
            } else if (sAIType.equals("TextEntryInteraction")) {
            } else if (sAIType.equals("GraphicOrderInteraction")) {
                GraphicOrderInteractionDoc goidoc = new GraphicOrderInteractionDoc(aqurateObjectFactory);
                GraphicOrderInteractionController goicontroller = new GraphicOrderInteractionController();
                GraphicOrderInteractionViewPanel goiview = new GraphicOrderInteractionViewPanel(owner, goicontroller);
                XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, goicontroller);
                goicontroller.addModel(goidoc);
                goicontroller.addView(goiview);
                goicontroller.addView(xmlViewPanel);
                goidoc.load(i, ai, sPath);
                xmlViewPanel.initialize();
                goidoc.setTicket(sTicket);
                createAssessmentItem(goidoc, goicontroller, goiview, xmlViewPanel);
            } else if (sAIType.equals("SliderInteraction")) {
                SliderInteractionDoc sidoc = new SliderInteractionDoc(aqurateObjectFactory);
                SliderInteractionController sicontroller = new SliderInteractionController();
                SliderInteractionViewPanel siview = new SliderInteractionViewPanel(owner, sicontroller);
                XMLViewPanel xmlViewPanel = new XMLViewPanel(owner, sicontroller);
                sicontroller.addModel(sidoc);
                sicontroller.addView(siview);
                sicontroller.addView(xmlViewPanel);
                sidoc.load(i, ai, sPath);
                xmlViewPanel.initialize();
                sidoc.setTicket(sTicket);
                createAssessmentItem(sidoc, sicontroller, siview, xmlViewPanel);
            } else {
                System.err.println("Loading error: Question type not recognized!");
            }
        }
    }

    public void mouseClicked(MouseEvent me) {
        int srow = jTableQuestionsPool.getSelectedRow();
        if (srow > -1) {
            FocusAIFrame(srow);
        }
        if (SwingUtilities.isRightMouseButton(me)) {
            Point eventPoint = me.getPoint();
            int selectedRow = jTableQuestionsPool.rowAtPoint(eventPoint);
            jTableQuestionsPool.setRowSelectionInterval(selectedRow, selectedRow);
            FocusAIFrame(selectedRow);
            this.getJPopupMenu().show(me.getComponent(), me.getX(), me.getY());
            if (selectedRow == 0) {
                this.jMenuItemPUUp.setEnabled(false);
            } else {
                this.jMenuItemPUUp.setEnabled(true);
            }
            if (selectedRow == jTableQuestionsPool.getRowCount() - 1) {
                this.jMenuItemPUDown.setEnabled(false);
            } else {
                this.jMenuItemPUDown.setEnabled(true);
            }
        }
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mousePressed(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void setTicket(String sTicket) {
        this.sTicket = sTicket;
    }

    public void disableMenuOptions() {
        this.jMenuItemClose.setEnabled(false);
        this.jMenuItemSave.setEnabled(false);
        this.jMenuItemExport.setEnabled(false);
        this.jMenuItemUpload.setEnabled(false);
        this.jMenuItemPreview.setEnabled(false);
        this.jMenuItemNewQuiz.setEnabled(false);
        this.jMenuItemSavePool.setEnabled(false);
        if (jTableQuestionsPool.getRowCount() < 2) {
            this.jMenuItemNewQuiz.setEnabled(false);
            this.jMenuItemSavePool.setEnabled(false);
        }
    }

    public void enableMenuOptions() {
        this.jMenuItemClose.setEnabled(true);
        this.jMenuItemSave.setEnabled(true);
        this.jMenuItemExport.setEnabled(true);
        this.jMenuItemUpload.setEnabled(true);
        this.jMenuItemPreview.setEnabled(true);
        if (jTableQuestionsPool.getRowCount() >= 2) {
            this.jMenuItemNewQuiz.setEnabled(true);
            this.jMenuItemSavePool.setEnabled(true);
        }
    }

    public class FilterBuilder extends FileFilter {

        String extension = "";

        public FilterBuilder(String ext) {
            extension = ext;
        }

        public boolean accept(File file) {
            String filename = file.getName();
            return filename.endsWith(extension);
        }

        public String getDescription() {
            return extension;
        }
    }
}
