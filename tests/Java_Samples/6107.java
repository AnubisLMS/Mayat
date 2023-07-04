package klava.proto;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Iterator;
import klava.KlavaException;
import klava.Tuple;
import klava.TupleItem;
import klava.TupleSpaceVector;
import klava.TupleSpace;
import klava.topology.KlavaProcess;
import org.mikado.imc.common.MigrationUnsupported;
import org.mikado.imc.mobility.MigratingCodeFactory;
import org.mikado.imc.protocols.Marshaler;
import org.mikado.imc.protocols.ProtocolException;
import org.mikado.imc.protocols.ProtocolStateSimple;
import org.mikado.imc.protocols.TransmissionChannel;
import org.mikado.imc.protocols.UnMarshaler;
import org.mikado.imc.protocols.WrongStringProtocolException;

/**
 * @author Lorenzo Bettini
 * @version $Revision: 1.4 $
 */
public class TupleState extends ProtocolStateSimple {

    /**
     * The constant string to communicate the start of a tuple
     */
    public static final String TUPLE_S = "TUPLE";

    /**
     * The constant string to communicate the end of a list
     */
    public static final String END_S = "END";

    /**
     * The constant string to communicate the beginning of tuple parameters
     */
    public static final String PARAMS_S = "PARAMS";

    /**
     * The constant string to communicate the beginning of tuple items
     */
    public static final String ITEMS_S = "ITEMS";

    /**
     * The constant string to communicate the beginning of an actual tuple item
     * value
     */
    public static final String ITEM_S = "ITEM";

    /**
     * The constant string to communicate a formal TupleItem
     */
    public static final String FORMAL_S = "FORMAL";

    /**
     * The constant string to communicate the tuple id
     */
    public static final String ID_S = "ID";

    /**
     * The constant string to communicate an actual TupleItem
     */
    public static final String ACTUAL_S = "ACTUAL";

    /**
     * The prefix used to specify a class
     */
    public static final String CLASS_PREFIX = "class ";

    /**
     * The already retrieved tuple identifiers
     */
    public static final String RETRIEVED_S = "RETRIEVED";

    /**
     * Whether the tuple handles already retrieved tuples
     */
    public static final String HRETRIEVED_S = "HRETRIEVED";

    /**
     * The tuple that matched this one
     */
    public static final String MATCHED_S = "MATCHED";

    /**
     * The original template with which we matched this tuple
     */
    public static final String ORIGTEMPLATE_S = "ORIGTEMPLATE";

    /**
     * The tuple on which this state operates
     */
    Tuple tuple = new Tuple();

    /**
     * Whether this state is used for reading or writing
     */
    boolean doRead;

    /**
     * This is used to dispatch and retrieve mobile code.
     */
    protected MigratingCodeFactory migratingCodeFactory;

    /**
     * 
     */
    public TupleState() {
        super();
    }

    /**
     * @param next_state
     */
    public TupleState(String next_state) {
        super(next_state);
    }

    /**
     * @see org.mikado.imc.protocols.ProtocolStateSimple#enter(java.lang.Object,
     *      org.mikado.imc.protocols.TransmissionChannel)
     */
    @Override
    public void enter(Object param, TransmissionChannel transmissionChannel) throws ProtocolException {
        try {
            if (doRead) {
                tuple = read(transmissionChannel);
            } else {
                write(transmissionChannel, tuple);
            }
        } catch (IOException e) {
            throw new ProtocolException(e);
        } catch (ClassNotFoundException e) {
            throw new ProtocolException(e);
        } catch (MigrationUnsupported e) {
            throw new ProtocolException(e);
        }
    }

