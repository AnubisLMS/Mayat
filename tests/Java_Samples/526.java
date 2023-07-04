package net.sf.telescope.communications.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import org.xiph.speex.spi.SpeexEncoding;

public class MicReader extends Thread {

    public static final int BUFFER_SIZE = 1024;

    String audioFileName;

    AudioInputStream audioInputStream;

    private boolean running;

    TargetDataLine line;

    IAudioManager mgr;

    public MicReader(IAudioManager am) {
        running = false;
        mgr = am;
        try {
            AudioFormat aFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0f, 16, 2, 4, 8000.0f, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, aFormat);
            line = (TargetDataLine) AudioSystem.getLine(info);
            audioInputStream = new AudioInputStream(line);
            line.open(aFormat);
            AudioFormat targetFormat = new AudioFormat(SpeexEncoding.SPEEX_Q0, aFormat.getSampleRate(), -1, aFormat.getChannels(), -1, -1, false);
            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
        } catch (Exception e) {
            System.out.println("Exception " + e);
            System.exit(0);
        }
    }

    public void startRecording() {
        running = true;
        line.start();
        start();
    }

    public void stopRecording() {
        line.close();
        running = false;
    }

    public void run() {
        byte[] data = new byte[BUFFER_SIZE];
        System.out.println("comecei o mic");
        while (running) {
            try {
                int bytes = line.read(data, 0, BUFFER_SIZE);
                mgr.sendPacket(data, bytes);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        System.out.println("parei o mic");
    }
}
