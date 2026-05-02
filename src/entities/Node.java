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
        this.A = new int[m];
        this.K = new int[m - 1];

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

    public int getK(int i) {
        return K[i - 1];
    }

    public void addKAPair(int key, int address) {
        if (n >= m - 1) {
            throw new IllegalStateException("Nó cheio");
        }

        K[n] = key;
        A[n + 1] = address;
        n++;
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