    /**
     * Writes a tuple by using a TransmissionChannel
     * 
     * @param transmissionChannel
     * @param tuple
     * @throws IOException
     * @throws MigrationUnsupported
     */
    void write(TransmissionChannel transmissionChannel, Tuple tuple) throws IOException, MigrationUnsupported {
        Marshaler marshaler = transmissionChannel.marshaler;
        marshaler.writeStringLine(TUPLE_S);
        marshaler.writeStringLine(PARAMS_S);
        marshaler.writeStringLine(ID_S);
        marshaler.writeStringLine(tuple.getTupleId());
        marshaler.writeStringLine(HRETRIEVED_S);
        if (tuple.isHandleRetrieved()) {
            marshaler.writeStringLine("TRUE");
            Iterator<String> iterator = tuple.getAlreadyRetrieved();
            StringBuffer alreadyRetrieved = new StringBuffer();
            int n = 0;
            while (iterator.hasNext()) {
                alreadyRetrieved.append(iterator.next() + "\n");
                ++n;
            }
            marshaler.writeStringLine("" + n);
            marshaler.writeBytes(alreadyRetrieved.toString());
        } else {
            marshaler.writeStringLine("FALSE");
        }
        if (tuple.getMatched() != null) {
            marshaler.writeStringLine(MATCHED_S);
            write(transmissionChannel, tuple.getMatched());
        }
        marshaler.writeStringLine(END_S + PARAMS_S);
        marshaler.writeStringLine(ITEMS_S);
        Enumeration<Object> tupleitems = tuple.tupleElements();
        while (tupleitems.hasMoreElements()) {
            Object item = tupleitems.nextElement();
            if (item instanceof TupleItem) {
                TupleItem tupleItem = (TupleItem) item;
                if (tupleItem.isFormal()) {
                    marshaler.writeStringLine(FORMAL_S);
                } else {
                    marshaler.writeStringLine(ACTUAL_S);
                    marshaler.writeStringLine(ITEM_S);
                }
                if (writeSpecial(transmissionChannel, item)) {
                    if (!tupleItem.isFormal()) marshaler.writeStringLine(END_S + ITEM_S);
                    continue;
                }
                marshaler.writeStringLine(tupleItem.getClass().getName());
                if (!tupleItem.isFormal()) {
                    marshaler.writeStringLine(tupleItem.toString());
                    marshaler.writeStringLine(END_S + ITEM_S);
                }
            } else {
                boolean isFormal = (item instanceof Class);
                if (isFormal) {
                    marshaler.writeStringLine(FORMAL_S);
                } else {
                    marshaler.writeStringLine(ACTUAL_S);
                    marshaler.writeStringLine(ITEM_S);
                }
                if (writeSpecial(transmissionChannel, item)) {
                    if (!isFormal) marshaler.writeStringLine(END_S + ITEM_S);
                    continue;
                }
                if (isFormal) {
                    marshaler.writeStringLine(item.toString());
                } else {
                    marshaler.writeStringLine(item.getClass().getName());
                }
                if (!isFormal) {
                    marshaler.writeStringLine(item.toString());
                    marshaler.writeStringLine(END_S + ITEM_S);
                }
            }
        }
        marshaler.writeStringLine(END_S + ITEMS_S);
        marshaler.writeStringLine(END_S + TUPLE_S);
    }

