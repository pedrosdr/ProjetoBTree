package entities;

public class Node {
    private final int m;
    private int n;
    private final int[] A;
    private final int[] K;

    public Node(int m) {
        if (m < 2) {
            throw new IllegalArgumentException("m deve ser pelo menos 2");
        }

        this.m = m;
        this.n = 0;
        this.A = new int[m+1];
        this.K = new int[m];

        this.A[0] = 0;
    }

    public int getN() {
        return n;
    }

    public int getM() {
        return m;
    }

    public void setA0(int value) {
        A[0] = value;
    }

    public int getA(int i) {
        return A[i];
    }

    private void setA(int i, int value) {
        A[i] = value;
    }

    public int getK(int i) {
        return K[i - 1];
    }

    private void setK(int i, int value) {
        K[i-1] = value;
    }

    public void addKAPair(int key, int address) {
        int i = findInsertIndex(key);
        addKAPair(i, key, address);
    }

    private void addKAPair(int i, int key, int address) {
        if (n >= m) {
            throw new IllegalStateException("Nó cheio");
        }

        if (i <= 0 || i > n + 1) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        for(int j = n; j >= i; j--) {
            setK(j+1, getK(j));
            setA(j+1, getA(j));
        }

        setK(i, key);
        setA(i, address);
        n++;
    }

    public boolean isLeaf() {
        return A[0] == 0;
    }

    public boolean hasMinimumKeys() {
        return n >= (int) Math.ceil(m / 2.0) - 1;
    }

    public boolean canLend() {
        return n > (int) Math.ceil(m / 2.0) - 1;
    }

    public void borrowFromRight(Node parent, int parentKeyIndex, Node right) {
        int parentKey = parent.getK(parentKeyIndex);
        int rightFirstKey = right.getK(1);
        int rightFirstAddress = right.getA(0);

        addKAPair(parentKey, rightFirstAddress);

        parent.replaceKey(parentKeyIndex, rightFirstKey);
        right.removeFirstKAPairAndShiftA0();
    }

    public void borrowFromLeft(Node parent, int parentKeyIndex, Node left) {
        int parentKey = parent.getK(parentKeyIndex);
        int oldA0 = getA(0);

        KAPair leftLast = left.removeLastKAPair();

        addFirstKAPair(parentKey, leftLast.getA(), oldA0);
        parent.replaceKey(parentKeyIndex, leftLast.getK());
    }

    public void mergeWithRight(int separatorKey, Node right) {
        addKAPair(separatorKey, right.getA(0));

        for (int i = 1; i <= right.getN(); i++) {
            addKAPair(right.getK(i), right.getA(i));
        }
    }

    public void replaceKey(int i, int key) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        setK(i, key);
    }

    public int findChildAddressIndex(int address) {
        for (int i = 0; i <= n; i++) {
            if (A[i] == address) {
                return i;
            }
        }

        throw new IllegalStateException("Endereço " + address + " não encontrado no nó");
    }

    public int findChildIndex(int key) {
        int i = 0;

        while (i < n && key >= getK(i + 1)) {
            i++;
        }

        return i;
    }

    private int findInsertIndex(int key) {
        return findChildIndex(key) + 1;
    }

    public void removeKAPairAt(int i) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        int index = i - 1;

        for (int j = index; j < n - 1; j++) {
            K[j] = K[j + 1];
            A[j + 1] = A[j + 2];
        }

        K[n - 1] = 0;
        A[n] = 0;
        n--;
    }

    public KAPair removeFirstKAPairAndShiftA0() {
        if (n == 0) {
            throw new IllegalStateException("Nó vazio");
        }

        KAPair removed = new KAPair(getK(1), getA(1));

        A[0] = A[1];

        for (int i = 1; i < n; i++) {
            setK(i, getK(i + 1));
            setA(i, getA(i + 1));
        }

        K[n - 1] = 0;
        A[n] = 0;
        n--;

        return removed;
    }

    public KAPair removeLastKAPair() {
        if (n == 0) {
            throw new IllegalStateException("Nó vazio");
        }

        KAPair removed = new KAPair(getK(n), getA(n));

        K[n - 1] = 0;
        A[n] = 0;
        n--;

        return removed;
    }

    public void addFirstKAPair(int key, int leftAddress, int rightAddress) {
        if (n >= m) {
            throw new IllegalStateException("Nó cheio");
        }

        for (int j = n; j >= 1; j--) {
            setK(j + 1, getK(j));
            setA(j + 1, getA(j));
        }

        A[0] = leftAddress;
        setK(1, key);
        setA(1, rightAddress);
        n++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(n);
        sb.append(",");
        sb.append(A[0]);

        for (int i = 1; i <= n; i++) {
            sb.append(",(");
            sb.append(getK(i));
            sb.append(",");
            sb.append(A[i]);
            sb.append(")");
        }

        return sb.toString();
    }
}