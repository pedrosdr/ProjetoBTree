package entities;

public class Node {
    // fields
    private final int m;
    private int n;
    private final int[] A;
    private final int[] K;

    // constructors
    @SuppressWarnings("unchecked")
    public Node(int m) {
        this.m = m;
        this.n = 0;
        A = new int[m];
        setA0(0);
        K = new int[m+1];
    }

    // properties
    public int getN(){
        return this.n;
    }

    public int getM() {
        return m;
    }

    public void setA0(int address) {
        A[0] = address;
    }

    public int getA0() {
        return A[0];
    }

    public int[] getA() {
        return A;
    }

    public int getA(int i) {
        return A[i];
    }

    public int[] getK() {
        return K;
    }

    public int getK(int i) {
        return K[i];
    }

    public void addKAPair(int key, int address) {
        if (n >= m - 1) {
            throw new IllegalStateException("Nó cheio");
        }

        n++;
        K[n] = key;
        A[n] = address;
    }

    public void removeKAPairAt(int i) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        for (int j = i; j < n; j++) {
            K[j] = K[j + 1];
            A[j] = A[j + 1];
        }

        K[n] = 0;
        A[n] = 0;
        n--;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(n);
        sb.append(",");
        sb.append(A[0]);

        for (int i = 1; i <= n; i++) {
            sb.append(",(");
            sb.append(K[i]);
            sb.append(",");
            sb.append(A[i]);
            sb.append(")");
        }

        return sb.toString();
    }
}
