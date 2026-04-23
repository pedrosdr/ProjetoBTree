package entities;

import java.util.ArrayList;
import java.util.HashMap;

public class BTree {
    // fields
    private HashMap<Integer, Node> file;

    // constructors
    public BTree(HashMap<Integer, Node> file) {
        this.file = file;
    }

    // properties
    public void setFile(HashMap<Integer, Node> file) {
        this.file = file;
    }

    public HashMap<Integer, Node> getFile() {
        return file;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("-----------------------------------\n");
        sb.append(" ID   NODE\n");
        sb.append("-----------------------------------\n");
        for(Integer key : file.keySet()) {
            sb.append(" ");
            sb.append(key);
            sb.append("   ");
            sb.append(file.get(key));
            sb.append("\n");
        }
        sb.append("------------------------------------\n");
        return sb.toString();
    }
}
