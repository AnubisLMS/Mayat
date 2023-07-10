package de.kumpe.hadooptimizer.examples;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Base class for all "executables" in the examples module. It delivers a basic
 * infrastructure for a common workflow:
 * <ol>
 * <li>{@link #createOptions()}
 * <li>{@link #processCommandLine()}
 * <li>{@link #execute()}
 * </ol>
 * 
 * @author <a href="http://kumpe.de/christian/java">Christian Kumpe</a>
 */
public abstract class Example extends Configured {

    public static final String BANNER_TEXT = "    ***********************************************************************\n" + "    * HadoOptimizer 0.0.1-SNAPSHOT                                        *\n" + "    * Copyright 2011 Christian Kumpe http://kumpe.de/christian/java       *\n" + "    * Licensed under the Apache License, Version 2.0                      *\n" + "    ***********************************************************************\n" + "                                                    _a,          awa,    _ \n" + "                                     _QQQL    '?Ss,mQQQF        :QQQ[    j \n" + "                                    _QQQQ@       -?QQQ@'       swQWT'    F \n" + "                         .aQQQL    jQQQQL         ]QQQQ4mw,   .QQQQw    ]' \n" + "                       .wQQQQQW   =QQQQQE         ]QQQQwD??   ]QQQQQ;   2  \n" + "                       mQQQW      ]QQQQQf         ]QQQQ@'     jQQQQQQwaaf  \n" + "                      <QQQQF      )QQQQQg,        ]QQQW'      ]QQQQ?-'4Q[  \n" + "            _aaaaa   _jQQQQm.     )QQQQ_]QQP?'   -jQQQk       jQQQQw  ]7   \n" + "         .amQQQQQW.  ]QQQQ?WL,     4QQQQc `       ]QQQm       $QQQQQw 2(   \n" + "       _sQQQQQQf     -?QQQa WQ     ]QQQQQ,        ]QQQQ[      ]QQf?WQQ[    \n" + "       ]QQQHQQQf       ]QQQa'!    <QQP]QQ(      _wQW?QQ[     _mWP   4Q[    \n" + "        ]QQQQHQQ      _m@?$Q.    ]Q@F ]QQ(     jQP! ]QQ[    wQP'    ]QL,   \n" + "      swQP4QQ'4Q;    jQP` 3Q     j@'  -HQ[   _mQ?    VQ(   _Q@'     _$QL   \n" + "     -Qa, )QQaj@'   -4$a  )Qaa, 'Qma,   Qmaa  ~9L     Qmaa ]Q6,     J $QDb \n" + "    ***********************************************************************";

    protected static final String OPTION_HELP = "h";

    protected static final String OPTION_GUI = "gui";

    protected static final String OPTION_LOG_LEVEL = "log";

    /**
	 * Command line key in {@link Configuration}
	 */
    public static final String COMMAND_LINE = "de.kumpe.hadooptimizer.examples.CliExamplesRunner.commandLine";

    class StdInListener implements Runnable {

        @Override
        public void run() {
            final BufferedReader stdIn = new BufferedReader(new InputStreamReader(Channels.newInputStream(new FileInputStream(FileDescriptor.in).getChannel())));
            try {
                for (; ; ) {
                    System.out.println("Press q ENTER ro quit...");
                    final String line = stdIn.readLine();
                    if ("q".equalsIgnoreCase(line)) {
                        break;
                    }
                }
                executeShutdownHooks();
                System.exit(1);
            } catch (final ClosedByInterruptException e) {
            } catch (final IOException e) {
                log.error("Error reading stdin: ", e);
            }
        }
    }

    final class CloseClosableHook implements Runnable {

        private final Closeable out;

        private CloseClosableHook(final Closeable out) {
            this.out = out;
        }

        @Override
        public void run() {
            if (log.isInfoEnabled()) {
                log.info("Closing results.txt");
            }
            try {
                out.close();
            } catch (final IOException e) {
                log.error("Error closing Closable: ", e);
            }
        }
    }

    protected static final Log log = LogFactory.getLog(OptimizerExample.class);

    protected final Collection<Runnable> shutdownHooks = new CopyOnWriteArrayList<Runnable>();

    protected CommandLine commandLine;

    protected final String baseDir;

    protected PrintWriter logFile;

    public Example() {
        super(new Configuration());
        baseDir = String.format("%s-%tY%<tm%<td-%<tH%<tM%<tS", getClass().getName(), new Date());
    }

    protected Options createOptions() throws Exception {
        final Options options = new Options();
        final Option logLevelOption = new Option(OPTION_LOG_LEVEL, true, "sets the log-level of HadoOptimizer");
        logLevelOption.setArgName("all|trace|debug|info|warn|error|fatal|off");
        options.addOption(logLevelOption);
        final Option guiOption = new Option(OPTION_GUI, false, "use gui if available");
        options.addOption(guiOption);
        return options;
    }

    public void run(final String[] args) throws Exception {
        if (log.isInfoEnabled()) {
            log.info("\n" + BANNER_TEXT);
            log.info("Running " + getClass().getName() + " (" + getVersionInfo() + ")");
        }
        log.debug("Creating options...");
        final Options options = createOptions();
        if (log.isTraceEnabled()) {
            log.trace("Created options: " + options);
        }
        log.trace("Add help-option...");
        final Option helpOption = new Option(OPTION_HELP, "help", false, "pring help");
        options.addOption(helpOption);
        log.debug("Parsing options...");
        commandLine = new GenericOptionsParser(getConf(), options, args).getCommandLine();
        if (log.isDebugEnabled()) {
            log.debug("Remaining arguments: " + commandLine.getArgList());
        }
        if (commandLine.hasOption(OPTION_HELP)) {
            printHelp(options);
            return;
        }
        checkForUnrecognizedOption();
        log.debug("Processing command-line...");
        processCommandLine();
        openLogFile();
        storeArgument(args);
        final Thread stdInListener = new Thread(new StdInListener(), "StdInListener");
        stdInListener.start();
        try {
            log.debug("Executing example...");
            execute();
        } finally {
            stdInListener.interrupt();
            executeShutdownHooks();
        }
    }

    private void openLogFile() throws IOException {
        OutputStream outputStream = outputStream("results.txt");
        try {
            final Path s3logPath = new Path("s3://hadooptimizer/" + baseDir + "/results.txt");
            final FileSystem s3FileSystem = s3logPath.getFileSystem(getConf());
            final OutputStream s3OutputStream = s3FileSystem.create(s3logPath);
            outputStream = new TeeOutputStream(outputStream, s3OutputStream);
        } catch (final Exception e) {
            log.debug("Ignoring s3: " + e);
        }
        logFile = new PrintWriter(outputStream);
        shutdownHooks.add(new CloseClosableHook(logFile));
        logFile.print("# ");
        try {
            logFile.print(InetAddress.getLocalHost().getHostName());
        } catch (final UnknownHostException e) {
            logFile.print("unknown host");
        }
        logFile.print(" ");
        logFile.println(new Date());
    }

    private void executeShutdownHooks() {
        for (final Runnable shutdownHook : shutdownHooks) {
            shutdownHook.run();
        }
    }

    private void storeArgument(final String[] args) {
        final StringBuilder buffer = new StringBuilder(getClass().getName());
        for (final String arg : args) {
            buffer.append(' ');
            buffer.append(arg);
        }
        logFile.print("# ");
        final String argumentsString = buffer.toString();
        logFile.println(argumentsString);
        getConf().set(COMMAND_LINE, argumentsString);
    }

    private void checkForUnrecognizedOption() {
        @SuppressWarnings("unchecked") final List<String> remainingArgs = commandLine.getArgList();
        for (final String arg : remainingArgs) {
            if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Unrecognized option: " + arg);
            }
        }
    }

    protected abstract String getVersionInfo();

    protected abstract void execute() throws Exception;

    protected void processCommandLine() throws Exception {
        final String logLevel = commandLine.getOptionValue(OPTION_LOG_LEVEL);
        if (null != logLevel) {
            final Level level = Level.toLevel(logLevel);
            final Logger logger = Logger.getLogger("de.kumpe.hadooptimizer");
            logger.setLevel(level);
        }
    }

    private void printHelp(final Options options) {
        new HelpFormatter().printHelp(Integer.MAX_VALUE, "hadoop jar hadooptimizer.jar exampleClass [option...]", "\navailable options (some are hadoop's general options):", options, "\n$Id: Example.java 3999 2011-05-06 09:13:39Z baumbart $\n");
    }

    public OutputStream outputStream(final String filename) throws IOException {
        final Path path = new Path(baseDir, filename);
        final FileSystem fs = path.getFileSystem(getConf());
        return fs.create(path);
    }

    public InputStream intputStream(final String filename) throws IOException {
        final FileSystem fs = FileSystem.get(getConf());
        Path path = new Path(baseDir, filename);
        if (fs.exists(path)) {
            return fs.open(path);
        }
        path = new Path(getClass().getName(), filename);
        if (fs.exists(path)) {
            return fs.open(path);
        }
        return getClass().getResourceAsStream(getClass().getName() + "/" + filename);
    }
}
