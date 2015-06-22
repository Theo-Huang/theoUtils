/**
 * 
 */
package tools.office;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import exception.UtilityException;

/**
 * @author Theo Huang
 * @mail theo_huang@trend.com.tw
 *
 */
public class HtmlUtils {

  public static List<Map<String, String>> htmlTableToListMap(String html, String encoding) throws IOException {
    Document doc = Jsoup.parse(html, encoding);
    Element htmlTable = doc.select("table").get(0);

    Elements headColumn = htmlTable.select("th");
    List<String> heads = Lists.newArrayList();


    if (headColumn.size() == 0) {
      headColumn = htmlTable.select("tr").get(0).select("td");
    }

    for (int i = 0, headColumnSize = headColumn.size(); i < headColumnSize; i++) {
      Element head = headColumn.get(i);
      heads.add(head.text());
    }

    // Always tr
    Elements rowFirstTrs = htmlTable.select("tr");

    List<Map<String, String>> listMap = Lists.newArrayList();
    // Skip type in index 0
    for (int i = 1, rowFirstTrsSize = rowFirstTrs.size(); i < rowFirstTrsSize; i++) {
      Elements row = rowFirstTrs.get(i).select("td");
      if (row.size() > 0) {
        int rowIndex = 0;
        Map<String, String> map = Maps.newTreeMap();
        listMap.add(map);
        for (String key : heads) {
          map.put(key, row.get(rowIndex).text());
          rowIndex++;
        }
      }
    }

    return listMap;

  }

  public static Table<String, String, String> htmlTableToGuavaTable(String html, String encoding) throws IOException {

    Document doc = Jsoup.parse(html, encoding);
    Element htmlTable = doc.select("table").get(0);

    Elements headColumn = htmlTable.select("th");
    List<String> heads = Lists.newArrayList();


    if (headColumn.size() == 0) {
      headColumn = htmlTable.select("tr").get(0).select("td");
    }

    // Skip first
    for (int i = 1, headColumnSize = headColumn.size(); i < headColumnSize; i++) {
      Element head = headColumn.get(i);
      heads.add(head.text());
    }

    // Always tr
    Elements rowFirstTrs = htmlTable.select("tr");

    Table<String, String, String> table = TreeBasedTable.create();
    // Skip type in index 0
    for (int i = 1, rowFirstTrsSize = rowFirstTrs.size(); i < rowFirstTrsSize; i++) {
      Elements row = rowFirstTrs.get(i).select("td");
      Element rowFirst = row.get(0);
      if (heads.contains(rowFirst.text())) {
        // should not happen
        throw new UtilityException("Unhandlable form.");
      }
      int rowIndex = 1;
      for (String column : heads) {
        table.put(rowFirst.text(), column, row.get(rowIndex).text());
        rowIndex++;
      }
    }

    return table;
  }
}
