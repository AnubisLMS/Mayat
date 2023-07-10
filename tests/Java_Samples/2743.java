package net.sf.jvdr.http.servlet.epg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.NoResultException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sf.exlp.util.DateUtil;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.data.comparator.EPGEntryComarator;
import net.sf.jvdr.data.ejb.VdrConfigTimer;
import net.sf.jvdr.data.ejb.VdrImageSearch;
import net.sf.jvdr.data.ejb.VdrUser;
import net.sf.jvdr.data.facade.VdrPersistence;
import net.sf.jvdr.util.Badge;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.HtmlHref;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.elements.WanParagraph;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.layer.WanLayer;
import net.sf.jwan.servlet.gui.menu.WanMenu;
import net.sf.jwan.servlet.gui.menu.WanMenuEntry;
import net.sf.jwan.servlet.util.ServletForm;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;
import org.hampelratte.svdrp.responses.highlevel.VDRTimer;

public class EpgDetailsServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(EpgDetailsServlet.class);

    public static final long serialVersionUID = 1;

    private WanLayer lyrEpgTimer, lyrSmartEpg, lyrImgSelect;

    private Configuration config;

    public EpgDetailsServlet(Configuration config) {
        super("lEpgDetail");
        this.config = config;
        layerTitle = "EPG Details";
        layerServletPath = "async";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        alWanRenderables.clear();
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) this.getServletContext().getAttribute(VdrCache.class.getSimpleName());
        ServletForm form = new ServletForm(request);
        HttpSession session = request.getSession();
        EPGEntry epg = vdrC.getEpg(form.get("chNu"), form.get("st"));
        List<EPGEntry> lEpgRepeats = vdrC.getEpgRepeats(epg, true);
        EPGEntryComarator epgComparator = new EPGEntryComarator();
        Comparator<EPGEntry> comparator = epgComparator.getComparator(EPGEntryComarator.CompTyp.DateDown);
        Collections.sort(lEpgRepeats, comparator);
        if (form.isAvailable("r") && form.getBoolean("r")) {
            asyncZone = getLayerId() + "R";
            createRepeats(lEpgRepeats, vdrC);
        } else {
            int epgRepeats = 0;
            if (lEpgRepeats != null) {
                epgRepeats = lEpgRepeats.size();
            }
            asyncZone = "wa" + getLayerId();
            createEpgDetail(form, epgRepeats, session, vdrP);
        }
        PrintWriter out = response.getWriter();
        try {
            out.println(renderAsync());
        } catch (WanRenderException e) {
            logger.error(e);
        } finally {
            out.close();
        }
    }

    public void createRepeats(List<EPGEntry> lEpgRepeats, VdrCache vdrC) {
        if (lEpgRepeats.size() > 0) {
            WanMenu wm = new WanMenu();
            wm.setMenuType(WanMenu.MenuType.SIMPLE);
            wm.setDiv(false);
            wm.setTitle("Wiederholung Aufnehmen");
            for (EPGEntry eRepeat : lEpgRepeats) {
                StringBuffer sbR = new StringBuffer();
                sbR.append(DateUtil.dayName(eRepeat.getStartTime(), 2));
                sbR.append(", " + DateUtil.tm(eRepeat.getStartTime()));
                sbR.append(": " + DateUtil.sm(eRepeat.getStartTime()));
                WanMenuEntry wmi = new WanMenuEntry();
                wmi.setHeader(eRepeat.getChannelName());
                wmi.setName(sbR.toString());
                HtmlHref href = lyrEpgTimer.getLayerTarget();
                href.setRev(HtmlHref.Rev.async);
                href.addHtPa("rep", 0);
                href.addHtPa("chNu", vdrC.getChNum(eRepeat.getChannelID()));
                href.addHtPa("st", eRepeat.getStartTime().getTimeInMillis());
                wmi.setHtmlref(href);
                wm.addItem(wmi);
            }
            alWanRenderables.add(wm);
        }
    }

    public void createEpgDetail(ServletForm form, int epgRepeats, HttpSession session, VdrPersistence vdrP) {
        VdrUser vu = (VdrUser) session.getAttribute(VdrUser.class.getSimpleName());
        VdrConfigTimer vcr = vdrP.fVdrConfigTimer(vu);
        VdrCache vdrC = (VdrCache) this.getServletContext().getAttribute(VdrCache.class.getSimpleName());
        String chNu = form.get("chNu");
        String st = form.get("st");
        EPGEntry epg = vdrC.getEpg(chNu, st);
        String style = "float:center;margin:5px";
        String recordIcon = config.getString("icons/icon[@type='record']");
        if (isTimerConflict(vcr, epg, new Integer(chNu), vdrC)) {
            recordIcon = config.getString("icons/icon[@type='recordwarn']");
        }
        HtmlHref hrefTimer = lyrEpgTimer.getLayerTarget();
        hrefTimer.setContent("<img src=\"" + config.getString("icons/@dir") + "/" + recordIcon + "\" style=\"" + style + "\" />");
        hrefTimer.setRev(HtmlHref.Rev.async);
        hrefTimer.addHtPa("chNu", chNu);
        hrefTimer.addHtPa("st", st);
        HtmlHref hrefSmart = lyrSmartEpg.getLayerTarget();
        hrefSmart.setContent("<img src=\"" + config.getString("icons/@dir") + "/" + config.getString("icons/icon[@type='smart']") + "\" style=\"" + style + "\" />");
        hrefSmart.setRev(HtmlHref.Rev.async);
        hrefSmart.addHtPa("chNu", chNu);
        hrefSmart.addHtPa("st", st);
        hrefSmart.addHtPa("new", true);
        String badgeId = "search";
        HtmlHref hrefRepeat = this.getLayerTarget();
        hrefRepeat.setContent("<img src=\"async/badge?badgeId=" + badgeId + "&number=" + epgRepeats + "\" style=\"" + style + "\" />");
        hrefRepeat.addHtPa("chNu", vdrC.getChNum(epg.getChannelID()));
        hrefRepeat.addHtPa("st", epg.getStartTime().getTimeInMillis());
        hrefRepeat.addHtPa("r", true);
        hrefRepeat.setRev(HtmlHref.Rev.async);
        Badge badge = new Badge();
        badge.setSourceImage("resources/images/src/system-search.svg");
        badge.setBadgeImage("resources/images/src/badge-search.svg");
        badge.setNumber(epgRepeats);
        session.removeAttribute(badgeId);
        session.setAttribute(badgeId, badge);
        badgeId = "image";
        int anzBilder;
        try {
            VdrImageSearch vis = vdrP.fVdrImageSearch(epg.getTitle());
            anzBilder = vis.getThumbnails().size();
        } catch (NoResultException e) {
            anzBilder = 0;
        }
        HtmlHref hrefImage = lyrImgSelect.getLayerTarget();
        hrefImage.setContent("<img src=\"async/badge?badgeId=" + badgeId + "&number=" + anzBilder + "\" style=\"" + style + "\" />");
        hrefImage.addHtPa("chNu", vdrC.getChNum(epg.getChannelID()));
        hrefImage.addHtPa("st", epg.getStartTime().getTimeInMillis());
        hrefImage.setRev(HtmlHref.Rev.async);
        badge = new Badge();
        badge.setSourceImage("resources/images/src/image-x-generic.svg");
        badge.setBadgeImage("resources/images/src/badge-image.svg");
        badge.setNumber(anzBilder);
        session.removeAttribute(badgeId);
        session.setAttribute(badgeId, badge);
        StringBuffer sbTitel = new StringBuffer();
        sbTitel.append(epg.getChannelName() + "<br/>");
        sbTitel.append("<b>" + epg.getTitle() + "</b><br/>");
        sbTitel.append(DateUtil.dayName(epg.getStartTime()) + " " + DateUtil.sm(epg.getStartTime()) + " - " + DateUtil.sm(epg.getEndTime()) + "<br/>");
        WanParagraph wpProgramInfo = new WanParagraph(sbTitel.toString());
        StringBuffer sbAction = new StringBuffer();
        sbAction.append(hrefTimer.render());
        sbAction.append(hrefSmart.render());
        sbAction.append(hrefRepeat.render());
        sbAction.append(hrefImage.render());
        WanParagraph wpUserAction = new WanParagraph(sbAction.toString());
        WanDiv wdRepeat = new WanDiv(WanDiv.DivClass.iMenu);
        wdRepeat.setId(getLayerId() + "R");
        alWanRenderables.add(wdRepeat);
        WanDiv wd = new WanDiv(WanDiv.DivClass.iBlock);
        wd.addContent(wpProgramInfo);
        wd.addContent(wpUserAction);
        wd.addContent(new WanParagraph(epg.getDescription()));
        alWanRenderables.add(wd);
    }

    private boolean isTimerConflict(VdrConfigTimer vcr, EPGEntry epg, int chNumber, VdrCache vdrC) {
        int before, after;
        if (vcr != null && vcr.isOwn()) {
            before = vcr.getTimeBefore();
            after = vcr.getTimeAfter();
        } else {
            before = config.getInt("vdr/timer/@minBefore");
            after = config.getInt("vdr/timer/@minAfter");
        }
        Date startDate = new Date(epg.getStartTime().getTime().getTime());
        Date endDate = new Date(epg.getEndTime().getTime().getTime());
        GregorianCalendar gcStartDate = DateUtil.getGC4D(startDate);
        GregorianCalendar gcEndDate = DateUtil.getGC4D(endDate);
        VDRTimer vdrTimer = new VDRTimer();
        vdrTimer.setStartTime(gcStartDate);
        vdrTimer.setEndTime(gcEndDate);
        vdrTimer.setChannelNumber(chNumber);
        vdrTimer.setID(-1);
        vdrTimer.getStartTime().add(Calendar.MINUTE, after);
        vdrTimer.getEndTime().add(Calendar.MINUTE, before);
        boolean isTimerConflict = vdrC.isTimerConflict(vdrTimer);
        logger.debug("Timer Conflict? " + isTimerConflict);
        return isTimerConflict;
    }

    public void setLyrEpgTimer(WanLayer lyrEpgTimer) {
        this.lyrEpgTimer = lyrEpgTimer;
    }

    public void setLyrSmartEpg(WanLayer lyrSmartEpg) {
        this.lyrSmartEpg = lyrSmartEpg;
    }

    public void setLyrImgSelect(WanLayer lyrImgSelect) {
        this.lyrImgSelect = lyrImgSelect;
    }
}
