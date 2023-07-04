package org.remus.infomngmnt.connector.googlecontacts;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.remus.infomngmnt.connector.googlecontacts.ContactsExampleParameters.Actions;
import com.google.gdata.client.Query;
import com.google.gdata.client.Service;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.client.http.HttpGDataRequest;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.contacts.ContactGroupEntry;
import com.google.gdata.data.contacts.ContactGroupFeed;
import com.google.gdata.data.extensions.ExtendedProperty;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.NoLongerAvailableException;
import com.google.gdata.util.ServiceException;

/**
 * Example command-line utility that demonstrates how to use the Google Data API
 * Java client libraries for Contacts. The example allows to run all the basic
 * contact related operations such as adding new contact, listing all contacts,
 * updating existing contacts, deleting the contacts.
 * <p/>
 * Full documentation about the API can be found at:
 * http://code.google.com/apis/contacts/
 * 
 * 
 * 
 * 
 */
public class ContactsExample {

    private enum SystemGroup {

        MY_CONTACTS("Contacts", "My Contacts"), FRIENDS("Friends", "Friends"), FAMILY("Family", "Family"), COWORKERS("Coworkers", "Coworkers");

        private final String systemGroupId;

        private final String prettyName;

        SystemGroup(final String systemGroupId, final String prettyName) {
            this.systemGroupId = systemGroupId;
            this.prettyName = prettyName;
        }

        static SystemGroup fromSystemGroupId(final String id) {
            for (SystemGroup group : SystemGroup.values()) {
                if (id.equals(group.systemGroupId)) {
                    return group;
                }
            }
            throw new IllegalArgumentException("Unrecognized system group id: " + id);
        }

        @Override
        public String toString() {
            return this.prettyName;
        }
    }

    /**
	 * Base URL for the feed
	 */
    private final URL feedUrl;

    /**
	 * Service used to communicate with contacts feed.
	 */
    private final ContactsService service;

    /**
	 * Projection used for the feed
	 */
    private final String projection;

    /**
	 * The ID of the last added contact or group. Used in case of script
	 * execution - you can add and remove contact just created.
	 */
    private static String lastAddedId;

    /**
	 * Reference to the logger for setting verbose mode.
	 */
    private static final Logger httpRequestLogger = Logger.getLogger(HttpGDataRequest.class.getName());

    /**
	 * Contacts Example.
	 * 
	 * @param parameters
	 *            command line parameters
	 */
    public ContactsExample(final ContactsExampleParameters parameters) throws MalformedURLException, AuthenticationException {
        this.projection = parameters.getProjection();
        String url = parameters.getBaseUrl() + (parameters.isGroupFeed() ? "groups/" : "contacts/") + parameters.getUserName() + "/" + this.projection;
        this.feedUrl = new URL(url);
        this.service = new ContactsService("Google-contactsExampleApp-3");
        String userName = parameters.getUserName();
        String password = parameters.getPassword();
        if (userName == null || password == null) {
            return;
        }
        this.service.setUserCredentials(userName, password);
    }

    /**
	 * Deletes a contact or a group
	 * 
	 * @param parameters
	 *            the parameters determining contact to delete.
	 */
    private void deleteEntry(final ContactsExampleParameters parameters) throws IOException, ServiceException {
        if (parameters.isGroupFeed()) {
            ContactGroupEntry group = getGroupInternal(parameters.getId());
            if (group == null) {
                System.err.println("No Group found with id: " + parameters.getId());
                return;
            }
            group.delete();
        } else {
            ContactEntry contact = getContactInternal(parameters.getId());
            if (contact == null) {
                System.err.println("No contact found with id: " + parameters.getId());
                return;
            }
            contact.delete();
        }
    }

    /**
	 * Updates a contact or a group. Presence of any property of a given kind
	 * (im, phone, mail, etc.) causes the existing properties of that kind to be
	 * replaced.
	 * 
	 * @param parameters
	 *            parameters storing updated contact values.
	 */
    public void updateEntry(final ContactsExampleParameters parameters) throws IOException, ServiceException {
        if (parameters.isGroupFeed()) {
            ContactGroupEntry group = buildGroup(parameters);
            ContactGroupEntry canonicalGroup = getGroupInternal(parameters.getId());
            canonicalGroup.setTitle(group.getTitle());
            canonicalGroup.setContent(group.getContent());
            List<ExtendedProperty> extendedProperties = canonicalGroup.getExtendedProperties();
            extendedProperties.clear();
            if (group.hasExtendedProperties()) {
                extendedProperties.addAll(group.getExtendedProperties());
            }
            printGroup(canonicalGroup.update());
        } else {
            ContactEntry contact = buildContact(parameters);
            ContactEntry canonicalContact = getContactInternal(parameters.getId());
            ElementHelper.updateContact(canonicalContact, contact);
            printContact(canonicalContact.update());
        }
    }

