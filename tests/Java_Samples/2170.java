package kjdss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * @author Kris
 *
 * This class provides synchronization between a digital signal processor and speaker output. 
 */
public class KJDigitalSignalSynchronizer {

    private static final boolean DEBUG_MODE = false;

    private static final int DEFAULT_FPS = 60;

    private static final int DEFAULT_FRRH_CALIBRATION = DEFAULT_FPS;

    private static final int DEFAULT_OVERRUN_PROTECTION = 8192;

    private static final int DEFAULT_WRITE_CHUNK_SIZE = 8192;

    private Object readWriteLock = new Object();

    private SourceDataLine sourceDataLine;

    private int sampleSize;

    private byte[] audioDataBuffer;

    private int fps;

    private int frrhc;

    private int position;

    private Context context;

    private Normalizer normalizer;

    private Synchronizer synchronizer;

    private ArrayList<KJDigitalSignalProcessor> dsps = new ArrayList<KJDigitalSignalProcessor>();

    private Map<String, Object> debugInfo;

    private boolean sourceDataLineWrite = true;

    /**
	 * Default constructor creates a both the default frames per second and frame rate ratio hint calibration.
	 */
    public KJDigitalSignalSynchronizer() {
        this(DEFAULT_FPS);
    }

    /**
	 * Default constructor creates a DSS with a specified FPS and default frame rate ratio hint calibration.
	 */
    public KJDigitalSignalSynchronizer(int pFramesPerSecond) {
        this(pFramesPerSecond, DEFAULT_FRRH_CALIBRATION);
    }

    /**
	 * @param pFramePerSecond The desired frames per second to update and registered DSPs.  
	 * @param pFrameRateRatioHintCalibration The frame rate calibration (in FPS) to have a DSP simulate
	 * 		when the desired FPS cannot be met.  
	 */
    public KJDigitalSignalSynchronizer(int pFramesPerSecond, int pFrameRateRatioHintCalibration) {
        fps = pFramesPerSecond;
        frrhc = pFrameRateRatioHintCalibration;
        if (DEBUG_MODE) {
            debugInfo = new HashMap<String, Object>();
        }
    }

    /**
	 * Adds a DSP to the DSS and forwards any audio data to it at the specified frame rate.
	 * 
	 * @param A class implementing the KJDigitalSignalProcessor interface. 
	 */
    public synchronized void add(KJDigitalSignalProcessor pSignalProcessor) {
        if (synchronizer != null) {
            pSignalProcessor.initialize(sampleSize, sourceDataLine);
        }
        dsps.add(pSignalProcessor);
    }

    /**
	 * Creates and returns a normalizer object that can be used to normalizer audio data.
	 * 
	 * @return A normalizer instance. 
	 */
    public Normalizer getNormalizer() {
        if (normalizer == null) {
            synchronized (this) {
                if (normalizer == null && synchronizer != null) {
                    normalizer = new Normalizer(sourceDataLine.getFormat());
                }
            }
        }
        return normalizer;
    }

    /**
	 * Returns the active state or the synchronizer.
	 * 
	 * @return A boolean. 
	 */
    public synchronized boolean isActive() {
        if (synchronizer != null) {
            return synchronizer.isActive();
        }
        return false;
    }

    /**
	 * Returns the paused state or the synchronizer.
	 * 
	 * @return A boolean. 
	 */
    public synchronized boolean isPaused() {
        if (synchronizer != null) {
            return synchronizer.isPaused();
        }
        return false;
    }

    /**
	 * Returns the source data line write state. 
	 * See {@link #setSourceDataLineWriteEnabled(boolean)} for details.
	 * 
	 * @return A boolean 
	 */
    public boolean isSourceDataLineWriteEnabled() {
        return sourceDataLineWrite;
    }

