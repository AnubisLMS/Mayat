package org.synerr.apps.iTunesAlbumArtExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

/**
 * This is the main class to extract the album art from iTunes.
 * It will recurse down through the iTunes album art directory
 * and create a mirror version with the files translated to
 * their native image formats (JPG and PNG).
 *
 * The image is a native image with 493 bytes of iTunes data
 * prepended. This object determines the native type, copies the
 * file to the mirror directory minus the first 493 bytes, and
 * gives it the proper file extension.
 *
 * @author Lucas McGregor
 */
public class Extractor extends ExtractionProgressBroadcaster {

    private static final boolean VERBOSE = false;

    private File artDir = null;

    private File targetDir = null;

    private FileTypeDeterminer fileTypeDeterminer = null;

    public static void main(String args[]) throws Exception {
        Extractor e = new Extractor();
        e.setArtDir((new File(args[0])));
        e.setTargetDir((new File(args[1])));
        e.extract();
    }

    /** Creates a new instance of Extractor */
    public Extractor() {
        super();
        fileTypeDeterminer = new FileTypeDeterminer();
    }

    public File getArtDir() {
        return artDir;
    }

    /**
     * Set the iTunes art directory. This is the directory
     * that it will mirror
     */
    public void setArtDir(File artDir) {
        this.artDir = artDir;
    }

    public File getTargetDir() {
        return targetDir;
    }

    /**
     * Set the directory for this to mirror into
     */
    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    /**
     * This method will take all the album art from the source dir
     * and extract it into a target dir.
     * @return the number of album art images extracted.
     */
    public int extract() throws Exception {
        int count = 0;
        if (VERBOSE) System.out.println("IAAE:Extractr.extract: getting ready to extract " + getArtDir().toString());
        ITCFileFilter iff = new ITCFileFilter();
        RecursiveFileIterator rfi = new RecursiveFileIterator(getArtDir(), iff);
        FileTypeDeterminer ftd = new FileTypeDeterminer();
        File artFile = null;
        File targetFile = null;
        broadcastStart();
        while (rfi.hasMoreElements()) {
            artFile = (File) rfi.nextElement();
            targetFile = getTargetFile(artFile);
            if (VERBOSE) System.out.println("IAAE:Extractr.extract: working ont " + artFile.toString());
            BufferedInputStream in = null;
            BufferedOutputStream out = null;
            try {
                in = new BufferedInputStream((new FileInputStream(artFile)));
                out = new BufferedOutputStream((new FileOutputStream(targetFile)));
                byte[] buffer = new byte[10240];
                int read = 0;
                int total = 0;
                read = in.read(buffer);
                while (read != -1) {
                    if ((total <= 491) && (read > 491)) {
                        out.write(buffer, 492, (read - 492));
                    } else if ((total <= 491) && (read <= 491)) {
                    } else {
                        out.write(buffer, 0, read);
                    }
                    total = total + read;
                    read = in.read(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
                broadcastFail();
            } finally {
                in.close();
                out.close();
            }
            broadcastSuccess();
            count++;
        }
        broadcastDone();
        return count;
    }

    /**
     * Return the file to write to
     */
    private File getTargetFile(File artFile) throws Exception {
        File tFile = artFile;
        File mFile = null;
        String path = null;
        while (!tFile.equals(getArtDir())) {
            if (path != null) path = tFile.getName() + File.separator + path; else if (tFile.isDirectory()) path = tFile.getName();
            tFile = tFile.getParentFile();
        }
        mFile = new File(getTargetDir(), path);
        if (!mFile.exists()) mFile.mkdirs();
        int type = fileTypeDeterminer.determineType(artFile);
        String newName = null;
        if (type == fileTypeDeterminer.TYPE__JPG) {
            newName = artFile.getName().substring(0, (artFile.getName().length() - 4)) + ".jpg";
        } else if (type == fileTypeDeterminer.TYPE__PNG) {
            newName = artFile.getName().substring(0, (artFile.getName().length() - 4)) + ".png";
        } else {
            newName = artFile.getName().substring(0, (artFile.getName().length() - 4));
        }
        return (new File(mFile, newName));
    }
}
