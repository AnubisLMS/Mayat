package uk.ac.gla.terrier.compression;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import uk.ac.gla.terrier.utility.io.RandomDataOutput;

/**
 * This class encapsulates a random access file and provides
 * the functionalities to write highly compressed data structures, eg binary encoded, unary encoded and gamma encoded
 * integers greater than zero, as well as specifying their offset in the file. It
 * is employed by the DirectIndex and the InvertedIndex classes.
 * The sequence of method calls to write a sequence of gamma encoded
 * and unary encoded numbers is:
 * <code>
 *	file.writeReset();<br>
 *	long startByte1 = file.getByteOffset();<br>
 *  byte startBit1 = file.getBitOffset();<br>
 *  file.writeGamma(20000);<br>
 *  file.writeUnary(2);<br>
 *  file.writeGamma(35000);<br>
 *  file.writeUnary(1);<br>
 *  file.writeGamma(3);<br>
 *  file.writeUnary(2);<br>
 *  file.writeFlush();<br>
 *  long endByte1 = file.getByteOffset();<br>
 *  byte endBit1 = file.getBitOffset();<br>
 *  if (endBit1 == 0 &amp;&amp; endByte1 &gt; 0) {<br>
 *      endBit1 = 7;<br>
 *      endByte1--;<br>
 *  }<br>
 * </code>
 * while for reading a sequence of numbers the sequence of calls is:<code>
 *  file.readReset((long) startByte1, (byte) startBit1, (long) endByte1, (byte) endBit1);<br>
 *  int gamma = file.readGamma();
 *	int unary = file.readUnary();</code>
 *  
 * @author Gianni Amati, Vassilis Plachouras, Douglas Johnson
 * @version $Revision: 1.4 $
 */
public class OldBitFile extends BitFile {

    /** The logger used */
    private static Logger logger = Logger.getRootLogger();

    /** A constant for saving time during writeGamma.*/
    protected static final double LOG_E_2 = Math.log(2.0d);

    /** Default file mode access for a BitFile object. 
	  * Currently "<tt>rw</tt>". */
    protected static final String DEFAULT_FILE_MODE = "rw";

    /** A flag set to true if the last byte writen in the file is incomplete.*/
    protected boolean isLastByteIncomplete;

    /** The current offset in the file in bits in the last byte.*/
    protected byte bitOffset;

    /** A buffer for storing processed bytes.*/
    protected ByteArrayOutputStream outBuffer;

    /** A byte long buffer.*/
    protected byte buffer;

    /** The number of bits read already from the buffer.*/
    protected int readBitOffset;

    /** A constuctor for an instance of this class, given an abstract file.
 	  * File access mode is DEFAULT_FILE_MODE. */
    public OldBitFile(File file) {
        super(file);
        bitOffset = 0;
        byteOffset = 0;
    }

    /** A constuctor for an instance of this class, given an abstract file.*/
    public OldBitFile(File file, String access) {
        super(file, access);
        bitOffset = 0;
        byteOffset = 0;
    }

    /** A constuctor for an instance of this class. File access mode is DEFAULT_FILE_MODE */
    public OldBitFile(String filename) {
        super(filename);
        bitOffset = 0;
        byteOffset = 0;
    }

    /** A constuctor for an instance of this class.*/
    public OldBitFile(String filename, String access) {
        super(filename, access);
    }

    /**
	 * Closes the random access file.
	 */
    public void close() {
        try {
            file.close();
        } catch (IOException ioe) {
            logger.error("Input/Output exception while closing the BitFile. Stack trace follows", ioe);
        }
    }

    /**
	 * Returns the bit offset of the last current byte in the buffer. 
	 * This offset corresponds to the position where the
	 * next bit is going to be written.
	 * @return the bit offset of the current byte in the buffer.
	 */
    public byte getBitOffset() {
        return bitOffset;
    }

    /** 
	 * Returns the byte offset in the buffer. This offset corresponds
	 * to the byte in which the next bit is going to be written or read from.
	 * @return the byte offset in the buffer.
	 */
    public long getByteOffset() {
        return byteOffset;
    }

