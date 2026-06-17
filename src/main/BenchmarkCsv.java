package entities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public class BenchmarkCsv {
    private final Path outputCsvPath;
    private final Path dataDirectory;

    private final int[] mValues;
    private final int[] nValues;
    private final String[] keyOrders;

    private final int searchSampleSize;
    private final long randomSeed;

    public BenchmarkCsv() {
        this(
                Path.of("btree_benchmark_results.csv"),
                Path.of("benchmark-data"),
                new int[]{3, 10, 100, 1000},
                new int[]{1_000, 10_000, 100_000},
                new String[]{"sequential", "random"},
                1000,
                123456789L
        );
    }

    public BenchmarkCsv(
            Path outputCsvPath,
            Path dataDirectory,
            int[] mValues,
            int[] nValues,
            String[] keyOrders,
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

        if (searchSampleSize <= 0) {
            throw new IllegalArgumentException("searchSampleSize deve ser maior que zero");
        }

        this.outputCsvPath = outputCsvPath;
        this.dataDirectory = dataDirectory;
        this.mValues = mValues;
        this.nValues = nValues;
        this.keyOrders = keyOrders;
        this.searchSampleSize = searchSampleSize;
        this.randomSeed = randomSeed;
    }

    public void run() {
        try {
            Files.createDirectories(dataDirectory);

            try (BufferedWriter writer = Files.newBufferedWriter(outputCsvPath)) {
                writeHeader(writer);

                int experimentId = 1;

                for (int m : mValues) {
                    for (int n : nValues) {
                        for (String keyOrder : keyOrders) {
                            System.out.println(
                                    "Executando experimento " + experimentId +
                                            " | m=" + m +
                                            " | n=" + n +
                                            " | ordem=" + keyOrder
                            );

                            runExperiment(writer, experimentId, m, n, keyOrder);

                            experimentId++;
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
            String keyOrder
    ) throws IOException {
        String prefix = "exp_" + experimentId +
                "_m_" + m +
                "_n_" + n +
                "_" + keyOrder;

        Path indexFilePath = dataDirectory.resolve(prefix + "_index.bin");
        Path mainFilePath = dataDirectory.resolve(prefix + "_main.bin");

        Files.deleteIfExists(indexFilePath);
        Files.deleteIfExists(mainFilePath);

        int[] keys = generateKeys(n, keyOrder, randomSeed + experimentId);

        BTree btree = BTree.createNew(
                indexFilePath.toString(),
                mainFilePath.toString(),
                m
        );

        btree.getDiskAccessCounter().reset();

        runInsertBuild(writer, experimentId, m, n, keyOrder, btree, keys);
        runSearchHit(writer, experimentId, m, n, keyOrder, btree, keys);
        runSearchMiss(writer, experimentId, m, n, keyOrder, btree);
        runRemoveHalf(writer, experimentId, m, n, keyOrder, btree, keys);
        runReinsertReuse(writer, experimentId, m, n, keyOrder, btree);
    }

    private void runInsertBuild(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
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
                "remove_half",
                operationCount,
                btree,
                metrics
        );
    }

    private void runReinsertReuse(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
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
                "reinsert_reuse",
                operationCount,
                btree,
                metrics
        );
    }

    private int[] generateKeys(int n, String keyOrder, long seed) {
        int[] keys = new int[n];

        for (int i = 0; i < n; i++) {
            keys[i] = i + 1;
        }

        if (keyOrder.equals("random")) {
            shuffle(keys, seed);
        } else if (!keyOrder.equals("sequential")) {
            throw new IllegalArgumentException("keyOrder inválido: " + keyOrder);
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

    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write(String.join(",",
                "experiment_id",
                "m",
                "n",
                "key_order",
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
                "btree_max_nodes",
                "mainfile_max_records",
                "btree_free_stack",
                "mainfile_free_stack",
                "reuse_enabled"
        ));

        writer.newLine();
    }

    private void writeRow(
            BufferedWriter writer,
            int experimentId,
            int m,
            int n,
            String keyOrder,
            String phase,
            int operationCount,
            BTree btree,
            Metrics metrics
    ) throws IOException {
        long indexFileBytes = fileSize(btree.getFilePath());
        long mainFileBytes = fileSize(btree.getMainFile().getFilePath());

        int height = height(btree);

        writer.write(String.join(",",
                String.valueOf(experimentId),
                String.valueOf(m),
                String.valueOf(n),
                keyOrder,
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
                String.valueOf(btree.getMaxRecords()),
                String.valueOf(btree.getMainFile().getMaxRecords()),
                String.valueOf(btree.getFreeStack()),
                String.valueOf(btree.getMainFile().getFreeStack()),
                "true"
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