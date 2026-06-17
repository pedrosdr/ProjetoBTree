package main;

import entities.BTree;
import entities.DiskAccessCounter;
import entities.Node;
import entities.Record;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

/**
 * Executa experimentos automatizados sobre a árvore B e salva os resultados em CSV.
 *
 * <p>O benchmark varia:</p>
 *
 * <ul>
 *     <li>ordem {@code m} da árvore B;</li>
 *     <li>quantidade de registros {@code n};</li>
 *     <li>ordem das chaves: sequencial ou aleatória;</li>
 *     <li>reaproveitamento de memória: habilitado ou desabilitado.</li>
 * </ul>
 *
 * <p>Para cada configuração, são medidas operações de inserção, busca com sucesso,
 * busca sem sucesso, remoção e reinserção após remoção.</p>
 */
public class BenchmarkCsv {
    private final Path outputCsvPath;
    private final Path dataDirectory;

    private final int[] mValues;
    private final int[] nValues;
    private final String[] keyOrders;
    private final boolean[] reuseOptions;

    private final int searchSampleSize;
    private final long randomSeed;

    /**
     * Cria um benchmark com configuração padrão.
     *
     * <p>O CSV será salvo em {@code btree_benchmark_results.csv}, e os arquivos
     * binários temporários serão salvos no diretório {@code benchmark-data}.</p>
     */
    public BenchmarkCsv() {
        this(
                Path.of("btree_benchmark_results.csv"),
                Path.of("benchmark-data"),
                new int[]{3, 10, 100, 1000},
                new int[]{1_000, 10_000, 100_000},
                new String[]{"sequential", "random"},
                new boolean[]{true, false},
                1000,
                123456789L
        );
    }

    /**
     * Cria um benchmark com configuração personalizada.
     *
     * @param outputCsvPath caminho do arquivo CSV de saída.
     * @param dataDirectory diretório onde os arquivos binários dos testes serão criados.
     * @param mValues valores de ordem {@code m} a testar.
     * @param nValues quantidades de registros a testar.
     * @param keyOrders tipos de entrada: {@code sequential} e/ou {@code random}.
     * @param reuseOptions opções de reaproveitamento: {@code true} e/ou {@code false}.
     * @param searchSampleSize quantidade máxima de buscas em cada fase de busca.
     * @param randomSeed semente usada para embaralhar chaves aleatórias.
     */
    public BenchmarkCsv(
            Path outputCsvPath,
            Path dataDirectory,
            int[] mValues,
            int[] nValues,
            String[] keyOrders,
            boolean[] reuseOptions,
            int searchSampleSize,
            long randomSeed
    ) {
        if (outputCsvPath == null) {
            throw new IllegalArgumentException("outputCsvPath não pode ser null");
        }

        if (dataDirectory == null) {
            throw new IllegalArgumentException("dataDirectory não pode ser null");
        }

        if (mValues == null || mValues.length == 0) {
            throw new IllegalArgumentException("mValues não pode ser vazio");
        }

        if (nValues == null || nValues.length == 0) {
            throw new IllegalArgumentException("nValues não pode ser vazio");
        }

        if (keyOrders == null || keyOrders.length == 0) {
            throw new IllegalArgumentException("keyOrders não pode ser vazio");
        }

        if (reuseOptions == null || reuseOptions.length == 0) {
            throw new IllegalArgumentException("reuseOptions não pode ser vazio");
        }

        if (searchSampleSize <= 0) {
            throw new IllegalArgumentException("searchSampleSize deve ser maior que zero");
        }

        this.outputCsvPath = outputCsvPath;
        this.dataDirectory = dataDirectory;
        this.mValues = mValues.clone();
        this.nValues = nValues.clone();
        this.keyOrders = keyOrders.clone();
        this.reuseOptions = reuseOptions.clone();
        this.searchSampleSize = searchSampleSize;
        this.randomSeed = randomSeed;
    }

