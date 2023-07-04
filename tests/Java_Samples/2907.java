package uk.ac.ebi.pride.tools.converter.gui.dialogs;

import uk.ac.ebi.pride.tools.converter.gui.NavigationPanel;
import uk.ac.ebi.pride.tools.converter.gui.component.table.BaseTable;
import uk.ac.ebi.pride.tools.converter.gui.component.table.model.PTMTableModel;
import uk.ac.ebi.pride.tools.converter.gui.interfaces.CvUpdatable;
import uk.ac.ebi.pride.tools.converter.gui.util.template.TemplateType;
import uk.ac.ebi.pride.tools.converter.gui.util.template.TemplateUtilities;
import uk.ac.ebi.pride.tools.converter.report.model.*;
import uk.ac.ebi.pride.tools.converter.utils.ConverterException;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: rcote
 * Date: 25/10/11
 * Time: 15:48
 */
public abstract class AbstractDialog extends JDialog {

    private static HashMap<Class, Class> REPORT_OBJECT_TO_DIALOG = new HashMap<Class, Class>();

    public static final String TEMPLATE_EXTENSION = ".xml";

    static {
        REPORT_OBJECT_TO_DIALOG.put(Contact.class, ContactDialog.class);
        REPORT_OBJECT_TO_DIALOG.put(CvParam.class, CvParamDialog.class);
        REPORT_OBJECT_TO_DIALOG.put(UserParam.class, UserParamDialog.class);
        REPORT_OBJECT_TO_DIALOG.put(Reference.class, ReferenceDialog.class);
        REPORT_OBJECT_TO_DIALOG.put(PTM.class, SimplePTMDialog.class);
        REPORT_OBJECT_TO_DIALOG.put(PTMTableModel.DecoratedPTM.class, SimplePTMDialog.class);
        REPORT_OBJECT_TO_DIALOG.put(DatabaseMapping.class, DatabaseMappingDialog.class);
    }

    public static AbstractDialog getInstance(BaseTable<? extends ReportObject> table, Class cls) {
        Class dialogClass = REPORT_OBJECT_TO_DIALOG.get(cls);
        if (dialogClass != null) {
            try {
                Constructor constructor = dialogClass.getConstructor(Frame.class, table.getClass());
                AbstractDialog ad = (AbstractDialog) constructor.newInstance(NavigationPanel.getInstance(), table);
                ad.isEditing = true;
                return ad;
            } catch (Exception e) {
                throw new IllegalStateException("Could not create dialog to edit this object: " + cls.toString(), e);
            }
        } else {
            throw new IllegalStateException("No dialog available to edit this object: " + cls.toString());
        }
    }

    protected CvUpdatable callback;

    protected boolean isEditing = false;

    protected AbstractDialog(Frame owner) {
        super(owner);
    }

    protected AbstractDialog(Dialog owner) {
        super(owner);
    }

    protected void validateRequiredField(Component source, KeyEvent event) {
        if (source instanceof JTextComponent) {
            JTextComponent text = (JTextComponent) source;
            String toValidate = text.getText();
            if (event != null) {
                toValidate += event.getKeyChar();
            }
            if (isNonNullTextField(toValidate)) {
                text.setBackground(Color.white);
            } else {
                text.setBackground(Color.pink);
            }
        } else if (source instanceof JComboBox) {
            JComboBox box = (JComboBox) source;
            if (box.getSelectedIndex() > -1) {
                box.setBackground(Color.white);
            } else {
                box.setBackground(Color.pink);
            }
        }
    }

    protected boolean isNonNullTextField(String text) {
        return text != null && text.trim().length() > 0;
    }

    public abstract void edit(ReportObject object);

    protected void saveTemplate(String templateName, TemplateType type, ReportObject templateObject) {
        File baseTemplatePath = TemplateUtilities.getUserTemplateDir();
        File templatePath = new File(baseTemplatePath, type.getTemplatePath());
        File template = new File(templatePath, templateName + TEMPLATE_EXTENSION);
        if (!template.exists()) {
            TemplateUtilities.writeTemplate(template, templateObject);
            JOptionPane.showMessageDialog(NavigationPanel.getInstance(), "Template saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            int value = JOptionPane.showConfirmDialog(NavigationPanel.getInstance(), "Template already exists. Click on OK to save and overwrite or CANCEL to abort", "Warning", JOptionPane.WARNING_MESSAGE);
            if (value == JOptionPane.OK_OPTION) {
                TemplateUtilities.writeTemplate(template, templateObject);
                JOptionPane.showMessageDialog(NavigationPanel.getInstance(), "Template saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void loadTemplate(String templateName) {
        throw new UnsupportedOperationException("This method is not supported in the abstract class!");
    }

    protected ReportObject loadTemplate(String templateName, TemplateType type) {
        File baseTemplatePath = TemplateUtilities.getUserTemplateDir();
        File templatePath = new File(baseTemplatePath, type.getTemplatePath());
        File template = new File(templatePath, templateName + TEMPLATE_EXTENSION);
        if (template.exists()) {
            return TemplateUtilities.loadTemplate(template, type.getObjectClass());
        } else {
            throw new ConverterException("Template " + templateName + " does not exist in " + templatePath.getAbsolutePath());
        }
    }

    protected String[] getTemplateNames(TemplateType type) {
        File baseTemplatePath = TemplateUtilities.getUserTemplateDir();
        File templatePath = new File(baseTemplatePath, type.getTemplatePath());
        String[] templates = templatePath.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(TEMPLATE_EXTENSION);
            }
        });
        Arrays.sort(templates, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        String[] filtered = new String[templates.length];
        int i = 0;
        for (String str : templates) {
            filtered[i++] = str.substring(0, str.lastIndexOf(TEMPLATE_EXTENSION));
        }
        return filtered;
    }
}
