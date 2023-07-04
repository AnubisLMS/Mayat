package org.atricore.idbus.kernel.main.mediation.provider;

import org.atricore.idbus.kernel.main.mediation.channel.AbstractFederationChannel;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a set of cohesive operations, like a protocol.
 *
 * Services have a default channel, and a set of overrides or profiles for that channel.
 *
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class FederationService implements Serializable {

    private String name;

    private String serviceType;

    private FederationChannel channel;

    private Set<FederationChannel> overrideChannels = new HashSet<FederationChannel>();

    public FederationService() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FederationService(FederationChannel channel) {
        this.channel = channel;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public FederationChannel getChannel() {
        return channel;
    }

    public void setChannel(FederationChannel channel) {
        this.channel = channel;
        ((AbstractFederationChannel) channel).setConfiguration(serviceType);
    }

    public Set<FederationChannel> getOverrideChannels() {
        return overrideChannels;
    }
}
