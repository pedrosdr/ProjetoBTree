package entities;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MainFile {
    private static final int NAME_SIZE = 80;

    private final String filePath;
    private int maxRecords;
    private int freeStack;

    public MainFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath não pode ser null");
        }

        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getMaxRecords() {
        return maxRecords;
    }

    public int getFreeStack() {
        return freeStack;
    }

    public int recordSize() {
        return Integer.BYTES + Character.BYTES * NAME_SIZE + Short.BYTES;
    }

    public int headerSize() {
        return recordSize();
    }

    public static MainFile fromFile(String filePath) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            int maxRecords = raf.readInt();
            int freeStack = raf.readInt();

            MainFile file = new MainFile(filePath);
            file.maxRecords = maxRecords;
            file.freeStack = freeStack;

            return file;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao carregar arquivo principal", ex);
        }
    }

    public static MainFile createNew(String filePath) {
        MainFile file = new MainFile(filePath);

        file.maxRecords = 0;
        file.freeStack = 0;
        file.initializeFile();

        return file;
    }

    public void initializeFile() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.setLength(headerSize());
            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inicializar arquivo principal", ex);
        }
    }

    public void clear() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            maxRecords = 0;
            freeStack = 0;

            raf.setLength(headerSize());
            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao limpar arquivo principal", ex);
        }
    }

    public void writeHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        raf.writeInt(maxRecords);
        raf.writeInt(freeStack);
    }

    private long recordPosition(int recordId) {
        if (recordId <= 0) {
            throw new IllegalArgumentException("recordId deve ser maior que zero");
        }

        return (long) headerSize() + (long) (recordId - 1) * recordSize();
    }

    private int nextRecordId(RandomAccessFile raf) throws IOException {
        if (freeStack != 0) {
            int id = freeStack;

            raf.seek(recordPosition(id));
            freeStack = raf.readInt();

            writeHeader(raf);

            return id;
        }

        maxRecords++;
        writeHeader(raf);

        return maxRecords;
    }

    public int insertRecord(Record record) {
        if (record == null) {
            throw new IllegalArgumentException("record não pode ser null");
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            int id = nextRecordId(raf);

            writeRecord(raf, record, id);

            return id;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inserir registro no arquivo principal", ex);
        }
    }

    public int writeRecord(Record record) {
        return insertRecord(record);
    }

    private void writeRecord(RandomAccessFile raf, Record record, int id) throws IOException {
        raf.seek(recordPosition(id));

        raf.writeInt(record.getKey());
        writeFixedString(raf, record.getName(), NAME_SIZE);
        raf.writeShort(record.getAge());
    }

    public Record readRecord(int id) {
        if (!recordExists(id)) {
            return null;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            return readRecord(raf, id);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao ler registro no arquivo principal", ex);
        }
    }

    private Record readRecord(RandomAccessFile raf, int id) throws IOException {
        raf.seek(recordPosition(id));

        int key = raf.readInt();
        String name = readFixedString(raf, NAME_SIZE);
        short age = raf.readShort();

        return new Record(key, name, age);
    }

    public void removeRecord(int id) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            releaseRecord(raf, id);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao remover registro do arquivo principal", ex);
        }
    }

    private void releaseRecord(RandomAccessFile raf, int id) throws IOException {
        if (!recordExists(id)) {
            throw new IllegalArgumentException("id inválido: " + id);
        }

        raf.seek(recordPosition(id));

        // Registro livre:
        // o primeiro int deixa de ser key
        // e passa a apontar para o próximo registro livre.
        raf.writeInt(freeStack);

        freeStack = id;
        writeHeader(raf);
    }

    public boolean recordExists(int id) {
        return id > 0 && id <= maxRecords;
    }

    private void writeFixedString(RandomAccessFile raf, String value, int size) throws IOException {
        int i = 0;

        while (i < value.length() && i < size) {
            raf.writeChar(value.charAt(i));
            i++;
        }

        while (i < size) {
            raf.writeChar('\0');
            i++;
        }
    }

    private String readFixedString(RandomAccessFile raf, int size) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < size; i++) {
            char c = raf.readChar();

            if (c != '\0') {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}