    /**
	 * Pauses synchronizer updates to registerd DSPs.
	 * 
	 * @param A class implementing the KJDigitalSignalProcessor interface. 
	 */
    public synchronized void setPaused(boolean pPaused) {
        if (synchronizer != null) {
            synchronizer.setPaused(pPaused);
        }
    }

    /**
	 * Removes the specified DSP from this DSPAC if it exists.
	 * 
	 * @param A class implementing the KJDigitalSignalProcessor interface. 
	 */
    public synchronized void remove(KJDigitalSignalProcessor pSignalProcessor) {
        dsps.remove(pSignalProcessor);
    }

    /**
	 * Specifies if this DSS should write directly to the SDL. 
	 * 
	 * NOTE: If this is disabled, reliable buffer synchronization is NOT possible between the SDL 
	 * and the DSS. Therefore it is only possible to write less than the SDL buffer size at a time 
	 * to the writeAudioData() method. This usually works fine for MP3 encoded files as they are decoded 
	 * into frames of about 4608 bytes. If an entire WAV file is loaded into memory ( 128kb or greater ) 
	 * this feature must be enabled in order to write the entire data buffer to the DSS at once. 
	 * If disabled, it is also necessary to write to the SDL manually (like KJ DSP v1.1) 
	 * 
	 * @param A boolean  
	 */
    public void setSourceDataLineWriteEnabled(boolean pSdlw) {
        sourceDataLineWrite = pSdlw;
    }

    /**
	 * Start monitoring the specified SourceDataLine.
	 * 
	 * @param pSdl A SourceDataLine.
	 */
    public synchronized void start(SourceDataLine pSdl) {
        if (synchronizer != null) {
            stop();
        }
        if (synchronizer == null) {
            sourceDataLine = pSdl;
            sampleSize = (int) (Math.round(sourceDataLine.getFormat().getFrameRate() / (float) fps));
            context = new Context(sampleSize);
            audioDataBuffer = new byte[pSdl.getBufferSize() + DEFAULT_OVERRUN_PROTECTION];
            position = 0;
            normalizer = null;
            for (KJDigitalSignalProcessor wDsp : dsps) {
                try {
                    wDsp.initialize(sampleSize, pSdl);
                } catch (Exception pEx) {
                    pEx.printStackTrace();
                }
            }
            synchronizer = new Synchronizer(fps, frrhc);
            new Thread(synchronizer).start();
            if (DEBUG_MODE) {
                debugInfo.put("FPS", fps);
                debugInfo.put("SS", sampleSize);
                debugInfo.put("BS", audioDataBuffer.length);
            }
        }
    }

    /**
	 * Stop monitoring the currect SourceDataLine.
	 */
    public synchronized void stop() {
        if (synchronizer != null) {
            synchronized (readWriteLock) {
                synchronizer.stop();
                synchronizer = null;
                audioDataBuffer = null;
                sourceDataLine = null;
            }
        }
    }

    protected void storeAudioData(byte[] pAudioData, int pOffset, int pLength) {
        if (audioDataBuffer == null) {
            return;
        }
        int wOverrun = 0;
        if (position + pLength > audioDataBuffer.length - 1) {
            wOverrun = (position + pLength) - audioDataBuffer.length;
            pLength = audioDataBuffer.length - position;
        }
        System.arraycopy(pAudioData, pOffset, audioDataBuffer, position, pLength);
        if (wOverrun > 0) {
            System.arraycopy(pAudioData, pOffset + pLength, audioDataBuffer, 0, wOverrun);
            position = wOverrun;
        } else {
            position += pLength;
        }
        if (DEBUG_MODE) {
            debugInfo.put("SP", position);
            debugInfo.put("SO", pOffset);
            debugInfo.put("SL", pLength);
            debugInfo.put("OR", wOverrun);
        }
    }

    /**
	 * Writes the entire specified buffer to the monitored source data line an any registered DSPs.
	 * 
	 * @param pAudioData Data to write.
	 */
    public void writeAudioData(byte[] pAudioData) {
        writeAudioData(pAudioData, 0, pAudioData.length);
    }