    /**
     * Executa todos os experimentos configurados e grava os resultados no CSV.
     */
    public void run() {
        try {
            createParentDirectoryIfNeeded(outputCsvPath);
            Files.createDirectories(dataDirectory);

            try (BufferedWriter writer = Files.newBufferedWriter(outputCsvPath)) {
                writeHeader(writer);

                int experimentId = 1;

                for (int m : mValues) {
                    for (int n : nValues) {
                        for (String keyOrder : keyOrders) {
                            validateKeyOrder(keyOrder);

                            for (boolean reuseEnabled : reuseOptions) {
                                System.out.println(
                                        "Executando experimento " + experimentId +
                                                " | m=" + m +
                                                " | n=" + n +
                                                " | ordem=" + keyOrder +
                                                " | reuseEnabled=" + reuseEnabled
                                );

                                runExperiment(
                                        writer,
                                        experimentId,
                                        m,
                                        n,
                                        keyOrder,
                                        reuseEnabled
                                );

                                experimentId++;
                            }
                        }
                    }
                }
            }

            System.out.println("CSV gerado com sucesso: " + outputCsvPath);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao executar benchmark", ex);
        }
    }

    private void runExperiment(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            boolean reuseEnabled
    ) throws IOException {
        String prefix = "exp_" + experimentId +
                "_m_" + m +
                "_n_" + n +
                "_" + keyOrder +
                "_reuse_" + reuseEnabled;

        Path indexFilePath = dataDirectory.resolve(prefix + "_index.bin");
        Path mainFilePath = dataDirectory.resolve(prefix + "_main.bin");

        Files.deleteIfExists(indexFilePath);
        Files.deleteIfExists(mainFilePath);

        int[] keys = generateKeys(n, keyOrder, randomSeed + experimentId);

        BTree btree = BTree.createNew(
                indexFilePath.toString(),
                mainFilePath.toString(),
                m,
                reuseEnabled
        );

        btree.getDiskAccessCounter().reset();

        runInsertBuild(writer, experimentId, m, n, keyOrder, reuseEnabled, btree, keys);
        runSearchHit(writer, experimentId, m, n, keyOrder, reuseEnabled, btree, keys);
        runSearchMiss(writer, experimentId, m, n, keyOrder, reuseEnabled, btree);
        runRemoveHalf(writer, experimentId, m, n, keyOrder, reuseEnabled, btree, keys);
        runReinsertAfterRemove(writer, experimentId, m, n, keyOrder, reuseEnabled, btree);
    }

    private void runInsertBuild(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            boolean reuseEnabled,
            BTree btree,
            int[] keys
    ) throws IOException {
        DiskAccessCounter counter = btree.getDiskAccessCounter();
        counter.reset();

        long start = System.nanoTime();

        for (int key : keys) {
            boolean inserted = btree.insertRecord(
                    new Record(key, "Pessoa " + key, (short) (key % 100))
            );

            if (!inserted) {
                throw new IllegalStateException("Chave duplicada inesperada: " + key);
            }
        }

        long elapsedNs = System.nanoTime() - start;

        Metrics metrics = Metrics.from(counter, keys.length, elapsedNs);

        writeRow(
                writer,
                experimentId,
                m,
                n,
                keyOrder,
                reuseEnabled,
                "insert_build",
                keys.length,
                btree,
                metrics
        );
    }

    private void runSearchHit(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            boolean reuseEnabled,
            BTree btree,
            int[] keys
    ) throws IOException {
        DiskAccessCounter counter = btree.getDiskAccessCounter();
        counter.reset();

        int operationCount = Math.min(searchSampleSize, keys.length);

        long start = System.nanoTime();

        for (int i = 0; i < operationCount; i++) {
            int index = (int) (((long) i * keys.length) / operationCount);
            int key = keys[index];

            Record record = btree.searchRecord(key);

            if (record == null) {
                throw new IllegalStateException("Busca deveria encontrar a chave: " + key);
            }
        }

        long elapsedNs = System.nanoTime() - start;

        Metrics metrics = Metrics.from(counter, operationCount, elapsedNs);

        writeRow(
                writer,
                experimentId,
                m,
                n,
                keyOrder,
                reuseEnabled,
                "search_hit",
                operationCount,
                btree,
                metrics
        );
    }

    private void runSearchMiss(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            boolean reuseEnabled,
            BTree btree
    ) throws IOException {
        DiskAccessCounter counter = btree.getDiskAccessCounter();
        counter.reset();

        int operationCount = Math.min(searchSampleSize, n);

        long start = System.nanoTime();

        for (int i = 0; i < operationCount; i++) {
            int key = -1 - i;

            Record record = btree.searchRecord(key);

            if (record != null) {
                throw new IllegalStateException("Busca não deveria encontrar a chave: " + key);
            }
        }

        long elapsedNs = System.nanoTime() - start;

        Metrics metrics = Metrics.from(counter, operationCount, elapsedNs);

        writeRow(
                writer,
                experimentId,
                m,
                n,
                keyOrder,
                reuseEnabled,
                "search_miss",
                operationCount,
                btree,
                metrics
        );
    }

