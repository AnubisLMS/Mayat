package com.volantis.mps.attachment;

import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.mps.message.MessageException;
import com.volantis.synergetics.localization.ExceptionLocalizer;

/**
 * Represents a device-specific {@link MessageAttachment} to be used with a
 * {@link com.volantis.mps.message.MultiChannelMessage MultiChannelMessage}.
 *
 * @volantis-api-include-in PublicAPI
 * @volantis-api-include-in ProfessionalServicesAPI
 * @volantis-api-include-in InternalAPI
 */
public class DeviceMessageAttachment extends MessageAttachment {

    /**
     * The exception localizer instance for this class.
     */
    private static final ExceptionLocalizer localizer = LocalizationFactory.createExceptionLocalizer(DeviceMessageAttachment.class);

    /**
     * The name of the device that the attachment is intended for
     */
    private String deviceName;

    /**
     * The channel name to which the attachment should be sent
     */
    private String channelName;

    /**
     * Initialize the new instance with an undefined type.
     */
    public DeviceMessageAttachment() {
    }

    /**
     * Initialize the new instance with a known type.
     *
     * @param value       The URL or name of the file that contains the
     *                    attachment content. Cannot be null.
     * @param mimeType    The attachment content type. Cannot be null.
     * @param valueType   The attachment type (URL or File).
     * @param deviceName  The name of the device that this attachment is
     *                    intended for. Cannot be null.
     * @param channelName The name of the channel on which the attachment
     *                    will be sent. Cannot be null.
     *
     * @throws MessageException if there were problems creating the message
     *                          attachment with the given values.
     */
    public DeviceMessageAttachment(String value, String mimeType, int valueType, String deviceName, String channelName) throws MessageException {
        super(value, mimeType, valueType);
        setDeviceName(deviceName);
        setChannelName(channelName);
    }

    /**
     * Sets the name of the device for which this attachment is intended.
     *
     * @param deviceName The name of the device. Cannot be null.
     * @throws MessageException if <code>deviceName</code> is null.
     */
    public void setDeviceName(String deviceName) throws MessageException {
        if (deviceName == null) {
            throw new MessageException(localizer.format("device-name-null-invalid"));
        }
        this.deviceName = deviceName;
    }

    /**
     * Returns the name of the device for which this attachment is intended.
     *
     * @return The name of the device.
     * @throws MessageException if there were problems when retrieving the
     *                          device name.
     */
    public String getDeviceName() throws MessageException {
        return deviceName;
    }

    /**
     * Sets the name of the channel on which to send the attachment.
     *
     * @param channelName The name of the channel. Cannot be null.
     * @throws MessageException if <code>channelName</code> is null.
     */
    public void setChannelName(String channelName) throws MessageException {
        if (channelName == null) {
            throw new MessageException(localizer.format("channel-name-null-invalid"));
        }
        this.channelName = channelName;
    }

    /**
     * Gets the name of the channel on which this attachment will be sent.
     *
     * @return The channel name.
     * @throws MessageException if there were problems retrieving the channel
     *                          name.
     */
    public String getChannelName() throws MessageException {
        return channelName;
    }

    public boolean equals(Object object) {
        boolean isEqual = false;
        if (super.equals(object)) {
            DeviceMessageAttachment attachment = (DeviceMessageAttachment) object;
            if (channelName != null ? channelName.equals(attachment.channelName) : attachment.channelName == null) {
                if (deviceName != null ? deviceName.equals(attachment.deviceName) : attachment.deviceName == null) {
                    isEqual = true;
                }
            }
        }
        return isEqual;
    }
}
