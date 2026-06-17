package entities;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Implementa uma árvore B persistida em arquivo, com suporte opcional à reutilização
 * de posições removidas e integração com um arquivo principal de registros.
 *
 * <p>A classe mantém um arquivo de índice composto por um cabeçalho e por nós de
 * tamanho fixo. Cada chave pode apontar para um endereço de registro no
 * {@link MainFile}, permitindo operações combinadas de índice e armazenamento de dados.</p>
 */
public class BTree {
    private final String filePath;
    private final int m;
    private final boolean reuseEnabled;

    private int root;
    private int maxRecords;
    private int freeStack;

    private MainFile mainFile;
    private final DiskAccessCounter diskAccessCounter;

    /**
     * Cria uma instância de árvore B usando reutilização de espaço por padrão.
     *
     * @param filePath caminho do arquivo de índice da árvore B.
     * @param m ordem da árvore B.
     * @throws IllegalArgumentException se o caminho for vazio ou se {@code m < 3}.
     */
    private BTree(String filePath, int m) {
        this(filePath, m, true);
    }

    /**
     * Cria uma instância de árvore B com configuração explícita de reutilização.
     *
     * @param filePath caminho do arquivo de índice da árvore B.
     * @param m ordem da árvore B.
     * @param reuseEnabled indica se nós removidos podem ser reaproveitados.
     * @throws IllegalArgumentException se o caminho for vazio ou se {@code m < 3}.
     */
    private BTree(String filePath, int m, boolean reuseEnabled) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath não pode ser vazio");
        }

        if (m < 3) {
            throw new IllegalArgumentException("m deve ser pelo menos 3");
        }

        this.filePath = filePath;
        this.m = m;
        this.reuseEnabled = reuseEnabled;
        this.diskAccessCounter = new DiskAccessCounter();
    }

    /**
     * Cria um novo arquivo de índice para uma árvore B sem arquivo principal associado.
     *
     * @param filePath caminho do arquivo de índice a ser criado ou sobrescrito.
     * @param m ordem da árvore B.
     * @return árvore B inicializada e vazia.
     */
    public static BTree createNew(String filePath, int m) {
        return createNew(filePath, m, true);
    }

    /**
     * Cria um novo arquivo de índice para uma árvore B sem arquivo principal associado.
     *
     * @param filePath caminho do arquivo de índice a ser criado ou sobrescrito.
     * @param m ordem da árvore B.
     * @param reuseEnabled indica se nós removidos podem ser reaproveitados.
     * @return árvore B inicializada e vazia.
     */
    public static BTree createNew(String filePath, int m, boolean reuseEnabled) {
        BTree btree = new BTree(filePath, m, reuseEnabled);

        btree.root = 0;
        btree.maxRecords = 0;
        btree.freeStack = 0;
        btree.mainFile = null;
        btree.initializeFile();

        return btree;
    }

    /**
     * Cria uma nova árvore B associada a um novo arquivo principal.
     *
     * @param indexFilePath caminho do arquivo de índice.
     * @param mainFilePath caminho do arquivo principal.
     * @param m ordem da árvore B.
     * @return árvore B vazia com arquivo principal associado.
     */
    public static BTree createNew(String indexFilePath, String mainFilePath, int m) {
        return createNew(indexFilePath, mainFilePath, m, true);
    }

    /**
     * Cria uma nova árvore B associada a um novo arquivo principal com configuração
     * explícita de reutilização de espaço.
     *
     * @param indexFilePath caminho do arquivo de índice.
     * @param mainFilePath caminho do arquivo principal.
     * @param m ordem da árvore B.
     * @param reuseEnabled indica se nós e registros removidos podem ser reaproveitados.
     * @return árvore B vazia com arquivo principal associado.
     */
    public static BTree createNew(
            String indexFilePath,
            String mainFilePath,
            int m,
            boolean reuseEnabled
    ) {
        BTree btree = new BTree(indexFilePath, m, reuseEnabled);

        btree.root = 0;
        btree.maxRecords = 0;
        btree.freeStack = 0;
        btree.mainFile = MainFile.createNew(
                mainFilePath,
                btree.diskAccessCounter,
                reuseEnabled
        );
        btree.initializeFile();

        return btree;
    }

    /**
     * Carrega uma árvore B existente a partir de um arquivo de índice.
     *
     * @param filePath caminho do arquivo de índice existente.
     * @return árvore B carregada sem arquivo principal associado.
     */
    public static BTree fromFile(String filePath) {
        return fromFile(filePath, true);
    }

    /**
     * Carrega uma árvore B existente a partir de um arquivo de índice.
     *
     * @param filePath caminho do arquivo de índice existente.
     * @param reuseEnabled indica se a pilha livre persistida deve ser usada.
     * @return árvore B carregada sem arquivo principal associado.
     */
    public static BTree fromFile(String filePath, boolean reuseEnabled) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            int root = raf.readInt();
            int m = raf.readInt();
            int maxRecords = raf.readInt();
            int freeStack = raf.readInt();

            BTree btree = new BTree(filePath, m, reuseEnabled);
            btree.countRead();

            btree.root = root;
            btree.maxRecords = maxRecords;
            btree.freeStack = reuseEnabled ? freeStack : 0;
            btree.mainFile = null;

            return btree;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao carregar arquivo da BTree", ex);
        }
    }

    /**
     * Carrega uma árvore B existente junto com seu arquivo principal.
     *
     * @param indexFilePath caminho do arquivo de índice existente.
     * @param mainFilePath caminho do arquivo principal existente.
     * @return árvore B carregada com arquivo principal associado.
     */
    public static BTree fromFile(String indexFilePath, String mainFilePath) {
        return fromFile(indexFilePath, mainFilePath, true);
    }

    /**
     * Carrega uma árvore B existente junto com seu arquivo principal e configuração
     * explícita de reutilização de espaço.
     *
     * @param indexFilePath caminho do arquivo de índice existente.
     * @param mainFilePath caminho do arquivo principal existente.
     * @param reuseEnabled indica se as pilhas livres persistidas devem ser usadas.
     * @return árvore B carregada com arquivo principal associado.
     */
    public static BTree fromFile(
            String indexFilePath,
            String mainFilePath,
            boolean reuseEnabled
    ) {
        try (RandomAccessFile raf = new RandomAccessFile(indexFilePath, "r")) {
            int root = raf.readInt();
            int m = raf.readInt();
            int maxRecords = raf.readInt();
            int freeStack = raf.readInt();

            BTree btree = new BTree(indexFilePath, m, reuseEnabled);
            btree.countRead();

            btree.root = root;
            btree.maxRecords = maxRecords;
            btree.freeStack = reuseEnabled ? freeStack : 0;
            btree.mainFile = MainFile.fromFile(
                    mainFilePath,
                    btree.diskAccessCounter,
                    reuseEnabled
            );

            return btree;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao carregar arquivo da BTree", ex);
        }
    }

    /**
     * Associa um arquivo principal à árvore B.
     *
     * @param mainFile arquivo principal a ser vinculado.
     * @throws IllegalArgumentException se o arquivo for nulo ou usar configuração de
     *                                  reutilização incompatível.
     */
    public void setMainFile(MainFile mainFile) {
        if (mainFile == null) {
            throw new IllegalArgumentException("mainFile não pode ser null");
        }

        if (mainFile.isReuseEnabled() != reuseEnabled) {
            throw new IllegalArgumentException(
                    "MainFile deve usar o mesmo valor de reuseEnabled da BTree"
            );
        }

        mainFile.setDiskAccessCounter(diskAccessCounter);
        this.mainFile = mainFile;
    }

    /**
     * Obtém o arquivo principal associado, exigindo que ele tenha sido configurado.
     *
     * @return arquivo principal associado.
     * @throws IllegalStateException se não houver arquivo principal associado.
     */
    private MainFile requireMainFile() {
        if (mainFile == null) {
            throw new IllegalStateException("Arquivo principal não foi associado à BTree");
        }

        return mainFile;
    }

    /**
     * Retorna o arquivo principal associado à árvore B.
     *
     * @return arquivo principal associado, ou {@code null} quando inexistente.
     */
    public MainFile getMainFile() {
        return mainFile;
    }

    /**
     * Retorna o contador compartilhado de acessos a disco.
     *
     * @return contador de leituras e escritas da árvore e do arquivo principal.
     */
    public DiskAccessCounter getDiskAccessCounter() {
        return diskAccessCounter;
    }

    /**
     * Informa se a reutilização de posições livres está habilitada.
     *
     * @return {@code true} se nós removidos podem ser reaproveitados.
     */
    public boolean isReuseEnabled() {
        return reuseEnabled;
    }

    /**
     * Retorna o topo da pilha livre de nós.
     *
     * @return identificador do primeiro nó livre, ou {@code 0} se a pilha estiver vazia.
     */
    public int getFreeStack() {
        return freeStack;
    }

    /**
     * Retorna o caminho do arquivo de índice da árvore B.
     *
     * @return caminho do arquivo persistente da árvore.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Retorna a ordem da árvore B.
     *
     * @return valor de {@code m} usado nos nós.
     */
    public int getM() {
        return m;
    }

    /**
     * Retorna o identificador do nó raiz.
     *
     * @return id da raiz, ou {@code 0} quando a árvore está vazia.
     */
    public int getRoot() {
        return root;
    }

    /**
     * Retorna o maior identificador de nó já alocado no arquivo.
     *
     * @return maior id de nó registrado no cabeçalho.
     */
    public int getMaxRecords() {
        return maxRecords;
    }

    /**
     * Registra uma leitura realizada no arquivo de índice da árvore B.
     */
    private void countRead() {
        diskAccessCounter.countBTreeRead();
    }

    /**
     * Registra uma escrita realizada no arquivo de índice da árvore B.
     */
    private void countWrite() {
        diskAccessCounter.countBTreeWrite();
    }

    /**
     * Inicializa ou sobrescreve o arquivo de índice com um cabeçalho vazio.
     */
    private void initializeFile() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            countWrite();
            raf.setLength(headerSize());

            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inicializar arquivo da BTree", ex);
        }
    }

    /**
     * Persiste o cabeçalho da árvore B no início do arquivo de índice.
     *
     * @param raf arquivo de índice aberto para escrita.
     * @throws IOException se ocorrer falha de E/S durante a escrita.
     */
    private void writeHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);

        countWrite();

        raf.writeInt(root);
        raf.writeInt(m);
        raf.writeInt(maxRecords);
        raf.writeInt(reuseEnabled ? freeStack : 0);
    }

    /**
     * Calcula o tamanho fixo, em bytes, de cada nó no arquivo.
     *
     * @return quantidade de bytes reservada para um nó.
     */
    private int nodeSize() {
        return Integer.BYTES
                + Integer.BYTES * m
                + Integer.BYTES * (m - 1)
                + Integer.BYTES * (m - 1);
    }

    /**
     * Calcula o tamanho do cabeçalho do arquivo de índice.
     *
     * @return tamanho do cabeçalho em bytes.
     */
    private int headerSize() {
        return nodeSize();
    }

    /**
     * Calcula a posição física de um nó no arquivo de índice.
     *
     * @param id identificador do nó, iniciando em {@code 1}.
     * @return offset do nó no arquivo.
     * @throws IllegalArgumentException se {@code id <= 0}.
     */
    private long nodePosition(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id deve ser maior que zero");
        }

        return headerSize() + (long) (id - 1) * nodeSize();
    }

    /**
     * Obtém o próximo identificador disponível para gravação de nó.
     *
     * <p>Quando a reutilização está habilitada, tenta remover um id da pilha livre;
     * caso contrário, aloca um novo id sequencial.</p>
     *
     * @param raf arquivo de índice aberto para leitura e escrita.
     * @return identificador disponível para um novo nó.
     * @throws IOException se ocorrer falha de E/S.
     */
    private int nextNodeId(RandomAccessFile raf) throws IOException {
        if (reuseEnabled && freeStack != 0) {
            int id = freeStack;

            raf.seek(nodePosition(id));

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
     * Libera um nó removido, encadeando-o na pilha livre quando permitido.
     *
     * @param raf arquivo de índice aberto para escrita.
     * @param id identificador do nó a liberar.
     * @throws IOException se ocorrer falha de E/S.
     * @throws IllegalArgumentException se o id for inválido.
     */
    private void releaseNode(RandomAccessFile raf, int id) throws IOException {
        if (id <= 0 || id > maxRecords) {
            throw new IllegalArgumentException("id inválido para liberação: " + id);
        }

        if (!reuseEnabled) {
            return;
        }

        raf.seek(nodePosition(id));

        countWrite();

        raf.writeInt(freeStack);

        freeStack = id;

        writeHeader(raf);
    }

    /**
     * Grava um nó no arquivo de índice usando seu identificador persistente.
     *
     * @param id identificador do nó a ser gravado.
     * @param node conteúdo do nó.
     * @throws RuntimeException se ocorrer falha de E/S.
     */
    public void writeNode(int id, Node node) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            boolean headerChanged = false;

            if (root == 0) {
                root = id;
                headerChanged = true;
            }

            if (id > maxRecords) {
                maxRecords = id;
                headerChanged = true;
            }

            writeNode(raf, id, node);

            if (headerChanged) {
                writeHeader(raf);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao escrever nó " + id, ex);
        }
    }

    /**
     * Grava um nó no arquivo de índice já aberto.
     *
     * @param raf arquivo de índice aberto para escrita.
     * @param id identificador do nó.
     * @param node conteúdo do nó.
     * @throws IOException se ocorrer falha de E/S.
     * @throws IllegalArgumentException se o nó for nulo ou incompatível com a árvore.
     */
    private void writeNode(RandomAccessFile raf, int id, Node node) throws IOException {
        if (node == null) {
            throw new IllegalArgumentException("node não pode ser null");
        }

        if (node.getM() != m) {
            throw new IllegalArgumentException("node possui m diferente da BTree");
        }

        if (node.getN() < 0 || node.getN() > m - 1) {
            throw new IllegalArgumentException("n inválido no nó");
        }

        raf.seek(nodePosition(id));

        countWrite();

        raf.writeInt(node.getN());

        for (int i = 0; i < m; i++) {
            raf.writeInt(node.getA(i));
        }

        for (int i = 1; i <= m - 1; i++) {
            if (i <= node.getN()) {
                raf.writeInt(node.getK(i));
            } else {
                raf.writeInt(0);
            }
        }

        for (int i = 1; i <= m - 1; i++) {
            if (i <= node.getN()) {
                raf.writeInt(node.getB(i));
            } else {
                raf.writeInt(0);
            }
        }
    }

    /**
     * Lê um nó do arquivo de índice.
     *
     * @param id identificador do nó desejado.
     * @return nó lido, ou {@code null} se o id não existir no arquivo.
     * @throws RuntimeException se ocorrer falha de E/S.
     */
    public Node readNode(int id) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            return readNode(raf, id);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao ler nó " + id, ex);
        }
    }

    /**
     * Lê um nó a partir de um arquivo de índice já aberto.
     *
     * @param raf arquivo de índice aberto para leitura.
     * @param id identificador do nó desejado.
     * @return nó lido, ou {@code null} se o nó não existir.
     * @throws IOException se ocorrer falha de E/S ou se o conteúdo do nó for inválido.
     */
    private Node readNode(RandomAccessFile raf, int id) throws IOException {
        if (!nodeExists(id)) {
            return null;
        }

        long position = nodePosition(id);

        if (position + nodeSize() > raf.length()) {
            return null;
        }

        raf.seek(position);

        countRead();

        int n = raf.readInt();

        if (n < 0 || n > m - 1) {
            throw new IOException("Nó inválido: n = " + n);
        }

        int[] addresses = new int[m];

        for (int i = 0; i < m; i++) {
            addresses[i] = raf.readInt();
        }

        int[] keys = new int[m - 1];

        for (int i = 0; i < m - 1; i++) {
            keys[i] = raf.readInt();
        }

        int[] recordAddresses = new int[m - 1];

        for (int i = 0; i < m - 1; i++) {
            recordAddresses[i] = raf.readInt();
        }

        Node node = new Node(m);
        node.setA0(addresses[0]);

        for (int i = 1; i <= n; i++) {
            node.addKAPair(keys[i - 1], addresses[i], recordAddresses[i - 1]);
        }

        return node;
    }

    /**
     * Busca uma chave na árvore B.
     *
     * @param key chave a ser procurada.
     * @return resultado contendo a posição encontrada ou o ponto de descida final.
     * @throws RuntimeException se ocorrer falha de E/S.
     */
    public SearchResult search(int key) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            return search(raf, key, null);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao buscar chave " + key, ex);
        }
    }

    /**
     * Busca uma chave usando um arquivo já aberto e, opcionalmente, registra o caminho percorrido.
     *
     * @param raf arquivo de índice aberto para leitura.
     * @param key chave a ser procurada.
     * @param path pilha a preencher com o caminho percorrido, ou {@code null} para ignorar.
     * @return resultado da busca.
     * @throws IOException se ocorrer falha de E/S.
     */
    private SearchResult search(RandomAccessFile raf, int key, Stack<PathEntry> path) throws IOException {
        if (path != null) {
            path.clear();
        }

        int p = root;
        int q = 0;
        int i = 0;

        while (p != 0) {
            Node node = readNode(raf, p);

            if (node == null) {
                throw new IllegalStateException("Nó " + p + " não existe no arquivo");
            }

            if (path != null) {
                path.push(new PathEntry(p, node));
            }

            i = node.findChildIndex(key);

            if (i >= 1 && key == node.getK(i)) {
                return new SearchResult(p, i, true, node.getB(i));
            }

            q = p;
            p = node.getA(i);
        }

        return new SearchResult(q, i, false, 0);
    }

    /**
     * Insere uma chave na árvore B sem endereço de registro associado.
     *
     * @param key chave a inserir.
     */
    public void insert(int key) {
        insert(key, 0);
    }

    /**
     * Insere uma chave na árvore B apontando para um endereço do arquivo principal.
     *
     * @param key chave a inserir.
     * @param recordAddress endereço do registro associado à chave.
     * @throws RuntimeException se ocorrer falha de E/S.
     */
    public void insert(int key, int recordAddress) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            insert(raf, key, recordAddress);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inserir chave " + key, ex);
        }
    }

    /**
     * Executa a inserção usando um arquivo de índice já aberto.
     *
     * <p>Quando há overflow, divide nós e propaga a chave promovida até que a árvore
     * volte a satisfazer as propriedades de árvore B.</p>
     *
     * @param raf arquivo de índice aberto para leitura e escrita.
     * @param key chave a inserir.
     * @param recordAddress endereço do registro associado.
     * @throws IOException se ocorrer falha de E/S.
     */
    private void insert(RandomAccessFile raf, int key, int recordAddress) throws IOException {
        if (root == 0) {
            Node p = new Node(m);
            p.setA0(0);
            p.addKAPair(key, 0, recordAddress);

            int pId = nextNodeId(raf);
            root = pId;

            writeNode(raf, pId, p);
            writeHeader(raf);
            return;
        }

        Stack<PathEntry> path = new Stack<>();
        SearchResult result = search(raf, key, path);

        if (result.getFound()) {
            return;
        }

        int promotedKey = key;
        int promotedAddress = 0;
        int promotedRecordAddress = recordAddress;
        int promotedPAddress = 0;
        boolean allocatedNewNode = false;

        while (!path.isEmpty()) {
            PathEntry entry = path.pop();

            int pId = entry.getId();
            Node p = entry.getNode();

            p.addKAPair(promotedKey, promotedAddress, promotedRecordAddress);

            if (p.getN() <= m - 1) {
                writeNode(raf, pId, p);

                if (allocatedNewNode) {
                    writeHeader(raf);
                }

                return;
            }

            SplitResult split = splitOverflowNode(p);

            p = split.getP();
            Node q = split.getQ();

            int qId = nextNodeId(raf);
            allocatedNewNode = true;

            writeNode(raf, pId, p);
            writeNode(raf, qId, q);

            promotedKey = split.getPromotedKey();
            promotedRecordAddress = split.getPromotedRecordAddress();
            promotedAddress = qId;
            promotedPAddress = pId;
        }

        Node newRoot = new Node(m);
        newRoot.setA0(promotedPAddress);
        newRoot.addKAPair(promotedKey, promotedAddress, promotedRecordAddress);

        int newRootId = nextNodeId(raf);
        root = newRootId;

        writeNode(raf, newRootId, newRoot);
        writeHeader(raf);
    }

    /**
     * Divide um nó em overflow e calcula a chave promovida ao pai.
     *
     * @param node nó temporariamente com excesso de chaves.
     * @return resultado contendo nó esquerdo, chave promovida e nó direito.
     */
    private SplitResult splitOverflowNode(Node node) {
        int middle = (int) Math.ceil(m / 2.0);

        Node p = new Node(m);
        Node q = new Node(m);

        p.setA0(node.getA(0));

        for (int j = 1; j < middle; j++) {
            p.addKAPair(node.getK(j), node.getA(j), node.getB(j));
        }

        q.setA0(node.getA(middle));

        for (int j = middle + 1; j <= node.getN(); j++) {
            q.addKAPair(node.getK(j), node.getA(j), node.getB(j));
        }

        int promotedKey = node.getK(middle);
        int promotedRecordAddress = node.getB(middle);

        return new SplitResult(p, promotedKey, promotedRecordAddress, q);
    }

    /**
     * Insere um registro no arquivo principal e indexa sua chave na árvore B.
     *
     * @param record registro a inserir.
     * @return {@code true} se o registro foi inserido; {@code false} se a chave já existia.
     * @throws IllegalArgumentException se o registro for nulo.
     * @throws IllegalStateException se não houver arquivo principal associado.
     */
    public boolean insertRecord(Record record) {
        if (record == null) {
            throw new IllegalArgumentException("record não pode ser null");
        }

        if (search(record.getKey()).getFound()) {
            return false;
        }

        int recordAddress = requireMainFile().insertRecord(record);

        insert(record.getKey(), recordAddress);

        return true;
    }

    /**
     * Busca um registro por chave usando o índice da árvore B.
     *
     * @param key chave do registro desejado.
     * @return registro encontrado, ou {@code null} se a chave não existir.
     * @throws IllegalStateException se a chave existir no índice e não houver arquivo principal associado.
     */
    public Record searchRecord(int key) {
        SearchResult result = search(key);

        if (!result.getFound()) {
            return null;
        }

        return requireMainFile().readRecord(result.getRecordAddress());
    }

    /**
     * Remove um registro do arquivo principal e sua chave correspondente do índice.
     *
     * @param key chave do registro a remover.
     * @return {@code true} se a chave foi encontrada e removida; {@code false} caso contrário.
     */
    public boolean removeRecord(int key) {
        SearchResult result = search(key);

        if (!result.getFound()) {
            return false;
        }

        int recordAddress = result.getRecordAddress();

        remove(key);

        requireMainFile().removeRecord(recordAddress);

        return true;
    }

    /**
     * Verifica se um identificador de nó está dentro do intervalo alocado.
     *
     * @param id identificador de nó a verificar.
     * @return {@code true} se o id for positivo e não exceder {@link #getMaxRecords()}.
     */
    public boolean nodeExists(int id) {
        return id > 0 && id <= maxRecords;
    }

    /**
     * Remove todo o conteúdo da árvore B e reinicializa o arquivo de índice.
     *
     * <p>Quando houver arquivo principal associado, ele também é limpo.</p>
     */
    public void clear() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            root = 0;
            maxRecords = 0;
            freeStack = 0;

            countWrite();
            raf.setLength(headerSize());

            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao limpar arquivo da BTree", ex);
        }

        if (mainFile != null) {
            mainFile.clear();
        }
    }

    /**
     * Gera uma listagem textual dos nós da árvore B.
     *
     * @return representação tabular dos nós ou indicação de árvore vazia.
     */
    public String dump() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            StringBuilder sb = new StringBuilder();

            sb.append("-----------------------------------\n");
            sb.append(String.format("%-5s %s\n", "ID", "NODE"));
            sb.append("-----------------------------------\n");

            if (root == 0) {
                sb.append("(árvore vazia)\n");
            } else {
                dumpNode(raf, root, sb, new HashSet<>());
            }

            sb.append("-----------------------------------");

            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao gerar dump da BTree", ex);
        }
    }

    /**
     * Acrescenta ao dump textual o nó informado e seus descendentes.
     *
     * @param raf arquivo de índice aberto para leitura.
     * @param nodeId identificador do nó atual.
     * @param sb acumulador textual do dump.
     * @param visited conjunto de nós já visitados para evitar ciclos.
     * @throws IOException se ocorrer falha de E/S.
     */
    private void dumpNode(
            RandomAccessFile raf,
            int nodeId,
            StringBuilder sb,
            Set<Integer> visited
    ) throws IOException {
        if (nodeId == 0) {
            return;
        }

        if (!visited.add(nodeId)) {
            sb.append(String.format("%-5d %s\n", nodeId, "<referência repetida>"));
            return;
        }

        Node node = readNode(raf, nodeId);

        if (node == null) {
            sb.append(String.format("%-5d %s\n", nodeId, "<nó não encontrado>"));
            return;
        }

        sb.append(String.format("%-5d %s\n", nodeId, node));

        for (int i = 0; i <= node.getN(); i++) {
            dumpNode(raf, node.getA(i), sb, visited);
        }
    }

    /**
     * Gera uma visualização textual hierárquica da árvore B.
     *
     * @return representação em níveis da árvore ou indicação de árvore vazia.
     */
    public String plot() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            StringBuilder sb = new StringBuilder();

            sb.append("-----------------------------------\n");

            if (root == 0) {
                sb.append("(árvore vazia)\n");
            } else {
                plotNode(raf, root, 0, sb, new HashSet<>());
            }

            sb.append("-----------------------------------");

            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao gerar plot da BTree", ex);
        }
    }

    /**
     * Acrescenta à visualização textual o nó informado e seus descendentes.
     *
     * @param raf arquivo de índice aberto para leitura.
     * @param nodeId identificador do nó atual.
     * @param level nível de indentação do nó.
     * @param sb acumulador textual da visualização.
     * @param visited conjunto de nós já visitados para evitar ciclos.
     * @throws IOException se ocorrer falha de E/S.
     */
    private void plotNode(
            RandomAccessFile raf,
            int nodeId,
            int level,
            StringBuilder sb,
            Set<Integer> visited
    ) throws IOException {
        if (nodeId == 0) {
            return;
        }

        if (!visited.add(nodeId)) {
            sb.append("    ".repeat(level));
            sb.append(nodeId);
            sb.append(": <referência repetida>\n");
            return;
        }

        Node node = readNode(raf, nodeId);

        if (node == null) {
            sb.append("    ".repeat(level));
            sb.append(nodeId);
            sb.append(": <nó não encontrado>\n");
            return;
        }

        plotNode(raf, node.getA(node.getN()), level + 1, sb, visited);

        sb.append("    ".repeat(level));
        sb.append(nodeId);
        sb.append(": ");
        sb.append(keysToString(node));
        sb.append("\n");

        for (int i = node.getN() - 1; i >= 0; i--) {
            plotNode(raf, node.getA(i), level + 1, sb, visited);
        }
    }

    /**
     * Converte as chaves de um nó em uma representação entre parênteses.
     *
     * @param node nó cujas chaves serão formatadas.
     * @return string contendo as chaves do nó.
     */
    private String keysToString(Node node) {
        StringBuilder sb = new StringBuilder();

        sb.append("(");

        for (int i = 1; i <= node.getN(); i++) {
            if (i > 1) {
                sb.append(", ");
            }

            sb.append(node.getK(i));
        }

        sb.append(")");

        return sb.toString();
    }

    /**
     * Remove uma chave da árvore B, se ela existir.
     *
     * @param key chave a remover.
     * @throws RuntimeException se ocorrer falha de E/S.
     */
    public void remove(int key) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            remove(raf, key);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao remover chave " + key, ex);
        }
    }

    /**
     * Executa a remoção de chave usando um arquivo de índice já aberto.
     *
     * @param raf arquivo de índice aberto para leitura e escrita.
     * @param key chave a remover.
     * @throws IOException se ocorrer falha de E/S.
     */
    private void remove(RandomAccessFile raf, int key) throws IOException {
        if (root == 0) {
            return;
        }

        Stack<PathEntry> path = new Stack<>();
        SearchResult result = search(raf, key, path);

        if (!result.getFound()) {
            return;
        }

        int pId = result.getP();
        int i = result.getI();
        Node p = readNode(raf, pId);

        if (p == null) {
            throw new IllegalStateException("Nó " + pId + " não existe no arquivo");
        }

        if (!p.isLeaf()) {
            int successorId = p.getA(i);
            Node successor = readNode(raf, successorId);

            if (successor == null) {
                throw new IllegalStateException("Nó sucessor " + successorId + " não existe no arquivo");
            }

            while (!successor.isLeaf()) {
                path.push(new PathEntry(successorId, successor));

                successorId = successor.getA(0);
                successor = readNode(raf, successorId);

                if (successor == null) {
                    throw new IllegalStateException("Nó sucessor " + successorId + " não existe no arquivo");
                }
            }

            path.push(new PathEntry(successorId, successor));

            int successorKey = successor.getK(1);
            int successorRecordAddress = successor.getB(1);

            p.replaceKAPair(i, successorKey, successorRecordAddress);
            writeNode(raf, pId, p);

            pId = successorId;
            i = 1;
            p = successor;
        }

        p.removeKAPairAt(i);

        fixAfterRemove(raf, path, pId, p);
    }

    /**
     * Restaura as propriedades da árvore B após a remoção de uma chave.
     *
     * <p>O método trata underflow por redistribuição com irmãos ou por concatenação de nós,
     * propagando ajustes até a raiz quando necessário.</p>
     *
     * @param raf arquivo de índice aberto para leitura e escrita.
     * @param path caminho percorrido até o nó removido.
     * @param pId identificador do nó que pode estar em underflow.
     * @param p nó que pode estar em underflow.
     * @throws IOException se ocorrer falha de E/S.
     */
    private void fixAfterRemove(RandomAccessFile raf, Stack<PathEntry> path, int pId, Node p) throws IOException {
        while (true) {
            if (pId == root) {
                if (p.getN() == 0) {
                    if (p.isLeaf()) {
                        root = 0;

                        if (reuseEnabled) {
                            maxRecords = 0;
                            freeStack = 0;

                            countWrite();
                            raf.setLength(headerSize());
                        }

                        writeHeader(raf);
                    } else {
                        root = p.getA(0);

                        if (reuseEnabled) {
                            releaseNode(raf, pId);
                        } else {
                            writeHeader(raf);
                        }
                    }
                } else {
                    writeNode(raf, pId, p);
                }

                return;
            }

            if (p.hasMinimumKeys()) {
                writeNode(raf, pId, p);
                return;
            }

            if (path.isEmpty() || path.peek().getId() != pId) {
                throw new IllegalStateException("Caminho inválido durante a remoção");
            }

            path.pop();

            if (path.isEmpty()) {
                throw new IllegalStateException("Nó não raiz sem pai durante a remoção");
            }

            int parentId = path.peek().getId();
            Node parent = readNode(raf, parentId);

            if (parent == null) {
                throw new IllegalStateException("Nó pai " + parentId + " não existe no arquivo");
            }

            int childIndex = parent.findChildAddressIndex(pId);

            if (childIndex < parent.getN()) {
                int rightId = parent.getA(childIndex + 1);
                Node right = readNode(raf, rightId);

                if (right == null) {
                    throw new IllegalStateException("Irmão direito " + rightId + " não existe no arquivo");
                }

                if (right.canLend()) {
                    p.borrowFromRight(parent, childIndex + 1, right);

                    writeNode(raf, pId, p);
                    writeNode(raf, parentId, parent);
                    writeNode(raf, rightId, right);

                    return;
                }
            }

            if (childIndex > 0) {
                int leftId = parent.getA(childIndex - 1);
                Node left = readNode(raf, leftId);

                if (left == null) {
                    throw new IllegalStateException("Irmão esquerdo " + leftId + " não existe no arquivo");
                }

                if (left.canLend()) {
                    p.borrowFromLeft(parent, childIndex, left);

                    writeNode(raf, leftId, left);
                    writeNode(raf, parentId, parent);
                    writeNode(raf, pId, p);

                    return;
                }
            }

            if (childIndex < parent.getN()) {
                int rightId = parent.getA(childIndex + 1);
                Node right = readNode(raf, rightId);

                if (right == null) {
                    throw new IllegalStateException("Irmão direito " + rightId + " não existe no arquivo");
                }

                p.mergeWithRight(parent.getK(childIndex + 1), parent.getB(childIndex + 1), right);
                parent.removeKAPairAt(childIndex + 1);

                writeNode(raf, pId, p);
                releaseNode(raf, rightId);
            } else {
                int leftId = parent.getA(childIndex - 1);
                Node left = readNode(raf, leftId);

                if (left == null) {
                    throw new IllegalStateException("Irmão esquerdo " + leftId + " não existe no arquivo");
                }

                left.mergeWithRight(parent.getK(childIndex), parent.getB(childIndex), p);
                parent.removeKAPairAt(childIndex);

                writeNode(raf, leftId, left);
                releaseNode(raf, pId);
            }

            pId = parentId;
            p = parent;
        }
    }
}
