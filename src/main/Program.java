package main;

import entities.BTree;
import entities.KAPair;
import entities.Node;

import java.util.HashMap;
import java.util.LinkedList;

public class Program {
    public static void main(String[] args) {
        HashMap<Integer, Node> file = new HashMap<>();

        Node node = new Node();
        node.setA0(0);
        node.addKAPair(0, new KAPair(20, 3));
        node.addKAPair(1, new KAPair(40, 4));
        file.put(1, node);

        node = new Node();
        node.addKAPair(0, new KAPair(10, 0));
        node.addKAPair(1, new KAPair(15, 0));
        file.put(2, node);

        node = new Node();
        node.addKAPair(0, new KAPair(25, 0));
        node.addKAPair(1, new KAPair(30, 5));
        file.put(3, node);

        node = new Node();
        node.addKAPair(0, new KAPair(45, 0));
        node.addKAPair(1, new KAPair(60, 0));
        file.put(4, node);

        node = new Node();
        node.addKAPair(0, new KAPair(35, 0));
        file.put(5, node);

        BTree btree = new BTree(file);

        System.out.println(btree);
    }
}
