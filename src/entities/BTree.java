package entities;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Stack;

public class BTree {
    private static final int HEADER_SIZE = 128;

    private final String filePath;
    private final int m;

    private int root;
    private int maxRecords;
    private int freeStack;

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
        btree.maxRecords = 0;
        btree.freeStack = 0;
        btree.initializeFile();

        return btree;
    }

    public static BTree fromFile(String filePath) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            int root = raf.readInt();
            int m = raf.readInt();
            int maxRecords = raf.readInt();
            int freeStack = raf.readInt();

            BTree btree = new BTree(filePath, m);
            btree.root = root;
            btree.maxRecords = maxRecords;
            btree.freeStack = freeStack;

            return btree;
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao carregar arquivo da BTree", ex);
        }
    }

    public int getFreeStack() {
        return freeStack;
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

    public int getMaxRecords() {
        return maxRecords;
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
        raf.writeInt(maxRecords);
        raf.writeInt(freeStack);
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

    private int nextNodeId(RandomAccessFile raf) throws IOException {
        if (freeStack != 0) {
            int id = freeStack;
            raf.seek(nodePosition(id));
            freeStack = raf.readInt();

            writeHeader(raf);

            return id;
        }

        maxRecords++;
        writeHeader(raf);

        return maxRecords;
    }

    private void releaseNode(RandomAccessFile raf, int id) throws IOException {
        if (id <= 0 || id > maxRecords) {
            throw new IllegalArgumentException("id inválido para liberação: " + id);
        }

        raf.seek(nodePosition(id));

        // No registro livre, o primeiro int deixa de ser n
        // e passa a apontar para o próximo registro livre.
        raf.writeInt(freeStack);

        freeStack = id;

        writeHeader(raf);
    }

    public void writeNode(int id, Node node) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            boolean headerChanged = false;

            if (root == 0) {
                root = id;
                headerChanged = true;
            }

            if (id > maxRecords) {
                maxRecords = id;
                headerChanged = true;
            }

            writeNode(raf, id, node);

            if (headerChanged) {
                writeHeader(raf);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao escrever nó " + id, ex);
        }
    }

    private void writeNode(RandomAccessFile raf, int id, Node node) throws IOException {
        if (node == null) {
            throw new IllegalArgumentException("node não pode ser null");
        }

        if (node.getM() != m) {
            throw new IllegalArgumentException("node possui m diferente da BTree");
        }

        if (node.getN() < 0 || node.getN() > m - 1) {
            throw new IllegalArgumentException("n inválido no nó");
        }

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
    }

    public Node readNode(int id) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            return readNode(raf, id);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao ler nó " + id, ex);
        }
    }

    private Node readNode(RandomAccessFile raf, int id) throws IOException {
        if (!nodeExists(id)) {
            return null;
        }

        long position = nodePosition(id);

        if (position + nodeSize() > raf.length()) {
            return null;
        }

        raf.seek(position);

        int n = raf.readInt();

        if (n < 0 || n > m - 1) {
            throw new IOException("Nó inválido: n = " + n);
        }

        int[] addresses = new int[m];

        for (int i = 0; i < m; i++) {
            addresses[i] = raf.readInt();
        }

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
    }

    public SearchResult search(int key) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            return search(raf, key, null);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao buscar chave " + key, ex);
        }
    }

    private SearchResult search(RandomAccessFile raf, int key, Stack<PathEntry> path) throws IOException {
        if (path != null) {
            path.clear();
        }

        int p = root;
        int q = 0;
        int i = 0;

        while (p != 0) {
            Node node = readNode(raf, p);

            if (node == null) {
                throw new IllegalStateException("Nó " + p + " não existe no arquivo");
            }

            if (path != null) {
                path.push(new PathEntry(p, node));
            }

            i = node.findChildIndex(key);

            if (i >= 1 && key == node.getK(i)) {
                return new SearchResult(p, i, true);
            }

            q = p;
            p = node.getA(i);
        }

        return new SearchResult(q, i, false);
    }

    public void insert(int key) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            insert(raf, key);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao inserir chave " + key, ex);
        }
    }

    private void insert(RandomAccessFile raf, int key) throws IOException {
        if (root == 0) {
            Node p = new Node(m);
            p.setA0(0);
            p.addKAPair(key, 0);

            int pId = nextNodeId(raf);
            root = pId;

            writeNode(raf, pId, p);
            writeHeader(raf);
            return;
        }

        Stack<PathEntry> path = new Stack<>();
        SearchResult result = search(raf, key, path);

        if (result.getFound()) {
            return;
        }

        int promotedKey = key;
        int promotedAddress = 0;
        int promotedPAddress = 0;
        boolean allocatedNewNode = false;

        while (!path.isEmpty()) {
            PathEntry entry = path.pop();

            int pId = entry.getId();
            Node p = entry.getNode();

            p.addKAPair(promotedKey, promotedAddress);

            if (p.getN() <= m - 1) {
                writeNode(raf, pId, p);

                if (allocatedNewNode) {
                    writeHeader(raf);
                }

                return;
            }

            SplitResult split = splitOverflowNode(p);

            p = split.getP();
            Node q = split.getQ();

            int qId = nextNodeId(raf);
            allocatedNewNode = true;

            writeNode(raf, pId, p);
            writeNode(raf, qId, q);

            promotedKey = split.getPromotedKey();
            promotedAddress = qId;
            promotedPAddress = pId;
        }

        Node newRoot = new Node(m);
        newRoot.setA0(promotedPAddress);
        newRoot.addKAPair(promotedKey, promotedAddress);

        int newRootId = nextNodeId(raf);
        root = newRootId;

        writeNode(raf, newRootId, newRoot);
        writeHeader(raf);
    }

    private SplitResult splitOverflowNode(Node node) {
        int middle = (int) Math.ceil(m / 2.0);

        Node p = new Node(m);
        Node q = new Node(m);

        p.setA0(node.getA(0));

        for (int j = 1; j < middle; j++) {
            p.addKAPair(node.getK(j), node.getA(j));
        }

        q.setA0(node.getA(middle));

        for (int j = middle + 1; j <= node.getN(); j++) {
            q.addKAPair(node.getK(j), node.getA(j));
        }

        int promotedKey = node.getK(middle);

        return new SplitResult(p, promotedKey, q);
    }

    public boolean nodeExists(int id) {
        return id > 0 && id <= maxRecords;
    }

    public void clear() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            root = 0;
            maxRecords = 0;
            freeStack = 0;

            raf.setLength(HEADER_SIZE);
            writeHeader(raf);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao limpar arquivo da BTree", ex);
        }
    }

    public String dump() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            StringBuilder sb = new StringBuilder();

            sb.append("-----------------------------------\n");
            sb.append(String.format("%-5s %s\n", "ID", "NODE"));
            sb.append("-----------------------------------\n");

            if (root == 0) {
                sb.append("(árvore vazia)\n");
            } else {
                dumpNode(raf, root, sb, new java.util.HashSet<>());
            }

            sb.append("-----------------------------------");

            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao gerar dump da BTree", ex);
        }
    }

    private void dumpNode(
            RandomAccessFile raf,
            int nodeId,
            StringBuilder sb,
            java.util.Set<Integer> visited
    ) throws IOException {
        if (nodeId == 0) {
            return;
        }

        if (!visited.add(nodeId)) {
            sb.append(String.format("%-5d %s\n", nodeId, "<referência repetida>"));
            return;
        }

        Node node = readNode(raf, nodeId);

        if (node == null) {
            sb.append(String.format("%-5d %s\n", nodeId, "<nó não encontrado>"));
            return;
        }

        sb.append(String.format("%-5d %s\n", nodeId, node));

        for (int i = 0; i <= node.getN(); i++) {
            dumpNode(raf, node.getA(i), sb, visited);
        }
    }

    public String plot() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            StringBuilder sb = new StringBuilder();

            sb.append("-----------------------------------\n");

            if (root == 0) {
                sb.append("(árvore vazia)\n");
            } else {
                plotNode(raf, root, 0, sb);
            }

            sb.append("-----------------------------------");

            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao gerar plot da BTree", ex);
        }
    }

    private void plotNode(RandomAccessFile raf, int nodeId, int level, StringBuilder sb) throws IOException {
        if (nodeId == 0) {
            return;
        }

        Node node = readNode(raf, nodeId);

        if (node == null) {
            sb.append("    ".repeat(level));
            sb.append(nodeId);
            sb.append(": <nó não encontrado>\n");
            return;
        }

        plotNode(raf, node.getA(node.getN()), level + 1, sb);

        sb.append("    ".repeat(level));
        sb.append(nodeId);
        sb.append(": ");
        sb.append(keysToString(node));
        sb.append("\n");

        for (int i = node.getN() - 1; i >= 0; i--) {
            plotNode(raf, node.getA(i), level + 1, sb);
        }
    }

    private String keysToString(Node node) {
        StringBuilder sb = new StringBuilder();

        sb.append("(");

        for (int i = 1; i <= node.getN(); i++) {
            if (i > 1) {
                sb.append(", ");
            }

            sb.append(node.getK(i));
        }

        sb.append(")");

        return sb.toString();
    }

    public void remove(int key) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            remove(raf, key);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao remover chave " + key, ex);
        }
    }

    private void remove(RandomAccessFile raf, int key) throws IOException {
        if (root == 0) {
            return;
        }

        Stack<PathEntry> path = new Stack<>();
        SearchResult result = search(raf, key, path);

        if (!result.getFound()) {
            return;
        }

        int pId = result.getP();
        int i = result.getI();
        Node p = readNode(raf, pId);

        if (p == null) {
            throw new IllegalStateException("Nó " + pId + " não existe no arquivo");
        }

        // Caso 1: a chave está em nó interno.
        // Substitui pela menor chave da subárvore direita.
        if (!p.isLeaf()) {
            int successorId = p.getA(i);
            Node successor = readNode(raf, successorId);

            if (successor == null) {
                throw new IllegalStateException("Nó sucessor " + successorId + " não existe no arquivo");
            }

            while (!successor.isLeaf()) {
                path.push(new PathEntry(successorId, successor));

                successorId = successor.getA(0);
                successor = readNode(raf, successorId);

                if (successor == null) {
                    throw new IllegalStateException("Nó sucessor " + successorId + " não existe no arquivo");
                }
            }

            path.push(new PathEntry(successorId, successor));

            int successorKey = successor.getK(1);

            p.replaceKey(i, successorKey);
            writeNode(raf, pId, p);

            pId = successorId;
            i = 1;
            p = successor;
        }

        // A remoção física sempre acontece em uma folha.
        p.removeKAPairAt(i);

        fixAfterRemove(raf, path, pId, p);
    }

    private void fixAfterRemove(RandomAccessFile raf, Stack<PathEntry> path, int pId, Node p) throws IOException {
        while (true) {
            // A raiz é caso especial: ela pode ficar com 0 chaves.
            if (pId == root) {
                if (p.getN() == 0) {
                    if (p.isLeaf()) {
                        root = 0;
                        maxRecords = 0;
                        freeStack = 0;

                        raf.setLength(HEADER_SIZE);
                        writeHeader(raf);
                    } else {

                        root = p.getA(0);

                        releaseNode(raf, pId);
                    }
                } else {
                    writeNode(raf, pId, p);
                }

                return;
            }

            // Se o nó não ficou abaixo do mínimo, grava em disco.
            if (p.hasMinimumKeys()) {
                writeNode(raf, pId, p);
                return;
            }

            if (path.isEmpty() || path.peek().getId() != pId) {
                throw new IllegalStateException("Caminho inválido durante a remoção");
            }

            path.pop();

            if (path.isEmpty()) {
                throw new IllegalStateException("Nó não raiz sem pai durante a remoção");
            }

            int parentId = path.peek().getId();
            Node parent = readNode(raf, parentId);

            if (parent == null) {
                throw new IllegalStateException("Nó pai " + parentId + " não existe no arquivo");
            }

            int childIndex = parent.findChildAddressIndex(pId);

            // 1) Tenta redistribuir com o irmão direito.
            if (childIndex < parent.getN()) {
                int rightId = parent.getA(childIndex + 1);
                Node right = readNode(raf, rightId);

                if (right == null) {
                    throw new IllegalStateException("Irmão direito " + rightId + " não existe no arquivo");
                }

                if (right.canLend()) {
                    p.borrowFromRight(parent, childIndex + 1, right);

                    writeNode(raf, pId, p);
                    writeNode(raf, parentId, parent);
                    writeNode(raf, rightId, right);

                    return;
                }
            }

            // 2) Tenta redistribuir com o irmão esquerdo.
            if (childIndex > 0) {
                int leftId = parent.getA(childIndex - 1);
                Node left = readNode(raf, leftId);

                if (left == null) {
                    throw new IllegalStateException("Irmão esquerdo " + leftId + " não existe no arquivo");
                }

                if (left.canLend()) {
                    p.borrowFromLeft(parent, childIndex, left);

                    writeNode(raf, leftId, left);
                    writeNode(raf, parentId, parent);
                    writeNode(raf, pId, p);

                    return;
                }
            }

            // 3) Se nenhum irmão pode emprestar, faz fusão.
            if (childIndex < parent.getN()) {
                int rightId = parent.getA(childIndex + 1);
                Node right = readNode(raf, rightId);

                if (right == null) {
                    throw new IllegalStateException("Irmão direito " + rightId + " não existe no arquivo");
                }

                p.mergeWithRight(parent.getK(childIndex + 1), right);
                parent.removeKAPairAt(childIndex + 1);

                writeNode(raf, pId, p);
                releaseNode(raf, rightId);
            } else {
                int leftId = parent.getA(childIndex - 1);
                Node left = readNode(raf, leftId);

                if (left == null) {
                    throw new IllegalStateException("Irmão esquerdo " + leftId + " não existe no arquivo");
                }

                left.mergeWithRight(parent.getK(childIndex), p);
                parent.removeKAPairAt(childIndex);

                writeNode(raf, leftId, left);
                releaseNode(raf, pId);
            }

            // Após a fusão, quem pode ter ficado abaixo do mínimo é o pai.
            pId = parentId;
            p = parent;
        }
    }
}