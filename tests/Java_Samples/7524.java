package org.scribble.model;

/**
 * This class represents an interaction.
 * 
 */
public class Interaction extends Behaviour {

    private static final long serialVersionUID = -3628413228540452576L;

    /**
	 * This class returns the message signature.
	 * 
	 * @return The message signature
	 */
    @Reference(containment = true)
    public MessageSignature getMessageSignature() {
        return (m_messageSignature);
    }

    /**
	 * This method sets the message signature.
	 * 
	 * @param signature The message signature
	 */
    public void setMessageSignature(MessageSignature signature) {
        if (m_messageSignature != null) {
            m_messageSignature.setParent(null);
        }
        m_messageSignature = signature;
        if (m_messageSignature != null) {
            m_messageSignature.setParent(this);
        }
    }

    /**
	 * This method returns the optional channel.
	 * 
	 * @return The channel
	 */
    public Channel getChannel() {
        return (m_channel);
    }

    /**
	 * This method sets the channel.
	 * 
	 * @param channel The channel
	 */
    public void setChannel(Channel channel) {
        m_channel = channel;
    }

    /**
	 * This method returns the optional 'from' role.
	 * 
	 * @return The optional 'from' role
	 */
    public Role getFromRole() {
        return (m_fromRole);
    }

    /**
	 * This method sets the optional 'from' role.
	 * 
	 * @param part The optional 'from' role
	 */
    public void setFromRole(Role part) {
        m_fromRole = part;
    }

    /**
	 * This method returns the optional 'to' role.
	 * 
	 * @return The optional 'to' role
	 */
    public Role getToRole() {
        return (m_toRole);
    }

    /**
	 * This method sets the optional 'to' role.
	 * 
	 * @param part The optional 'to' role
	 */
    public void setToRole(Role part) {
        m_toRole = part;
    }

    /**
	 * This method returns the label used to identify
	 * this request.
	 * 
	 * @return The request label
	 */
    public String getRequestLabel() {
        return (m_requestLabel);
    }

    /**
	 * This method sets the label used to identify this
	 * request.
	 * 
	 * @param label The request label
	 */
    public void setRequestLabel(String label) {
        m_requestLabel = label;
    }

    /**
	 * This method returns the label used to correlate
	 * this response with a previous request.
	 * 
	 * @return The replyTo label
	 */
    public String getReplyToLabel() {
        return (m_replyToLabel);
    }

    /**
	 * This method sets the label used to correlate
	 * this response with a previous request.
	 * 
	 * @param label The replyTo label
	 */
    public void setReplyToLabel(String label) {
        m_replyToLabel = label;
    }

    /**
	 * This method returns the list of roles that are
	 * responsible for initiating the activity. This can
	 * be used to determine whether the model is
	 * consistent in terms of decision makers subsequently
	 * initiating actions.
	 * 
	 * @return The list of initiator roles
	 */
    @Override
    public java.util.List<Role> getInitiatorRoles() {
        java.util.List<Role> ret = super.getInitiatorRoles();
        if (getFromRole() != null) {
            if (ret.contains(getFromRole()) == false) {
                ret.add(getFromRole());
            }
        } else {
            Definition defn = getEnclosingDefinition();
            if (defn != null) {
                Role locatedRole = defn.getLocatedName().getRole();
                if (locatedRole != null && getToRole() != null && getToRole().equals(locatedRole) == false && ret.contains(locatedRole) == false) {
                    ret.add(locatedRole);
                }
            }
        }
        return (ret);
    }

    /**
	 * This method returns the list of roles that are
	 * associated with the outcome of the activity.
	 * 
	 * @return The list of final roles
	 */
    @Override
    public java.util.List<Role> getFinalRoles() {
        java.util.List<Role> ret = super.getFinalRoles();
        if (getToRole() != null) {
            if (ret.contains(getToRole()) == false) {
                ret.add(getToRole());
            }
        } else {
            Definition defn = getEnclosingDefinition();
            if (defn != null) {
                Role locatedRole = defn.getLocatedName().getRole();
                if (locatedRole != null && getFromRole() != null && getFromRole().equals(locatedRole) == false && ret.contains(locatedRole) == false) {
                    ret.add(locatedRole);
                }
            }
        }
        return (ret);
    }

    /**
	 * This method returns the list of roles that are
	 * associated with the behaviour.
	 * 
	 * @return The list of associated roles
	 */
    @Override
    public java.util.List<Role> getAssociatedRoles() {
        java.util.List<Role> ret = super.getAssociatedRoles();
        if (getToRole() != null && ret.contains(getToRole()) == false) {
            ret.add(getToRole());
        }
        if (getFromRole() != null && ret.contains(getFromRole()) == false) {
            ret.add(getFromRole());
        }
        if (getToRole() == null || getFromRole() == null) {
            Role locatedRole = getLocatedRole();
            if (locatedRole != null && ret.contains(locatedRole) == false) {
                ret.add(locatedRole);
            }
        }
        return (ret);
    }

    /**
	 * This method returns whether the behaviour is a wait
	 * state.
	 * 
	 * @return Whether the behaviour is a wait state
	 */
    @Override
    public boolean isWaitState() {
        boolean ret = false;
        Role role = getLocatedRole();
        if (role != null && ((getToRole() != null && role.equals(getToRole())) || (getFromRole() != null && role.equals(getFromRole()) == false))) {
            ret = true;
        }
        return (ret);
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();
        if (getMessageSignature() != null) {
            ret.append(getMessageSignature());
            ret.append(" ");
        }
        if (getFromRole() != null) {
            ret.append(getFromRole());
            ret.append("->");
            if (getToRole() != null) {
                ret.append(getToRole());
            }
        } else {
            ret.append("->");
            if (getToRole() != null) {
                ret.append(getToRole());
            }
        }
        return (ret.toString());
    }

    private MessageSignature m_messageSignature = null;

    private Channel m_channel = null;

    private Role m_fromRole = null;

    private Role m_toRole = null;

    private String m_requestLabel = null;

    private String m_replyToLabel = null;
}
