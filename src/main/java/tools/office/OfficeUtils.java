package tools.office;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class OfficeUtils {
  private static final Object syncObj = new Object();

  public static final synchronized Sheet getExcelSheet(Workbook wb, String targetSheetPattern) throws InvalidFormatException, IOException {
    int sheetAmount = wb.getNumberOfSheets();
    for (int i = 0; i < sheetAmount; i++) {
      if (wb.getSheetName(i).matches(targetSheetPattern)) {
        return wb.getSheetAt(i);
      }
    }
    return null;
  }

  public static final synchronized Workbook getExcelWorkbook(File excel) throws InvalidFormatException, FileNotFoundException, IOException {
    synchronized (syncObj) {
      return WorkbookFactory.create(new FileInputStream(excel));
    }
  }
}
