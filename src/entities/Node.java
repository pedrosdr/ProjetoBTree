package entities;

import java.util.ArrayList;
import java.util.LinkedList;

public class Node {
    // fields
    private int n = 0;
    private int a0 = 0;
    private LinkedList<KAPair> an = new LinkedList<>();

    // constructors
    public Node() {}

    // properties
    public int getN(){
        return this.n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public void setA0(int a0) {
        this.a0 = a0;
        if(a0 > 0) n++;
    }

    public int getA0() {
        return a0;
    }

    public LinkedList<KAPair> getAn() {
        return an;
    }

    // methods
    public void addKAPair(int pos, KAPair ka) {
        an.add(ka);
        if(ka.getA() > 0) n++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(n);
        sb.append(", ");
        sb.append(a0);
        sb.append(", ");
        for(int i = 0; i < an.size(); i++) {
            sb.append(an.get(i));
            if(i != an.size()-1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
