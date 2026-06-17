package entities;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Gerencia o arquivo principal de registros de tamanho fixo associado à árvore B.
 *
 * <p>O arquivo possui um cabeçalho com metadados e armazena registros sequenciais.
 * Quando habilitado, posições removidas são encadeadas em uma pilha livre para
 * reaproveitamento em inserções futuras.</p>
 */
public class MainFile {
    private static final int NAME_SIZE = 80;

    private final String filePath;
    private final boolean reuseEnabled;

    private int maxRecords;
    private int freeStack;

    private DiskAccessCounter diskAccessCounter;

    /**
     * Cria um gerenciador de arquivo principal sem contador de acessos externo.
     *
     * @param filePath caminho do arquivo principal.
     */
    public MainFile(String filePath) {
        this(filePath, null, true);
    }

    /**
     * Cria um gerenciador de arquivo principal com contador de acessos compartilhado.
     *
     * @param filePath caminho do arquivo principal.
     * @param diskAccessCounter contador de acessos a disco, podendo ser {@code null}.
     */
    public MainFile(String filePath, DiskAccessCounter diskAccessCounter) {
        this(filePath, diskAccessCounter, true);
    }

    /**
     * Cria um gerenciador de arquivo principal com configuração explícita de reutilização.
     *
     * @param filePath caminho do arquivo principal.
     * @param diskAccessCounter contador de acessos a disco, podendo ser {@code null}.
     * @param reuseEnabled indica se registros removidos podem ser reaproveitados.
     * @throws IllegalArgumentException se o caminho for nulo ou vazio.
     */
    public MainFile(String filePath, DiskAccessCounter diskAccessCounter, boolean reuseEnabled) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath não pode ser null");
        }

        this.filePath = filePath;
        this.diskAccessCounter = diskAccessCounter;
        this.reuseEnabled = reuseEnabled;
    }

    /**
     * Define o contador de acessos a disco usado pelo arquivo principal.
     *
     * @param diskAccessCounter contador a usar, podendo ser {@code null}.
     */
    public void setDiskAccessCounter(DiskAccessCounter diskAccessCounter) {
        this.diskAccessCounter = diskAccessCounter;
    }

    /**
     * Retorna o caminho do arquivo principal.
     *
     * @return caminho do arquivo persistente de registros.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Informa se a reutilização de registros removidos está habilitada.
     *
     * @return {@code true} se posições removidas podem ser reaproveitadas.
     */
    public boolean isReuseEnabled() {
        return reuseEnabled;
    }

    /**
     * Retorna o maior identificador de registro já alocado.
     *
     * @return maior id de registro registrado no cabeçalho.
     */
    public int getMaxRecords() {
        return maxRecords;
    }

    /**
     * Retorna o topo da pilha livre de registros.
     *
     * @return identificador do primeiro registro livre, ou {@code 0} se inexistente.
     */
    public int getFreeStack() {
        return freeStack;
    }

    /**
     * Calcula o tamanho fixo de um registro no arquivo.
     *
     * @return tamanho de cada registro em bytes.
     */
    public int recordSize() {
        return Integer.BYTES + Character.BYTES * NAME_SIZE + Short.BYTES;
    }

    /**
     * Calcula o tamanho do cabeçalho do arquivo principal.
     *
     * @return tamanho do cabeçalho em bytes.
     */
    public int headerSize() {
        return recordSize();
    }

    /**
     * Registra uma leitura no arquivo principal, quando houver contador associado.
     */
    private void countRead() {
        if (diskAccessCounter != null) {
            diskAccessCounter.countMainFileRead();
        }
    }

    /**
     * Registra uma escrita no arquivo principal, quando houver contador associado.
     */
    private void countWrite() {
        if (diskAccessCounter != null) {
            diskAccessCounter.countMainFileWrite();
        }
    }

    /**
     * Carrega um arquivo principal existente sem contador de acessos externo.
     *
     * @param filePath caminho do arquivo principal existente.
     * @return gerenciador carregado do arquivo.
     */
    public static MainFile fromFile(String filePath) {
        return fromFile(filePath, null, true);
    }

    /**
     * Carrega um arquivo principal existente com contador de acessos compartilhado.
     *
     * @param filePath caminho do arquivo principal existente.
     * @param diskAccessCounter contador de acessos a disco, podendo ser {@code null}.
     * @return gerenciador carregado do arquivo.
     */
    public static MainFile fromFile(String filePath, DiskAccessCounter diskAccessCounter) {
        return fromFile(filePath, diskAccessCounter, true);
    }

    /**
     * Carrega um arquivo principal existente com configuração explícita de reutilização.
     *
     * @param filePath caminho do arquivo principal existente.
     * @param diskAccessCounter contador de acessos a disco, podendo ser {@code null}.
     * @param reuseEnabled indica se a pilha livre persistida deve ser usada.
     * @return gerenciador carregado do arquivo.
     */
    public static MainFile fromFile(
            String filePath,
            DiskAccessCounter diskAccessCounter,
            boolean reuseEnabled
    ) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            MainFile file = new MainFile(filePath, diskAccessCounter, reuseEnabled);

            countMainFileHeaderRead(file);

            int maxRecords = raf.readInt();
            int freeStack = raf.readInt();

            file.maxRecords = maxRecords;
            file.freeStack = reuseEnabled ? freeStack : 0;

            return file;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao carregar arquivo principal", ex);
        }
    }

    /**
     * Registra a leitura do cabeçalho de um arquivo principal.
     *
     * @param file arquivo cujo contador será incrementado.
     */
    private static void countMainFileHeaderRead(MainFile file) {
        file.countRead();
    }

    /**
     * Cria e inicializa um novo arquivo principal sem contador externo.
     *
     * @param filePath caminho do arquivo a ser criado ou sobrescrito.
     * @return gerenciador inicializado com arquivo vazio.
     */
    public static MainFile createNew(String filePath) {
        return createNew(filePath, null, true);
    }

    /**
     * Cria e inicializa um novo arquivo principal com contador compartilhado.
     *
     * @param filePath caminho do arquivo a ser criado ou sobrescrito.
     * @param diskAccessCounter contador de acessos a disco, podendo ser {@code null}.
     * @return gerenciador inicializado com arquivo vazio.
     */
    public static MainFile createNew(String filePath, DiskAccessCounter diskAccessCounter) {
        return createNew(filePath, diskAccessCounter, true);
    }

    /**
     * Cria e inicializa um novo arquivo principal com configuração explícita de reutilização.
     *
     * @param filePath caminho do arquivo a ser criado ou sobrescrito.
     * @param diskAccessCounter contador de acessos a disco, podendo ser {@code null}.
     * @param reuseEnabled indica se registros removidos podem ser reaproveitados.
     * @return gerenciador inicializado com arquivo vazio.
     */
    public static MainFile createNew(
            String filePath,
            DiskAccessCounter diskAccessCounter,
            boolean reuseEnabled
    ) {
        MainFile file = new MainFile(filePath, diskAccessCounter, reuseEnabled);

        file.maxRecords = 0;
        file.freeStack = 0;
        file.initializeFile();

        return file;
    }

    /**
     * Inicializa ou sobrescreve o arquivo principal com cabeçalho vazio.
     */
    public void initializeFile() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            countWrite();
            raf.setLength(headerSize());

            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inicializar arquivo principal", ex);
        }
    }

    /**
     * Remove todos os registros e reinicializa o cabeçalho do arquivo principal.
     */
    public void clear() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            maxRecords = 0;
            freeStack = 0;

            countWrite();
            raf.setLength(headerSize());

            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao limpar arquivo principal", ex);
        }
    }

    /**
     * Persiste o cabeçalho do arquivo principal.
     *
     * @param raf arquivo principal aberto para escrita.
     * @throws IOException se ocorrer falha de E/S.
     */
    public void writeHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);

        countWrite();

        raf.writeInt(maxRecords);
        raf.writeInt(reuseEnabled ? freeStack : 0);
    }

    /**
     * Calcula a posição física de um registro no arquivo.
     *
     * @param recordId identificador do registro, iniciando em {@code 1}.
     * @return offset do registro no arquivo.
     * @throws IllegalArgumentException se {@code recordId <= 0}.
     */
    private long recordPosition(int recordId) {
        if (recordId <= 0) {
            throw new IllegalArgumentException("recordId deve ser maior que zero");
        }

        return (long) headerSize() + (long) (recordId - 1) * recordSize();
    }

    /**
     * Obtém o próximo identificador disponível para gravação de registro.
     *
     * @param raf arquivo principal aberto para leitura e escrita.
     * @return identificador disponível para o novo registro.
     * @throws IOException se ocorrer falha de E/S.
     */
    private int nextRecordId(RandomAccessFile raf) throws IOException {
        if (reuseEnabled && freeStack != 0) {
            int id = freeStack;

            raf.seek(recordPosition(id));

            countRead();

            freeStack = raf.readInt();

            writeHeader(raf);

            return id;
        }

        maxRecords++;
        writeHeader(raf);

        return maxRecords;
    }

    /**
     * Insere um registro no arquivo principal.
     *
     * @param record registro a inserir.
     * @return identificador/endereço do registro gravado.
     * @throws IllegalArgumentException se o registro for nulo.
     */
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

    /**
     * Alias de {@link #insertRecord(Record)} mantido para escrita de registro.
     *
     * @param record registro a gravar.
     * @return identificador/endereço do registro gravado.
     */
    public int writeRecord(Record record) {
        return insertRecord(record);
    }

    /**
     * Grava o conteúdo de um registro em uma posição específica do arquivo.
     *
     * @param raf arquivo principal aberto para escrita.
     * @param record registro a gravar.
     * @param id identificador da posição de destino.
     * @throws IOException se ocorrer falha de E/S.
     */
    private void writeRecord(RandomAccessFile raf, Record record, int id) throws IOException {
        raf.seek(recordPosition(id));

        countWrite();

        raf.writeInt(record.getKey());
        writeFixedString(raf, record.getName(), NAME_SIZE);
        raf.writeShort(record.getAge());
    }

    /**
     * Lê um registro do arquivo principal.
     *
     * @param id identificador/endereço do registro.
     * @return registro lido, ou {@code null} se o id não existir.
     */
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

    /**
     * Lê um registro usando um arquivo principal já aberto.
     *
     * @param raf arquivo principal aberto para leitura.
     * @param id identificador/endereço do registro.
     * @return registro lido.
     * @throws IOException se ocorrer falha de E/S.
     */
    private Record readRecord(RandomAccessFile raf, int id) throws IOException {
        raf.seek(recordPosition(id));

        countRead();

        int key = raf.readInt();
        String name = readFixedString(raf, NAME_SIZE);
        short age = raf.readShort();

        return new Record(key, name, age);
    }

    /**
     * Remove logicamente um registro do arquivo principal.
     *
     * @param id identificador do registro a remover.
     */
    public void removeRecord(int id) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            releaseRecord(raf, id);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao remover registro do arquivo principal", ex);
        }
    }

    /**
     * Encadeia um registro removido na pilha livre quando a reutilização está habilitada.
     *
     * @param raf arquivo principal aberto para escrita.
     * @param id identificador do registro a liberar.
     * @throws IOException se ocorrer falha de E/S.
     * @throws IllegalArgumentException se o id não existir.
     */
    private void releaseRecord(RandomAccessFile raf, int id) throws IOException {
        if (!recordExists(id)) {
            throw new IllegalArgumentException("id inválido: " + id);
        }

        if (!reuseEnabled) {
            return;
        }

        raf.seek(recordPosition(id));

        countWrite();

        raf.writeInt(freeStack);

        freeStack = id;
        writeHeader(raf);
    }

    /**
     * Verifica se um identificador de registro está dentro do intervalo alocado.
     *
     * @param id identificador a verificar.
     * @return {@code true} se o id for positivo e não exceder {@link #getMaxRecords()}.
     */
    public boolean recordExists(int id) {
        return id > 0 && id <= maxRecords;
    }

    /**
     * Escreve uma string com tamanho fixo, preenchendo caracteres restantes com nulo.
     *
     * @param raf arquivo aberto para escrita.
     * @param value texto a gravar.
     * @param size quantidade máxima de caracteres.
     * @throws IOException se ocorrer falha de E/S.
     */
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

    /**
     * Lê uma string de tamanho fixo, ignorando caracteres nulos de preenchimento.
     *
     * @param raf arquivo aberto para leitura.
     * @param size quantidade de caracteres a ler.
     * @return texto reconstruído sem preenchimentos nulos.
     * @throws IOException se ocorrer falha de E/S.
     */
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
