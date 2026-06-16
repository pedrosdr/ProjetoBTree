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

    public static void memoryReuseMainFile() {
        MainFile file = MainFile.createNew("records-test.bin");

        int r1 = file.insertRecord(new Record(10, "Ana", (short) 20));
        int r2 = file.insertRecord(new Record(20, "Bruno", (short) 25));
        int r3 = file.insertRecord(new Record(30, "Carlos", (short) 30));

        System.out.println("Após inserir:");
        System.out.println("r1 = " + r1);
        System.out.println("r2 = " + r2);
        System.out.println("r3 = " + r3);
        System.out.println("maxRecords = " + file.getMaxRecords());
        System.out.println("freeStack = " + file.getFreeStack());

        file.removeRecord(r2);

        System.out.println("\nApós remover r2:");
        System.out.println("maxRecords = " + file.getMaxRecords());
        System.out.println("freeStack = " + file.getFreeStack());

        int r4 = file.insertRecord(new Record(40, "Daniela", (short) 22));

        System.out.println("\nApós inserir r4:");
        System.out.println("id r4 = " + r4);
        System.out.println("maxRecords = " + file.getMaxRecords());
        System.out.println("freeStack = " + file.getFreeStack());
        System.out.println("Registro r4 = " + file.readRecord(r4));
    }
}
