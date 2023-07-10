package org.poimodel.generator.helper;

import org.poimodel.config.Model;
import org.poimodel.config.Column;
import org.poimodel.entity.Property;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: sedov
 * Date: 05.06.2009
 * Time: 17:57:12
 * To change this template use File | Settings | File Templates.
 */
public class ReaderModelPrinter extends GenericModelPrinter {

    public void fillFile(Model model) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%s%sReader.java", model.getDirName(), model.getClassName())));
        writeLine(writer, String.format("package %s ;", model.getPackageName()));
        writeLine(writer, String.format("import java.util.List ;"));
        writeLine(writer, String.format("import java.util.ArrayList ;"));
        writeLine(writer, String.format("import org.apache.poi.hssf.usermodel.* ;"));
        writeLine(writer, "");
        writeLine(writer, "");
        writeLine(writer, String.format("public class %sReader { ", model.getClassName()));
        writeLine(writer, "");
        writeLine(writer, String.format("    public List<%s> readSheet(HSSFWorkbook workbook, int sheetIndex, Integer startRow, Integer endRow, Integer startColumn, Integer endColumn) {", model.getClassName()));
        writeLine(writer, String.format("        List<%s> vals = new ArrayList<%s>() ;", model.getClassName(), model.getClassName()));
        writeLine(writer, String.format("        HSSFSheet sheet = workbook.getSheetAt(sheetIndex) ;"));
        writeLine(writer, String.format("        int endRowIndex = endRow > 0 ? endRow : sheet.getLastRowNum() ;"));
        writeLine(writer, String.format("        for (int i = startRow; i < endRowIndex; i ++) {"));
        writeLine(writer, String.format("            HSSFRow row = sheet.getRow(i) ;"));
        writeLine(writer, String.format("            if (row != null) {"));
        writeLine(writer, String.format("                HSSFCell cell ;"));
        writeLine(writer, String.format("                %s val = new %s() ;", model.getClassName(), model.getClassName()));
        writeLine(writer, String.format("                for (int j = startColumn == null ? row.getFirstCellNum() : startColumn; j < (endColumn == null  ? row.getLastCellNum() : endColumn); j ++) {"));
        writeLine(writer, String.format("                    cell = row.getCell(j) ;"));
        writeLine(writer, String.format("                    if (cell != null && cell.getCellType() != HSSFCell.CELL_TYPE_BLANK) { "));
        writeLine(writer, String.format("                        fillVal(val,  cell) ;"));
        writeLine(writer, String.format("                    }"));
        writeLine(writer, String.format("                }"));
        writeLine(writer, String.format("                vals.add(val) ;"));
        writeLine(writer, String.format("            } "));
        writeLine(writer, String.format("        } "));
        writeLine(writer, String.format("        return vals ; "));
        writeLine(writer, String.format("    } "));
        writeLine(writer, String.format(""));
        writeLine(writer, String.format(""));
        writeLine(writer, String.format("    private void fillVal(%s val, HSSFCell cell) {", model.getClassName()));
        writeLine(writer, String.format("        switch (cell.getColumnIndex()) {"));
        for (Column column : model.getColumns()) {
            StringBuffer upperName = new StringBuffer(column.getName());
            upperName.replace(0, 1, String.valueOf(column.getName().charAt(0)).toUpperCase());
            writeLine(writer, String.format("            case %d : ", column.getIndex()));
            if (column.getType().equals(Property.STRING)) {
                writeLine(writer, String.format("                val.set%s(cell.getRichStringCellValue().getString()) ;", upperName.toString()));
            } else if (column.getType().equals(Property.BOOLEAN)) {
                writeLine(writer, String.format("                val.set%s(cell.getBooleanCellValue()) ;", upperName.toString()));
            } else if (column.getType().equals(Property.DOUBLE)) {
                writeLine(writer, String.format("                val.set%s(cell.getNumericCellValue()) ;", upperName.toString()));
            } else if (column.getType().equals(Property.FLOAT)) {
                writeLine(writer, String.format("                val.set%s(new Double(cell.getNumericCellValue()).floatValue()) ;", upperName.toString()));
            } else if (column.getType().equals(Property.INTEGER)) {
                writeLine(writer, String.format("                val.set%s(new Double(cell.getNumericCellValue()).intValue()) ;", upperName.toString()));
            }
            writeLine(writer, String.format("                break ;"));
        }
        writeLine(writer, String.format("        } "));
        writeLine(writer, String.format("    } "));
        writeLine(writer, String.format("} "));
        System.out.println("before flush");
        writer.flush();
        writer.close();
    }
}
