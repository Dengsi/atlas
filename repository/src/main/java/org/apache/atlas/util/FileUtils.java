/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.util;

import com.opencsv.CSVReader;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.atlas.repository.Constants.GlossaryImportSupportedFileExtensions.*;

public class FileUtils {
    public static final String PIPE_CHARACTER   = "|";
    public static final String COLON_CHARACTER  = ":";
    public static final String ESCAPE_CHARACTER = "\\";

    public static List<String[]> readFileData(String fileName, InputStream inputStream) throws IOException, AtlasBaseException {
        List<String[]>                        ret;
        String                                extension     = FilenameUtils.getExtension(fileName);

        if (extension.equalsIgnoreCase(CSV.name())) {
            ret = readCSV(inputStream);
        } else if (extension.equalsIgnoreCase(XLS.name()) || extension.equalsIgnoreCase(XLSX.name())) {
            ret = readExcel(inputStream, extension);
        } else {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_FILE_TYPE);
        }

        if (CollectionUtils.isEmpty(ret)) {
            throw new AtlasBaseException(AtlasErrorCode.NO_DATA_FOUND);
        }

        return ret;
    }

    public static List<String[]> readCSV(InputStream inputStream) throws IOException {
        List<String[]> ret = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] header = csvReader.readNext();

            if (header == null || header.length == 0) {
                return ret;
            }

            String[] data;

            while ((data = csvReader.readNext()) != null) {
                if (data.length > 1) {
                    ret.add(data);
                }
            }
        }

        return ret;
    }

    public static List<String[]> readExcel(InputStream inputStream, String extension) throws IOException {
        List<String[]> ret        = new ArrayList<>();
        Workbook       excelBook  = extension.equalsIgnoreCase(XLS.name()) ? new HSSFWorkbook(inputStream) : new XSSFWorkbook(inputStream);
        Sheet          excelSheet = excelBook.getSheetAt(0);
        Iterator       itr        = excelSheet.rowIterator();
        Row            headerRow  = (Row) itr.next();

        if (isRowEmpty(headerRow)) {
            return ret;
        }

        while (itr.hasNext()) {
            Row row = (Row) itr.next();

            if (!isRowEmpty(row)) {
                String[] data = new String[row.getLastCellNum()];

                for (int i = 0; i < row.getLastCellNum(); i++) {
                    data[i] = (row.getCell(i) != null) ? row.getCell(i).getStringCellValue().trim() : null;
                }

                ret.add(data);
            }
        }

        return ret;
    }

    private static boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);

            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                return false;
            }
        }

        return true;
    }
}