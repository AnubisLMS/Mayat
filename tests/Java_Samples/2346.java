package jm.music.data;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import jm.JMC;
import ren.util.PO;

/**
 * The Score class is used to hold score data.  Score data includes
 * is primarily made up of a vector of Part objects. Commonly score
 * data is algorithmically generated or read from a standard MIDI file, but can also be read and saved to 
 * file using Java's object serialization. In this way a Score's data can
 * be saved in a more native context.
 * To find out how to read from and write to standard MIDI files
 * or to use object serializationcheck out 
 * the jm.util.Read and km.util.Write classes.
 * 
 * @see Part 
 * @see jm.midi.SMF
 * @author Andrew Sorensen, Andrew Brown, Adam Kirby
 * @version 1.0,Sun Feb 25 18:43:33  2001
 */
public class Score implements JMC, Cloneable, Serializable {

    public static final String DEFAULT_TITLE = "Untitled Score";

    public static final double DEFAULT_TEMPO = 60.0;

    public static final int DEFAULT_VOLUME = 100;

    public static final int DEFAULT_KEY_SIGNATURE = 0;

    public static final int DEFAULT_KEY_QUALITY = 0;

    public static final int DEFAULT_NUMERATOR = 4;

    public static final int DEFAULT_DENOMINATOR = 4;

    /** the name assigned to a Score */
    private String title;

    /** 
	 * a Vector containing the Part objects associated with this score 
	 */
    private Vector partList;

    /** the speed for this score */
    private double tempo;

    /** the loudness for this score */
    private int volume;

    /** the number of accidentals this score 
	* negative numbers are Flats, positive numbers are Sharps
	*/
    private int keySignature;

    /** 0 = major, 1 = minor, others modes not specified */
    private int keyQuality;

    /** the top number of the time signature */
    private int numerator;

    /** the bottom number of the time signature */
    private int denominator;

    /**
	 * Constructs an empty score with a default name
	 */
    public Score() {
        this(DEFAULT_TITLE);
    }

    /**
	 * Constructs an empty score.
	 * @param String title - give the score a name
	 */
    public Score(String title) {
        this(title, DEFAULT_TEMPO);
    }

    /**
	 * Constructs an empty score at the specified tempo
	 * @param tempo The speed for this score in beats per minute
	 */
    public Score(double tempo) {
        this(DEFAULT_TITLE, tempo);
    }

    /**
	 * Constructs an empty score.
	 * @param String title - give the score a name
	 * @param int tempo - define speed of playback in bpm
	 */
    public Score(String title, double tempo) {
        this.title = title;
        this.tempo = tempo;
        this.partList = new Vector();
        this.volume = DEFAULT_VOLUME;
        this.keySignature = DEFAULT_KEY_SIGNATURE;
        this.keyQuality = DEFAULT_KEY_QUALITY;
        this.numerator = DEFAULT_NUMERATOR;
        this.denominator = DEFAULT_DENOMINATOR;
    }

    /**
	* Constructs a Score containing the specified <CODE>part</CODE>.
	*
	* @param part  Part to be contained in the Score
	*/
    public Score(Part part) {
        this();
        this.addPart(part);
    }

    /**
	* Constructs a Score containing the specified <CODE>part</CODE>.
	* @param String title - give the score a name
	* @param int tempo - define speed of playback in bpm
	* @param part  Part to be contained in the Score
	*/
    public Score(String title, double tempo, Part part) {
        this(title, tempo);
        this.addPart(part);
    }

    /**
     * Constructs a Score containing the specified <CODE>parts</CODE>.
     *
     * @param parts array of Parts to be contained in the Score
     */
    public Score(Part[] parts) {
        this();
        addPartList(parts);
    }

    /**
     * Constructs a Score containing the specified <CODE>part</CODE> with
     * the specified <CODE>title</CODE>.
     *
     * @param part  Part to be contained in the Score
     * @param title String describing the title of the Score
     */
    public Score(Part part, String title) {
        this(title);
        addPart(part);
    }

