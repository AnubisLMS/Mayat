package org.easyway.tiles.generic;

import static org.easyway.tiles.generic.GraphicBoard.imageGrid;
import static org.easyway.tiles.generic.GraphicBoard.tileHeight;
import static org.easyway.tiles.generic.GraphicBoard.tileWidth;
import static org.easyway.tiles.generic.GraphicBoard.worldHeight;
import static org.easyway.tiles.generic.GraphicBoard.worldWidth;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;
import javax.swing.JPanel;

public class OptimizedSaver {

    public static Vector<CustomImage> images;

    public static GraphicBoard graphicBoard;

    public static JPanel textureArea;

    public static int getNumberOfImages() {
        int x, y;
        CustomImage timage;
        images = new Vector<CustomImage>(20);
        for (x = 0; x < worldWidth; ++x) {
            for (y = 0; y < worldHeight; ++y) {
                if ((timage = imageGrid[x][y]) != null) {
                    if (!images.contains(timage)) {
                        System.out.println("TYPE: " + timage.type);
                        images.add(timage);
                    }
                }
            }
        }
        return images.size();
    }

    public static void saveTextures(String directory) {
        String path;
        for (CustomImage image : images) {
            try {
                path = image.filename.substring(image.filename.lastIndexOf('\\'));
            } catch (StringIndexOutOfBoundsException e) {
                path = image.filename.substring(image.filename.lastIndexOf('/'));
            }
            if (image.filename.charAt(0) == '\\') {
                image.filename = image.filename.replace('\\', '/');
            }
            try {
                copy(new File(image.filename), new File(directory + path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Fast & simple file copy. */
    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    public static void saveWorld(String path, String filename, String prefix) {
        int num = getNumberOfImages();
        if (num <= 0) {
            System.err.println("the graphic board is empty?..");
            new Exception();
            return;
        }
        if (prefix == null) prefix = "";
        FileOutputStream outStream;
        ObjectOutputStream objStream;
        try {
            outStream = new FileOutputStream(path + filename);
            objStream = new ObjectOutputStream(outStream);
            objStream.writeInt(GraphicBoard.worldWidth);
            objStream.writeInt(GraphicBoard.worldHeight);
            objStream.writeInt(GraphicBoard.tileWidth);
            objStream.writeInt(GraphicBoard.tileHeight);
            objStream.writeInt(num);
            String ifilename;
            for (int i = 0; i < num; ++i) {
                ifilename = new File(images.get(i).filename).getName();
                ifilename = prefix + ifilename;
                objStream.writeObject(ifilename);
                objStream.writeObject(images.get(i).type);
            }
            for (int i = 0; i < GraphicBoard.worldWidth; ++i) for (int j = 0; j < GraphicBoard.worldHeight; ++j) {
                if (imageGrid[i][j] != null) {
                    objStream.writeByte(images.indexOf(imageGrid[i][j]));
                } else {
                    objStream.writeByte((byte) -1);
                }
            }
            objStream.close();
            outStream.close();
            System.out.println("Successfully saved at " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadWorld(String filename) {
        filename = filename.replace('\\', '/');
        String path = filename.substring(0, filename.lastIndexOf('/'));
        FileInputStream inStream;
        ObjectInputStream objStream;
        try {
            inStream = new FileInputStream(filename);
            objStream = new ObjectInputStream(inStream);
            worldWidth = objStream.readInt();
            worldHeight = objStream.readInt();
            tileWidth = objStream.readInt();
            tileHeight = objStream.readInt();
            graphicBoard.changeDimension(worldWidth, worldHeight, tileWidth, tileHeight);
            int num = objStream.readInt();
            images = new Vector<CustomImage>(num);
            String name, type;
            boolean finded = false;
            CustomImage timage;
            for (int i = 0; i < num; ++i) {
                name = (String) objStream.readObject();
                type = (String) objStream.readObject();
                name = name.replace('/', '\\');
                images.add(timage = new CustomImage(path + '\\' + name));
                timage.type = type;
                finded = false;
                for (int j = 0; j < textureArea.getComponentCount(); ++j) {
                    if (((TextureButton) textureArea.getComponent(j)).equals(timage)) {
                        finded = true;
                        break;
                    }
                }
                if (!finded) {
                    textureArea.add(new TextureButton(timage));
                }
            }
            textureArea.revalidate();
            textureArea.repaint();
            int number;
            for (int i = 0; i < GraphicBoard.worldWidth; ++i) for (int j = 0; j < GraphicBoard.worldHeight; ++j) {
                number = (byte) objStream.readByte();
                if (number != (byte) -1) {
                    imageGrid[i][j] = images.get(number);
                }
            }
            objStream.close();
            inStream.close();
            System.out.println("Successfully saved at " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