    /**
	 * Writes part of specified buffer to the monitored source data line an any registered DSPs.
	 * 
	 * @param pAudioData Data to write.
	 * @param pOffset Offset to start reading from the buffer.
	 * @param pLength The length from the specified offset to read.
	 */
    public void writeAudioData(byte[] pAudioData, int pOffset, int pLength) {
        synchronized (readWriteLock) {
            if (sourceDataLineWrite) {
                writeChunkedAudioData(pAudioData, pOffset, pLength);
            } else {
                storeAudioData(pAudioData, pOffset, pLength);
            }
        }
    }

    /**
	 * Writes part of specified buffer to the monitored source data line an any registered DSPs.
	 * 
	 * @param pAudioData Data to write.
	 * @param pOffset Offset to start reading from the buffer.
	 * @param pLength The length from the specified offset to read.
	 */
    protected void writeChunkedAudioData(byte[] pAudioData, int pOffset, int pLength) {
        if (audioDataBuffer == null) {
            return;
        }
        int wWl;
        for (int o = pOffset; o < pOffset + pLength; o += DEFAULT_WRITE_CHUNK_SIZE) {
            wWl = DEFAULT_WRITE_CHUNK_SIZE;
            if (o + wWl >= pLength) {
                wWl = pLength - o;
            }
            sourceDataLine.write(pAudioData, o, wWl);
            storeAudioData(pAudioData, o, wWl);
        }
    }

    public class Context {

        int offset;

        int length;

        float frh;

        /**
		 * Create a DSS context with a fixed sample length.
		 * 
		 * @param pLength The sample length.
		 */
        public Context(int pLength) {
            length = pLength;
        }

        /**
		 * Returns the data buffer of this DSS.
		 * 
		 * @return Data buffer.
		 */
        public byte[] getDataBuffer() {
            return audioDataBuffer;
        }

        /**
		 * Returns a normalized sample from the DSS data buffer. This method can be used to make a DSP
		 * based on KJ DSP v1.1 work properly again.
		 * 
		 * @return Normalized data.
		 */
        public float[][] getDataNormalized() {
            return getNormalizer().normalize(audioDataBuffer, offset, length);
        }

        /**
		 * Return debugging information of the DSS if debugging is enabled.
		 * 
		 * @return A map with debugging information.
		 */
        public Map<String, Object> getDebugInfo() {
            return debugInfo;
        }

        /**
		 * Returns the frame ratio hint.
		 * 
		 * @return 
		 */
        public float getFrameRatioHint() {
            return frh;
        }

        /**
		 * Returns the sample length to read from the data buffer.
		 * 
		 * @return int
		 */
        public int getLength() {
            return length;
        }

        /**
		 * Returns the data buffer offset to start reading from. Please note that the offset + length 
		 * can be beyond the buffere length. This simply means, the rest of data sample has rolled over
		 * to the beginning of the data buffer. See the Normalizer inner class for an example. 
		 * 
		 * @return int
		 */
        public int getOffset() {
            return offset;
        }

        /**
		 * Returns the monitored source data line.
		 * 
		 * @return A source data line.
		 */
        public SourceDataLine getSourceDataLine() {
            return sourceDataLine;
        }
    }

    public class Normalizer {

        private AudioFormat audioFormat;

        private float[][] channels;

        private int channelSize;

        private long audioSampleSize;

        public Normalizer(AudioFormat pFormat) {
            audioFormat = pFormat;
            channels = new float[pFormat.getChannels()][];
            for (int c = 0; c < pFormat.getChannels(); c++) {
                channels[c] = new float[sampleSize];
            }
            channelSize = audioFormat.getFrameSize() / audioFormat.getChannels();
            audioSampleSize = (1 << (audioFormat.getSampleSizeInBits() - 1));
        }