    private void runRemoveHalf(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            boolean reuseEnabled,
            BTree btree,
            int[] keys
    ) throws IOException {
        DiskAccessCounter counter = btree.getDiskAccessCounter();
        counter.reset();

        int operationCount = n / 2;

        long start = System.nanoTime();

        for (int i = 0; i < operationCount; i++) {
            int key = keys[i];

            boolean removed = btree.removeRecord(key);

            if (!removed) {
                throw new IllegalStateException("Remoção deveria encontrar a chave: " + key);
            }
        }

        long elapsedNs = System.nanoTime() - start;

        Metrics metrics = Metrics.from(counter, operationCount, elapsedNs);

        writeRow(
                writer,
                experimentId,
                m,
                n,
                keyOrder,
                reuseEnabled,
                "remove_half",
                operationCount,
                btree,
                metrics
        );
    }

    private void runReinsertAfterRemove(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            boolean reuseEnabled,
            BTree btree
    ) throws IOException {
        DiskAccessCounter counter = btree.getDiskAccessCounter();
        counter.reset();

        int operationCount = n / 2;
        int firstNewKey = n * 10 + 1;

        long start = System.nanoTime();

        for (int i = 0; i < operationCount; i++) {
            int key = firstNewKey + i;

            boolean inserted = btree.insertRecord(
                    new Record(key, "Reinserido " + key, (short) (key % 100))
            );

            if (!inserted) {
                throw new IllegalStateException("Reinserção falhou para a chave: " + key);
            }
        }

        long elapsedNs = System.nanoTime() - start;

        Metrics metrics = Metrics.from(counter, operationCount, elapsedNs);

        writeRow(
                writer,
                experimentId,
                m,
                n,
                keyOrder,
                reuseEnabled,
                "reinsert_after_remove",
                operationCount,
                btree,
                metrics
        );
    }

    private int[] generateKeys(int n, String keyOrder, long seed) {
        validateKeyOrder(keyOrder);

        int[] keys = new int[n];

        for (int i = 0; i < n; i++) {
            keys[i] = i + 1;
        }

        if (keyOrder.equals("random")) {
            shuffle(keys, seed);
        }

        return keys;
    }

    private void shuffle(int[] values, long seed) {
        Random random = new Random(seed);

        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);

