package main;

import entities.BTree;
import entities.Node;

public class Program {
    public static void main(String[] args) {
        BTree btree = new BTree(
                "data.bin",
                3
        );

        Node a = new Node(3);
        a.setA0(2);
        a.addKAPair(20, 3);
        a.addKAPair(40, 4);

        Node b = new Node(3);
        b.setA0(0);
        b.addKAPair(10, 0);
        b.addKAPair(15, 0);

        Node c = new Node(3);
        c.setA0(0);
        c.addKAPair(25, 0);
        c.addKAPair(30, 5);

        Node d = new Node(3);
        d.setA0(0);
        d.addKAPair(45, 0);
        d.addKAPair(50, 0);

        Node e = new Node(3);
        e.setA0(0);
        e.addKAPair(35, 0);

        btree.writeNode(1, a);
        btree.writeNode(2, b);
        btree.writeNode(3, c);
        btree.writeNode(4, d);
        btree.writeNode(5, e);

        System.out.println(btree.dump(1, 5));
    }
}