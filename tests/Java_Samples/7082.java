package com.salas.bb.channelguide;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.logging.Logger;
import com.salas.bb.core.*;

/**
 * ChannelGuide - Contains a list of ChannelGuideEntries. A ChannelGuideEntry describes a channel. 
 * There are different sources for CGEs (e.g. a directory on the web, a live RSS feed, etc.)
 * 
 * N.B. This class is persisted as XML using XMLEncoder. XMLEncoder's default behavior is 
 * that it will write out all bean properties. Therefore the part of this object's state
 * which is available as a Bean Propertyu is exactly what will be written out. This is subtle
 * so be careful if you play with getXXX and setXXX methods which is how by default you can
 * tell what's a bean property and what is not.
 * 
 */
public class ChannelGuide {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private List channels;

    protected ChannelGuideEntry selectedCGE;

    private String textName;

    public ChannelGuide() {
        channels = new ArrayList();
        selectedCGE = null;
    }

    int getChanCount() {
        return channels.size();
    }

    /**
	 * newEntry - Add a ChannelGuideEntry to the end of this ChannelGuide. 
	 * 
	 * @param entry - CGE Object
	 */
    public void appendNewEntry(ChannelGuideEntry entry) {
        int index = channels.size();
        channels.add(entry);
        entry.setIndexInChannelGuide(index);
        ChannelGuideModel.SINGLETON.elementsAdded(index + 1, index + 1);
    }

    /**
	 * insertEntryAfter - Add a ChannelGuideEntry to the middle of this ChannelGuide.
	 * 
	 * @param selected - CGE afterwhich new entry is added
	 * @param entry - new entry
	 */
    public void insertEntryAfter(ChannelGuideEntry selected, ChannelGuideEntry entry) {
        log.fine("insertEntryAfter (" + selected + "): " + entry);
        int newIndex = selected.getIndexInChannelGuide() + 1;
        channels.add(newIndex, entry);
        ListIterator iter = channels.listIterator(newIndex);
        while (iter.hasNext()) {
            ((ChannelGuideEntry) iter.next()).setIndexInChannelGuide(newIndex++);
        }
        ChannelGuideModel.SINGLETON.elementsAdded(newIndex, newIndex);
    }

    /**
	 * deleteEntry - Delete the indicated ChannelGuideEntry from this ChannelGuide.
	 * Don't allow the deletion of the last one.
	 * 
	 * @param cge - the CGE to delete
	 * @return - true if successful; false if there was only cge so it couldnt be deleted
	 */
    public boolean deleteEntry(ChannelGuideEntry cge) {
        log.fine("delete Channel: " + cge);
        ChannelGuideEntry cgeNowSelected;
        if (channels.size() == 1) return false;
        int index = cge.getIndexInChannelGuide();
        channels.remove(index);
        ChannelGuideModel.SINGLETON.elementsDeleted(index, index);
        if (selectedCGE == cge) {
            if (index != channels.size()) {
                cgeNowSelected = getEntryAt(index);
            } else {
                cgeNowSelected = getEntryAt(index - 1);
                log.config("Deleted the last CGE(" + cge + ") in ChannelGuide(" + this + ")\n			Now selecting: " + cgeNowSelected);
            }
            selectCGE(cgeNowSelected);
            GlobalController.SINGLETON.getMainframe().showSelectedCGE();
        }
        ListIterator iter = channels.listIterator(index);
        while (iter.hasNext()) {
            ((ChannelGuideEntry) iter.next()).setIndexInChannelGuide(index++);
        }
        return true;
    }

    /**
	 * initTransientState - Called after reading in a persisted GlobalModel to
	 * finish initializing aspects of the state which are not saved.
	 * @TODO: Probably selection should be persisted
	 * @TODO: Need to deal with the case of zero CGEs and zero Articles 
	 */
    public void initTransientState(ChannelGuideSet theSet) {
        log.finer("InitTransient state of: " + this);
        setSelectedCGE(getEntryAt(0));
        selectedCGE.setSelectedArticle(0);
        Iterator cgeIterator = iterator();
        while (cgeIterator.hasNext()) {
            ((ChannelGuideEntry) cgeIterator.next()).initTransientState();
        }
    }

    /**
   * activateDaemons - Start polling the back end for this ChannelGuide.
   * 
   *  - 
   */
    public void activateDaemons() {
        ChannelGuideSet theSet = GlobalModel.SINGLETON.getChannelGuideSet();
        theSet.getInformaBackEnd().activateMemoryChannels4CG(this);
    }

    /**
	 * selectedCGE - 
	 * 
	 * @return - Guide currently selected in this CGE, or null if there isn't one selected
	 * N.B. The only way the selectedCGE should be null is if there are no CGEs in this Guide.
	 * N.B. This method is not called getSelectedCGE for a very specific reason. If it were 
	 * called getSelectedCGE it would make SelectedCGE a bean property, which would then
	 * cause the CGE to be stored in the persistent state of blog bridge. (See persistAsXML)
	 */
    public ChannelGuideEntry selectedCGE() {
        assert selectedCGE != null;
        return selectedCGE;
    }

    public void deselectCGE() {
        if (selectedCGE != null) selectedCGE.deselect();
    }

    public boolean contains(ChannelGuideEntry entry) {
        return (channels.contains(entry));
    }

    public void selectCGE(ChannelGuideEntry entry) {
        assert (contains(entry));
        selectedCGE = entry;
        entry.select();
    }

    public void setSelectedCGE(ChannelGuideEntry entry) {
        selectedCGE = entry;
    }

    /**
	* getEntryAt - Simply return the ChannelGuideEntry that can be found at the specified
	* index.
	* 
	* @param index
	* @return - Specified ChannelGuideEntry 
	*/
    public ChannelGuideEntry getEntryAt(int index) {
        assert index < channels.size();
        log.finer("GetEntryAt: " + index);
        return (ChannelGuideEntry) channels.get(index);
    }

    public String toString() {
        return textName;
    }

    /**
   * iterator - Return an iterator which will traverse all the CGEs of this ChanneGuide.
   * 
   * @return - 
   */
    public CGEIterator iterator() {
        log.fine("Creating new CGEIterator for: " + this);
        Iterator newiterator = new CGEIterator(channels.iterator());
        return (CGEIterator) newiterator;
    }

    /**
	 * getTextName - 
	 * 
	 * @return - 
	 */
    public String getTextName() {
        return textName;
    }

    /**
	 * setTextName - 
	 * 
	 * @param string - 
	 */
    public void setTextName(String string) {
        textName = string;
    }

    /**
	 * getChannels - 
	 * 
	 * @return - 
	 */
    public List getChannels() {
        return channels;
    }

    /**
	 * setChannels - 
	 * 
	 * @param list - 
	 */
    public void setChannels(List list) {
        channels = list;
    }
}
