package org.tripcom.security;

import static org.junit.Assert.assertNotNull;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;
import org.junit.After;
import org.junit.Before;
import org.tripcom.integration.entry.InternalDataTSAdapterEntry;
import org.tripcom.integration.entry.SMTSAdapterResultEntry;
import org.tripcom.integration.javaspace.JavaSpaceDebugWrapper;
import org.tripcom.security.main.RecoverableJavaSpace;
import org.tripcom.security.main.SecurityManager;
import org.tripcom.security.memory.JavaSpaceMemory.TokenEntry;
import org.tripcom.security.stubs.JavaSpaceStub;
import org.tripcom.security.util.ConfigurableThreadFactory;
import org.tripcom.security.util.JavaSpaces;
import org.tripcom.security.util.Resources;
import org.tripcom.security.util.Scheduler;

/**
 * Abstract test case class for JUnit component level testing.
 *
 * @author Michael Lafite
 */
public abstract class AbstractComponentTest {

    protected static final Long DEFAULT_TIMEOUT = 4000L;

    protected static final String ROOT_SPACE = "http://www.tripcom.org/testing/security/";

    protected static final String SOME_SPACE = "http://somehost/somespace";

    private static final boolean WRAPPER_USED = true;

    private enum SpaceType {

        STUB, RECOVERABLE
    }

    ;

    private static final SpaceType SPACE_TYPE = SpaceType.STUB;

    private static final boolean TSADAPTER_HELPER_THREAD_NEVER_STARTED = true;

    private Thread tsAdapterHelperThread;

    private Scheduler scheduler;

    protected SecurityManager securityManager;

    public static final String CERTIFICATE_HANS = "-----BEGIN CERTIFICATE-----\n" + "MIIDVTCCAr6gAwIBAgIJAJErup9ICWQfMA0GCSqGSIb3DQEBBAUAMHsxCzAJBgNV\n" + "BAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMQ8wDQYDVQQHEwZWaWVubmExEDAO\n" + "BgNVBAoTB2V4YW1wbGUxEzARBgNVBAMTCmhhbnMgbWF5ZXIxHzAdBgkqhkiG9w0B\n" + "CQEWEGhhbnNAZXhhbXBsZS5jb20wHhcNMDgwMTMxMTIwMzI0WhcNMTgwMTI4MTIw\n" + "MzI0WjB7MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEPMA0GA1UE\n" + "BxMGVmllbm5hMRAwDgYDVQQKEwdleGFtcGxlMRMwEQYDVQQDEwpoYW5zIG1heWVy\n" + "MR8wHQYJKoZIhvcNAQkBFhBoYW5zQGV4YW1wbGUuY29tMIGfMA0GCSqGSIb3DQEB\n" + "AQUAA4GNADCBiQKBgQC/qbjN8d3fh0dJsUOUSw3DSlJUpzG9iqdtdzTiiegaxHVn\n" + "iVEcZJIutKIavapvDSkewClcD+L5dWc09aW7SKj/xKI3UNE5KRd3NGvn7w81/6jN\n" + "NEYAhpgzvt8D7/Tr91CHrCHYwGJiwRo3BqFESOnK8Knjvkchkc8V1viWYehrsQID\n" + "AQABo4HgMIHdMB0GA1UdDgQWBBQM7MykqQa36JvWc83trIevR/NIfTCBrQYDVR0j\n" + "BIGlMIGigBQM7MykqQa36JvWc83trIevR/NIfaF/pH0wezELMAkGA1UEBhMCQVUx\n" + "EzARBgNVBAgTClNvbWUtU3RhdGUxDzANBgNVBAcTBlZpZW5uYTEQMA4GA1UEChMH\n" + "ZXhhbXBsZTETMBEGA1UEAxMKaGFucyBtYXllcjEfMB0GCSqGSIb3DQEJARYQaGFu\n" + "c0BleGFtcGxlLmNvbYIJAJErup9ICWQfMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcN\n" + "AQEEBQADgYEApYfmxYE0HSJH+qEo/chXoZfc/8V6WV33W5azWALeuJeguTmTJjN+\n" + "mAukFJcZAT/87Y5m3oP37EAgxEmy2Q3NmYK5T92qYVAEnOKHwrTbl5sra6QvqCNc\n" + "JEgyXQGVR/KGdIFm709aQZNz+0ZuImXunksZlq+/997Qj+KMhyKzIr4=\n" + "-----END CERTIFICATE-----";

    private JavaSpace space;

