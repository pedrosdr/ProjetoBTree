package entities;

import java.io.IOException;
import java.io.RandomAccessFile;

public interface KeySerializer<KType extends Comparable<KType>> {
    int size();

    void write(RandomAccessFile raf, KType key) throws IOException;

    KType read(RandomAccessFile raf) throws IOException;

    void writeDefault(RandomAccessFile raf) throws IOException;
}