    /**
     * Reads a tuple from the TransmissionChannel
     * 
     * @param transmissionChannel
     * @return The read tuple
     * @throws IOException
     * @throws ProtocolException
     * @throws ClassNotFoundException
     * @throws MigrationUnsupported
     */
    Tuple read(TransmissionChannel transmissionChannel) throws IOException, ProtocolException, ClassNotFoundException, MigrationUnsupported {
        Tuple tuple = new Tuple();
        UnMarshaler unMarshaler = getUnMarshaler(transmissionChannel);
        String s = unMarshaler.readStringLine();
        if (!s.equals(TUPLE_S)) {
            throw new WrongStringProtocolException(TUPLE_S, s);
        }
        s = unMarshaler.readStringLine();
        if (s.equals(PARAMS_S)) {
            while (true) {
                s = unMarshaler.readStringLine();
                if (s.equals(ID_S)) {
                    String id = unMarshaler.readStringLine();
                    tuple.setTupleId(id);
                } else if (s.equals(HRETRIEVED_S)) {
                    tuple.setHandleRetrieved(Boolean.parseBoolean(unMarshaler.readStringLine()));
                    if (tuple.isHandleRetrieved()) {
                        try {
                            int n = Integer.parseInt(unMarshaler.readStringLine());
                            for (int i = 1; i <= n; ++i) tuple.addRetrieved(unMarshaler.readStringLine());
                        } catch (NumberFormatException e) {
                            throw new ProtocolException(e);
                        }
                    }
                } else if (s.equals(MATCHED_S)) {
                    tuple.setMatched(read(transmissionChannel));
                } else {
                    break;
                }
            }
            if (!s.equals(END_S + PARAMS_S)) {
                throw new WrongStringProtocolException(END_S + PARAMS_S, s);
            }
        }
        s = unMarshaler.readStringLine();
        if (!s.equals(ITEMS_S)) {
            throw new WrongStringProtocolException(ITEMS_S, s);
        }
        while (true) {
            s = unMarshaler.readStringLine();
            if (s.equals(FORMAL_S)) {
                String type = unMarshaler.readStringLine();
                if (type.startsWith(CLASS_PREFIX)) {
                    tuple.add(Class.forName(type.substring(CLASS_PREFIX.length())));
                } else {
                    Class c = Class.forName(type);
                    try {
                        tuple.add(c.newInstance());
                    } catch (InstantiationException e) {
                        throw new ProtocolException(e);
                    } catch (IllegalAccessException e) {
                        throw new ProtocolException(e);
                    }
                }
            } else if (s.equals(ACTUAL_S)) {
                String itemS = unMarshaler.readStringLine();
                if (!itemS.equals(ITEM_S)) throw new WrongStringProtocolException(ITEM_S, itemS);
                String type = unMarshaler.readStringLine();
                if (type.startsWith(CLASS_PREFIX)) {
                    type = type.substring(CLASS_PREFIX.length());
                }
                if (readSpecial(transmissionChannel, tuple, type)) {
                    String end = unMarshaler.readStringLine();
                    if (!end.equals(END_S + ITEM_S)) throw new WrongStringProtocolException(END_S + ITEM_S, end);
                    continue;
                }
                Class c = Class.forName(type);
                String actual = readActual(transmissionChannel);
                Class parameters[] = { java.lang.String.class };
                try {
                    Constructor cons = c.getConstructor(parameters);
                    Object args[] = { actual };
                    tuple.add(cons.newInstance(args));
                } catch (NoSuchMethodException e) {
                    try {
                        Object o = c.newInstance();
                        if (!(o instanceof TupleItem)) {
                            throw new ProtocolException(e);
                        }
                        ((TupleItem) o).setValue(actual);
                        tuple.add(o);
                    } catch (InstantiationException e1) {
                        throw new ProtocolException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new ProtocolException(e1);
                    } catch (KlavaException e1) {
                        throw new ProtocolException(e1);
                    }
                } catch (Exception e) {
                    throw new ProtocolException(e);
                }
            } else {
                break;
            }
        }
        if (!s.equals(END_S + ITEMS_S)) {
            throw new WrongStringProtocolException(END_S + ITEMS_S, s);
        }
        s = unMarshaler.readStringLine();
        if (!s.equals(END_S + TUPLE_S)) {
            throw new WrongStringProtocolException(END_S + TUPLE_S, s);
        }
        return tuple;
    }

    /**
     * Reads from the passed TransmissionChannel the actual value (that can
     * spawn also more than one line) till it reaches END_S + ITEM_S.
     * 
     * @param transmissionChannel
     * @return The actual value read
     * @throws IOException
     */
    String readActual(TransmissionChannel transmissionChannel) throws IOException {
        String s = "";
        String line;
        do {
            line = transmissionChannel.readStringLine();
            if (!line.equals(END_S + ITEM_S)) {
                s += (s.length() > 0 ? "\n" : "") + line;
            }
        } while (!line.equals(END_S + ITEM_S));
        return s;
    }

