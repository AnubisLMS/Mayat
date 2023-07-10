package cn.edu.wuse.musicxml.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.swing.JPanel;

/**
 * 钢琴面板
 * @author Apex.Mao
 */
public class KeyBoard extends JPanel implements MouseListener {

    private static final long serialVersionUID = 1L;

    public static final int BLACK_KEY_SIZE = 20;

    public static final int WHITE_KEY_SIZE = 29;

    public static final int WHITE_KEY_WIDTH = 20;

    public static final int WHITE_KEY_HEIGHT = 100;

    public static final int BLACK_KEY_WIDTH = 14;

    public static final int BLACK_KEY_HEIGHT = 65;

    private static final int SUPPLEMENT = 3;

    private static final Rectangle[] whiteKey;

    private static final Rectangle[] blackKey;

    private static final int[] scale = { 0, 2, 4, 5, 7, 9, 11 };

    private static final int[] revalScale = { 0, 1, 3, 5, 7, 8, 10 };

    private static final Point left[];

    private int channel;

    private int centerC = 14;

    private int octaveView = 2;

    private static final Color markC = Color.RED;

    private int value;

    static {
        whiteKey = new Rectangle[WHITE_KEY_SIZE];
        blackKey = new Rectangle[BLACK_KEY_SIZE];
        for (int i = 0; i < WHITE_KEY_SIZE; i++) whiteKey[i] = new Rectangle(i * WHITE_KEY_WIDTH, 0, WHITE_KEY_WIDTH, WHITE_KEY_HEIGHT);
        blackKey[0] = new Rectangle(whiteKey[0].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[1] = new Rectangle(whiteKey[1].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[2] = new Rectangle(whiteKey[3].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[3] = new Rectangle(whiteKey[4].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[4] = new Rectangle(whiteKey[5].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[5] = new Rectangle(whiteKey[7].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[6] = new Rectangle(whiteKey[8].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[7] = new Rectangle(whiteKey[10].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[8] = new Rectangle(whiteKey[11].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[9] = new Rectangle(whiteKey[12].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[10] = new Rectangle(whiteKey[14].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[11] = new Rectangle(whiteKey[15].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[12] = new Rectangle(whiteKey[17].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[13] = new Rectangle(whiteKey[18].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[14] = new Rectangle(whiteKey[19].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[15] = new Rectangle(whiteKey[21].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[16] = new Rectangle(whiteKey[22].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[17] = new Rectangle(whiteKey[24].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[18] = new Rectangle(whiteKey[25].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        blackKey[19] = new Rectangle(whiteKey[26].x + WHITE_KEY_WIDTH / 2 + SUPPLEMENT, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT);
        left = new Point[] { new Point(0, 0), new Point(1, 1), new Point(3, 2), new Point(4, 3), new Point(5, 4), new Point(7, 5), new Point(8, 6), new Point(10, 7), new Point(11, 8), new Point(12, 9), new Point(14, 10), new Point(15, 11), new Point(17, 12), new Point(18, 13), new Point(19, 14), new Point(21, 15), new Point(22, 16), new Point(24, 17), new Point(25, 18), new Point(26, 19) };
    }

    public KeyBoard(int channel) {
        super();
        this.channel = channel;
        setSize(28 * WHITE_KEY_WIDTH, WHITE_KEY_HEIGHT);
        addMouseListener(this);
    }

    public KeyBoard() {
        this(0);
    }

    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.WHITE);
        for (int i = 0; i < WHITE_KEY_SIZE; i++) g.fill3DRect(whiteKey[i].x, whiteKey[i].y, whiteKey[i].width, whiteKey[i].height, true);
        g.setColor(Color.BLACK);
        for (int i = 0; i < BLACK_KEY_SIZE; i++) g.fill3DRect(blackKey[i].x, blackKey[i].y, blackKey[i].width, blackKey[i].height, true);
        g.setColor(Color.RED);
        g.fillRect(whiteKey[centerC].x + 4, whiteKey[centerC].height - 12, 8, 8);
    }

    private final void getMidiValue(Point p) {
        int alter = 0, val = 60, offset = 0, oct;
        Rectangle r = null;
        int i = 0;
        for (i = 0; i < WHITE_KEY_SIZE; i++) if (whiteKey[i].contains(p)) {
            r = whiteKey[i];
            break;
        }
        if (r == null) {
            value = -1;
            return;
        }
        offset = i - (4 - octaveView) * 7;
        int absOffset = Math.abs(offset);
        oct = absOffset / 7;
        oct *= 12;
        if (offset < 0) {
            val -= oct;
            val -= revalScale[absOffset % 7];
        } else if (offset > 0) {
            val += oct;
            val += scale[absOffset % 7];
        }
        int j = 0;
        for (; j < BLACK_KEY_SIZE; j++) if (blackKey[j].contains(p)) {
            break;
        }
        if (j != 20) {
            int k;
            for (k = 0; k < left.length; k++) if (left[k].x == i && left[k].y == j) {
                alter++;
                break;
            }
            if (k == left.length) alter--;
        }
        val += alter;
        this.value = val;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        try {
            getMidiValue(e.getPoint());
            if (value == -1) return;
            ShortMessage s = new ShortMessage();
            s.setMessage(ShortMessage.NOTE_ON, channel, value, 96);
            Player.processRealTimeMessage(s);
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }
    }

    public void mouseReleased(MouseEvent e) {
        try {
            if (value == -1) return;
            ShortMessage s = new ShortMessage();
            s.setMessage(ShortMessage.NOTE_OFF, channel, value, 96);
            Player.processRealTimeMessage(s);
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void processPlayEvent(PlayEvent playEvent) {
    }

    public int getCenterC() {
        return centerC;
    }

    public void setCenterC(int centerC) {
        this.centerC = centerC;
    }

    public int getOctaveView() {
        return octaveView;
    }

    public void setOctaveView(int octaveView) {
        Graphics g = getGraphics();
        if (g == null) return;
        g.setColor(Color.WHITE);
        g.fillRect(whiteKey[centerC].x + 4, whiteKey[centerC].height - 12, 8, 8);
        this.octaveView = octaveView;
        this.centerC = (4 - octaveView) * 7;
        g.setColor(markC);
        g.fillRect(whiteKey[centerC].x + 4, whiteKey[centerC].height - 12, 8, 8);
    }
}
