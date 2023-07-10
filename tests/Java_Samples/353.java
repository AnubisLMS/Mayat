package uk.ac.bath.gui.vamp;

import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class IPatchDriver {

    int channel;

    Receiver recv;

    public int getChannel() {
        return channel;
    }

    public void send(ShortMessage m) {
        recv.send(m, -1);
    }
}