    /**
	 * Gets a contact by it's id.
	 * 
	 * @param id
	 *            the id of the contact.
	 * @return the ContactEntry or null if not found.
	 */
    private ContactEntry getContactInternal(final String id) throws IOException, ServiceException {
        return this.service.getEntry(new URL(id.replace("/base/", "/" + this.projection + "/")), ContactEntry.class);
    }

    /**
	 * Gets a Group by it's id.
	 * 
	 * @param id
	 *            the id of the group.
	 * @return the GroupEntry or null if not found.
	 */
    private ContactGroupEntry getGroupInternal(final String id) throws IOException, ServiceException {
        return this.service.getEntry(new URL(id.replace("/base/", "/" + this.projection + "/")), ContactGroupEntry.class);
    }

    /**
	 * Print the contents of a ContactEntry to System.err.
	 * 
	 * @param contact
	 *            The ContactEntry to display.
	 */
    private static void printContact(final ContactEntry contact) {
        System.err.println("Id: " + contact.getId());
        if (contact.getTitle() != null) {
            System.err.println("Contact name: " + contact.getTitle().getPlainText());
        } else {
            System.err.println("Contact has no name");
        }
        System.err.println("Last updated: " + contact.getUpdated().toUiString());
        if (contact.hasDeleted()) {
            System.err.println("Deleted:");
        }
        ElementHelper.printContact(System.err, contact);
        Link photoLink = contact.getLink("http://schemas.google.com/contacts/2008/rel#photo", "image/*");
        System.err.println("Photo link: " + photoLink.getHref());
        String photoEtag = photoLink.getEtag();
        System.err.println("  Photo ETag: " + (photoEtag != null ? photoEtag : "(No contact photo uploaded)"));
        System.err.println("Self link: " + contact.getSelfLink().getHref());
        System.err.println("Edit link: " + contact.getEditLink().getHref());
        System.err.println("ETag: " + contact.getEtag());
        System.err.println("-------------------------------------------\n");
    }

    /**
	 * Prints the contents of a GroupEntry to System.err
	 * 
	 * @param groupEntry
	 *            The GroupEntry to display
	 */
    private static void printGroup(final ContactGroupEntry groupEntry) {
        System.err.println("Id: " + groupEntry.getId());
        System.err.println("Group Name: " + groupEntry.getTitle().getPlainText());
        System.err.println("Last Updated: " + groupEntry.getUpdated());
        System.err.println("Extended Properties:");
        for (ExtendedProperty property : groupEntry.getExtendedProperties()) {
            if (property.getValue() != null) {
                System.err.println("  " + property.getName() + "(value) = " + property.getValue());
            } else if (property.getXmlBlob() != null) {
                System.err.println("  " + property.getName() + "(xmlBlob) = " + property.getXmlBlob().getBlob());
            }
        }
        System.err.print("Which System Group: ");
        if (groupEntry.hasSystemGroup()) {
            SystemGroup systemGroup = SystemGroup.fromSystemGroupId(groupEntry.getSystemGroup().getId());
            System.err.println(systemGroup);
        } else {
            System.err.println("(Not a system group)");
        }
        System.err.println("Self Link: " + groupEntry.getSelfLink().getHref());
        if (!groupEntry.hasSystemGroup()) {
            System.err.println("Edit Link: " + groupEntry.getEditLink().getHref());
        }
        System.err.println("-------------------------------------------\n");
    }

