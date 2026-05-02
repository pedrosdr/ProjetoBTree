package entities;

import entities.Node;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BTree {
    private static final int HEADER_SIZE = 128;

    private final String filePath;
    private final int m;

    private int root;
    private int top;

    private BTree(String filePath, int m) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath não pode ser vazio");
        }

        if (m < 2) {
            throw new IllegalArgumentException("m deve ser pelo menos 2");
        }

        this.filePath = filePath;
        this.m = m;
    }

    public static BTree createNew(String filePath, int m) {
        BTree btree = new BTree(filePath, m);

        btree.root = 0;
        btree.top = 0;
        btree.initializeFile();

        return btree;
    }

    public static BTree fromFile(String filePath) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            int root = raf.readInt();
            int m = raf.readInt();
            int top = raf.readInt();

            BTree btree = new BTree(filePath, m);
            btree.root = root;
            btree.top = top;

            return btree;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao carregar arquivo da BTree", ex);
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public int getM() {
        return m;
    }

    public int getRoot() {
        return root;
    }

    public int getTop() {
        return top;
    }

    private void initializeFile() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.setLength(HEADER_SIZE);
            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inicializar arquivo da BTree", ex);
        }
    }

    private void writeHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        raf.writeInt(root);
        raf.writeInt(m);
        raf.writeInt(top);
    }

    private int nodeSize() {
        return Integer.BYTES
                + Integer.BYTES * m
                + Integer.BYTES * (m - 1);
    }

    private long nodePosition(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id deve ser maior que zero");
        }

        return HEADER_SIZE + (long) (id - 1) * nodeSize();
    }

    public void writeNode(int id, Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node não pode ser null");
        }

        if (node.getM() != m) {
            throw new IllegalArgumentException("node possui m diferente da BTree");
        }

        if (node.getN() < 0 || node.getN() > m - 1) {
            throw new IllegalArgumentException("n inválido no nó");
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            if (root == 0) {
                root = id;
            }

            if (id > top) {
                top = id;
            }

            writeHeader(raf);
            raf.seek(nodePosition(id));
            raf.writeInt(node.getN());

            for (int i = 0; i < m; i++) {
                raf.writeInt(node.getA(i));
            }

            for (int i = 1; i <= m - 1; i++) {
                if (i <= node.getN()) {
                    raf.writeInt(node.getK(i));
                } else {
                    raf.writeInt(0);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao escrever nó " + id, ex);
        }
    }

    public Node readNode(int id) {
        if (!nodeExists(id)) {
            return null;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(nodePosition(id));

            // 1. lê n
            int n = raf.readInt();

            if (n < 0 || n > m - 1) {
                throw new IOException("Nó inválido: n = " + n);
            }

            // 2. lê todos os A
            int[] addresses = new int[m];

            for (int i = 0; i < m; i++) {
                addresses[i] = raf.readInt();
            }

            // 3. lê apenas os K reais
            int[] keys = new int[m - 1];

            for (int i = 0; i < m - 1; i++) {
                keys[i] = raf.readInt();
            }

            Node node = new Node(m);
            node.setA0(addresses[0]);

            for (int i = 1; i <= n; i++) {
                node.addKAPair(keys[i - 1], addresses[i]);
            }

            return node;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao ler nó " + id, ex);
        }
    }

    public SearchResult search(int key) {
        int p = root;
        int q = 0;
        int i = 0;

        while (p != 0) {
            Node node = readNode(p);

            if (node == null) {
                throw new IllegalStateException("Nó " + p + " não existe no arquivo");
            }

            i = node.findChildIndex(key);

            if (i >= 1 && key == node.getK(i)) {
                return new SearchResult(p, i, true);
            }

            q = p;
            p = node.getA(i);
        }

        return new SearchResult(q, i+1, false);
    }

    public boolean nodeExists(int id) {
        if (id <= 0 || id > top) {
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            return nodePosition(id) + nodeSize() <= raf.length();
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao verificar nó " + id, ex);
        }
    }

    public void clear() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            root = 0;
            top = 0;

            raf.setLength(HEADER_SIZE);
            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao limpar arquivo da BTree", ex);
        }
    }

    public String dump() {
        return dump(1, top);
    }

    public String dump(int firstId, int lastId) {
        if (firstId <= 0 || lastId < firstId) {
            throw new IllegalArgumentException("Intervalo inválido");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("-----------------------------------\n");
        sb.append(String.format("%-5s %s\n", "ID", "NODE"));
        sb.append("-----------------------------------\n");

        for (int id = firstId; id <= lastId; id++) {
            Node node = readNode(id);

            if (node != null) {
                sb.append(String.format("%-5d %s\n", id, node));
            }
        }

        sb.append("-----------------------------------");

        return sb.toString();
    }
}