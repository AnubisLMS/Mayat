package org.processmining.analysis.redesign.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import org.deckfour.gantzgraf.canvas.event.GGToggleGraphSelectionModel;
import org.deckfour.gantzgraf.model.GGEdge;
import org.deckfour.gantzgraf.model.GGNode;
import org.deckfour.gantzgraf.ui.GGGraphView;
import org.deckfour.slickerbox.components.GradientPanel;
import org.deckfour.slickerbox.components.HeaderBar;
import org.deckfour.slickerbox.components.RoundedPanel;
import org.deckfour.slickerbox.components.SlickerButton;
import org.deckfour.slickerbox.ui.SlickerComboBoxUI;
import org.processmining.analysis.edithlprocess.EditHighLevelProcessGui;
import org.processmining.analysis.petrinet.cpnexport.ColoredPetriNet;
import org.processmining.analysis.petrinet.cpnexport.CpnExport20;
import org.processmining.analysis.petrinet.cpnexport.CpnExportSettings;
import org.processmining.analysis.petrinet.cpnexport.CpnUtils;
import org.processmining.analysis.petrinet.cpnexport.ManagerConfiguration;
import org.processmining.analysis.redesign.*;
import org.processmining.analysis.redesign.ui.petri.*;
import org.processmining.framework.models.hlprocess.hlmodel.HLPetriNet;
import org.processmining.framework.models.hlprocess.pattern.Component;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.framework.plugin.Provider;
import org.processmining.framework.ui.ComponentFrame;
import org.processmining.framework.ui.MainUI;
import org.processmining.framework.ui.Message;

/**
 * The UI that provides the possibility to create redesigns for a given
 * high-level Petri net. The redesign is done step by step by transforming a
 * certain process part with a certain redesign type. In this way, gradually a
 * tree of redesign alternatives is created. The performance of the original and
 * the alternative models can be evaluated with simulation.
 * 
 * @see HLPetriNet
 * 
 * @author Mariska Netjes
 */
public class RedesignAnalysisUI extends JPanel implements Provider {

    private static final long serialVersionUID = 1L;

    /**
	 * Specifies the redesign type. The redesign type determines the specific
	 * transformation rule, e.g., parallel.
	 * 
	 * @return enumeration of the redesign types
	 */
    public enum RedesignType {

        Parallel, Sequence, Group, AddTask, RemoveTask, AddConstraint, RemoveConstraint, CopyModel
    }

    ;

    public enum kpiType {

        ThroughputTime, WaitingTime, ResourceUtilization, InventoryCosts, CustomerSatisfaction, LaborFlexibility
    }

    private HLPetriNet originalNet;

    private RedesignGraph redesignGraph;

    private JPanel contentPane;

    private JComponent view;

    private JComponent treeView;

    private GGGraphView graphView;

    private JComboBox redesignBox = new JComboBox(RedesignType.values());

    /**
	 * needed to show the online documentation of the CPN export
	 */
    private CpnExport20 myAlgorithm;

    /**
	 * the following strings are used to create the folder structure
	 */
    private static String folderPref = "C:" + "\\" + "RedesignAnalysis";

    private static String folderPrefCPN = "C:/RedesignAnalysis";

    private int experimentCounter = 1;

    public String locationForCurrentSimSettings = folderPref + "\\" + "currentSimSettings";

    public static String locationForCurrentSimSettingsInCPNExport = folderPrefCPN + "/currentSimSettings";

    public static String locationForCurrentSimModels = folderPref + "\\" + "currentSimModels";

    public File currentSimFolder = new File(locationForCurrentSimModels);