        public float[][] normalize(byte[] pData, int pPosition, int pLength) {
            int wChannels = audioFormat.getChannels();
            int wSsib = audioFormat.getSampleSizeInBits();
            int wFrameSize = audioFormat.getFrameSize();
            for (int sp = 0; sp < sampleSize; sp++) {
                if (pPosition >= pData.length) {
                    pPosition = 0;
                }
                int cdp = 0;
                for (int ch = 0; ch < wChannels; ch++) {
                    long sm = (pData[pPosition + cdp] & 0xFF) - 128;
                    for (int bt = 8, bp = 1; bt < wSsib; bt += 8) {
                        sm += pData[pPosition + cdp + bp] << bt;
                        bp++;
                    }
                    channels[ch][sp] = (float) sm / audioSampleSize;
                    cdp += channelSize;
                }
                pPosition += wFrameSize;
            }
            return channels;
        }
    }

    private class Synchronizer implements Runnable {

        private boolean active = true;

        private boolean paused = false;

        private int frameSize;

        private long fpsAsNS;

        private long desiredFpsAsNS;

        private long frrhcAsNS;

        public Synchronizer(int pFps, int pFrrhc) {
            desiredFpsAsNS = 1000000000L / (long) pFps;
            fpsAsNS = desiredFpsAsNS;
            frrhcAsNS = 1000000000L / pFrrhc;
            frameSize = sourceDataLine.getFormat().getFrameSize();
        }

        private int calculateSamplePosition() {
            if (DEBUG_MODE) {
                debugInfo.put("FP", sourceDataLine.getLongFramePosition());
                debugInfo.put("DP", (long) (sourceDataLine.getLongFramePosition() * frameSize) % (long) (audioDataBuffer.length));
            }
            return (int) ((long) (sourceDataLine.getLongFramePosition() * frameSize) % (long) (audioDataBuffer.length));
        }

        public boolean isActive() {
            return active;
        }

        public boolean isPaused() {
            return paused;
        }

        public void run() {
            long wStn = 0;
            long wDelay = 0;
            while (active) {
                try {
                    context.offset = calculateSamplePosition();
                    context.frh = (float) fpsAsNS / (float) frrhcAsNS;
                    if (DEBUG_MODE) {
                        debugInfo.put("POS", context.offset);
                        debugInfo.put("FRH", context.frh);
                        debugInfo.put("AFPSN", fpsAsNS);
                        debugInfo.put("DFPSN", desiredFpsAsNS);
                        debugInfo.put("FRRHCN", frrhcAsNS);
                    }
                    wStn = System.nanoTime();
                    if (!paused) {
                        for (int a = 0; a < dsps.size(); a++) {
                            try {
                                ((KJDigitalSignalProcessor) dsps.get(a)).process(context);
                            } catch (Exception pEx) {
                                System.err.println("-- DSP Exception: ");
                                pEx.printStackTrace();
                            }
                        }
                    }
                    wDelay = fpsAsNS - (System.nanoTime() - wStn);
                    if (DEBUG_MODE) {
                        debugInfo.put("CD", "[" + (wDelay) + ", " + (wDelay / 1000000) + ", " + (wDelay % 1000000) + "]");
                    }
                    if (dsps.isEmpty() || paused) {
                        wDelay = 1000000000;
                    }
                    if (wDelay > 0) {
                        try {
                            Thread.sleep(wDelay / 1000000, (int) wDelay % 1000000);
                        } catch (Exception pEx) {
                        }
                        if (fpsAsNS > desiredFpsAsNS) {
                            fpsAsNS -= wDelay;
                        } else {
                            fpsAsNS = desiredFpsAsNS;
                        }
                    } else {
                        fpsAsNS += -wDelay;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException pEx) {
                        }
                    }
                } catch (Exception pEx) {
                    System.err.println("- DSP Exception: ");
                    pEx.printStackTrace();
                }
            }
        }

        public void setPaused(boolean pPaused) {
            paused = pPaused;
        }

        public void stop() {
            active = false;
        }
    }
}