    /**
	 * Reads and decodes a gamma encoded integer from the already read buffer.
	 * @return the decoded integer
	 */
    public int readGamma() {
        final int unaryPart = readUnary();
        int binaryPart = 0;
        for (int i = 0; i < unaryPart - 1; i++) {
            if ((inBuffer[(int) byteOffset] & (1 << (bitOffset))) != 0) binaryPart = binaryPart + (1 << i);
            readBitOffset++;
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
            }
        }
        return binaryPart + (1 << (unaryPart - 1));
    }

    /**
     * Aligns the stream to the next byte
     * @throws IOException if an I/O error occurs
     */
    public void align() {
        if ((bitOffset & 7) == 0) return;
        bitOffset = 0;
        byteOffset++;
    }

    /**
	 * Reads from the file a specific number of bytes and after this
	 * call, a sequence of read calls may follow. The offsets given 
	 * as arguments are inclusive. For example, if we call this method
	 * with arguments 0, 2, 1, 7, it will read in a buffer the contents 
	 * of the underlying file from the third bit of the first byte to the 
	 * last bit of the second byte.
	 * @param startByteOffset the starting byte to read from
	 * @param startBitOffset the bit offset in the starting byte
	 * @param endByteOffset the ending byte 
	 * @param endBitOffset the bit offset in the ending byte. 
	 *        This bit is the last bit of this entry.
	 */
    public BitIn readReset(long startByteOffset, byte startBitOffset, long endByteOffset, byte endBitOffset) {
        try {
            file.seek(startByteOffset);
            inBuffer = new byte[(int) (endByteOffset - startByteOffset + 1)];
            file.readFully(inBuffer);
            readBits = (inBuffer.length * 8) - startBitOffset - (8 - endBitOffset) + startBitOffset;
            byteOffset = 0;
            readBitOffset = startBitOffset;
            bitOffset = startBitOffset;
        } catch (IOException ioe) {
            logger.error("Input/Output exception while reading from a random access file. Stack trace follows", ioe);
        }
        return this;
    }

    /**
	 * Reads a unary integer from the already read buffer.
	 * @return the decoded integer
	 */
    public int readUnary() {
        int result = 0;
        while (readBitOffset <= readBits) {
            if ((inBuffer[(int) byteOffset] & (1 << (bitOffset))) != 0) {
                result++;
                readBitOffset++;
                bitOffset++;
                if (bitOffset == 8) {
                    bitOffset = 0;
                    byteOffset++;
                }
            } else {
                result++;
                readBitOffset++;
                bitOffset++;
                if (bitOffset == 8) {
                    bitOffset = 0;
                    byteOffset++;
                }
                break;
            }
        }
        return result;
    }

    /** Returns the current buffer being processed */
    public byte[] getInBuffer() {
        return inBuffer;
    }

    /** 
	 * Flushes the in-memory buffer to the file after 
	 * finishing a sequence of write calls.
	 * @deprecated
	 */
    public void writeFlush() {
        try {
            if (bitOffset > 0) outBuffer.write(buffer);
            if (isLastByteIncomplete) {
                writeFile.seek(file.length() - 1);
                writeFile.write(outBuffer.toByteArray());
            } else writeFile.write(outBuffer.toByteArray());
        } catch (IOException ioe) {
            logger.error("Input/Output exception while writing to the file. Stack trace follows.", ioe);
        }
    }

    /**
	 * Reads a binary integer from the already read buffer.
	 * No IO and 0 is returned if noBits == 0.
	 * <b>NB</b>: noBits &gt; than 32 will give undefined results.
	 * @param noBits the number of binary bits to read
	 * @return the decoded integer
	 */
    public int readBinary(final int noBits) {
        if (noBits == 0) return 0;
        int binary = 0;
        for (int i = 0; i < noBits; i++) {
            if ((inBuffer[(int) byteOffset] & (1 << (bitOffset))) != 0) {
                binary = binary + (1 << i);
            }
            readBitOffset++;
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
            }
        }
        return binary;
    }

    /** Skip a number of bits in the current input stream
	 * @param noBits The number of bits to skip
	 */
    public void skipBits(final int noBits) {
        if (noBits == 0) return;
        for (int i = 0; i < noBits; i++) {
            readBitOffset++;
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
            }
        }
    }

    /**
	 * Writes a binary integer, of a given length, to the already read buffer.
	 * @param bitsToWrite the number of bits to write
	 * @param n the integer to write
	 * @return SHOULD returns number of bits written, but doesnt 
	 * @deprecated
	*/
    public int writeBinary(int bitsToWrite, int n) {
        byte rem;
        while (n != 0) {
            rem = (byte) (n % 2);
            buffer |= (rem << bitOffset);
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
                outBuffer.write(buffer);
                buffer = 0;
            }
            n = n / 2;
            bitsToWrite--;
        }
        while (bitsToWrite > 0) {
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
                outBuffer.write(buffer);
                buffer = 0;
            }
            bitsToWrite--;
        }
        return -1;
    }

    /** 
	 * Writes an gamma encoded integer in the buffer.
	 * @param n The integer to be encoded and saved in the buffer.
	 * @return SHOULD returns number of bits written, but doesnt
	 * @deprecated
	 */
    public int writeGamma(int n) {
        final byte mask = 1;
        final int floor = (int) Math.floor(Math.log(n) / LOG_E_2);
        writeUnary(floor + 1);
        final int secondPart = (int) (n - (1 << (floor)));
        for (int i = 0; i < floor; i++) {
            if ((secondPart & (1 << i)) != 0) {
                buffer |= (mask << bitOffset);
                bitOffset++;
                if (bitOffset == 8) {
                    bitOffset = 0;
                    byteOffset++;
                    outBuffer.write(buffer);
                    buffer = 0;
                }
            } else {
                bitOffset++;
                if (bitOffset == 8) {
                    bitOffset = 0;
                    byteOffset++;
                    outBuffer.write(buffer);
                    buffer = 0;
                }
            }
        }
        return -1;
    }

    /** 
	 * Prepares for writing to the file unary or gamma encoded integers.
	 * It reads the last incomplete byte from the file, according to the
	 * bitOffset value
	 * @deprecated
	 */
    public void writeReset() throws IOException {
        if (!(file instanceof RandomDataOutput)) throw new IOException("Cannot write to read only BitFile file");
        writeFile = (RandomDataOutput) file;
        outBuffer = new ByteArrayOutputStream();
        if (bitOffset != 0) {
            isLastByteIncomplete = true;
            try {
                byteOffset = file.length() - 1;
                file.seek(byteOffset);
                buffer = file.readByte();
            } catch (IOException ioe) {
                logger.error("Input/output exception while reading file. Stack trace follows.", ioe);
            }
        } else {
            isLastByteIncomplete = false;
            buffer = 0;
        }
    }

    /** 
	 * Writes a unary integer to the buffer.
	 * @param n The integer to be encoded and writen in the buffer.
	 * @return SHOULD returns number of bits written, but doesnt
	 * @deprecated
	 */
    public int writeUnary(int n) {
        final byte mask = 1;
        for (int i = 0; i < n - 1; i++) {
            buffer |= (mask << bitOffset);
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
                outBuffer.write(buffer);
                buffer = 0;
            }
        }
        bitOffset++;
        if (bitOffset == 8) {
            bitOffset = 0;
            byteOffset++;
            outBuffer.write(buffer);
            buffer = 0;
        }
        return -1;
    }
}
