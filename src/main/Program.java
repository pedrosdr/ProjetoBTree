package main;

import entities.BTree;
import entities.Node;

import java.util.Arrays;
import java.util.Stack;

public class Program {
    public static void main(String[] args) {
        BTree btree = BTree.createNew("data.bin", 3);

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
}