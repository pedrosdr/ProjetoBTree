package entities;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BTree {
    private static final int HEADER_SIZE = 128;

    private final String filePath;
    private final int m;

    public BTree(String filePath, int m) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath não pode ser vazio");
        }

        if (m < 2) {
            throw new IllegalArgumentException("m deve ser pelo menos 2");
        }

        this.filePath = filePath;
        this.m = m;

        initializeFile();
    }

    public String getFilePath() {
        return filePath;
    }

    public int getM() {
        return m;
    }

    private void initializeFile() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            if (raf.length() < HEADER_SIZE) {
                raf.setLength(HEADER_SIZE);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inicializar arquivo da BTree", ex);
        }
    }

    private int nodeSize() {
        return Integer.BYTES                 // n
                + Integer.BYTES * m          // A[0] até A[m - 1]
                + Integer.BYTES * (m - 1); // K[1] até K[m - 1]
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

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(nodePosition(id));

            // 1. escreve n
            raf.writeInt(node.getN());

            // 2. escreve todos os As: A[0] até A[m - 1]
            for (int i = 0; i < m; i++) {
                raf.writeInt(node.getA(i));
            }

            // 3. escreve apenas os Ks reais: K[1] até K[m - 1]
            for (int i = 1; i <= m - 1; i++) {
                int key = node.getK(i);
                raf.writeInt(key);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao escrever nó " + id, ex);
        }
    }

    public Node readNode(int id) {
        long position = nodePosition(id);

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            if (position >= raf.length()) {
                return null;
            }

            raf.seek(position);

            // 1. lê n
            int n = raf.readInt();

            if (n < 0 || n > m - 1) {
                throw new IOException("Nó inválido: n = " + n);
            }

            // 2. lê todos os As
            int[] addresses = new int[m];

            for (int i = 0; i < m; i++) {
                addresses[i] = raf.readInt();
            }

            // 3. lê apenas os Ks reais
            @SuppressWarnings("unchecked")
            int[] keys = new int[m-1];

            for (int i = 0; i < m - 1; i++) {
                keys[i] = raf.readInt();
            }

            Node node = new Node(m);
            node.setA0(addresses[0]);

            for (int i = 1; i <= n; i++) {
                node.addKAPair(keys[i-1], addresses[i]);
            }

            return node;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao ler nó " + id, ex);
        }
    }

    public boolean nodeExists(int id) {
        if (id <= 0) {
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
            raf.setLength(HEADER_SIZE);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao limpar arquivo da BTree", ex);
        }
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