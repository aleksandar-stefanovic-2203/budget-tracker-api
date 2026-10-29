package com.aleksandarstefanovic.budgettracker.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo.CsvImportSession;
import com.aleksandarstefanovic.budgettracker.db.dao.validation.TransferValidator;
import com.aleksandarstefanovic.budgettracker.models.Transfer;

@Service
public class TransferCsvImportService {
    private static final int MAX_DATA_ROWS = 1_000_000;
    private static final List<String> REQUIRED_HEADERS = List.of("type", "amount", "date", "category");

    private final TransferRepo transferRepo;

    public TransferCsvImportService(TransferRepo transferRepo) {
        this.transferRepo = transferRepo;
    }

    public record RowError(int row, String code, String message) {}

    public record ImportReport(int totalRows, int importedRows, int failedRows, List<RowError> errors) {}

    public static final class InvalidCsvException extends Exception {
        private final ErrorCode errorCode;

        public InvalidCsvException(ErrorCode errorCode) {
            super(errorCode.getDescription());
            this.errorCode = errorCode;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }

    private record CsvRecord(int startLine, List<String> fields) {}

    public ImportReport importTransactions(MultipartFile file) throws IOException, SQLException, InvalidCsvException {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }

        Map<String, Integer> headerIndexes = validateFile(file);
        int totalRows = 0;
        int importedRows = 0;
        List<RowError> errors = new ArrayList<>();

        try (CsvParser parser = new CsvParser(openReader(file));
                CsvImportSession importSession = transferRepo.openCsvImportSession()) {
            parser.nextRecord();
            CsvRecord record;
            while ((record = parser.nextRecord()) != null) {
                if (isBlank(record.fields())) continue;

                totalRows++;
                ErrorCode rowError;
                if (record.fields().size() != headerIndexes.size()) {
                    rowError = ErrorCode.TRANSACTION_CSV_ROW_INVALID;
                } else {
                    try {
                        String categoryName = value(record.fields(), headerIndexes, "category");
                        Transfer transfer = transferFrom(record.fields(), headerIndexes);
                        rowError = validateTransfer(transfer);
                        if (rowError == ErrorCode.OK) {
                            rowError = importSession.insertTransfer(categoryName, transfer);
                        }
                    } catch (RowConversionException exception) {
                        rowError = exception.errorCode;
                    }
                }

                if (rowError == ErrorCode.OK) {
                    importedRows++;
                } else {
                    errors.add(new RowError(record.startLine(), rowError.name(), rowError.getDescription()));
                }
            }
        } catch (CharacterCodingException exception) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }

        return new ImportReport(totalRows, importedRows, errors.size(), List.copyOf(errors));
    }

    private static Map<String, Integer> validateFile(MultipartFile file) throws IOException, InvalidCsvException {
        try (CsvParser parser = new CsvParser(openReader(file))) {
            CsvRecord header = parser.nextRecord();
            if (header == null) throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
            Map<String, Integer> indexes = validateHeader(header);

            int dataRows = 0;
            CsvRecord record;
            while ((record = parser.nextRecord()) != null) {
                if (isBlank(record.fields())) continue;
                dataRows++;
                if (dataRows > MAX_DATA_ROWS) {
                    throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_TOO_MANY_ROWS);
                }
            }
            return indexes;
        } catch (CharacterCodingException exception) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }
    }

    private static BufferedReader openReader(MultipartFile file) throws IOException {
        return new BufferedReader(new InputStreamReader(
                file.getInputStream(),
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)));
    }

    private static Map<String, Integer> validateHeader(CsvRecord header) throws InvalidCsvException {
        Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < header.fields().size(); index++) {
            String name = header.fields().get(index).strip().toLowerCase(Locale.ROOT);
            if (index == 0 && name.startsWith("\uFEFF")) name = name.substring(1);
            if (name.isEmpty() || indexes.putIfAbsent(name, index) != null) {
                throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
            }
        }
        if (!indexes.keySet().containsAll(REQUIRED_HEADERS)
                || indexes.size() != header.fields().size()
                || indexes.keySet().stream().anyMatch(
                        name -> !REQUIRED_HEADERS.contains(name) && !name.equals("description"))) {
            throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
        }
        return indexes;
    }

    private static boolean isBlank(List<String> fields) {
        return fields.size() == 1 && fields.get(0).isBlank();
    }

    private static Transfer transferFrom(List<String> fields, Map<String, Integer> indexes)
            throws RowConversionException {
        String type = value(fields, indexes, "type");
        if (type != null) type = type.toUpperCase(Locale.ROOT);

        String amountText = value(fields, indexes, "amount");
        String dateText = value(fields, indexes, "date");
        String description = indexes.containsKey("description")
                ? fields.get(indexes.get("description"))
                : null;

        Double amount = null;
        if (amountText != null) {
            try {
                amount = Double.valueOf(amountText);
            } catch (NumberFormatException exception) {
                throw new RowConversionException(ErrorCode.TRANSACTION_AMOUNT_INVALID_FORMAT);
            }
        }

        Date date = null;
        if (dateText != null) {
            try {
                date = Date.valueOf(LocalDate.parse(dateText));
            } catch (DateTimeParseException exception) {
                throw new RowConversionException(ErrorCode.TRANSACTION_DATE_INVALID_FORMAT);
            }
        }

        if (description != null && description.isBlank()) description = null;
        return new Transfer(null, type, amount, date, description, null);
    }

    private static String value(List<String> fields, Map<String, Integer> indexes, String name) {
        Integer index = indexes.get(name);
        if (index == null) return null;
        String value = fields.get(index).strip();
        return value.isEmpty() ? null : value;
    }

    private static ErrorCode validateTransfer(Transfer transfer) {
        ErrorCode errorCode = TransferValidator.isTypeValid(transfer.getType());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isAmountValid(transfer.getAmount());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isDateValid(transfer.getDate());
        if (errorCode != ErrorCode.OK) return errorCode;
        return TransferValidator.isDescriptionValid(transfer.getDescription());
    }

    private static final class RowConversionException extends Exception {
        private final ErrorCode errorCode;

        private RowConversionException(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }
    }

    private static final class CsvParser implements AutoCloseable {
        private final PushbackReader reader;
        private int line = 1;
        private int recordStartLine = 1;
        private boolean firstCharacter = true;

        private CsvParser(BufferedReader reader) {
            this.reader = new PushbackReader(reader, 1);
        }

        private CsvRecord nextRecord() throws IOException, InvalidCsvException {
            List<String> fields = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean inQuotes = false;
            boolean afterClosingQuote = false;
            boolean hasRecordContent = false;

            while (true) {
                int next = reader.read();
                if (next == -1) {
                    if (inQuotes) throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
                    if (!hasRecordContent && fields.isEmpty() && field.isEmpty()) return null;
                    fields.add(field.toString());
                    CsvRecord record = new CsvRecord(recordStartLine, List.copyOf(fields));
                    recordStartLine = line;
                    return record;
                }

                char current = (char) next;
                if (firstCharacter) {
                    firstCharacter = false;
                    if (current == '\uFEFF') continue;
                }
                hasRecordContent = true;

                if (inQuotes) {
                    if (current == '"') {
                        int following = reader.read();
                        if (following == '"') {
                            field.append('"');
                        } else {
                            inQuotes = false;
                            afterClosingQuote = true;
                            if (following != -1) reader.unread(following);
                        }
                    } else {
                        field.append(current);
                        if (current == '\r' || current == '\n') {
                            if (consumeLineEnding(current) == '\n') field.append('\n');
                        }
                    }
                    continue;
                }

                if (afterClosingQuote) {
                    if (current == ',') {
                        fields.add(field.toString());
                        field.setLength(0);
                        afterClosingQuote = false;
                    } else if (current == '\r' || current == '\n') {
                        fields.add(field.toString());
                        consumeLineEnding(current);
                        CsvRecord record = new CsvRecord(recordStartLine, List.copyOf(fields));
                        recordStartLine = line;
                        return record;
                    } else if (current != ' ' && current != '\t') {
                        throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
                    }
                    continue;
                }

                if (current == '"') {
                    if (!field.isEmpty()) throw new InvalidCsvException(ErrorCode.TRANSACTION_CSV_FILE_INVALID);
                    inQuotes = true;
                } else if (current == ',') {
                    fields.add(field.toString());
                    field.setLength(0);
                } else if (current == '\r' || current == '\n') {
                    fields.add(field.toString());
                    consumeLineEnding(current);
                    CsvRecord record = new CsvRecord(recordStartLine, List.copyOf(fields));
                    recordStartLine = line;
                    return record;
                } else {
                    field.append(current);
                }
            }
        }

        private int consumeLineEnding(char current) throws IOException {
            int following = -1;
            if (current == '\r') {
                following = reader.read();
                if (following != '\n' && following != -1) reader.unread(following);
            }
            line++;
            return following;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
