package org.example.apiend.utils;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class LoadExcelDocuments {

    private static final String EXCEL_FOLDER = "F:\\大学\\技术\\比赛\\2025全国大学生服务外包创新创业大赛\\api-end\\src\\main\\resources\\content";

    // 🔥 核心：手动设置起始行（默认1 = 从第一条数据开始读）
    // 想从第10行开始，就改成 10
    public static final int START_ROW = 1;

    public static List<Document> loadAllExcelWithDynamicHeader() {
        List<Document> documents = new ArrayList<>();
        File folder = new File(EXCEL_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".xlsx") || name.endsWith(".xls"));
        if (files == null) return documents;

        int globalId = 1;

        for (File file : files) {
            String fileName = file.getName();
            System.out.println("✅ 解析Excel：" + fileName);

            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = WorkbookFactory.create(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet.getLastRowNum() < 1) continue;

                // 读取表头
                Row headerRow = sheet.getRow(0);
                List<String> headers = new ArrayList<>();
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    headers.add(getCellValue(headerRow.getCell(j)));
                }

                // 🔥 从【手动指定的START_ROW】开始读
                for (int i = START_ROW; i <= sheet.getLastRowNum(); i++) {
                    Row dataRow = sheet.getRow(i);
                    if (dataRow == null) continue;

                    StringBuilder content = new StringBuilder();
                    for (int j = 0; j < headers.size(); j++) {
                        String header = headers.get(j);
                        String value = getCellValue(dataRow.getCell(j));
                        if (!header.isBlank() && !value.isBlank()) {
                            content.append(header).append("：").append(value).append("，");
                        }
                    }

                    if (content.length() == 0) continue;
                    String text = content.substring(0, content.length() - 1);

                    // 元数据 + ID
                    Metadata metadata = new Metadata();
                    metadata.put("id", "rag_data_" + globalId++);
                    metadata.put("source_file", fileName);
                    metadata.put("row", String.valueOf(i));

                    Document document = Document.from(text, metadata);
                    documents.add(document);
                }
            } catch (Exception e) {
                System.err.println("❌ 解析失败：" + fileName + " → " + e.getMessage());
            }
        }
        return documents;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}