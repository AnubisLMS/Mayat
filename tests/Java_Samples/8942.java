package server.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javolution.util.Index;
import server.execution.AbstractExecution;
import topology.GraphInterface;
import topology.SerializableGraphRepresentation;
import topology.graphParsers.GraphParserFactory;

public class Network {

    private String m_networkName = null;

    private SerializableGraphRepresentation m_serGraph = null;

    /** CONSTANTS */
    private static final String _GRAPH = ".graph";

    private static final String NEW_LINE = "\n";

    /** The constructor sets the given name.
	 * @param networkName
	 */
    public Network(String networkName) {
        this.m_networkName = networkName;
    }

    public Network(String networkName, GraphInterface<Index> graph) {
        m_networkName = networkName;
        m_serGraph = new SerializableGraphRepresentation(graph);
    }

    /** Loads from the data directory the already parsed graph file (which has the network's name).
	 * During the loading, updates the given AbstractExecution object.
	 * @param exe
	 * @return True if the load has been successful and False otherwise.
	 */
    public boolean loadNetwork(AbstractExecution exe) {
        reportSuccess(exe, AbstractExecution.PHASE_FAILURE, AbstractExecution.PHASE_NONCOMPLETE);
        boolean success = true;
        try {
            File graphFile = new File(ServerConstants.DATA_DIR + m_networkName + _GRAPH);
            if (graphFile.exists()) {
                try {
                    ObjectInputStream in = null;
                    try {
                        in = new ObjectInputStream(new FileInputStream(graphFile));
                        m_serGraph = (SerializableGraphRepresentation) in.readObject();
                    } catch (ClassNotFoundException ex) {
                        LoggingManager.getInstance().writeSystem("A ClassNotFoundException has occured while trying to read the graph from file " + graphFile.getName(), ServerConstants.NETWORK, ServerConstants.LOAD_NETWORK, ex);
                        throw new Exception("A ClassNotFoundException has occured while trying to read the graph from file " + graphFile.getName() + "\n" + ex.getMessage());
                    } catch (IOException ex) {
                        LoggingManager.getInstance().writeSystem("An IOException has occured while trying to read the graph from file " + graphFile.getName(), ServerConstants.NETWORK, ServerConstants.LOAD_NETWORK, ex);
                        throw new Exception("An IOException has occured while trying to read the graph from file " + graphFile.getName() + "\n" + ex.getMessage() + "\n" + ex.getStackTrace());
                    } finally {
                        try {
                            if (in != null) in.close();
                        } catch (IOException ex) {
                            LoggingManager.getInstance().writeSystem("An IOException has occured while trying to close the input stream after reading the file: " + graphFile.getName(), ServerConstants.NETWORK, ServerConstants.LOAD_NETWORK, ex);
                            throw new Exception("An IOException has occured while trying to close the input stream after reading the file: " + graphFile.getName() + "\n" + ex.getMessage() + "\n" + ex.getStackTrace());
                        }
                    }
                    reportSuccess(exe, AbstractExecution.PHASE_SUCCESS, AbstractExecution.PHASE_COMPLETE);
                    success = true;
                } catch (Exception ex) {
                    reportSuccess(exe, AbstractExecution.PHASE_FAILURE, AbstractExecution.PHASE_COMPLETE);
                    LoggingManager.getInstance().writeSystem("Couldn't load " + m_networkName + ".graph.", ServerConstants.NETWORK, ServerConstants.LOAD_NETWORK, ex);
                    success = false;
                }
            } else {
                reportSuccess(exe, AbstractExecution.PHASE_FAILURE, AbstractExecution.PHASE_COMPLETE);
                LoggingManager.getInstance().writeSystem("The file " + ServerConstants.DATA_DIR + m_networkName + ".graph doesn't exist.", ServerConstants.NETWORK, ServerConstants.LOAD_NETWORK, null);
                success = false;
            }
        } catch (RuntimeException ex) {
            reportSuccess(exe, AbstractExecution.PHASE_FAILURE, AbstractExecution.PHASE_COMPLETE);
            LoggingManager.getInstance().writeSystem("The file " + ServerConstants.DATA_DIR + m_networkName + ".graph doesn't exist.", ServerConstants.NETWORK, ServerConstants.LOAD_NETWORK, null);
            success = false;
        }
        LoggingManager.getInstance().writeTrace("Finishing graph loading.", ServerConstants.NETWORK, ServerConstants.LOAD_NETWORK, null);
        return success;
    }