    /**
     * Constructs a Score containing the specified <CODE>parts</CODE> with
     * the specified <CODE>title</CODE>.
     *
     * @param parts array of Parts to be contained in the Score
     * @param title String describing the title of the Score
     */
    public Score(Part[] parts, String title) {
        this(title);
        addPartList(parts);
    }

    /**
     * Constructs a Score containing the specified <CODE>part</CODE> with
     * the specified <CODE>title</CODE> and the specified <CODE>tempo</CODE>.
     *
     * @param part  Part to be contained in the Score
     * @param title String describing the title of the Score
     * @param tempo double describing the tempo of the Score
     */
    public Score(Part part, String title, double tempo) {
        this(title, tempo);
        addPart(part);
    }

    /**
     * Constructs a Score containing the specified <CODE>parts</CODE> with
     * the specified <CODE>title</CODE> and the specified <CODE>tempo</CODE>.
     *
     * @param parts array of Parts to be contained in the Score
     * @param title String describing the title of the Score
     * @param tempo double describing the tempo of the Score
     */
    public Score(Part[] parts, String title, double tempo) {
        this(title, tempo);
        addPartList(parts);
    }

    /**
     * initialises all sixteen parts and allocates the specified memory to them
     *
     *@param memorySlots is the number of slots in the array for each
     *part
     */
    transient Part tempPart;

    public void initAllParts(int memorySlots) {
        for (int i = 0; i < 16; i++) {
            tempPart = new Part(1, i + 1);
            tempPart.getPhraseList().ensureCapacity(memorySlots);
            this.add(tempPart);
        }
    }

    /**
	 * Add a Track object to this Score
	 */
    public void add(Part part) {
        this.addPart(part);
    }

    /**
	 * Add a Track object to this Score
	 */
    public void addPart(Part part) {
        part.setMyScore(this);
        this.partList.addElement(part);
    }

    /**
     * Inserts <CODE>part</CODE> at the specified position, shifting all parts
     * with indices greater than or equal to <CODE>index</CODE> up one position.
     *
     * @param part  Part to be added
     * @param index where it is to be inserted
     * @throws ArrayIndexOutOfBoundsException
     *              when <CODE>index</CODE> is beyond the range of current
     *              parts.
     */
    public void insertPart(Part part, int index) throws ArrayIndexOutOfBoundsException {
        this.partList.insertElementAt(part, index);
        part.setMyScore(this);
    }

    /**
	 * Adds multiple parts to the score from an array of parts
	 * @param partArray
	 */
    public void addPartList(Part[] partArray) {
        for (int i = 0; i < partArray.length; i++) {
            this.addPart(partArray[i]);
        }
    }

    /**
	 * Deletes the specified Part in the Score
	 * @param int partNumb the index of the part to be deleted
	 */
    public void removePart(int partNumb) {
        Vector vct = (Vector) this.partList;
        try {
            vct.removeElement(vct.elementAt(partNumb));
        } catch (RuntimeException re) {
            System.err.println("The Part index to be deleted must be within the score.");
        }
    }

    /**
    * Deletes the first occurence of the specified part in the Score.
    * @param part  the Part object to be deleted.
    */
    public void removePart(Part part) {
        this.partList.removeElement(part);
    }

    /**
	 * Deletes the last Part added to the Score
	 */
    public void removeLastPart() {
        Vector vct = (Vector) this.partList;
        vct.removeElement(vct.lastElement());
    }

    /**
	 * Deletes all the parts previously added to the score
	 */
    public void removeAllParts() {
        this.partList.removeAllElements();
    }

    /**
	 * Returns the Scores List of Tracks
	 */
    public Vector getPartList() {
        return partList;
    }

    /**
	 * Returns the all Parts in this Score as a array
	 * @return Part[] An array containing all Part objects in this score
	 */
    public Part[] getPartArray() {
        Vector vct = (Vector) this.partList.clone();
        Part[] partArray = new Part[vct.size()];
        for (int i = 0; i < partArray.length; i++) {
            partArray[i] = (Part) vct.elementAt(i);
        }
        return partArray;
    }

