package com.bitgate.util.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.zip.GZIPOutputStream;
import org.w3c.dom.Node;
import com.bitgate.server.Server;
import com.bitgate.util.archive.Archive;
import com.bitgate.util.node.NodeUtil;
import com.bitgate.util.scheduler.SchedulerInterface;
import com.sun.org.apache.xpath.internal.XPathAPI;

/**
 * The debug logging class.  We could use log4j, but this is much more lightweight, and much easier to use.  It is not
 * intended to be lightweight in the sense that it uses less calls to handle the debugging.  It was intended to be a
 * single all-inclusive class that handles the output of debugging.
 * <p/>
 * It would have been ideal to write a better way to display debugging information to both the desktop and file-based
 * debugging, but this was not a luxury I was granted with Java 1.4 and below.  Especially for the fact that I could
 * not output mission-critical information on a system crash.  This can only be implemented with Java 1.5 and beyond.
 * Therefore, this code is less efficient than it can be.
 * <p/>
 * It would be advisable to make a property that allows for the debugging output to be stored up for a certain amount
 * of time, then output to the log in one large write.  But this, again, makes it impossible to log mission-critical
 * errors - particularly those that cause system crashes.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/debug/Debug.java#47 $
 */
public class Debug {

    private static File logfile;

    private static final Debug debug = new Debug();

    private static PrintStream ps;

    private static String logMethod, logFile;

    private static String lastMessage;

    private static boolean useFile, beQuiet, calltrace, enabled, isFile, registeredSchedule;

    private static int logValue;

    private static int timesRepeated;

    private static int rotateTimeout, rotateDays;

    private static String rotateDest, rotateArchive;

    private static boolean rotateCompress, rotateDelete;

    /**
     * This is the constructor.
     */
    public Debug() {
        String enabledString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.service']/property[@type='engine.debug']/@value");
        enabled = true;
        isFile = false;
        registeredSchedule = false;
        if (enabledString != null && enabledString.equalsIgnoreCase("disabled")) {
            enabled = false;
        }
        reconfigureDebug();
    }

