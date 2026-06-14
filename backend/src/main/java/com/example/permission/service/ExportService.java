package com.example.permission.service;

import com.example.permission.entity.Room;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    @Autowired
    private RoomService roomService;

    private static final Map<Integer, String> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put(1, "空闲");
        STATUS_MAP.put(2, "已预订");
        STATUS_MAP.put(3, "已入住");
        STATUS_MAP.put(4, "待清洁");
        STATUS_MAP.put(5, "清洁中");
        STATUS_MAP.put(6, "维修中");
        STATUS_MAP.put(7, "停用");
    }

    public byte[] exportRooms(List<Room> rooms, List<String> exportFields,
                               String operatorName, String filterDesc) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("房间数据");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle infoStyle = createInfoStyle(workbook);

            int rowIdx = 0;

            Row infoRow1 = sheet.createRow(rowIdx++);
            Cell cell1 = infoRow1.createCell(0);
            cell1.setCellValue("导出时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            cell1.setCellStyle(infoStyle);

            Row infoRow2 = sheet.createRow(rowIdx++);
            Cell cell2 = infoRow2.createCell(0);
            cell2.setCellValue("导出人：" + operatorName);
            cell2.setCellStyle(infoStyle);

            Row infoRow3 = sheet.createRow(rowIdx++);
            Cell cell3 = infoRow3.createCell(0);
            cell3.setCellValue("筛选条件：" + (filterDesc != null ? filterDesc : "全部房间"));
            cell3.setCellStyle(infoStyle);

            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            int colIdx = 0;

            Map<String, Integer> fieldIndex = new HashMap<>();

            if (exportFields.contains("roomNumber")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("房间号");
                c.setCellStyle(headerStyle);
                fieldIndex.put("roomNumber", colIdx++);
            }
            if (exportFields.contains("building")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("楼栋");
                c.setCellStyle(headerStyle);
                fieldIndex.put("building", colIdx++);
            }
            if (exportFields.contains("floor")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("楼层");
                c.setCellStyle(headerStyle);
                fieldIndex.put("floor", colIdx++);
            }
            if (exportFields.contains("roomType")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("房型");
                c.setCellStyle(headerStyle);
                fieldIndex.put("roomType", colIdx++);
            }
            if (exportFields.contains("orientation")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("朝向");
                c.setCellStyle(headerStyle);
                fieldIndex.put("orientation", colIdx++);
            }
            if (exportFields.contains("viewType")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("景观");
                c.setCellStyle(headerStyle);
                fieldIndex.put("viewType", colIdx++);
            }
            if (exportFields.contains("locationFeatures")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("位置特点");
                c.setCellStyle(headerStyle);
                fieldIndex.put("locationFeatures", colIdx++);
            }
            if (exportFields.contains("specialTags")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("特殊标识");
                c.setCellStyle(headerStyle);
                fieldIndex.put("specialTags", colIdx++);
            }
            if (exportFields.contains("status")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("当前状态");
                c.setCellStyle(headerStyle);
                fieldIndex.put("status", colIdx++);
            }
            if (exportFields.contains("basePrice")) {
                Cell c = headerRow.createCell(colIdx);
                c.setCellValue("房型基础价格");
                c.setCellStyle(headerStyle);
                fieldIndex.put("basePrice", colIdx++);
            }

            for (Room room : rooms) {
                Row dataRow = sheet.createRow(rowIdx++);
                if (fieldIndex.containsKey("roomNumber")) {
                    dataRow.createCell(fieldIndex.get("roomNumber")).setCellValue(room.getRoomNumber() != null ? room.getRoomNumber() : "");
                }
                if (fieldIndex.containsKey("building")) {
                    dataRow.createCell(fieldIndex.get("building")).setCellValue(room.getBuildingName() != null ? room.getBuildingName() : "");
                }
                if (fieldIndex.containsKey("floor")) {
                    dataRow.createCell(fieldIndex.get("floor")).setCellValue(room.getFloorName() != null ? room.getFloorName() : (room.getFloorNumber() != null ? room.getFloorNumber() + "层" : ""));
                }
                if (fieldIndex.containsKey("roomType")) {
                    dataRow.createCell(fieldIndex.get("roomType")).setCellValue(room.getRoomTypeName() != null ? room.getRoomTypeName() : "");
                }
                if (fieldIndex.containsKey("orientation")) {
                    dataRow.createCell(fieldIndex.get("orientation")).setCellValue(room.getOrientation() != null ? room.getOrientation() : "");
                }
                if (fieldIndex.containsKey("viewType")) {
                    dataRow.createCell(fieldIndex.get("viewType")).setCellValue(room.getViewType() != null ? room.getViewType() : "");
                }
                if (fieldIndex.containsKey("locationFeatures")) {
                    dataRow.createCell(fieldIndex.get("locationFeatures")).setCellValue(room.getLocationFeatures() != null ? room.getLocationFeatures() : "");
                }
                if (fieldIndex.containsKey("specialTags")) {
                    dataRow.createCell(fieldIndex.get("specialTags")).setCellValue(room.getSpecialTags() != null ? room.getSpecialTags() : "");
                }
                if (fieldIndex.containsKey("status")) {
                    String statusStr = room.getStatus() != null ? (STATUS_MAP.getOrDefault(room.getStatus(), "未知") + "(" + room.getStatus() + ")") : "";
                    dataRow.createCell(fieldIndex.get("status")).setCellValue(statusStr);
                }
                if (fieldIndex.containsKey("basePrice")) {
                    if (room.getRoomType() != null && room.getRoomType().getBasePrice() != null) {
                        dataRow.createCell(fieldIndex.get("basePrice")).setCellValue(room.getRoomType().getBasePrice().doubleValue());
                    } else {
                        dataRow.createCell(fieldIndex.get("basePrice")).setCellValue("");
                    }
                }
            }

            for (int i = 0; i < colIdx; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                if (width < 3000) width = 3000;
                if (width > 15000) width = 15000;
                sheet.setColumnWidth(i, width);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败：" + e.getMessage(), e);
        }
    }

    public String generateFileName() {
        return "房间数据_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }
}
