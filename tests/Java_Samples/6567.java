package com.liferay.portal.portletcontainer;

import com.liferay.portlet.PortletURLImpl;
import com.sun.portal.container.ChannelURLType;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletRequest;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="LiferayPortletURLImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Deepak Gothe
 *
 */
public class LiferayPortletURLImpl extends PortletURLImpl {

    public LiferayPortletURLImpl(HttpServletRequest request, String portletId, WindowState windowState, PortletMode portletMode, long plid, String lifecycle) {
        super(request, portletId, plid, lifecycle);
        setLifecycle(getLifecyclePhase(lifecycle));
        setURLType(getChannelURlType(lifecycle));
        try {
            setWindowState(windowState);
        } catch (WindowStateException wse1) {
            if (_log.isWarnEnabled()) {
                _log.warn("Exception while setting window state for " + portletId, wse1);
            }
            try {
                setWindowState(WindowState.NORMAL);
            } catch (WindowStateException wse2) {
            }
        }
        try {
            setPortletMode(portletMode);
        } catch (PortletModeException pme1) {
            if (_log.isWarnEnabled()) {
                _log.warn("Exception while setting portlet mode for " + portletId, pme1);
            }
            try {
                setPortletMode(PortletMode.VIEW);
            } catch (PortletModeException pme2) {
            }
        }
    }

    protected ChannelURLType getChannelURlType(String lifecycle) {
        if (PortletRequest.ACTION_PHASE.equals(lifecycle)) {
            return ChannelURLType.ACTION;
        } else if (PortletRequest.RENDER_PHASE.equals(lifecycle)) {
            return ChannelURLType.RENDER;
        } else if (PortletRequest.RESOURCE_PHASE.equals(lifecycle)) {
            return ChannelURLType.RESOURCE;
        } else {
            return ChannelURLType.RENDER;
        }
    }

    protected String getLifecyclePhase(String lifecycle) {
        if (PortletRequest.RENDER_PHASE.equals(lifecycle)) {
            return PortletRequest.ACTION_PHASE;
        }
        return lifecycle;
    }

    private static Log _log = LogFactory.getLog(LiferayPortletURLImpl.class);
}
