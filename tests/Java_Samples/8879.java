package avisync.decoder;

public class AC3Frame {

    public static final int AC3_HEADER_SIZE = 7;

    private static final int AC3_SYNC_MARKER = 0x0B77;

    private static final int AC3_MAX_SAMPLE_RATE_CODE = 3;

    private static final int AC3_MAX_FRAME_SIZE_CODE = 38;

    private static final int[] SAMPLE_RATE = { 48000, 44100, 32000, 0 };

    private static final int[] BIT_RATE = { 32, 32, 40, 40, 48, 48, 56, 56, 64, 64, 80, 80, 96, 96, 112, 112, 128, 128, 160, 160, 192, 192, 224, 224, 256, 256, 320, 320, 384, 384, 448, 448, 512, 512, 576, 576, 640, 640 };

    private static final int[][] FRAME_SIZE = { { 64, 64, 80, 80, 96, 96, 112, 112, 128, 128, 160, 160, 192, 192, 224, 224, 256, 256, 320, 320, 384, 384, 448, 448, 512, 512, 640, 640, 768, 768, 896, 896, 1024, 1024, 1152, 1152, 1280, 1280 }, { 69, 70, 87, 88, 104, 105, 121, 122, 139, 140, 174, 175, 208, 209, 243, 244, 278, 279, 348, 349, 417, 418, 487, 488, 557, 558, 696, 697, 835, 836, 975, 976, 1114, 1115, 1253, 1254, 1393, 1394 }, { 96, 96, 120, 120, 144, 144, 168, 168, 192, 192, 240, 240, 288, 288, 336, 336, 384, 384, 480, 480, 576, 576, 672, 672, 768, 768, 960, 960, 1152, 1152, 1344, 1344, 1536, 1536, 1728, 1728, 1920, 1920 } };

    private static final int[] CHANNEL_ID = { 2, 1, 2, 3, 3, 4, 4, 5 };

    private byte[] header;

    public AC3Frame(byte[] header) {
        setHeader(header);
    }

    public byte[] getHeader() {
        return header;
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }

    public int getBitrate() {
        return BIT_RATE[getFrameSizeCode()];
    }

    public int getFrameSize() {
        return 2 * FRAME_SIZE[getSampleRateCode()][getFrameSizeCode()];
    }

    public int getChannels() {
        return CHANNEL_ID[getChannelIndex()];
    }

    public int getSampleRate() {
        return SAMPLE_RATE[getSampleRateCode()];
    }

    public int getSyncword() {
        return ((header[0] & 0xff) << 8) + (header[1] & 0xff);
    }

    public int getCrc1() {
        return ((header[2] & 0xff) << 8) + (header[3] & 0xff);
    }

    public int getFrmsizcod() {
        return header[4] & 0xff;
    }

    public int getBsi() {
        return header[5] & 0xff;
    }

    public int getAC3mod() {
        return header[6] & 0xff;
    }

    public boolean verify() {
        return getSyncword() == AC3_SYNC_MARKER && getSampleRateCode() < AC3_MAX_SAMPLE_RATE_CODE && getFrameSizeCode() < AC3_MAX_FRAME_SIZE_CODE;
    }

    private int getSampleRateCode() {
        return (getFrmsizcod() >> 6) & 0x03;
    }

    private int getFrameSizeCode() {
        return (getFrmsizcod() >> 0) & 0x3f;
    }

    private int getChannelIndex() {
        return (getAC3mod() >> 5) & 0x07;
    }

    public static boolean isSupportedChannels(int channels) {
        return channels >= 1 && channels <= 6;
    }

    public static boolean isSupportedSampleRate(int frequency) {
        for (int index = 0; index < AC3_MAX_SAMPLE_RATE_CODE; index++) {
            if (frequency == SAMPLE_RATE[index]) return true;
        }
        return false;
    }

    public static boolean isSupportedBitRate(int bitRate) {
        for (int index = 0; index < AC3_MAX_FRAME_SIZE_CODE; index++) {
            if (bitRate == 1000 * BIT_RATE[index]) return true;
        }
        return false;
    }

    public static int getMaxBytesPerFrame(int bitRate, int sampleRate) {
        for (int sampleRateCode = 0; sampleRateCode < AC3_MAX_SAMPLE_RATE_CODE; sampleRateCode++) {
            if (sampleRate == SAMPLE_RATE[sampleRateCode]) {
                for (int frameSizeCode = 1; frameSizeCode < AC3_MAX_FRAME_SIZE_CODE; frameSizeCode += 2) {
                    if (bitRate == 1000 * BIT_RATE[frameSizeCode]) return 2 * FRAME_SIZE[sampleRateCode][frameSizeCode];
                }
                break;
            }
        }
        return 0;
    }
}
