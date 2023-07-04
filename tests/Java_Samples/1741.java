package com.prolix.editor.resourcemanager.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import com.prolix.editor.resourcemanager.exceptions.GLMRessourceManagerException;
import com.prolix.editor.resourcemanager.exceptions.GLMRessourceZIPException;

/**
 * Manages the Content Zip File (zip and unzip etc.)
 * 
 * @author Susanne Neumann, Stefan Zander, Philipp Prenner
 */
public class ZipManager {

    private static final String tempFolderPrefName = "glmFolder";

    private static int count = 0;

    private String zipFile;

    private String tempDirectory;

    private RessourceManager manager;

    protected ZipManager(String zipFile, RessourceManager manager) throws GLMRessourceManagerException {
        super();
        this.zipFile = zipFile;
        this.manager = manager;
        createTempFolder();
    }

    protected ZipManager(String zipFile, String tempDirectory, RessourceManager manager) {
        super();
        this.manager = manager;
        this.zipFile = zipFile;
        this.tempDirectory = tempDirectory;
    }

    protected void load() throws GLMRessourceManagerException {
        File zip = new File(zipFile);
        if (zip.exists()) copyFromZip(zip); else throw new GLMRessourceZIPException(6);
    }

    protected void create() throws GLMRessourceManagerException {
        createTempFolder();
    }

    private void copyFromZip(File zipFile) throws GLMRessourceManagerException {
        if (zipFile == null) throw new GLMRessourceZIPException(1);
        if (!zipFile.exists()) throw new GLMRessourceZIPException(2);
        int len = 0;
        byte[] buffer = ContentManager.getDefaultBuffer();
        try {
            ZipInputStream zip_in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            ZipEntry zipEntry;
            File rootDir = null;
            while ((zipEntry = zip_in.getNextEntry()) != null) {
                File destFile = new File(tempDirectory, zipEntry.getName());
                if (rootDir == null) rootDir = destFile.getParentFile();
                if (!zipEntry.isDirectory() && destFile.getParentFile().equals(rootDir)) {
                    if (!zipEntry.getName().equals(ContentManager.IMS_MANIFEST_FILENAME)) {
                        FileOutputStream file_out = new FileOutputStream(new File(tempDirectory, zipEntry.getName()));
                        while ((len = zip_in.read(buffer)) > 0) file_out.write(buffer, 0, len);
                        file_out.flush();
                        file_out.close();
                    }
                }
            }
            zip_in.close();
        } catch (Exception e) {
            throw new GLMRessourceZIPException(3);
        }
    }

    private void cleanTempDirectory() throws GLMRessourceManagerException {
        String tempFolder = ContentManager.getTempFolder();
        File cleanIt = new File(tempFolder);
        File[] list = cleanIt.listFiles();
        for (int i = 0; i < list.length; i++) {
            if (list[i].getName().contains(tempFolderPrefName)) FileManager.deleteDirectory(list[i]);
        }
    }

    private void createTempFolder() throws GLMRessourceManagerException {
        if (tempDirectory != null) return;
        if (count == 0) cleanTempDirectory();
        tempDirectory = ContentManager.getTempFolder() + File.separator + tempFolderPrefName + (count++);
        try {
            FileManager.createDirectory(tempDirectory);
        } catch (Exception e) {
            throw new GLMRessourceZIPException(4);
        }
    }

    protected String getTempDirectory() {
        return tempDirectory;
    }

    protected String getZipFileName() {
        return zipFile;
    }

    protected void save(boolean export) throws GLMRessourceManagerException {
        try {
            FileManager.deleteAbspolutFile(zipFile);
        } catch (GLMRessourceManagerException e) {
        }
        copyFromTemp(export);
    }

    private void copyFromTemp(boolean export) throws GLMRessourceManagerException {
        try {
            ZipOutputStream zip_out = new ZipOutputStream(new FileOutputStream(new File(zipFile)));
            zip_out.setMethod(ZipOutputStream.DEFLATED);
            copyFromRootTmpDirectory(zip_out, export);
            zip_out.close();
        } catch (Exception e) {
            throw new GLMRessourceZIPException(5);
        }
    }

    private void copyFromRootTmpDirectory(ZipOutputStream zipOut, boolean export) throws Exception {
        File dir = new File(tempDirectory);
        Iterator it = new FileSyncManager(manager).getUsedFileList(export).iterator();
        while (it.hasNext()) {
            File file = new File(dir, it.next().toString());
            copyFromFile(zipOut, file, "");
        }
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (file.isDirectory()) copyFromTmpDirectory(zipOut, file, file.getName() + File.separator);
        }
    }

    /**
	 * won't create folders which contain no files
	 * 
	 * @param zipOut
	 * @param dir
	 * @param root
	 * @throws Exception
	 */
    private void copyFromTmpDirectory(ZipOutputStream zipOut, File dir, String root) throws Exception {
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (!file.isDirectory()) copyFromFile(zipOut, file, root); else copyFromTmpDirectory(zipOut, file, root + file.getName() + File.separator);
        }
    }

    private void copyFromFile(ZipOutputStream zipOut, File file, String root) throws Exception {
        int len = 0;
        byte[] buffer = ContentManager.getDefaultBuffer();
        BufferedInputStream buff_inf;
        buff_inf = new BufferedInputStream(new FileInputStream(file), buffer.length);
        zipOut.putNextEntry(new ZipEntry(root + file.getName()));
        while ((len = buff_inf.read(buffer)) > 0) zipOut.write(buffer, 0, len);
        buff_inf.close();
    }
}
