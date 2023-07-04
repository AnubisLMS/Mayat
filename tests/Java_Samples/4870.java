package cn.sduo.app.util.mail;

import java.io.File;
import javax.mail.internet.MimeMessage;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

public class MailHelper {

    private final Log logger = LogFactory.getLog(this.getClass());

    private JavaMailSender mailSender;

    private long sendTimeout;

    private MessageChannel channel;

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public MailHelper() {
        super();
    }

    public MailHelper(JavaMailSender mailSender) {
        super();
        this.mailSender = mailSender;
    }

    public MailHelper(MessageChannel channel) {
        super();
        this.channel = channel;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    public long getSendTimeout() {
        return sendTimeout;
    }

    public void setSendTimeout(long sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    public boolean sendMail(PCCWEMailData mailData, boolean isAsync) {
        logger.info("Send Email Start................");
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            if (mailData.isStreamAttachmentExist() || mailData.isFileAttachmentExist()) {
                helper = new MimeMessageHelper(msg, true, "UTF-8");
            } else {
                helper = new MimeMessageHelper(msg, false, "UTF-8");
            }
            helper.setTo(mailData.getTo());
            helper.setFrom(mailData.getFrom());
            helper.setSubject(mailData.getSubject());
            helper.setText(mailData.getText());
            if (!CollectionUtils.isEmpty(mailData.getCc())) {
                int size = mailData.getCc().size();
                helper.setCc(mailData.getCc().toArray(new String[size]));
            }
            if (mailData.isStreamAttachmentExist()) {
                for (StreamAttachmentDataSource streamAttachment : mailData.getStreamAttachmentList()) {
                    helper.addAttachment(streamAttachment.getName(), streamAttachment);
                }
            }
            if (mailData.isFileAttachmentExist()) {
                for (File fileAttachment : mailData.getFileAttachmentList()) helper.addAttachment(fileAttachment.getName(), new FileSystemResource(fileAttachment));
            }
            logger.info("Sending Email ................");
            logger.info("Send Email to: " + mailData.getTo());
            logger.info("Send Email subject: " + mailData.getSubject());
            logger.info("Send Email text: " + mailData.getText());
            if (!isAsync) {
                if (mailSender != null) {
                    logger.info(msg);
                    mailSender.send(msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean result = false;
        if (channel != null && isAsync) {
            GenericMessage<MimeMessage> genericMIMEMsg = new GenericMessage<MimeMessage>(msg);
            result = channel.send(genericMIMEMsg, sendTimeout);
            logger.info("Message sent successfully? " + result);
        }
        logger.info("Sending Email End................");
        return result;
    }

    public boolean sendAsyncMail(PCCWEMailData mailData) {
        logger.info("Send Async Email Start................");
        boolean result = this.sendMail(mailData, true);
        logger.info("Sending Async Email End................");
        return result;
    }

    public boolean sendSyncMail(PCCWEMailData mailData) {
        logger.info("Send sync Email Start................");
        boolean result = this.sendMail(mailData, false);
        logger.info("Sending sync Email End................");
        return result;
    }
}
