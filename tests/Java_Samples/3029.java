package gov.sns.ca.correlator;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.tools.correlator.CorrelationFilter;
import gov.sns.tools.correlator.Correlator;
import gov.sns.tools.correlator.RecordFilter;
import gov.sns.tools.correlator.SourceAgent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * ChannelCorrelator is a subclass of the Correlator specifically for correlating
 * channel monitor events.  It adds convenience methods that make it easier 
 * to add channels as sources.  It implements <code>newSourceAgent()</code> to 
 * generate a ChannelAgent as a source agent.
 * The Correlator is the class that is used to setup monitoring of correlated
 * events.  It is the sole entry point to the outside world.  When correlations 
 * are found, the Correlator broadcasts the correlation.
 *
 * Note that all time is in seconds unless otherwise stated.
 *
 * @author  tap
 */
public class ChannelCorrelator extends Correlator {

    /** Creates new ChannelCorrelator */
    public ChannelCorrelator(double aBinTimespan) {
        this(aBinTimespan, null);
    }

    /** 
     * Creates new ChannelCorrelator.
     * @param aBinTimespan The time resolution for accepting two events as correlated.
     * @param aFilter A filter to apply to the correlation.
     */
    public ChannelCorrelator(double aBinTimespan, CorrelationFilter aFilter) {
        super(aBinTimespan, aFilter);
    }

    /**
     * Overrides the parent method to create and return a ChannelAgent as a 
     * source agent for this correlator.
     * @param source The new source to monitor and correlate.
     * @param sourceName The name to be associated with the source.
     * @param recordFilter The filter to apply to the source's records.
     */
    @Override
    protected SourceAgent newSourceAgent(Object source, String sourceName, RecordFilter recordFilter) {
        Channel channel = (Channel) source;
        ChannelAgent channelAgent = new ChannelAgent(localCenter, channel, sourceName, recordFilter, correlationTester);
        return channelAgent;
    }

    /** 
     * Get the number of actively monitored channels.
     * @return The number of actively monitored channels.
     */
    public synchronized int numActiveChannels() {
        return numSources() - numInactiveChannels();
    }

    /** 
     * Get the number of channels that are inactive due to connection or monitor 
     * failure or simply not monitored.
     * @return The number of channels that are inactive.
     */
    public synchronized int numInactiveChannels() {
        int numFailed = 0;
        Collection allSources = getSourceAgents();
        Iterator sourceIter = allSources.iterator();
        while (sourceIter.hasNext()) {
            ChannelAgent channelAgent = (ChannelAgent) sourceIter.next();
            numFailed += (channelAgent.isActive()) ? 0 : 1;
        }
        return numFailed;
    }

    /** 
     * Get the names of channels that are not being monitored due to connection 
     * or monitor failure or simply not monitoried.
     * @return The collection of names of channels that are not active.
     */
    public synchronized Collection inactiveChannelsByName() {
        Collection failedChannels = new HashSet();
        Collection allSources = getSourceAgents();
        Iterator sourceIter = allSources.iterator();
        while (sourceIter.hasNext()) {
            ChannelAgent channelAgent = (ChannelAgent) sourceIter.next();
            if (!channelAgent.isActive()) {
                failedChannels.add(channelAgent.name());
            }
        }
        return failedChannels;
    }

    /** 
     * Add a channel to monitor.  If we already monitor a channel, do nothing.
     * @param channelId The PV name to monitor. 
     */
    public void addChannel(String channelId) {
        addChannel(channelId, null);
    }

    /** 
     * Add a channel to monitor.  If we already monitor a channel, do nothing. 
     * The record filter is used to determine whether or not to accept the 
     * reading of the specified channel when the event is handled.
     * @param channelId The PV name to monitor.
     * @param recordFilter The filter to apply to the channel's records.
     */
    public final void addChannel(String channelId, RecordFilter recordFilter) {
        if (hasSource(channelId)) return;
        Channel channel = ChannelFactory.defaultFactory().getChannel(channelId);
        addChannel(channel, recordFilter);
    }

    /** 
     * Add a channel to monitor.  If we already monitor a channel, do nothing. 
     * @param channel The channel to monitor for correlations.
     */
    public final void addChannel(Channel channel) {
        addChannel(channel, (RecordFilter) null);
    }

    /** 
     * Add a channel to monitor.  If we already monitor a channel, do nothing. 
     * The record filter is used to determine whether or not to accept the 
     * reading of the specified channel when the event is handled.
     * @param channel The channel to monitor for correlations.
     * @param recordFilter The filter to apply to the channel's records.
     */
    public final void addChannel(Channel channel, RecordFilter recordFilter) {
        String channelId = channel.getId();
        addChannel(channel, channelId, recordFilter);
    }

    /** 
     * Add a channel to monitor.  If we already monitor a channel, do nothing. 
     * This method allows channels to be specified with an alternate id than 
     * the default one.
     * @param channel The channel to monitor for correlations.
     * @param channelId A unique identifier of the channel.
     */
    public final void addChannel(Channel channel, String channelId) {
        addChannel(channel, channelId, null);
    }

    /** 
     * Add a channel to monitor.  If we already monitor a channel, do nothing. 
     * The record filter is used to determine whether or not to accept the 
     * reading of the specified channel when the event is handled.
     * This method allows channels to be specified with an alternate id than 
     * the default one.
     * @param channel The channel to monitor for correlations.
     * @param recordFilter The filter to apply to the channel's records.
     */
    public final synchronized void addChannel(Channel channel, String channelId, RecordFilter recordFilter) {
        addSource(channel, channelId, recordFilter);
    }

    /** 
     * Stop managing the specified channel. 
     * @param channel The channel we are requesting to stop monitoring and correlating.
     */
    public void removeChannel(Channel channel) {
        String channelId = channel.getId();
        removeChannel(channelId);
    }

    /** 
     * Stop managing the specified channel 
     * @param channel The id of the channel we are requesting to stop monitoring and correlating.
     */
    public void removeChannel(String channelId) {
        removeSource(channelId);
    }

    /** 
     * Remove all registered channels.
     */
    public void removeAllChannels() {
        removeAllSources();
    }
}