    /**
	 * Processes script consisting of sequence of parameter lines in the same
	 * form as command line parameters.
	 * 
	 * @param example
	 *            object controlling the execution
	 * @param parameters
	 *            parameters passed from command line
	 */
    private static void processScript(final ContactsExample example, final ContactsExampleParameters parameters) throws IOException, ServiceException {
        BufferedReader reader = new BufferedReader(new FileReader(parameters.getScript()));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                ContactsExampleParameters newParams = new ContactsExampleParameters(parameters, line);
                processAction(example, newParams);
                if (lastAddedId != null) {
                    parameters.setId(lastAddedId);
                    lastAddedId = null;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
	 * Performs action specified as action parameter.
	 * 
	 * @param example
	 *            object controlling the execution
	 * @param parameters
	 *            parameters from command line or script
	 */
    private static void processAction(final ContactsExample example, final ContactsExampleParameters parameters) throws IOException, ServiceException {
        Actions action = parameters.getAction();
        System.err.println("Executing action: " + action);
        switch(action) {
            case LIST:
                example.listEntries(parameters);
                break;
            case QUERY:
                example.queryEntries(parameters);
                break;
            case ADD:
                example.addEntry(parameters);
                break;
            case DELETE:
                example.deleteEntry(parameters);
                break;
            case UPDATE:
                example.updateEntry(parameters);
                break;
            default:
                System.err.println("No such action");
        }
    }

    /**
	 * Query entries (Contacts/Groups) according to parameters specified.
	 * 
	 * @param parameters
	 *            parameter for contact quest
	 */
    private void queryEntries(final ContactsExampleParameters parameters) throws IOException, ServiceException {
        Query myQuery = new Query(this.feedUrl);
        if (parameters.getUpdatedMin() != null) {
            DateTime startTime = DateTime.parseDateTime(parameters.getUpdatedMin());
            myQuery.setUpdatedMin(startTime);
        }
        if (parameters.getMaxResults() != null) {
            myQuery.setMaxResults(parameters.getMaxResults().intValue());
        }
        if (parameters.getStartIndex() != null) {
            myQuery.setStartIndex(parameters.getStartIndex());
        }
        if (parameters.isShowDeleted()) {
            myQuery.setStringCustomParameter("showdeleted", "true");
        }
        if (parameters.getRequireAllDeleted() != null) {
            myQuery.setStringCustomParameter("requirealldeleted", parameters.getRequireAllDeleted());
        }
        if (parameters.getSortorder() != null) {
            myQuery.setStringCustomParameter("sortorder", parameters.getSortorder());
        }
        if (parameters.getOrderBy() != null) {
            myQuery.setStringCustomParameter("orderby", parameters.getOrderBy());
        }
        if (parameters.getGroup() != null) {
            myQuery.setStringCustomParameter("group", parameters.getGroup());
        }
        try {
            if (parameters.isGroupFeed()) {
                ContactGroupFeed groupFeed = this.service.query(myQuery, ContactGroupFeed.class);
                for (ContactGroupEntry entry : groupFeed.getEntries()) {
                    printGroup(entry);
                }
                System.err.println("Total: " + groupFeed.getEntries().size() + " entries found");
            } else {
                ContactFeed resultFeed = this.service.query(myQuery, ContactFeed.class);
                for (ContactEntry entry : resultFeed.getEntries()) {
                    printContact(entry);
                }
                System.err.println("Total: " + resultFeed.getEntries().size() + " entries found");
            }
        } catch (NoLongerAvailableException ex) {
            System.err.println("Not all placehorders of deleted entries are available");
        }
    }

    /**
	 * List Contacts or Group entries (no parameter are taken into account)
	 * Note! only 25 results will be returned - this is default.
	 * 
	 * @param parameters
	 */
    private void listEntries(final ContactsExampleParameters parameters) throws IOException, ServiceException {
        if (parameters.isGroupFeed()) {
            ContactGroupFeed groupFeed = this.service.getFeed(this.feedUrl, ContactGroupFeed.class);
            System.err.println(groupFeed.getTitle().getPlainText());
            for (ContactGroupEntry entry : groupFeed.getEntries()) {
                printGroup(entry);
            }
            System.err.println("Total: " + groupFeed.getEntries().size() + " groups found");
        } else {
            ContactFeed resultFeed = this.service.getFeed(this.feedUrl, ContactFeed.class);
            System.err.println(resultFeed.getTitle().getPlainText());
            for (ContactEntry entry : resultFeed.getEntries()) {
                printContact(entry);
                Link photoLink = entry.getLink("http://schemas.google.com/contacts/2008/rel#photo", "image/*");
                if (photoLink.getEtag() != null) {
                    Service.GDataRequest request = this.service.createLinkQueryRequest(photoLink);
                    request.execute();
                    InputStream in = request.getResponseStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    RandomAccessFile file = new RandomAccessFile("/tmp/" + entry.getSelfLink().getHref().substring(entry.getSelfLink().getHref().lastIndexOf('/') + 1), "rw");
                    byte[] buffer = new byte[4096];
                    for (int read = 0; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)) {
                    }
                    file.write(out.toByteArray());
                    file.close();
                    in.close();
                    request.end();
                }
            }
            System.err.println("Total: " + resultFeed.getEntries().size() + " entries found");
        }
    }

    /**
	 * Adds contact or group entry according to the parameters specified.
	 * 
	 * @param parameters
	 *            parameters for contact adding
	 */
    private void addEntry(final ContactsExampleParameters parameters) throws IOException, ServiceException {
        if (parameters.isGroupFeed()) {
            ContactGroupEntry addedGroup = this.service.insert(this.feedUrl, buildGroup(parameters));
            printGroup(addedGroup);
            lastAddedId = addedGroup.getId();
        } else {
            ContactEntry addedContact = this.service.insert(this.feedUrl, buildContact(parameters));
            printContact(addedContact);
            lastAddedId = addedContact.getId();
        }
    }

    /**
	 * Build ContactEntry from parameters.
	 * 
	 * @param parameters
	 *            parameters
	 * @return A contact.
	 */
    private static ContactEntry buildContact(final ContactsExampleParameters parameters) {
        ContactEntry contact = new ContactEntry();
        ElementHelper.buildContact(contact, parameters.getElementDesc());
        return contact;
    }

    /**
	 * Builds GroupEntry from parameters
	 * 
	 * @param parameters
	 *            ContactExamplParameters
	 * @return GroupEntry Object
	 */
    private static ContactGroupEntry buildGroup(final ContactsExampleParameters parameters) {
        ContactGroupEntry groupEntry = new ContactGroupEntry();
        ElementHelper.buildGroup(groupEntry, parameters.getElementDesc());
        return groupEntry;
    }

    /**
	 * Displays usage information.
	 */
    private static void displayUsage() {
        String usageInstructions = "USAGE:\n" + " -----------------------------------------------------------\n" + "  Basic command line usage:\n" + "    ContactsExample [<options>] <authenticationInformation> " + "<--contactfeed|--groupfeed> " + "--action=<action> [<action options>]  " + "(default contactfeed)\n" + "  Scripting commands usage:\n" + "    contactsExample [<options>] <authenticationInformation> " + "<--contactfeed|--groupfeed>   --script=<script file>  " + "(default contactFeed) \n" + "  Print usage (this screen):\n" + "   --help\n" + " -----------------------------------------------------------\n\n" + "  Options: \n" + "    --base-url=<url to connect to> " + "(default http://www.google.com/m8/feeds/) \n" + "    --projection=[thin|full|property-KEY] " + "(default thin)\n" + "    --verbose : dumps communication information\n" + "  Authentication Information (obligatory on command line): \n" + "    --username=<username email> --password=<password>\n" + "  Actions: \n" + "     * list  list all contacts\n" + "     * query  query contacts\n" + "        options:\n" + "             --showdeleted : shows also deleted contacts\n" + "             --updated-min=YYYY-MM-DDTHH:MM:SS : only updated " + "after the time specified\n" + "             --requre-all-deleted=[true|false] : specifies " + "server behaviour in case of placeholders for deleted entries are" + "lost. Relevant only if --showdeleted and --updated-min also " + "provided.\n" + "             --orderby=lastmodified : order by last modified\n" + "             --sortorder=[ascending|descending] : sort order\n" + "             --max-results=<n> : return maximum n results\n" + "             --start-index=<n> : return results starting from " + "the starting index\n" + "             --querygroupid=<groupid> : return results from the " + "group\n" + "    * add  add new contact\n" + "        options:\n" + ElementHelper.getUsageString() + "    * delete  delete contact\n" + "        options:\n" + "             --id=<contact id>\n" + "    * update  updates contact\n" + "        options:\n" + "             --id=<contact id>\n" + ElementHelper.getUsageString();
        System.err.println(usageInstructions);
    }

    /**
	 * Run the example program.
	 * 
	 * @param args
	 *            Command-line arguments.
	 */
    public static void main(final String[] args) throws ServiceException, IOException {
        ContactsExampleParameters parameters = new ContactsExampleParameters(args);
        if (parameters.isVerbose()) {
            httpRequestLogger.setLevel(Level.FINEST);
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.FINEST);
            httpRequestLogger.addHandler(handler);
            httpRequestLogger.setUseParentHandlers(false);
        }
        if (parameters.numberOfParameters() == 0 || parameters.isHelp() || (parameters.getAction() == null && parameters.getScript() == null)) {
            displayUsage();
            return;
        }
        if (parameters.getUserName() == null || parameters.getPassword() == null) {
            System.err.println("Both username and password must be specified.");
            return;
        }
        if (parameters.isContactFeed() && parameters.isGroupFeed()) {
            throw new RuntimeException("Only one of contactfeed / groupfeed should" + "be specified");
        }
        ContactsExample example = new ContactsExample(parameters);
        if (parameters.getScript() != null) {
            processScript(example, parameters);
        } else {
            processAction(example, parameters);
        }
        System.out.flush();
    }
}
