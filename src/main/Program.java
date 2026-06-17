package main;

import entities.BTree;
import entities.Record;

import java.util.List;

public class Program {
    public static void main(String[] args) throws Exception {
//        BenchmarkCsv benchmark = new BenchmarkCsv(
//                Path.of("resultados_btree.csv"),
//                Path.of("dados-benchmark"),
//                new int[]{3, 10},
//                new int[]{1_000, 10_000},
//                new String[]{"sequential", "random"},
//                new boolean[]{true, false},
//                1000,
//                123456789L
//        );
//
//        benchmark.run();

        BTree btree = Tests.createRandomBTree(
                "random-index.bin",
                "random-main.bin",
                3,
                30,
                1,
                999,
                true,
                123456789L
        );

        Query query = new Query(btree);
        List<Record> records = query.select().whereKeyGreaterThan(99).execute();
        records.forEach(System.out::println);
    }
}