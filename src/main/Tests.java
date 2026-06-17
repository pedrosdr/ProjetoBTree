package main;

import entities.BTree;
import entities.DiskAccessCounter;
import entities.MainFile;
import entities.Record;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class Tests {
    public static void insertAndDelete() {
        BTree btree = BTree.createNew("btree.bin", 3);

        int[] keys = {
                50, 30, 60, 70, 10, 40, 55, 65, 75, 80, 58
        };

        for (int key : keys) {
            btree.insert(key);
        }

        System.out.println("Árvore inicial");
        System.out.println("freeStack = " + btree.getFreeStack());
        System.out.println(btree.plot());

        btree.remove(58);
        System.out.println("Após remover 58");
        System.out.println("freeStack = " + btree.getFreeStack());
        System.out.println(btree.plot());

        btree.remove(65);
        System.out.println("Após remover 65");
        System.out.println("freeStack = " + btree.getFreeStack());
        System.out.println(btree.plot());

        btree.remove(55);
        System.out.println("Após remover 55");
        System.out.println("freeStack = " + btree.getFreeStack());
        System.out.println(btree.plot());

        btree.remove(40);
        System.out.println("Após remover 40");
        System.out.println("freeStack = " + btree.getFreeStack());
        System.out.println(btree.plot());

        btree.insert(57);
        System.out.println("Após inserir 57");
        System.out.println("freeStack = " + btree.getFreeStack());
        System.out.println(btree.plot());

        btree.insert(42);
        System.out.println("Após inserir 42");
        System.out.println("freeStack = " + btree.getFreeStack());
        System.out.println(btree.plot());
    }

    public static void mainFile() {
        MainFile file = MainFile.createNew("records.bin");

        int r1 = file.insertRecord(new Record(10, "Ana", (short) 20));
        int r2 = file.insertRecord(new Record(20, "Bruno", (short) 25));
        int r3 = file.insertRecord(new Record(30, "Carlos", (short) 30));

        System.out.println(r1); // 1
        System.out.println(r2); // 2
        System.out.println(r3); // 3

        file.removeRecord(r2);

        int r4 = file.insertRecord(new Record(40, "Daniela", (short) 22));

        System.out.println(r4); // 2, reaproveitado

        System.out.println(file.readRecord(r1));
        System.out.println(file.readRecord(r4));
    }

    public static void bTreeMainFile() {
        BTree btree = BTree.createNew("index.bin", "main.bin", 3);

        btree.insertRecord(new Record(10, "Ana", (short) 20));
        btree.insertRecord(new Record(20, "Bruno", (short) 25));
        btree.insertRecord(new Record(30, "Carlos", (short) 30));

        System.out.println(btree.searchRecord(20));
        System.out.println(btree.plot());

        btree.removeRecord(20);

        System.out.println(btree.searchRecord(20));
        System.out.println(btree.plot());
    }

    public static void memoryReuse() {
        BTree btree = BTree.createNew("index-reuse-test.bin", "main-reuse-test.bin", 3);

        System.out.println("=== INSERINDO REGISTROS INICIAIS ===");

        for (int key = 1; key <= 100; key++) {
            boolean inserted = btree.insertRecord(
                    new Record(key, "Pessoa " + key, (short) (key % 100))
            );

            if (!inserted) {
                throw new AssertionError("Falha: chave duplicada inesperada: " + key);
            }
        }

        System.out.println("Após inserir 100 registros:");
        System.out.println("BTree maxRecords = " + btree.getMaxRecords());
        System.out.println("BTree freeStack = " + btree.getFreeStack());
        System.out.println("MainFile maxRecords = " + btree.getMainFile().getMaxRecords());
        System.out.println("MainFile freeStack = " + btree.getMainFile().getFreeStack());

        if (btree.getFreeStack() != 0) {
            throw new AssertionError("Falha: freeStack da BTree deveria começar em 0.");
        }

        if (btree.getMainFile().getFreeStack() != 0) {
            throw new AssertionError("Falha: freeStack do MainFile deveria começar em 0.");
        }

        int btreeMaxBeforeRemove = btree.getMaxRecords();
        int mainMaxBeforeRemove = btree.getMainFile().getMaxRecords();

        System.out.println("\n=== REMOVENDO REGISTROS PARA GERAR ESPAÇO LIVRE ===");

        for (int key = 1; key <= 70; key++) {
            boolean removed = btree.removeRecord(key);

            if (!removed) {
                throw new AssertionError("Falha: chave deveria existir para remoção: " + key);
            }
        }

        System.out.println("Após remover 70 registros:");
        System.out.println("BTree maxRecords = " + btree.getMaxRecords());
        System.out.println("BTree freeStack = " + btree.getFreeStack());
        System.out.println("MainFile maxRecords = " + btree.getMainFile().getMaxRecords());
        System.out.println("MainFile freeStack = " + btree.getMainFile().getFreeStack());

        if (btree.getMainFile().getFreeStack() == 0) {
            throw new AssertionError("Falha: MainFile deveria ter registros livres.");
        }

        if (btree.getFreeStack() == 0) {
            throw new AssertionError("Falha: BTree deveria ter nós livres após remoções/fusões.");
        }

        if (btree.getMaxRecords() != btreeMaxBeforeRemove) {
            throw new AssertionError("Falha: maxRecords da BTree não deveria diminuir.");
        }

        if (btree.getMainFile().getMaxRecords() != mainMaxBeforeRemove) {
            throw new AssertionError("Falha: maxRecords do MainFile não deveria diminuir.");
        }

        int btreeFreeAfterRemove = btree.getFreeStack();
        int mainFreeAfterRemove = btree.getMainFile().getFreeStack();

        System.out.println("\nfreeStack inicial da BTree após remoções = " + btreeFreeAfterRemove);
        System.out.println("freeStack inicial do MainFile após remoções = " + mainFreeAfterRemove);

        System.out.println("\n=== INSERINDO NOVOS REGISTROS ATÉ ZERAR AS DUAS FREESTACKS ===");

        int nextKey = 1000;
        int insertedCount = 0;

        while (btree.getFreeStack() != 0 || btree.getMainFile().getFreeStack() != 0) {
            boolean inserted = btree.insertRecord(
                    new Record(nextKey, "Nova Pessoa " + nextKey, (short) (nextKey % 100))
            );

            if (!inserted) {
                throw new AssertionError("Falha: chave duplicada inesperada: " + nextKey);
            }

            insertedCount++;

            System.out.println(
                    "Inseriu key=" + nextKey +
                            " | BTree freeStack=" + btree.getFreeStack() +
                            " | MainFile freeStack=" + btree.getMainFile().getFreeStack()
            );

            nextKey++;

            if (insertedCount > 500) {
                throw new AssertionError("Falha: passou de 500 inserções e alguma freeStack não zerou.");
            }
        }

        System.out.println("\n=== RESULTADO FINAL ===");
        System.out.println("Registros novos inseridos = " + insertedCount);
        System.out.println("BTree maxRecords = " + btree.getMaxRecords());
        System.out.println("BTree freeStack = " + btree.getFreeStack());
        System.out.println("MainFile maxRecords = " + btree.getMainFile().getMaxRecords());
        System.out.println("MainFile freeStack = " + btree.getMainFile().getFreeStack());

        if (btree.getFreeStack() != 0) {
            throw new AssertionError("Falha: freeStack da BTree deveria estar zerada.");
        }

        if (btree.getMainFile().getFreeStack() != 0) {
            throw new AssertionError("Falha: freeStack do MainFile deveria estar zerada.");
        }

        for (int key = 71; key <= 100; key++) {
            Record record = btree.searchRecord(key);

            if (record == null) {
                throw new AssertionError("Falha: chave antiga deveria continuar existindo: " + key);
            }
        }

        for (int key = 1000; key < nextKey; key++) {
            Record record = btree.searchRecord(key);

            if (record == null) {
                throw new AssertionError("Falha: chave nova deveria existir: " + key);
            }
        }

        for (int key = 1; key <= 70; key++) {
            Record record = btree.searchRecord(key);

            if (record != null) {
                throw new AssertionError("Falha: chave removida não deveria existir: " + key);
            }
        }

        System.out.println("\nTeste passou: MainFile e BTree reutilizaram memória até freeStack = 0.");
    }

    public static void diskAccess() {
        BTree btree = BTree.createNew("index-access-test.bin", "main-access-test.bin", 3);

        DiskAccessCounter counter = btree.getDiskAccessCounter();

// Não contar criação/inicialização dos arquivos.
        counter.reset();

        System.out.println("=== TESTE DE ACESSOS A DISCO ===");

        System.out.println("\n--- Inserindo registros ---");

        btree.insertRecord(new Record(50, "Pessoa 50", (short) 50));
        btree.insertRecord(new Record(30, "Pessoa 30", (short) 30));
        btree.insertRecord(new Record(60, "Pessoa 60", (short) 60));
        btree.insertRecord(new Record(70, "Pessoa 70", (short) 70));
        btree.insertRecord(new Record(10, "Pessoa 10", (short) 10));
        btree.insertRecord(new Record(40, "Pessoa 40", (short) 40));
        btree.insertRecord(new Record(55, "Pessoa 55", (short) 55));
        btree.insertRecord(new Record(65, "Pessoa 65", (short) 65));
        btree.insertRecord(new Record(75, "Pessoa 75", (short) 75));
        btree.insertRecord(new Record(80, "Pessoa 80", (short) 80));

        System.out.println("Após inserções:");
        System.out.println(counter.report());

        long readsAfterInsert = counter.getTotalReads();
        long writesAfterInsert = counter.getTotalWrites();
        long totalAfterInsert = counter.getTotal();

        if (totalAfterInsert <= 0) {
            throw new AssertionError("Falha: inserções deveriam gerar acessos a disco.");
        }

        if (counter.getBtreeTotal() <= 0) {
            throw new AssertionError("Falha: inserções deveriam acessar a BTree.");
        }

        if (counter.getMainFileTotal() <= 0) {
            throw new AssertionError("Falha: inserções deveriam acessar o MainFile.");
        }

        System.out.println("\n--- Buscando registros existentes e inexistentes ---");

        counter.reset();

        Record r50 = btree.searchRecord(50);
        Record r75 = btree.searchRecord(75);
        Record r999 = btree.searchRecord(999);

        System.out.println("Resultado busca 50 = " + r50);
        System.out.println("Resultado busca 75 = " + r75);
        System.out.println("Resultado busca 999 = " + r999);

        System.out.println("\nApós buscas:");
        System.out.println(counter.report());

        if (r50 == null) {
            throw new AssertionError("Falha: chave 50 deveria existir.");
        }

        if (r75 == null) {
            throw new AssertionError("Falha: chave 75 deveria existir.");
        }

        if (r999 != null) {
            throw new AssertionError("Falha: chave 999 não deveria existir.");
        }

        if (counter.getBtreeReads() <= 0) {
            throw new AssertionError("Falha: busca deveria ler nós da BTree.");
        }

        if (counter.getBtreeWrites() != 0) {
            throw new AssertionError("Falha: busca não deveria escrever na BTree.");
        }

        if (counter.getMainFileReads() <= 0) {
            throw new AssertionError("Falha: busca de chaves existentes deveria ler registros do MainFile.");
        }

        if (counter.getMainFileWrites() != 0) {
            throw new AssertionError("Falha: busca não deveria escrever no MainFile.");
        }

        System.out.println("\n--- Removendo registros ---");

        counter.reset();

        boolean removed30 = btree.removeRecord(30);
        boolean removed60 = btree.removeRecord(60);
        boolean removed999 = btree.removeRecord(999);

        System.out.println("remove 30 = " + removed30);
        System.out.println("remove 60 = " + removed60);
        System.out.println("remove 999 = " + removed999);

        System.out.println("\nApós remoções:");
        System.out.println(counter.report());

        if (!removed30) {
            throw new AssertionError("Falha: chave 30 deveria ser removida.");
        }

        if (!removed60) {
            throw new AssertionError("Falha: chave 60 deveria ser removida.");
        }

        if (removed999) {
            throw new AssertionError("Falha: chave 999 não deveria ser removida.");
        }

        if (counter.getBtreeReads() <= 0) {
            throw new AssertionError("Falha: remoção deveria ler a BTree.");
        }

        if (counter.getBtreeWrites() <= 0) {
            throw new AssertionError("Falha: remoção deveria escrever na BTree.");
        }

        if (counter.getMainFileWrites() <= 0) {
            throw new AssertionError("Falha: remoção deveria liberar registros no MainFile.");
        }

        System.out.println("\n--- Testando reuso de memória com contagem ---");

        counter.reset();

        btree.insertRecord(new Record(35, "Pessoa 35", (short) 35));
        btree.insertRecord(new Record(62, "Pessoa 62", (short) 62));

        System.out.println("Após inserir 35 e 62:");
        System.out.println(counter.report());

        if (counter.getMainFileReads() <= 0) {
            throw new AssertionError("Falha: reuso no MainFile deveria ler a freeStack.");
        }

        if (counter.getMainFileWrites() <= 0) {
            throw new AssertionError("Falha: reuso no MainFile deveria escrever header/registro.");
        }

        System.out.println("\n=== RESUMO DOS ACESSOS NAS INSERÇÕES INICIAIS ===");
        System.out.println("Total reads após inserções = " + readsAfterInsert);
        System.out.println("Total writes após inserções = " + writesAfterInsert);
        System.out.println("Total accesses após inserções = " + totalAfterInsert);

        System.out.println("\nTeste passou: acessos a disco foram contabilizados em BTree e MainFile.");
    }

    public static BTree createRandomBTree(
            String indexFilePath,
            String mainFilePath,
            int m,
            int amount,
            int minKey,
            int maxKey,
            boolean reuseEnabled,
            long seed
    ) throws Exception {
        Files.deleteIfExists(Path.of(indexFilePath));
        Files.deleteIfExists(Path.of(mainFilePath));

        BTree btree = BTree.createNew(
                indexFilePath,
                mainFilePath,
                m,
                reuseEnabled
        );

        int[] keys = generateRandomKeys(amount, minKey, maxKey, seed);

        btree.getDiskAccessCounter().reset();

        for (int key : keys) {
            Record record = new Record(
                    key,
                    "Pessoa " + key,
                    (short) (key % 100)
            );

            boolean inserted = btree.insertRecord(record);

            if (!inserted) {
                throw new IllegalStateException("Chave duplicada inesperada: " + key);
            }
        }

        return btree;
    }

    private static int[] generateRandomKeys(
            int amount,
            int minKey,
            int maxKey,
            long seed
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount deve ser maior que zero");
        }

        if (minKey > maxKey) {
            throw new IllegalArgumentException("minKey não pode ser maior que maxKey");
        }

        int range = maxKey - minKey + 1;

        if (amount > range) {
            throw new IllegalArgumentException(
                    "amount não pode ser maior que a quantidade de chaves possíveis"
            );
        }

        int[] values = new int[range];

        for (int i = 0; i < range; i++) {
            values[i] = minKey + i;
        }

        Random random = new Random(seed);

        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);

            int temp = values[i];
            values[i] = values[j];
            values[j] = temp;
        }

        int[] result = new int[amount];

        System.arraycopy(values, 0, result, 0, amount);

        return result;
    }
}
