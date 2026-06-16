package main;

import entities.BTree;
import entities.MainFile;
import entities.Record;

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
}