    public RedesignAnalysisUI(HLPetriNet net) {
        originalNet = net;
        redesignGraph = new RedesignGraph(this, originalNet);
        originalNet.getPNModel().setIdentifier("redesign0");
        try {
            (new File(folderPref)).mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String dirDest = folderPref + "\\" + "experiment_" + experimentCounter;
        try {
            (new File(dirDest)).mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File currentSetFolder = new File(locationForCurrentSimSettings);
        try {
            currentSetFolder.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SimSettingsUI set = new SimSettingsUI(originalNet.getHLProcess().getGlobalInfo(), locationForCurrentSimSettings);
        set.createSettingFiles(locationForCurrentSimSettings, originalNet.getHLProcess().getGlobalInfo().getCaseGenerationScheme());
        try {
            currentSimFolder.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
        createSimModel(originalNet);
        constructUI();
    }

    /**
	 * Specifies the UI of the plug-in. The upper part of the UI contains the
	 * tree of alternatives models and the simulation functionality. The lower
	 * part of the UI contains the selected model and the redesign
	 * possibilities. Initially, no model is selected in the tree.
	 */
    private void constructUI() {
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setLayout(new BorderLayout());
        HeaderBar header = new HeaderBar("Evolutionary Redesign");
        header.setHeight(35);
        this.add(header, BorderLayout.NORTH);
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder());
        GradientPanel upperView = new GradientPanel(new Color(60, 60, 60), new Color(90, 90, 90));
        upperView.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        upperView.setLayout(new BorderLayout());
        JPanel controlPanel = new RoundedPanel(10, 5, 3);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(140, 140, 140));
        controlPanel.setMinimumSize(new Dimension(200, 100));
        controlPanel.setMaximumSize(new Dimension(200, 300));
        controlPanel.setPreferredSize(new Dimension(200, 270));
        SlickerButton kpiButton = new SlickerButton("Select KPI");
        kpiButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        kpiButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
            }
        });
        final JComboBox kpiBox = new JComboBox(kpiType.values());
        kpiBox.setUI(new SlickerComboBoxUI());
        kpiBox.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
        SlickerButton settingsButton = new SlickerButton("Select settings");
        settingsButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        settingsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimSettingsUI simUI = new SimSettingsUI(originalNet.getHLProcess().getGlobalInfo(), locationForCurrentSimSettings);
                MainUI.getInstance().createFrame("Set simulation settings", simUI.getPanel());
            }
        });
        SlickerButton simulateButton = new SlickerButton("Simulate models");
        simulateButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        simulateButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent eventvent) {
                performSimulation();
            }
        });
        SlickerButton removeButton = new SlickerButton("Remove model");
        removeButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeSelectedNode();
            }
        });
        SlickerButton addForSimButton = new SlickerButton("(De)select for simulation");
        addForSimButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        addForSimButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                inOrExcludeNodeForSimulation();
            }
        });
        JLabel title1 = new JLabel("Simulation actions");
        title1.setOpaque(false);
        title1.setFont(title1.getFont().deriveFont(14f));
        JLabel title2 = new JLabel("Edit actions");
        title2.setOpaque(false);
        title2.setFont(title2.getFont().deriveFont(14f));
        controlPanel.add(title1);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(kpiButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(kpiBox);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(settingsButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(simulateButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(title2);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(removeButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(addForSimButton);
        controlPanel.add(Box.createVerticalStrut(8));
        this.graphView = new GGGraphView(redesignGraph, 1.5f);
        this.graphView.setMaximumSize(new Dimension(3000, 260));
        this.graphView.setMinimumSize(new Dimension(200, 260));
        this.graphView.setPreferredSize(new Dimension(2000, 260));
        upperView.add(graphView, BorderLayout.CENTER);
        upperView.add(controlPanel, BorderLayout.WEST);
        this.treeView = upperView;
        contentPane.add(this.treeView, BorderLayout.NORTH);
        GradientPanel tmpView = new GradientPanel(new Color(80, 80, 80), new Color(50, 50, 50));
        tmpView.setLayout(new BoxLayout(tmpView, BoxLayout.X_AXIS));
        RoundedPanel innerPanel = new RoundedPanel(20, 0, 0);
        innerPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        innerPanel.setBackground(new Color(150, 150, 150, 200));
        innerPanel.setMinimumSize(new Dimension(200, 50));
        innerPanel.setMaximumSize(new Dimension(300, 50));
        innerPanel.setPreferredSize(new Dimension(200, 50));
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        JLabel innerLabel = new JLabel("Select a model.");
        innerLabel.setFont(innerLabel.getFont().deriveFont(16f));
        innerLabel.setForeground(new Color(20, 20, 20));
        innerLabel.setOpaque(false);
        innerLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        innerLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
        innerPanel.add(Box.createVerticalGlue());
        innerPanel.add(innerLabel);
        innerPanel.add(Box.createVerticalGlue());
        tmpView.add(Box.createHorizontalGlue());
        tmpView.add(innerPanel);
        tmpView.add(Box.createHorizontalGlue());
        this.view = tmpView;
        contentPane.add(tmpView, BorderLayout.CENTER);
        this.add(contentPane, BorderLayout.CENTER);
        revalidate();
    }

    /**
	 * When a model is selected in the upper part of the UI, the lower part of
	 * the UI has to show the selected model.
	 */
    public void showModel(final HLPetriNet model) {
        final PetriNetGraph pnGraph = new PetriNetGraph(this, model);
        GradientPanel modelView = new GradientPanel(new Color(60, 60, 60), new Color(90, 90, 90));
        modelView.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        modelView.setLayout(new BorderLayout());
        JPanel controlPanel = new RoundedPanel(10, 5, 3);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(140, 140, 140));
        controlPanel.setMinimumSize(new Dimension(200, 100));
        controlPanel.setMaximumSize(new Dimension(200, 300));
        controlPanel.setPreferredSize(new Dimension(200, 260));
        redesignBox.setUI(new SlickerComboBoxUI());
        redesignBox.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
        SlickerButton selectTButton = new SlickerButton("Select transformation");
        selectTButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        selectTButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                displaySelectedComponents(model, pnGraph);
            }
        });
        SlickerButton selectButton = new SlickerButton("Deselect process part");
        selectButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                for (GGNode node : pnGraph.nodes()) {
                    node.setSelected(false);
                }
                for (GGEdge edge : pnGraph.edges()) {
                    edge.setSelected(false);
                }
                displaySelectedComponents(model, pnGraph);
            }
        });
        SlickerButton redesignButton = new SlickerButton("Redesign model");
        redesignButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        redesignButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                List<Transition> selectedTransitions = pnGraph.getSelectedPetriNetTransitions();
                RedesignType type = (RedesignType) redesignBox.getSelectedItem();
                Component comp = null;
                if (isIncludingAllSelTransitions(type.ordinal(), model, selectedTransitions)) {
                    try {
                        comp = RedesignFactory.getMinimalComponent(type.ordinal(), model, selectedTransitions);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                performRedesign(model, type, comp);
            }
        });
        SlickerButton analysisButton = new SlickerButton("Modify model");
        analysisButton.setAlignmentX(SlickerButton.LEFT_ALIGNMENT);
        analysisButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                EditHighLevelProcessGui gui = new EditHighLevelProcessGui(model);
                ComponentFrame frame = MainUI.getInstance().createAndReturnFrame("View/Edit High Level Process", gui.getVisualization());
                frame.addInternalFrameListener(new InternalFrameListener() {

                    public void internalFrameClosing(InternalFrameEvent e) {
                        HLPetriNet cloned = (HLPetriNet) model.clone();
                        ColoredPetriNet netToExport = new ColoredPetriNet(cloned);
                        CpnExportSettings exportCPN = new CpnExportSettings(myAlgorithm, netToExport, false);
                        exportCPN.saveCPNmodel();
                        String distrLoc = locationForCurrentSimSettings + "\\" + "arrivalRate.sml";
                        FileWriter dout = null;
                        try {
                            dout = new FileWriter(distrLoc);
                            dout.write("fun readArrivalRate() = " + CpnUtils.getCpnDistributionFunction(model.getHLProcess().getGlobalInfo().getCaseGenerationScheme()) + ";");
                            dout.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }

                    public void internalFrameActivated(InternalFrameEvent e) {
                    }

                    public void internalFrameClosed(InternalFrameEvent e) {
                    }

                    public void internalFrameDeiconified(InternalFrameEvent e) {
                    }

                    public void internalFrameIconified(InternalFrameEvent e) {
                    }

                    public void internalFrameOpened(InternalFrameEvent e) {
                    }

                    public void internalFrameDeactivated(InternalFrameEvent e) {
                    }
                });
            }
        });
        JLabel title = new JLabel("Redesign actions");
        title.setOpaque(false);
        title.setFont(title.getFont().deriveFont(14f));
        JLabel title2 = new JLabel("Refinement actions");
        title2.setOpaque(false);
        title2.setFont(title2.getFont().deriveFont(14f));
        controlPanel.add(title);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(selectTButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(redesignBox);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(selectButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(redesignButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(title2);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(analysisButton);
        final GGGraphView graphView = new GGGraphView();
        graphView.setGraphSelectionModel(new GGToggleGraphSelectionModel());
        modelView.add(graphView, BorderLayout.CENTER);
        modelView.add(controlPanel, BorderLayout.WEST);
        switchView(modelView);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                graphView.setGraph(pnGraph);
            }
        });
    }

    /**
	 * Determine all components on which the selected redesign type can be
	 * applied. If any transitions in the model are selected, these transitions
	 * need to be included in the resulting components.
	 * 
	 * @param type
	 *            RedesignType the selected redesign type
	 * @param selectedTransitions
	 *            List<Transition> the selected transitions
	 * @param model
	 *            HLPetriNet the input model for redesign
	 * @return comps Set<Component> set of all components, if no transitions
	 *         selected
	 * @return selComps Set<Component> set of components including all selected
	 *         transitions
	 */
    public Set<Component> getComponentsToDisplay(RedesignType type, List<Transition> selectedTransitions, HLPetriNet model) {
        Set<Component> comps = new HashSet<Component>();
        try {
            comps = RedesignFactory.getAllComponents(type.ordinal(), model);
        } catch (Exception ex) {
            Message.add("No components created!", Message.ERROR);
            ex.printStackTrace();
        }
        if (selectedTransitions.size() == model.getPNModel().getTransitions().size()) {
            return comps;
        } else {
            Set<Component> selComps = new HashSet<Component>();
            for (Component comp : comps) {
                if (comp.getNodeList().containsAll(selectedTransitions)) {
                    selComps.add(comp);
                }
            }
            return selComps;
        }
    }

    /**
	 * highlight the transitions in the model that are included in the set of
	 * components
	 * 
	 * @param comps
	 *            Set<Component> the components to display
	 * @param model
	 *            HLPetriNet the input model for redesign
	 * @param pnGraph
	 *            PetriNetGraph the visualization of the PetriNet in model
	 */
    public void displayComponents(Set<Component> comps, HLPetriNet model, PetriNetGraph pnGraph) {
        resetDisplay(model, pnGraph);
        if (!comps.isEmpty()) {
            Set<Transition> allIncludedNodes = new HashSet<Transition>();
            Set<Transition> yellowNodes = new HashSet<Transition>();
            for (Component comp : comps) {
                if (comp.getTransitions().size() != 0) {
                    for (Transition node : comp.getTransitions()) {
                        allIncludedNodes.add(node);
                        if (model.getPNModel().getTransitions().contains(node)) {
                            PetriNetTransition t = (PetriNetTransition) pnGraph.getNodeByIdentifier(node.getIdentifier());
                            if (t.isSelected()) {
                                t.setIsSelectedInComponent(true);
                            } else {
                                yellowNodes.add(node);
                            }
                        }
                    }
                }
            }
            if (!yellowNodes.isEmpty()) {
                for (Transition yNode : yellowNodes) {
                    PetriNetTransition t = (PetriNetTransition) pnGraph.getNodeByIdentifier(yNode.getIdentifier());
                    t.setToBeSelectedInComponent(true);
                }
            }
            for (Transition node : model.getPNModel().getTransitions()) {
                if (!allIncludedNodes.contains(node)) {
                    PetriNetTransition t = (PetriNetTransition) pnGraph.getNodeByIdentifier(node.getIdentifier());
                    t.setNotToBeSelectedInComponent(true);
                }
            }
        } else {
            Message.add("There are no components to highlight.", Message.ERROR);
            for (Transition node : model.getPNModel().getTransitions()) {
                PetriNetTransition t = (PetriNetTransition) pnGraph.getNodeByIdentifier(node.getIdentifier());
                if (t.isSelected()) {
                    t.setIsSelectedInComponent(true);
                } else {
                    t.setNotToBeSelectedInComponent(true);
                }
            }
        }
    }

    /**
	 * highlight the transitions in the model that are included in a component
	 * 
	 * @param type
	 *            RedesignType the selected redesign type
	 * @param model
	 *            HLPetriNet the input model for redesign
	 * @param pnGraph
	 *            PetriNetGraph the visualization of the PetriNet in model
	 */
    public void displaySelectedComponents(HLPetriNet model, PetriNetGraph pnGraph) {
        RedesignType type = (RedesignType) redesignBox.getSelectedItem();
        List<Transition> selectedTransitions = pnGraph.getSelectedPetriNetTransitions();
        Set<Component> comps = getComponentsToDisplay(type, selectedTransitions, model);
        displayComponents(comps, model, pnGraph);
    }

    /**
	 * Displays a given component in the model by highlighting its transitions
	 * 
	 * @param comp
	 *            Component the given component
	 * @param model
	 *            HLPetriNet the input model for redesign
	 * @param pnGraph
	 *            PetriNetGraph the visualization of the PetriNet in model
	 */
    public void displayComponent(Component comp, HLPetriNet model, PetriNetGraph pnGraph) {
        resetDisplay(model, pnGraph);
        if (comp != null) {
            if (comp.getTransitions().size() != 0) {
                for (Transition node : comp.getTransitions()) {
                    if (model.getPNModel().getTransitions().contains(node)) {
                        PetriNetTransition t = (PetriNetTransition) pnGraph.getNodeByIdentifier(node.getIdentifier());
                        t.setIsSelectedInComponent(true);
                    }
                }
            } else {
                Message.add("There is no component to highlight.", Message.ERROR);
            }
        } else {
            Message.add("There is no component to highlight.", Message.ERROR);
        }
    }

    /**
	 * Reset highlight of all transitions
	 */
    public void resetDisplay(HLPetriNet model, PetriNetGraph pnGraph) {
        for (Transition t : model.getPNModel().getTransitions()) {
            PetriNetTransition pt = (PetriNetTransition) pnGraph.getNodeByIdentifier(t.getIdentifier());
            pt.setIsSelectedInComponent(false);
            pt.setToBeSelectedInComponent(false);
            pt.setNotToBeSelectedInComponent(false);
        }
    }

    /**
	 * Determines if button is enabled or disabled
	 * 
	 * @param button
	 *            SlickerButton the involved button
	 */
    public void updateButton(SlickerButton button, HLPetriNet model, PetriNetGraph pnGraph) {
        RedesignType type = (RedesignType) redesignBox.getSelectedItem();
        List<Transition> selectedTransitions = pnGraph.getSelectedPetriNetTransitions();
        if (isIncludingAllSelTransitions(type.ordinal(), model, selectedTransitions)) {
            button.setEnabled(true);
        } else {
            button.setEnabled(false);
        }
    }

    /**
	 * Determines the minimal component enclosing the selected nodes and checks
	 * if the selected nodes enclose all transitions in the component, i.e., the
	 * component does not enclose more transitions than the selected ones.
	 * 
	 * @param indexRedesignType
	 *            int Redesign type.
	 * @param model
	 *            HLPetriNet Model to be redesigned.
	 * @param nodes
	 *            List<PNNode> the nodes to redesign, can be all nodes.
	 * @returns true Boolean returned if component encloses exactly the selected
	 *          transitions.
	 */
    public static boolean isIncludingAllSelTransitions(int indexRedesignType, HLPetriNet model, List<Transition> transitions) {
        Component comp = null;
        try {
            comp = RedesignFactory.getMinimalComponent(indexRedesignType, model, transitions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (comp != null && transitions.containsAll(comp.getTransitions())) {
            return true;
        }
        return false;
    }

    /**
	 * Specifies the creation of a redesign, i.e., an alternative model.
	 * 
	 * @param model
	 *            HLPetriNet the input model of the redesign
	 * @param type
	 *            RedesignType the selected transformation rule
	 * @param comp
	 *            Component the component including the nodes to be redesigned
	 */
    private void performRedesign(HLPetriNet model, RedesignType type, Component comp) {
        try {
            HLPetriNet alt = RedesignFactory.getRedesign(type.ordinal(), model, comp);
            if (alt != null) {
                RedesignNode redesignNode = redesignGraph.addRedesign(model, alt, type);
                int modelID = redesignNode.getModelID();
                alt.getPNModel().setIdentifier("redesign" + modelID);
                createSimModel(alt);
                for (GGNode node : redesignGraph.nodes()) {
                    node.setSelected(false);
                }
                graphView.getCanvas().getGraphSelectionModel().clickedNode(redesignNode, graphView.getCanvas(), 0, 0);
                redesignGraph.setWidth(0);
                redesignGraph.setHeight(0);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        graphView.setGraph(redesignGraph);
                    }
                });
                showModel(alt);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Specifies the removal of an alternative model from the tree. Note that
	 * the original model can not be removed.
	 */
    private void removeSelectedNode() {
        if (redesignGraph.getSelectedRedesignNodes().size() != 1) {
            JOptionPane.showMessageDialog(null, "Please make sure that exactly one model is selected.");
        } else if (redesignGraph.getSelectedRedesignNodes().get(0).getModelID() == 0) {
            JOptionPane.showMessageDialog(null, "You are not allowed to remove the first node, i.e., \n" + "the node related to the original model.");
        } else {
            final JOptionPane optionPane = new JOptionPane("You are about to remove the selected node and its successors.", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            final JDialog dialog = new JDialog(MainUI.getFrames()[0], "Warning", true);
            dialog.setContentPane(optionPane);
            optionPane.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent e) {
                    String prop = e.getPropertyName();
                    if (dialog.isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                        dialog.setVisible(false);
                    }
                }
            });
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            int value = ((Integer) optionPane.getValue()).intValue();
            if (value == JOptionPane.OK_OPTION) {
                RedesignNode nodeToRemove = redesignGraph.getSelectedRedesignNodes().get(0);
                String rName = currentSimFolder + "\\redesign" + nodeToRemove.getModelID() + ".cpn";
                File r = new File(rName);
                r.delete();
                for (RedesignNode suc : redesignGraph.getAllSuccessors(nodeToRemove)) {
                    String fileName = currentSimFolder + "\\redesign" + suc.getModelID() + ".cpn";
                    File f = new File(fileName);
                    f.delete();
                }
                redesignGraph.removeAllSuccessors(nodeToRemove);
                redesignGraph.remove(nodeToRemove);
                redesignGraph.removeDanglingEdges();
                RedesignNode firstNode = redesignGraph.getNode(0);
                firstNode.setSelected(true);
                this.showModel((firstNode).getModel());
                redesignGraph.updateView();
            }
        }
    }

    /**
	 * Specifies the (de)selection of a model for simulation.
	 * 
	 */
    private void inOrExcludeNodeForSimulation() {
        if (redesignGraph.getSelectedRedesignNodes().size() != 1) {
            JOptionPane.showMessageDialog(null, "Please make sure that exactly one node is selected.");
        } else {
            RedesignNode nodeToSim = redesignGraph.getSelectedRedesignNodes().get(0);
            if (redesignGraph.getRedesignNodesForSimulation().contains(nodeToSim) && nodeToSim.getModelID() != 0) {
                nodeToSim.setSelectedForSimulation(false);
            } else {
                nodeToSim.setSelectedForSimulation(true);
            }
            redesignGraph.updateView();
        }
    }

    /**
	 * Specifies the simulation of all models that are selected for simulation.
	 */
    private void performSimulation() {
        String dirDest = folderPref + "\\" + "experiment_" + experimentCounter;
        String meansDest = dirDest + "\\" + "meansOfAllNodes";
        File meansFile = new File(meansDest);
        try {
            meansFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String settingDest = dirDest + "\\" + "simSettings";
        File newSettingFolder = new File(settingDest);
        try {
            newSettingFolder.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File oldSettingFolder = new File(locationForCurrentSimSettings);
        copyFiles(oldSettingFolder, newSettingFolder);
        for (RedesignNode node : redesignGraph.getRedesignNodes()) {
            node.setSimulated(false);
        }
        experimentCounter++;
        String newDest = folderPref + "\\" + "experiment_" + experimentCounter;
        try {
            (new File(newDest)).mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Specifies the creation of a simulation model.
	 * 
	 * @see CPN Export
	 * @param net
	 *            HLPetriNet the net from which a simulation model is created.
	 */
    private void createSimModel(HLPetriNet net) {
        HLPetriNet cloned = (HLPetriNet) net.clone();
        ColoredPetriNet netToExport = new ColoredPetriNet(cloned);
        String filename = locationForCurrentSimModels + "\\" + net.getPNModel().getIdentifier() + ".cpn";
        try {
            FileOutputStream out = new FileOutputStream(filename);
            BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(out));
            netToExport.isRedesign(true);
            ManagerConfiguration.getInstance().setRedesignConfiguration();
            netToExport.writeToFile(outWriter, null, null);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
	 * Specifies the copying of files from one folder to another folder.
	 * 
	 * @param oldFolder
	 *            File the folder from which files have to be copied
	 * @param newFolder
	 *            File the folder to which files are copied
	 */
    private void copyFiles(File oldFolder, File newFolder) {
        for (File fileToCopy : oldFolder.listFiles()) {
            File copiedFile = new File(newFolder.getAbsolutePath() + "\\" + fileToCopy.getName());
            try {
                FileInputStream source = new FileInputStream(fileToCopy);
                FileOutputStream destination = new FileOutputStream(copiedFile);
                FileChannel sourceFileChannel = source.getChannel();
                FileChannel destinationFileChannel = destination.getChannel();
                long size = sourceFileChannel.size();
                sourceFileChannel.transferTo(0, size, destinationFileChannel);
                source.close();
                destination.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    /**
	 * Specifies the copying of one file.
	 * 
	 * @param oldPath
	 *            String the old path of the file
	 * @param newPath
	 *            String the new path of the file
	 */
    private void copyOneFile(String oldPath, String newPath) {
        File copiedFile = new File(newPath);
        try {
            FileInputStream source = new FileInputStream(oldPath);
            FileOutputStream destination = new FileOutputStream(copiedFile);
            FileChannel sourceFileChannel = source.getChannel();
            FileChannel destinationFileChannel = destination.getChannel();
            long size = sourceFileChannel.size();
            sourceFileChannel.transferTo(0, size, destinationFileChannel);
            source.close();
            destination.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
	 * Specifies the switch from the view on the lower part of the UI when no
	 * model is selected to the view on a selected model.
	 */
    private void switchView(JComponent updatedView) {
        contentPane.remove(this.view);
        this.view = updatedView;
        contentPane.add(this.view, BorderLayout.CENTER);
        contentPane.revalidate();
    }

    /**
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.framework.plugin.Provider#getProvidedObjects()
	 */
    public ProvidedObject[] getProvidedObjects() {
        return null;
    }
}