    /**
     * Handles special cases of writing a tuple item.
     * 
     * @param transmissionChannel
     * @param tupleItem
     * @return true if this method already handled this situation
     * @throws IOException
     * @throws MigrationUnsupported
     */
    boolean writeSpecial(TransmissionChannel transmissionChannel, Object tupleItem) throws IOException, MigrationUnsupported {
        if (tupleItem instanceof Tuple) {
            transmissionChannel.marshaler.writeStringLine(tupleItem.getClass().getName());
            Tuple tuple = (Tuple) tupleItem;
            write(transmissionChannel, tuple);
            return true;
        } else if (tupleItem instanceof TupleSpaceVector) {
            transmissionChannel.marshaler.writeStringLine(tupleItem.getClass().getName());
            if ((tupleItem instanceof TupleItem) && ((TupleItem) tupleItem).isFormal()) {
                return true;
            }
            TupleSpace tupleSpace = (TupleSpace) tupleItem;
            transmissionChannel.writeStringLine("" + tupleSpace.length());
            Enumeration<Tuple> tuples = tupleSpace.getTupleEnumeration();
            while (tuples.hasMoreElements()) {
                write(transmissionChannel, tuples.nextElement());
            }
            return true;
        } else if (tupleItem instanceof KlavaProcess) {
            transmissionChannel.marshaler.writeStringLine(KlavaProcess.class.getName());
            transmissionChannel.marshaler.setMigratingCodeFactory(migratingCodeFactory);
            transmissionChannel.marshaler.writeMigratingCode((KlavaProcess) tupleItem);
            return true;
        }
        return false;
    }

    /**
     * Handle special cases of reading a tuple item. The passed tuple must be
     * already initialized and special cases are inserted directly in the passed
     * tuple.
     * 
     * @param transmissionChannel
     * @param tuple
     * @param type
     * @return true if this method already handled this situation
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ProtocolException
     * @throws MigrationUnsupported
     */
    boolean readSpecial(TransmissionChannel transmissionChannel, Tuple tuple, String type) throws ProtocolException, IOException, ClassNotFoundException, MigrationUnsupported {
        if (type.equals(Tuple.class.getName())) {
            tuple.add(read(transmissionChannel));
            return true;
        } else if (type.equals(TupleSpaceVector.class.getName())) {
            TupleSpaceVector tupleSpace = new TupleSpaceVector();
            String l = transmissionChannel.unMarshaler.readStringLine();
            try {
                int length = Integer.parseInt(l);
                for (int i = 1; i <= length; ++i) {
                    tupleSpace.add(read(transmissionChannel));
                }
                tuple.add(tupleSpace);
            } catch (NumberFormatException e) {
                throw new WrongStringProtocolException("tuple space length", l);
            }
            return true;
        } else if (type.equals(KlavaProcess.class.getName())) {
            transmissionChannel.unMarshaler.setMigratingCodeFactory(migratingCodeFactory);
            tuple.add(transmissionChannel.unMarshaler.readMigratingCode());
            return true;
        }
        return false;
    }

    /**
     * @return Returns the doRead.
     */
    public final boolean isDoRead() {
        return doRead;
    }

    /**
     * @param doRead
     *            The doRead to set.
     */
    public final void setDoRead(boolean doRead) {
        this.doRead = doRead;
    }

    /**
     * @return Returns the tuple.
     */
    public final Tuple getTuple() {
        return tuple;
    }

    /**
     * @param tuple
     *            The tuple to set.
     */
    public final void setTuple(Tuple tuple) {
        this.tuple = tuple;
    }

    /**
     * @return Returns the migratingCodeFactory.
     */
    public MigratingCodeFactory getMigratingCodeFactory() {
        return migratingCodeFactory;
    }

    /**
     * @param migratingCodeFactory
     *            The migratingCodeFactory to set.
     */
    public void setMigratingCodeFactory(MigratingCodeFactory migratingCodeFactory) {
        this.migratingCodeFactory = migratingCodeFactory;
    }
}
