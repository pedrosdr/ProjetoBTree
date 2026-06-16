package entities;

public class DiskAccessCounter {
    private long btreeReads;
    private long btreeWrites;
    private long mainFileReads;
    private long mainFileWrites;

    public void reset() {
        btreeReads = 0;
        btreeWrites = 0;
        mainFileReads = 0;
        mainFileWrites = 0;
    }

    public void countBTreeRead() {
        btreeReads++;
    }

    public void countBTreeWrite() {
        btreeWrites++;
    }

    public void countMainFileRead() {
        mainFileReads++;
    }

    public void countMainFileWrite() {
        mainFileWrites++;
    }

    public long getBtreeReads() {
        return btreeReads;
    }

    public long getBtreeWrites() {
        return btreeWrites;
    }

    public long getMainFileReads() {
        return mainFileReads;
    }

    public long getMainFileWrites() {
        return mainFileWrites;
    }

    public long getBtreeTotal() {
        return btreeReads + btreeWrites;
    }

    public long getMainFileTotal() {
        return mainFileReads + mainFileWrites;
    }

    public long getTotalReads() {
        return btreeReads + mainFileReads;
    }

    public long getTotalWrites() {
        return btreeWrites + mainFileWrites;
    }

    public long getTotal() {
        return getBtreeTotal() + getMainFileTotal();
    }

    public String report() {
        return """
                -------- DISK ACCESS REPORT --------
                BTree reads:       %d
                BTree writes:      %d
                BTree total:       %d

                MainFile reads:    %d
                MainFile writes:   %d
                MainFile total:    %d

                Total reads:       %d
                Total writes:      %d
                Total accesses:    %d
                ------------------------------------
                """.formatted(
                btreeReads,
                btreeWrites,
                getBtreeTotal(),
                mainFileReads,
                mainFileWrites,
                getMainFileTotal(),
                getTotalReads(),
                getTotalWrites(),
                getTotal()
        );
    }
}