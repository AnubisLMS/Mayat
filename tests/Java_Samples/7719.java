package randres.kindle.previewer;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import randres.kindle.previewer.data.EXTHeader;
import randres.kindle.previewer.data.ImageSection;
import randres.kindle.previewer.data.MOBIHeader;
import randres.kindle.previewer.data.PRCHeader;
import randres.kindle.previewer.data.PalmDocHeader;
import randres.kindle.previewer.data.Record;
import randres.kindle.previewer.huffmandec.DHDecompiler;
import randres.kindle.previewer.huffmandec.HuffmanDecoder;
import randres.kindle.previewer.util.LZ77;
import randres.kindle.previewer.util.MyRandomAccessFile;
import randres.kindle.previewer.util.NamingUtils;

public class MobiDecoder {

    private ReadableByteChannel channel;

    private boolean debug = false;

    private long filesize;

    private String title;

    private PRCHeader prcHeader;

    private PalmDocHeader palmDocHeader;

    private MOBIHeader mobiHeader;

    private EXTHeader extHeader;

    private List<ImageSection> imageSections;

    private File file;

    private HuffmanDecoder dec;

    public MobiDecoder(String filename) throws Exception {
        file = new File(filename);
        filesize = file.length();
        FileInputStream fin = new FileInputStream(file);
        channel = fin.getChannel();
        prcHeader = PRCHeader.read(channel);
        palmDocHeader = PalmDocHeader.read(channel);
        mobiHeader = MOBIHeader.read(channel);
        String encoding = mobiHeader.getEncoding();
        extHeader = EXTHeader.read(channel, encoding);
        channel.close();
        fin.close();
        if (palmDocHeader.getCompression() == 17480) {
            int recordOffset = mobiHeader.getHuffmanRecordOffset();
            int recordCount = mobiHeader.getHuffmanRecordCount();
            List<Record> list = new ArrayList<Record>();
            for (int i = 0; i < recordCount; i++) {
                Record record = prcHeader.getRecordList().get(recordOffset + i);
                list.add(record);
            }
            dec = new HuffmanDecoder(file, list, mobiHeader.getEncoding());
            dec.process();
        }
        imageSections = getImageSections();
        if (mobiHeader.getFullNameOffset() != -1) {
            int initialOffset = prcHeader.getRecordList().get(0).getRecordOffset();
            title = NamingUtils.getFullname(file, initialOffset, mobiHeader.getFullNameOffset(), mobiHeader.getFullNameLength(), encoding);
        }
    }

    public String getSection(int sectionNumber) {
        String sample = "";
        try {
            MyRandomAccessFile fileAccessor = new MyRandomAccessFile(file);
            Record record = prcHeader.getRecordList().get(sectionNumber);
            int recordLength = record.getRecordLength();
            int offset = record.getRecordOffset();
            byte[] section = new byte[recordLength + 1];
            fileAccessor.read(section, offset, recordLength + 1);
            String data = "Unsupported Compression Method ";
            if (palmDocHeader.getCompression() == 2) {
                int calculateBufferLength = LZ77.calculateBufferLength(section);
                byte[] decompressBuffer = LZ77.decompressBuffer(section, calculateBufferLength);
                data = new String(decompressBuffer, Charset.forName(mobiHeader.getEncoding()));
            } else if (dec != null) {
                data = DHDecompiler.unpack(section, dec);
            }
            fileAccessor.close();
            sample = data;
        } catch (Exception e) {
            sample = "Problems getting sample";
        }
        return sample;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(prcHeader);
        sb.append(String.format("Title: %s \n", title));
        sb.append(String.format("Author: %s \n", extHeader.getEXTH(EXTHeader.AUTHOR_KEY)));
        return sb.toString();
    }

    public PreviewInfo getPreviewInfo() {
        PreviewInfo info = new PreviewInfo();
        info.setTitle(title);
        info.setAuthor(extHeader.getEXTH(EXTHeader.AUTHOR_KEY));
        info.setResume(toHTMLString());
        Integer coverOffset = extHeader.getEXTHInteger(EXTHeader.COVER_OFFSET_KEY);
        if (coverOffset == null) {
            coverOffset = 0;
        }
        ImageSection imageSection = imageSections.get(coverOffset);
        info.setData(PreviewInfo.COVER_IMAGE_OFFSET, Integer.toString(imageSection.getStart()));
        info.setData(PreviewInfo.COVER_IMAGE_END, Integer.toString(imageSection.getEnd()));
        info.setData(PreviewInfo.FILE_SIZE, Long.toString(filesize));
        info.setData(PreviewInfo.SAMPLE, getSample());
        return info;
    }

    private String getSample() {
        String data = "";
        int randomSection = new Double(Math.random() * mobiHeader.getFirstImageIndex()).intValue();
        try {
            data = NamingUtils.extractParagraph(getSection(randomSection));
        } catch (Exception e) {
            data = "Sample not extractable";
        }
        return data;
    }

    private List<ImageSection> getImageSections() {
        List<ImageSection> images = new ArrayList<ImageSection>();
        int currentRecord = mobiHeader.getFirstImageIndex();
        List<Record> recordList = prcHeader.getRecordList();
        int prev = -1;
        for (int i = currentRecord; i < recordList.size(); i++) {
            Record record = recordList.get(i);
            int current = record.getRecordOffset();
            if (prev != -1) {
                ImageSection im = new ImageSection(prev, current);
                images.add(im);
            }
            prev = current;
        }
        return images;
    }

    public String toHTMLString() {
        StringBuffer sb = new StringBuffer();
        sb.append(prcHeader.toHTMLString());
        sb.append(mobiHeader.toHTMLString());
        sb.append(String.format("<b>Title:</b>  %s  <br>", title));
        sb.append(String.format("<b>Author:</b>  %s  <br>", extHeader.getEXTH(EXTHeader.AUTHOR_KEY)));
        return sb.toString();
    }

    public void debug(String mssg) {
        if (debug) {
            System.out.println(mssg);
        }
    }

    public static void main(String[] args) {
        String filename = "/home/randres/Escritorio/kindle/books/Rice, Anne - La voz del diablo.prc";
        try {
            MobiDecoder preview = new MobiDecoder(filename);
            System.out.println(preview.getSample());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getMaxTextSection() {
        return mobiHeader.getFirstImageIndex() - 1;
    }

    public String getEncoding() {
        return mobiHeader.getEncoding();
    }
}