    /** If the given file contents String is not null and not empty, then the contents are parsed into a graph.
     * Otherwise, a file with the given name is loaded from the data directory and is parsed into a graph.
     * During the loading, updates the given AbstractExecution object.
     * @param filename - The name of the network file to parse.
     * @param importedNet - The contents of the network file to parse.
     * @return True if the import has been successful and False otherwise.
     */
    public boolean importNetwork(AbstractExecution exe, String filename_with_extension, String importedNet) {
        reportSuccess(exe, AbstractExecution.PHASE_FAILURE, AbstractExecution.PHASE_NONCOMPLETE);
        boolean success = true;
        try {
            setPhaseProgress(exe, 0);
            GraphInterface<Index> graph = GraphParserFactory.getGraph(ServerConstants.DATA_DIR, filename_with_extension, importedNet, exe, 1);
            if (graph == null) {
                reportSuccess(exe, AbstractExecution.PHASE_FAILURE, AbstractExecution.PHASE_COMPLETE);
                LoggingManager.getInstance().writeSystem("Couldn't import " + m_networkName + "\n" + importedNet, ServerConstants.NETWORK, ServerConstants.IMPORT_NETWORK, null);
                success = false;
            } else {
                m_serGraph = new SerializableGraphRepresentation(graph);
                reportSuccess(exe, AbstractExecution.PHASE_SUCCESS, AbstractExecution.PHASE_COMPLETE);
                success = true;
            }
        } catch (RuntimeException ex) {
            reportSuccess(exe, AbstractExecution.PHASE_FAILURE, AbstractExecution.PHASE_COMPLETE);
            LoggingManager.getInstance().writeSystem("Couldn't import " + m_networkName + NEW_LINE + importedNet, ServerConstants.NETWORK, ServerConstants.IMPORT_NETWORK, ex);
            success = false;
        }
        LoggingManager.getInstance().writeTrace("Finishing network importing.", ServerConstants.NETWORK, ServerConstants.IMPORT_NETWORK, null);
        return success;
    }

    /** Writes the graph to a file with the network's name and saves it into data directory. 
	 * @return True if the storing has been successful and False otherwise.
	 */
    public boolean storeGraph() {
        boolean success = true;
        String graphFileName = m_networkName + _GRAPH;
        try {
            File outFile = new File(ServerConstants.DATA_DIR + graphFileName);
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(new FileOutputStream(outFile));
                out.writeObject(m_serGraph);
            } catch (IOException ex) {
                LoggingManager.getInstance().writeSystem("An IOException has occured while trying to save graph representation to file " + outFile.getName(), ServerConstants.NETWORK, ServerConstants.STORE_GRAPH, ex);
                throw new IOException("An IOException has occured while trying to save graph representation to file " + outFile.getName() + "\n" + ex.getMessage());
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                } catch (IOException ex) {
                    LoggingManager.getInstance().writeSystem("An IOException has occured while trying to close the output stream after writting the file: " + outFile.getName(), ServerConstants.NETWORK, ServerConstants.STORE_GRAPH, ex);
                    throw new IOException("An IOException has occured while trying to close the output stream after writting the file: " + outFile.getName() + "\n" + ex.getMessage());
                }
            }
        } catch (RuntimeException rex) {
            LoggingManager.getInstance().writeSystem("A RuntimeException has occured while storing graph representation.", ServerConstants.NETWORK, ServerConstants.STORE_GRAPH, rex);
            success = false;
        } catch (Exception ex) {
            LoggingManager.getInstance().writeSystem("An exception has occured while storing graph representation:\n" + ex.getMessage() + NEW_LINE + ex.getStackTrace(), ServerConstants.NETWORK, ServerConstants.STORE_GRAPH, ex);
            success = false;
        }
        LoggingManager.getInstance().writeTrace("Finishing storing graph representation.", ServerConstants.NETWORK, ServerConstants.STORE_GRAPH, null);
        return success;
    }

    /** @return the network's graph. */
    public GraphInterface<Index> getGraph() {
        return m_serGraph.getGraph();
    }

    /** @return the network's name. */
    public String getName() {
        return m_networkName;
    }

    private void setPhaseProgress(AbstractExecution exe, double progress) {
        exe.setProgress(progress);
    }

    private void setPhaseSuccess(AbstractExecution exe, int success) {
        exe.setSuccess(success);
    }

    private void reportSuccess(AbstractExecution exe, int success, double progress) {
        setPhaseProgress(exe, progress);
        setPhaseSuccess(exe, success);
    }
}