    private static void reconfigureDebug() {
        useFile = false;
        logValue = 0;
        String methodString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.method']/@value");
        String levelString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.level']/@value");
        String quietString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.quiet']/@value");
        String fileString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.file']/@value");
        String filemodeString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.filemode']/@value");
        String calltraceString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.calltrace']/@value");
        String rotateTimeoutString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.rotatetimeout']/@value");
        String rotateDestString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.rotatedest']/@value");
        String rotateCompressString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.rotatecompress']/@value");
        String rotateDaysString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.rotatedays']/@value");
        String rotateArchiveString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.rotatearchive']/@value");
        String rotateDeleteString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.debug']/property[@type='engine.rotatedelete']/@value");
        String dirName = ".";
        if (rotateTimeoutString != null) {
            rotateTimeout = Integer.parseInt(rotateTimeoutString);
        }
        if (rotateDestString != null) {
            rotateDest = rotateDestString;
        }
        if (rotateCompressString != null && rotateCompressString.equalsIgnoreCase("true")) {
            rotateCompress = true;
        }
        if (rotateDaysString != null) {
            rotateDays = Integer.parseInt(rotateDaysString);
        }
        if (rotateArchiveString != null) {
            rotateArchive = rotateArchiveString;
        }
        if (rotateDeleteString != null && rotateDeleteString.equalsIgnoreCase("true")) {
            rotateDelete = true;
        }
        if (fileString != null && fileString.indexOf("/") != -1) {
            dirName = fileString.substring(0, fileString.lastIndexOf("/"));
            (new File(dirName)).mkdirs();
        }
        if (methodString != null) {
            logMethod = methodString;
        } else {
            logMethod = "file";
        }
        if (levelString != null) {
            logValue = Integer.parseInt(levelString);
        } else {
            logValue = 0;
        }
        if (calltraceString != null && calltraceString.equalsIgnoreCase("true")) {
            calltrace = true;
        } else {
            calltrace = false;
        }
        if (logMethod == null) {
            logMethod = "file";
        }
        if (quietString != null) {
            if (quietString.equalsIgnoreCase("true")) {
                beQuiet = true;
            }
        }
        if (logMethod != null) {
            if (logMethod.equalsIgnoreCase("file")) {
                if (fileString != null) {
                    logFile = fileString;
                } else {
                    logFile = "log.txt";
                }
                useFile = true;
            }
        } else {
            System.err.println("*** A debugging method (debug.method) is required in properties file!");
            System.err.println("*** Please refer to configuration documentation.");
            System.exit(-1);
        }
        timesRepeated = 0;
        lastMessage = null;
        if (useFile) {
            logfile = new File(logFile);
            try {
                if (filemodeString != null && filemodeString.equalsIgnoreCase("append")) {
                    ps = new PrintStream(new FileOutputStream(logfile, true));
                } else {
                    ps = new PrintStream(new FileOutputStream(logfile));
                }
                isFile = true;
                Calendar calendar = new GregorianCalendar();
                Date date = calendar.getTime();
                DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                ps.println();
                ps.println("--- Log file opened " + format1.format(date) + " ---");
            } catch (FileNotFoundException e) {
                System.out.println("Debug: Unable to instantiate debugger: " + e.getMessage());
                System.exit(-1);
            } catch (Exception e) {
                System.out.println("Debug: Unable to instantiate debugger - internal error: " + e.getMessage());
                System.exit(-1);
            }
        }
        if (!registeredSchedule) {
            registeredSchedule = true;
            if (Server.getScheduler() != null) {
                Server.getScheduler().register("Log File Rotator for '" + logFile + "'", new SchedulerInterface() {

                    public int getScheduleRate() {
                        if (rotateTimeout != 0) {
                            return rotateTimeout / 10;
                        }
                        return 0;
                    }

                    public void handle() {
                        FileChannel srcChannel, destChannel;
                        String destOutFile = logFile + "." + System.currentTimeMillis();
                        String destOutFileCompressed = logFile + "." + System.currentTimeMillis() + ".gz";
                        if (rotateDest != null) {
                            (new File(rotateDest)).mkdirs();
                            if (destOutFile.indexOf("/") != -1) {
                                destOutFile = rotateDest + "/" + destOutFile.substring(destOutFile.lastIndexOf("/") + 1);
                            }
                            if (destOutFileCompressed.indexOf("/") != -1) {
                                destOutFileCompressed = rotateDest + "/" + destOutFileCompressed.substring(destOutFileCompressed.lastIndexOf("/") + 1);
                            }
                        }
                        if (rotateCompress) {
                            try {
                                GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(destOutFileCompressed));
                                FileInputStream in = new FileInputStream(logFile);
                                byte buf[] = new byte[1024];
                                int len;
                                while ((len = in.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                                in.close();
                                out.finish();
                                out.close();
                                buf = null;
                                in = null;
                                out = null;
                                Debug.debug("Rotated log file '" + logFile + "' to '" + destOutFileCompressed + "'");
                            } catch (Exception e) {
                                Debug.debug("Unable to rotate log file '" + logFile + "': " + e);
                            }
                        } else {
                            try {
                                srcChannel = new FileInputStream(logFile).getChannel();
                            } catch (IOException e) {
                                Debug.debug("Unable to read log file '" + logFile + "': " + e.getMessage());
                                return;
                            }
                            try {
                                destChannel = new FileOutputStream(destOutFile).getChannel();
                            } catch (IOException e) {
                                Debug.debug("Unable to rotate log file to '" + destOutFile + "': " + e.getMessage());
                                return;
                            }
                            try {
                                destChannel.transferFrom(srcChannel, 0, srcChannel.size());
                                srcChannel.close();
                                destChannel.close();
                                srcChannel = null;
                                destChannel = null;
                            } catch (IOException e) {
                                Debug.debug("Unable to copy data for file rotation: " + e.getMessage());
                                return;
                            }
                            Debug.debug("Rotated log file '" + logFile + "' to '" + destOutFile + "'");
                        }
                        if (rotateDelete && isFile) {
                            try {
                                ps.close();
                            } catch (Exception e) {
                            }
                            isFile = false;
                            ps = null;
                            (new File(logFile)).delete();
                            reconfigureDebug();
                        }
                        if (rotateDest != null) {
                            long comparisonTime = rotateDays * (60 * 60 * 24 * 1000);
                            long currentTime = System.currentTimeMillis();
                            File fileList[] = (new File(rotateDest)).listFiles();
                            DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                            java.util.Date date = new java.util.Date(currentTime);
                            String archiveFile = format1.format(date).toString() + ".zip";
                            if (rotateArchive != null) {
                                archiveFile = rotateArchive + "/" + archiveFile;
                                (new File(rotateArchive)).mkdirs();
                            }
                            Archive archive = new Archive(archiveFile);
                            for (int i = 0; i < fileList.length; i++) {
                                String currentFilename = fileList[i].getName();
                                long timeDifference = (currentTime - fileList[i].lastModified());
                                if ((rotateCompress && currentFilename.endsWith(".gz")) || (!rotateCompress && currentFilename.indexOf(logFile + ".") != -1)) {
                                    if (rotateDest != null) {
                                        currentFilename = rotateDest + "/" + currentFilename;
                                    }
                                    if (timeDifference > comparisonTime) {
                                        archive.addFile(fileList[i].getName(), currentFilename);
                                        fileList[i].delete();
                                    }
                                }
                            }
                            archive = null;
                            fileList = null;
                            format1 = null;
                            date = null;
                        }
                    }

                    public String identString() {
                        return "Debug Rotator for logs";
                    }
                });
            }
        }
    }

    private static String getCallingFunction(int stackPosition) {
        StackTraceElement ste[] = (new Throwable()).getStackTrace();
        if (stackPosition > ste.length) {
            stackPosition = ste.length;
        }
        try {
            String className = ste[stackPosition].getClassName();
            int lineNumber = ste[stackPosition].getLineNumber();
            if (className.startsWith("nuklees")) {
                className = className.substring(className.indexOf(".") + 1);
            }
            if (lineNumber == -1) {
                return new String(className + "(" + ste[stackPosition].getMethodName() + "): ");
            } else {
                return new String(className + "(" + ste[stackPosition].getMethodName() + ":" + lineNumber + "): ");
            }
        } catch (Exception e) {
            return new String("STACK_UNAVAILABLE: ");
        }
    }

    /**
     * This function logs to the NOTICE logging facility.  Use this for notificational messages.
     *
     * @param @str String to send to debugging.
     */
    public static void notice(String str) {
        logMsg(1, str);
    }

    /**
     * This function logs a WARNING to the NOTICE logging facility.  Use this for notificational messages.
     *
     * @param @str String to send to debugging.
     */
    public static void warn(String str) {
        notice("*** WARNING: " + str);
    }

    /**
     * This function logs to the INFO logging facility.  Use this for informational messages.
     *
     * @param @str String to send to debugging.
     */
    public static void info(String str) {
        if (!enabled) {
            return;
        }
        logMsg(2, str);
    }

    /**
     * This function logs to the DEBUG logging facility.  Use this for debugging messages.
     *
     * @param str String to send to debugging.
     */
    public static void log(String str) {
        if (!enabled) {
            return;
        }
        if (!calltrace) {
            logMsg(3, str);
            return;
        }
        logMsg(3, getCallingFunction(2) + str);
    }

    /**
     * This function logs to the DEBUG logging facility.  Use this for debugging messages.
     *
     * @param str String to send to debugging.
     */
    public static void debug(String str) {
        if (!enabled) {
            return;
        }
        if (!calltrace) {
            logMsg(3, str);
            return;
        }
        logMsg(3, getCallingFunction(2) + str);
    }

    /**
     * This function logs to the DEBUG logging facility.  Use this for debugging messages.
     *
     * @param str String to send to debugging.
     */
    public static void raw(String str) {
        if (!enabled) {
            return;
        }
        if (!calltrace) {
            logMsg(3, str);
            return;
        }
        logMsg(3, str);
    }

    /**
     * This function logs to the DEBUG logging facility.  Use this for debugging messages.
     *
     * @param str String to send to debugging.
     */
    public static void user(String ident, String str) {
        if (!enabled) {
            return;
        }
        if (!calltrace) {
            logMsg(3, str);
            return;
        }
        logMsg(3, getCallingFunction(2) + str);
        return;
    }

    /**
     * This function logs informative messages during debugging.  This is an undocumented logging level.
     *
     * @param str String to send to debugging.
     */
    public static void inform(String str) {
        if (!enabled) {
            return;
        }
        if (!calltrace) {
            logMsg(4, str);
            return;
        }
        logMsg(4, getCallingFunction(2) + str);
    }

    private static void logMsg(int level, String str) {
        String msg;
        if (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }
        if (lastMessage != null && str.equals(lastMessage)) {
            timesRepeated++;
            return;
        }
        if (timesRepeated > 0) {
            msg = new String("... last message repeated " + timesRepeated + " time(s) ...\n" + str);
            lastMessage = null;
            timesRepeated = 0;
        } else {
            msg = str;
        }
        if (level == 0) {
            System.err.println(str);
            if (useFile) {
                if (debug.ps != null) {
                    debug.ps.println(str);
                }
            }
            return;
        }
        lastMessage = str;
        if (logValue >= level) {
            if (useFile) {
                if (debug.ps != null) {
                    debug.ps.println(msg);
                }
            }
            if (!beQuiet) {
                System.out.println(msg);
            }
        }
    }

    /**
     * This function logs to the critical logging facility.  Critical messages will always be logged, regardless of
     * whether or not logging to files is done.
     *
     * @param str String to send to debugging.
     */
    public static void crit(String str) {
        if (!enabled) {
            System.err.println("*** CRITICAL: " + str);
            return;
        }
        logMsg(0, "*** CRITICAL: " + str);
    }

    /**
     * This function shows a banner message.
     *
     * @param str String to send to debugging.
     */
    public static void banner(String str) {
        logMsg(0, str);
    }

    /**
     * This function retrieves an entire stack trace from a Throwable object.  This way, the code can throw a workable
     * set of information when a critical error occurs.
     *
     * @return StringBuffer containing the entire stack trace.
     */
    public static StringBuffer getStackTrace(Throwable obj) {
        StackTraceElement ste[] = obj.getStackTrace();
        StringBuffer buf = new StringBuffer();
        if (obj.getMessage() == null) {
            buf.append(obj);
            buf.append("\n\n");
        } else {
            buf.append(obj.getMessage());
            buf.append("\n\n");
        }
        for (int i = 0; i < ste.length; i++) {
            String classname = ste[i].getClassName();
            String filename = ste[i].getFileName();
            int linenumber = ste[i].getLineNumber();
            String methodname = ste[i].getMethodName();
            boolean nativemethod = ste[i].isNativeMethod();
            buf.append(classname);
            buf.append('.');
            buf.append(methodname);
            buf.append("(");
            if (filename != null) {
                buf.append(filename);
            } else {
                buf.append("Unknown.java");
            }
            if (linenumber > 0) {
                buf.append(":");
                buf.append(linenumber);
            }
            buf.append(")");
            if (nativemethod) {
                buf.append(" [native call]");
            }
            buf.append("\n");
        }
        return buf;
    }
}
