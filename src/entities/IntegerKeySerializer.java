package entities;

import java.io.IOException;
import java.io.RandomAccessFile;

public class IntegerKeySerializer implements KeySerializer<Integer> {
    @Override
    public int size() {
        return Integer.BYTES;
    }

    @Override
    public void write(RandomAccessFile raf, Integer key) throws IOException {
        raf.writeInt(key);
    }

    @Override
    public Integer read(RandomAccessFile raf) throws IOException {
        return raf.readInt();
    }

    @Override
    public void writeDefault(RandomAccessFile raf) throws IOException {
        raf.writeInt(0);
    }
}