    /**
	 * Get an individual Track object from its title 
	 * @param String title - the name of the Track to return
	 * @return Track answer - the Track to return
	 */
    public Part getPart(String title) {
        Enumeration enumr = partList.elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            if (part.getTitle().equalsIgnoreCase(title)) {
                return part;
            }
        }
        return null;
    }

    /**
	 * Get an individual Track object from its number 
	 * @param int number - the number of the Track to return
	 * @return Track answer - the Track to return
	 */
    public Part getPart(int number) {
        Enumeration enumr = partList.elements();
        int counter = 0;
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            if (counter == number) {
                return part;
            }
            counter++;
        }
        return null;
    }

    public Part[] getPartsOfChannel(int ch) {
        int pchcount = 0;
        Object[] tparr = partList.toArray();
        int[] pcloc = new int[tparr.length];
        Part[] toRetpc;
        for (int i = 0; i < tparr.length; i++) {
            if (((Part) tparr[i]).getChannel() == ch) pcloc[pchcount++] = i;
        }
        toRetpc = new Part[pchcount];
        for (int i = 0; i < toRetpc.length; i++) {
            toRetpc[i] = ((Part) tparr[pcloc[i]]);
        }
        return toRetpc;
    }

    public void fillWithChannelID(Vector tfill, int ich) {
        tfill.clear();
        int[] pindex = new int[this.size()];
        for (int i = 0; i < pindex.length; i++) {
            pindex[i] = 0;
        }
        boolean go = true;
        while (go) {
            go = false;
            int fore = -1;
            for (int i = 0; i < size(); i++) {
                Phrase chphr = getIdChPhrase(i, ich, pindex);
                if (chphr != null) {
                    if (fore == -1 || chphr.getStartTime() < getPart(fore).getPhrase(pindex[fore]).getStartTime()) {
                        fore = i;
                    }
                }
            }
            if (fore > -1) {
                tfill.add(getPart(fore).getPhrase(pindex[fore]++));
            }
        }
    }

    private Phrase getIdChPhrase(int pc, int ic, int[] pindex) {
        if (pindex[pc] == -1) {
            return null;
        }
        while (pindex[pc] < getPart(pc).size() && pindex[pc] > -1) {
            if (getPart(pc).getPhrase(pindex[pc]).getIdChannel() == ic && !getPart(pc).getPhrase(pindex[pc]).getNote(0).isRest()) {
                return getPart(pc).getPhrase(pindex[pc]);
            } else {
                pindex[pc]++;
            }
        }
        pindex[pc] = -1;
        return null;
    }

    /**
	 * Return the title of this Score
	 * @return String title - the name of this Score
	 */
    public String getTitle() {
        return title;
    }

    /**
	 * Assign a title to this Score
	 * @param String title - the name of this Score
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * Returns the Score's tempo 
	 * @return double tempo
	 */
    public double getTempo() {
        return this.tempo;
    }

    /**
	 * Sets the Score's tempo 
	 * @param double tempo
	 */
    public void setTempo(double tempo) {
        this.tempo = tempo;
    }

    /**
	 * Returns the Score's volume 
	 * @return int volume
	 */
    public int getVolume() {
        return this.volume;
    }

    /**
	 * Sets the Score's volume 
	 * @param int volume
	 */
    public void setVolume(int volume) {
        this.volume = volume;
    }

    /**'
	 * Returns the Score's key signature 
	 * The number of sharps (+) or flats (-)
	 * @return int key signature
	 */
    public int getKeySignature() {
        return this.keySignature;
    }

    /**
	 * Specifies the Score's key signature 
	 * The number of sharps (+) or flats (-)
	 * @param int key signature
	 */
    public void setKeySignature(int newSig) {
        this.keySignature = newSig;
    }

    /**
	 * Returns the Score's key quality 
	 * 0 is Major, 1 is minor
	 * @return int key quality
	 */
    public int getKeyQuality() {
        return this.keyQuality;
    }

    /**
	 * Specifies the Score's key quality 
	 * 0 is Major, 1 is minor
	 * @param int key quality (modality)
	 */
    public void setKeyQuality(int newQual) {
        this.keyQuality = newQual;
    }

    /**
	 * Returns the Score's time signature numerator 
	 * @return int time signature numerator
	 */
    public int getNumerator() {
        return this.numerator;
    }

    /**
	 * Specifies the Score's time signature numerator 
	 * @param int time signature numerator
	 */
    public void setNumerator(int num) {
        this.numerator = num;
    }

    /**
	 * Returns the Score's time signature denominator 
	 * @return int time signature denominator
	 */
    public int getDenominator() {
        return this.denominator;
    }

    /**
	 * Specifies the Score's time signature denominator
	 * @param int time signature denominator
	 */
    public void setDenominator(int dem) {
        this.denominator = dem;
    }

    /**
    * Specifies the Score's time signature
     * @param num - Time signature numerator
     * @param dem - Time signature denominator
     */
    public void setTimeSignature(int num, int dem) {
        this.numerator = num;
        this.denominator = dem;
    }

    /**
	 * Make a copy of this Score object
	 * @return Score - return a new Score Object
	 */
    public Score copy() {
        Score newScore = copyEmpty();
        Enumeration enumr = this.partList.elements();
        while (enumr.hasMoreElements()) {
            Part oldPart = (Part) enumr.nextElement();
            newScore.addPart((Part) oldPart.copy());
        }
        return (Score) newScore;
    }

    public Score copyEmpty() {
        Score newScore = new Score(title + " copy");
        newScore.setTempo(this.tempo);
        newScore.setVolume(this.volume);
        return newScore;
    }

    public Score copyLast(int num) {
        Score newScore = copyEmpty();
        Enumeration enumr = this.partList.elements();
        while (enumr.hasMoreElements()) {
            Part oldPart = (Part) enumr.nextElement();
            newScore.addPart((Part) oldPart.copyLast(num));
        }
        return (Score) newScore;
    }

    public Score copy(final double startTime, final double endTime) {
        Score score = this.copy();
        score.removeAllParts();
        int scoresize = this.size();
        for (int i = 0; i < scoresize; i++) {
            score.addPart(this.getPart(i).copy(startTime, endTime));
        }
        return score;
    }

    /**
        * Returns a copy of the Score  between specified loactions
         * @param boolean wether to trim the notes or not
         * @param boolean wether to truncated the notes duration
         * when trimming them or not
         * @param boolean wether to set the start time of the phrases
         * in relation to the start of the
         * <br> old part (true) or the new one (false) maybe should be
         * called "relative to old"
         * @param double start of copy section in beats
         * @param double end of copy section in beats
         * @return Part a copy of section of the Part
         */
    public Score copy(double startTime, double endTime, boolean trimmed, boolean truncated, boolean relativeStartLoc) {
        Score score = this.copy();
        score.removeAllParts();
        int scoresize = this.size();
        for (int i = 0; i < scoresize; i++) {
            score.addPart(this.getPart(i).copy(startTime, endTime, trimmed, truncated, relativeStartLoc));
        }
        return score;
    }

    /**
	 * Return the beat where score ends. Where it's last Part ends.
	 * @return double the Parts endTime
	 */
    public double getEndTime() {
        double endTime = 0.0;
        Enumeration enumr = this.partList.elements();
        while (enumr.hasMoreElements()) {
            Part nextPart = (Part) enumr.nextElement();
            double partEnd = nextPart.getEndTime();
            if (partEnd > endTime) endTime = partEnd;
        }
        return endTime;
    }

    /**
	 * Print the titles of all tracks to stdout
	 */
    public String toString() {
        String scoreData = new String("***** jMusic SCORE: '" + title + "' contains " + this.size() + " parts. ****" + '\n');
        scoreData += "Score Tempo = " + this.tempo + " bpm" + '\n';
        Enumeration enumr = partList.elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            scoreData = scoreData + part.toString() + '\n';
        }
        return scoreData;
    }

    /**
	* Empty removes all elements in the vector
	*/
    public void empty() {
        this.empty(false);
    }

    /**
	* Empty removes all elements in the vector.
	* 
	* java.lang.ClassCastException
	at jm.music.data.Score.empty(Score.java:612)
	* 
        * @param nullObjects If ture this sets all jMusic data objects to null
        *			priot to removing. This facilitates garbage collection.
	*/
    public void empty(boolean nullObjects) {
        if (nullObjects) {
            Enumeration enumr = getPartList().elements();
            while (enumr.hasMoreElements()) {
                Part part = (Part) enumr.nextElement();
                Enumeration enumr2 = part.getPhraseList().elements();
                while (enumr2.hasMoreElements()) {
                    Phrase phrase = (Phrase) enumr2.nextElement();
                    Enumeration enumr3 = part.getPhraseList().elements();
                    while (enumr3.hasMoreElements()) {
                        Note note = (Note) enumr3.nextElement();
                        note = null;
                    }
                    phrase = null;
                }
                part = null;
            }
        }
        partList.removeAllElements();
    }

    /**
	 * Get the number of Parts in this score
	 * @return int  The number of parts
	 */
    public int length() {
        return size();
    }

    /**
	 * Get the number of Parts in this score
	 * @return int  length - the number of parts
	 */
    public int size() {
        return (partList.size());
    }

    /**
	 * Get the number of Parts in this score
	 * @return int  length - the number of parts
	 */
    public int getSize() {
        return (partList.size());
    }

    /**
	 * Remove any empty Parts or phrases from the Score.
	 */
    public void clean() {
        Enumeration enumr = getPartList().elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            part.clean();
            if (part.getPhraseList().size() == 0) {
                this.removePart(part);
            }
        }
    }

    /**
	 * Return the value of the highest note in the Score.
     */
    public int getHighestPitch() {
        int max = 0;
        Enumeration enumr = getPartList().elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            if (part.getHighestPitch() > max) max = part.getHighestPitch();
        }
        return max;
    }

    /**
	 * Return the value of the lowest note in the Score.
     */
    public int getLowestPitch() {
        int min = 127;
        Enumeration enumr = getPartList().elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            if (part.getLowestPitch() < min) min = part.getLowestPitch();
        }
        return min;
    }

    /**
	 * Return the value of the longest rhythm value in the Score.
     */
    public double getLongestRhythmValue() {
        double max = 0.0;
        Enumeration enumr = getPartList().elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            if (part.getLongestRhythmValue() > max) max = part.getLongestRhythmValue();
        }
        return max;
    }

    /**
	 * Return the value of the shortest rhythm value in the Score.
     */
    public double getShortestRhythmValue() {
        double min = 1000.0;
        Enumeration enumr = getPartList().elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            if (part.getShortestRhythmValue() < min) min = part.getShortestRhythmValue();
        }
        return min;
    }

    /**
	* Determine the pan position for all notes in this Score.
	 * @param double the phrase's pan setting
	 */
    public void setPan(double pan) {
        Enumeration enumr = partList.elements();
        while (enumr.hasMoreElements()) {
            Part part = (Part) enumr.nextElement();
            part.setPan(pan);
        }
    }

    /**
        * Generates and returns a new empty part 
        * and adds it to the score.
        */
    public Part createPart() {
        Part p = new Part();
        this.addPart(p);
        return p;
    }

    public void setPartList(Vector vec) {
        this.partList = vec;
    }

    public void sortChan() {
        Part[] arr = this.getPartArray();
        quickSortChan(arr, 0, arr.length - 1);
        this.partList.removeAllElements();
        this.partList.ensureCapacity(arr.length);
        for (int i = 0; i < arr.length; i++) {
            this.partList.add(arr[i]);
        }
    }

    private void quickSortChan(Part[] a, int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        Part mid;
        if (hi0 > lo0) {
            mid = a[(lo0 + hi0) / 2];
            while (lo <= hi) {
                while ((lo < hi0) && (a[lo].getChannel() < mid.getChannel())) ++lo;
                while ((hi > lo0) && (a[hi].getChannel() > mid.getChannel())) --hi;
                if (lo <= hi) {
                    swap(a, lo, hi);
                    ++lo;
                    --hi;
                }
            }
            if (lo0 < hi) quickSortChan(a, lo0, hi);
            if (lo < hi0) quickSortChan(a, lo, hi0);
        }
    }

    private void swap(Part[] parr, int i, int j) {
        Part temp;
        temp = parr[i];
        parr[i] = parr[j];
        parr[j] = temp;
    }
}