    /**
	 * Initialize the text fixture.
	 */
    @Before
    public void setUp() throws Exception {
        try {
            this.scheduler = new Scheduler(2, new ConfigurableThreadFactory("sm-", false));
            switch(SPACE_TYPE) {
                case STUB:
                    this.space = new JavaSpaceStub("");
                    break;
                case RECOVERABLE:
                    this.space = new RecoverableJavaSpace(this.scheduler, "localhost", 4160);
                    break;
            }
            if (WRAPPER_USED) {
                Class<?>[] notLoggedEntriesLevel1 = new Class<?>[] { TokenEntry.class };
                Class<?>[] notLoggedEntriesLevel2 = null;
                this.space = new JavaSpaceDebugWrapper(this.space, "", System.out, 2, true, false, true, notLoggedEntriesLevel1, notLoggedEntriesLevel2);
            }
            Properties configuration = Resources.loadProperties("org/tripcom/security/main/" + "default.properties");
            configuration.setProperty("org.tripcom.security.rootSpaceURLs", ROOT_SPACE);
            configuration.setProperty("org.tripcom.security.applicationContext", "org/tripcom/security/main/" + "security-manager.test.context.xml");
            this.securityManager = new SecurityManager(configuration, this.scheduler, this.space, this.space, this.space, this.space);
            this.securityManager.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
        if (this.tsAdapterHelperThread != null && this.tsAdapterHelperThread.isAlive()) {
            this.tsAdapterHelperThread.join();
        }
    }

    /**
	 * Release the text fixture.
	 */
    @After
    public void tearDown() throws Exception {
        if (SPACE_TYPE != SpaceType.STUB) {
            while (true) {
                Entry entry = getSpace().take(null, null, 1000);
                if (entry == null) {
                    break;
                }
            }
        }
        if (this.tsAdapterHelperThread != null && this.tsAdapterHelperThread.isAlive()) {
            this.tsAdapterHelperThread.join();
        }
        this.securityManager.stop();
        this.scheduler.shutdown();
        this.scheduler.awaitTermination(1000, TimeUnit.MILLISECONDS);
        this.scheduler.shutdownNow();
    }

    /**
	 * Writes an entry to the spaces and then checks if the SM returns the
	 * expected entry.
	 *
	 * @param entryToWrite
	 *            entry to be written to the system bus.
	 * @param expectedReturn
	 *            template for the entry expected to be written by the SM.
	 * @param failureMessage
	 *            message that is printed if the test fails.
	 * @param tsAdapterThreadNeeded
	 *            {@code true}, if the {@link #startTSAdapterHelperThread()}
	 *            shall be called.
	 * @return the entry written by the SM.
	 * @throws Exception
	 *             if any error occurs.
	 */
    protected Entry write(Entry entryToWrite, Entry expectedReturn, String failureMessage, boolean tsAdapterThreadNeeded) throws Exception {
        if (tsAdapterThreadNeeded) {
            startTSAdapterHelperThread();
        }
        getSpace().write(entryToWrite, null, Lease.FOREVER);
        Entry entry = getSpace().take(expectedReturn, null, DEFAULT_TIMEOUT);
        if (failureMessage == null) {
            assertNotNull("Couldn't take " + expectedReturn.getClass().getSimpleName(), entry);
        } else {
            assertNotNull(failureMessage, entry);
        }
        return entry;
    }

    protected Entry[] write(Entry entryToWrite, Entry... expectedReturns) throws Exception {
        Entry[] returnedEntries = new Entry[expectedReturns.length];
        getSpace().write(entryToWrite, null, Lease.FOREVER);
        int i = 0;
        for (Entry expectedReturn : expectedReturns) {
            Entry entry = getSpace().take(expectedReturn, null, DEFAULT_TIMEOUT);
            assertNotNull("Couldn't take " + expectedReturn.getClass().getSimpleName(), entry);
            returnedEntries[i] = entry;
            i++;
        }
        return returnedEntries;
    }

    protected void startTSAdapterHelperThread() {
        if (!TSADAPTER_HELPER_THREAD_NEVER_STARTED) {
            this.tsAdapterHelperThread = new Thread() {

                public void run() {
                    try {
                        InternalDataTSAdapterEntry smtsAdapterQueryEntry = (InternalDataTSAdapterEntry) getSpace().take(new InternalDataTSAdapterEntry(), null, 10000L);
                        assertNotNull(smtsAdapterQueryEntry);
                        JavaSpaces.write(getSpace(), new SMTSAdapterResultEntry());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            this.tsAdapterHelperThread.start();
        }
    }

    protected JavaSpace getSpace() {
        return this.space;
    }
}