            int temp = values[i];
            values[i] = values[j];
            values[j] = temp;
        }
    }

    private void validateKeyOrder(String keyOrder) {
        if (!keyOrder.equals("sequential") && !keyOrder.equals("random")) {
            throw new IllegalArgumentException(
                    "keyOrder inválido: " + keyOrder +
                            ". Use 'sequential' ou 'random'."
            );
        }
    }

    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write(String.join(",",
                "experiment_id",
                "m",
                "n",
                "key_order",
                "reuse_enabled",
                "phase",
                "operation_count",
                "btree_reads",
                "btree_writes",
                "btree_total",
                "mainfile_reads",
                "mainfile_writes",
                "mainfile_total",
                "total_reads",
                "total_writes",
                "total_accesses",
                "avg_accesses_per_operation",
                "elapsed_ms",
                "avg_ms_per_operation",
                "height",
                "index_file_bytes",
                "main_file_bytes",
                "total_file_bytes",
                "btree_max_nodes",
                "mainfile_max_records",
                "btree_free_stack",
                "mainfile_free_stack"
        ));

        writer.newLine();
    }

    private void writeRow(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            boolean reuseEnabled,
            String phase,
            int operationCount,
            BTree btree,
            Metrics metrics
    ) throws IOException {
        long indexFileBytes = fileSize(btree.getFilePath());
        long mainFileBytes = fileSize(btree.getMainFile().getFilePath());
        long totalFileBytes = indexFileBytes + mainFileBytes;

        int height = height(btree);

        writer.write(String.join(",",
                String.valueOf(experimentId),
                String.valueOf(m),
                String.valueOf(n),
                keyOrder,
                String.valueOf(reuseEnabled),
                phase,
                String.valueOf(operationCount),
                String.valueOf(metrics.btreeReads),
                String.valueOf(metrics.btreeWrites),
                String.valueOf(metrics.btreeTotal),
                String.valueOf(metrics.mainFileReads),
                String.valueOf(metrics.mainFileWrites),
                String.valueOf(metrics.mainFileTotal),
                String.valueOf(metrics.totalReads),
                String.valueOf(metrics.totalWrites),
                String.valueOf(metrics.totalAccesses),
                formatDouble(metrics.avgAccessesPerOperation),
                formatDouble(metrics.elapsedMs),
                formatDouble(metrics.avgMsPerOperation),
                String.valueOf(height),
                String.valueOf(indexFileBytes),
                String.valueOf(mainFileBytes),
                String.valueOf(totalFileBytes),
                String.valueOf(btree.getMaxRecords()),
                String.valueOf(btree.getMainFile().getMaxRecords()),
                String.valueOf(btree.getFreeStack()),
                String.valueOf(btree.getMainFile().getFreeStack())
        ));

        writer.newLine();
        writer.flush();
    }

    private long fileSize(String filePath) throws IOException {
        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            return 0;
        }

        return Files.size(path);
    }

    private int height(BTree btree) {
        if (btree.getRoot() == 0) {
            return 0;
        }

        int height = 0;
        int nodeId = btree.getRoot();

        while (nodeId != 0) {
            Node node = btree.readNode(nodeId);

            if (node == null) {
                throw new IllegalStateException("Nó inválido ao calcular altura: " + nodeId);
            }

            height++;

            if (node.isLeaf()) {
                break;
            }

            nodeId = node.getA(0);
        }

        return height;
    }

    private String formatDouble(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private void createParentDirectoryIfNeeded(Path path) throws IOException {
        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Armazena as métricas capturadas imediatamente após cada fase do benchmark.
     */
    private static class Metrics {
        private final long btreeReads;
        private final long btreeWrites;
        private final long btreeTotal;

        private final long mainFileReads;
        private final long mainFileWrites;
        private final long mainFileTotal;

        private final long totalReads;
        private final long totalWrites;
        private final long totalAccesses;

        private final double avgAccessesPerOperation;
        private final double elapsedMs;
        private final double avgMsPerOperation;

        private Metrics(
                long btreeReads,
                long btreeWrites,
                long btreeTotal,
                long mainFileReads,
                long mainFileWrites,
                long mainFileTotal,
                long totalReads,
                long totalWrites,
                long totalAccesses,
                double avgAccessesPerOperation,
                double elapsedMs,
                double avgMsPerOperation
        ) {
            this.btreeReads = btreeReads;
            this.btreeWrites = btreeWrites;
            this.btreeTotal = btreeTotal;
            this.mainFileReads = mainFileReads;
            this.mainFileWrites = mainFileWrites;
            this.mainFileTotal = mainFileTotal;
            this.totalReads = totalReads;
            this.totalWrites = totalWrites;
            this.totalAccesses = totalAccesses;
            this.avgAccessesPerOperation = avgAccessesPerOperation;
            this.elapsedMs = elapsedMs;
            this.avgMsPerOperation = avgMsPerOperation;
        }

        private static Metrics from(DiskAccessCounter counter, int operationCount, long elapsedNs) {
            long btreeReads = counter.getBtreeReads();
            long btreeWrites = counter.getBtreeWrites();
            long btreeTotal = counter.getBtreeTotal();

            long mainFileReads = counter.getMainFileReads();
            long mainFileWrites = counter.getMainFileWrites();
            long mainFileTotal = counter.getMainFileTotal();

            long totalReads = counter.getTotalReads();
            long totalWrites = counter.getTotalWrites();
            long totalAccesses = counter.getTotal();

            double elapsedMs = elapsedNs / 1_000_000.0;

            double avgAccessesPerOperation = operationCount == 0
                    ? 0.0
                    : (double) totalAccesses / operationCount;

            double avgMsPerOperation = operationCount == 0
                    ? 0.0
                    : elapsedMs / operationCount;

            return new Metrics(
                    btreeReads,
                    btreeWrites,
                    btreeTotal,
                    mainFileReads,
                    mainFileWrites,
                    mainFileTotal,
                    totalReads,
                    totalWrites,
                    totalAccesses,
                    avgAccessesPerOperation,
                    elapsedMs,
                    avgMsPerOperation
            );
        }
    }
}