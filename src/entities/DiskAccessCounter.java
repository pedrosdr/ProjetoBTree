package entities;

/**
 * Contabiliza leituras e escritas realizadas nos arquivos físicos usados pela
 * árvore B e pelo arquivo principal.
 *
 * <p>Os contadores são acumulativos até que {@link #reset()} seja chamado.</p>
 */
public class DiskAccessCounter {
    /**
     * Cria um contador de acessos a disco com todos os valores inicialmente zerados.
     */
    public DiskAccessCounter() {
    }

    private long btreeReads;
    private long btreeWrites;
    private long mainFileReads;
    private long mainFileWrites;

    /**
     * Zera todos os contadores de leitura e escrita.
     */
    public void reset() {
        btreeReads = 0;
        btreeWrites = 0;
        mainFileReads = 0;
        mainFileWrites = 0;
    }

    /**
     * Incrementa o contador de leituras realizadas no arquivo da árvore B.
     */
    public void countBTreeRead() {
        btreeReads++;
    }

    /**
     * Incrementa o contador de escritas realizadas no arquivo da árvore B.
     */
    public void countBTreeWrite() {
        btreeWrites++;
    }

    /**
     * Incrementa o contador de leituras realizadas no arquivo principal.
     */
    public void countMainFileRead() {
        mainFileReads++;
    }

    /**
     * Incrementa o contador de escritas realizadas no arquivo principal.
     */
    public void countMainFileWrite() {
        mainFileWrites++;
    }

    /**
     * Retorna a quantidade de leituras na árvore B.
     *
     * @return total de leituras do arquivo de índice.
     */
    public long getBtreeReads() {
        return btreeReads;
    }

    /**
     * Retorna a quantidade de escritas na árvore B.
     *
     * @return total de escritas do arquivo de índice.
     */
    public long getBtreeWrites() {
        return btreeWrites;
    }

    /**
     * Retorna a quantidade de leituras no arquivo principal.
     *
     * @return total de leituras do arquivo principal.
     */
    public long getMainFileReads() {
        return mainFileReads;
    }

    /**
     * Retorna a quantidade de escritas no arquivo principal.
     *
     * @return total de escritas do arquivo principal.
     */
    public long getMainFileWrites() {
        return mainFileWrites;
    }

    /**
     * Retorna o total de acessos ao arquivo da árvore B.
     *
     * @return soma de leituras e escritas da árvore B.
     */
    public long getBtreeTotal() {
        return btreeReads + btreeWrites;
    }

    /**
     * Retorna o total de acessos ao arquivo principal.
     *
     * @return soma de leituras e escritas do arquivo principal.
     */
    public long getMainFileTotal() {
        return mainFileReads + mainFileWrites;
    }

    /**
     * Retorna o total geral de leituras.
     *
     * @return soma das leituras da árvore B e do arquivo principal.
     */
    public long getTotalReads() {
        return btreeReads + mainFileReads;
    }

    /**
     * Retorna o total geral de escritas.
     *
     * @return soma das escritas da árvore B e do arquivo principal.
     */
    public long getTotalWrites() {
        return btreeWrites + mainFileWrites;
    }

    /**
     * Retorna o total geral de acessos a disco contabilizados.
     *
     * @return soma de todos os acessos contabilizados.
     */
    public long getTotal() {
        return getBtreeTotal() + getMainFileTotal();
    }

    /**
     * Gera um relatório textual com todos os contadores de acesso a disco.
     *
     * @return relatório formatado com leituras, escritas e totais.
     */
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
