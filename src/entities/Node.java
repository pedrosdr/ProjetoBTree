package entities;

public class Node {
    private final int m;
    private int n;
    private final int[] A;
    private final int[] K;
    private final int[] B;

    public Node(int m) {
        if (m < 3) {
            throw new IllegalArgumentException("m deve ser pelo menos 3");
        }

        this.m = m;
        this.n = 0;
        this.A = new int[m + 1];
        this.K = new int[m];
        this.B = new int[m];

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
        K[i - 1] = value;
    }

    public int getB(int i) {
        return B[i - 1];
    }

    private void setB(int i, int value) {
        B[i - 1] = value;
    }

    public void addKAPair(int key, int address) {
        addKAPair(key, address, 0);
    }

    public void addKAPair(int key, int address, int recordAddress) {
        int i = findInsertIndex(key);
        addKAPair(i, key, address, recordAddress);
    }

    private void addKAPair(int i, int key, int address, int recordAddress) {
        if (n >= m) {
            throw new IllegalStateException("Nó cheio");
        }

        if (i <= 0 || i > n + 1) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        for (int j = n; j >= i; j--) {
            setK(j + 1, getK(j));
            setA(j + 1, getA(j));
            setB(j + 1, getB(j));
        }

        setK(i, key);
        setA(i, address);
        setB(i, recordAddress);
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
        int parentRecordAddress = parent.getB(parentKeyIndex);

        int rightFirstKey = right.getK(1);
        int rightFirstRecordAddress = right.getB(1);
        int rightFirstAddress = right.getA(0);

        addKAPair(parentKey, rightFirstAddress, parentRecordAddress);

        parent.replaceKAPair(parentKeyIndex, rightFirstKey, rightFirstRecordAddress);
        right.removeFirstKAPairAndShiftA0();
    }

    public void borrowFromLeft(Node parent, int parentKeyIndex, Node left) {
        int parentKey = parent.getK(parentKeyIndex);
        int parentRecordAddress = parent.getB(parentKeyIndex);
        int oldA0 = getA(0);

        KAPair leftLast = left.removeLastKAPair();

        addFirstKAPair(parentKey, leftLast.getA(), oldA0, parentRecordAddress);
        parent.replaceKAPair(parentKeyIndex, leftLast.getK(), leftLast.getB());
    }

    public void mergeWithRight(int separatorKey, Node right) {
        mergeWithRight(separatorKey, 0, right);
    }

    public void mergeWithRight(int separatorKey, int separatorRecordAddress, Node right) {
        addKAPair(separatorKey, right.getA(0), separatorRecordAddress);

        for (int i = 1; i <= right.getN(); i++) {
            addKAPair(right.getK(i), right.getA(i), right.getB(i));
        }
    }

    public void replaceKey(int i, int key) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        setK(i, key);
    }

    public void replaceKAPair(int i, int key, int recordAddress) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        setK(i, key);
        setB(i, recordAddress);
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
            B[j] = B[j + 1];
            A[j + 1] = A[j + 2];
        }

        K[n - 1] = 0;
        B[n - 1] = 0;
        A[n] = 0;
        n--;
    }

    public KAPair removeFirstKAPairAndShiftA0() {
        if (n == 0) {
            throw new IllegalStateException("Nó vazio");
        }

        KAPair removed = new KAPair(getK(1), getA(1), getB(1));

        A[0] = A[1];

        for (int i = 1; i < n; i++) {
            setK(i, getK(i + 1));
            setA(i, getA(i + 1));
            setB(i, getB(i + 1));
        }

        K[n - 1] = 0;
        B[n - 1] = 0;
        A[n] = 0;
        n--;

        return removed;
    }

    public KAPair removeLastKAPair() {
        if (n == 0) {
            throw new IllegalStateException("Nó vazio");
        }

        KAPair removed = new KAPair(getK(n), getA(n), getB(n));

        K[n - 1] = 0;
        B[n - 1] = 0;
        A[n] = 0;
        n--;

        return removed;
    }

    public void addFirstKAPair(int key, int leftAddress, int rightAddress) {
        addFirstKAPair(key, leftAddress, rightAddress, 0);
    }

    public void addFirstKAPair(int key, int leftAddress, int rightAddress, int recordAddress) {
        if (n >= m) {
            throw new IllegalStateException("Nó cheio");
        }

        for (int j = n; j >= 1; j--) {
            setK(j + 1, getK(j));
            setA(j + 1, getA(j));
            setB(j + 1, getB(j));
        }

        A[0] = leftAddress;
        setK(1, key);
        setA(1, rightAddress);
        setB(1, recordAddress);
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
            sb.append(",");
            sb.append(B[i - 1]);
            sb.append(")");
        }

        return sb.toString();
    }
}