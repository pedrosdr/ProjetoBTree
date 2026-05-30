package main;

import entities.BTree;
import entities.Node;

import java.util.Arrays;
import java.util.Stack;

public class Program {
    public static void main(String[] args) {
        BTree btree = BTree.createNew(
                "data.bin",
                3
        );

        Node a = new Node(3);
        a.setA0(2);
        a.addKAPair(30, 3);

        Node b = new Node(3);
        b.setA0(4);
        b.addKAPair(20, 5);

        Node c = new Node(3);
        c.setA0(6);
        c.addKAPair(40, 7);

        Node d = new Node(3);
        d.setA0(0);
        d.addKAPair(10, 0);
        d.addKAPair(15, 0);

        Node e = new Node(3);
        e.setA0(0);
        e.addKAPair(25, 0);

        Node f = new Node(3);
        f.setA0(0);
        f.addKAPair(35, 0);
        f.addKAPair(38, 0);

        Node g = new Node(3);
        g.setA0(0);
        g.addKAPair(45, 0);
        g.addKAPair(50, 0);

        btree.writeNode(1, a);
        btree.writeNode(2, b);
        btree.writeNode(3, c);
        btree.writeNode(4, d);
        btree.writeNode(5, e);
        btree.writeNode(6, f);
        btree.writeNode(7, g);

        btree = BTree.fromFile("data.bin");
        System.out.println(btree.plot());
        btree.remove(40);
        btree.remove(50);
        System.out.println(btree.plot());

    }
}