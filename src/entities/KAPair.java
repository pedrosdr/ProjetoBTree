package entities;

public class KAPair {
    private int k;
    private int a;
    private int b;

    public KAPair() {}

    public KAPair(int k, int a) {
        this(k, a, 0);
    }

    public KAPair(int k, int a, int b) {
        this.k = k;
        this.a = a;
        this.b = b;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    @Override
    public String toString() {
        return "(" + k + ", " + a + ", " + b + ")";
    }
}