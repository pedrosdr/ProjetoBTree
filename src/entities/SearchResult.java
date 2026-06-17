package entities;

/**
 * Encapsula o resultado de uma busca na árvore B.
 *
 * <p>O resultado informa o nó analisado, o índice interno da chave ou ponteiro,
 * se a chave foi encontrada e o endereço do registro associado quando existir.</p>
 */
public class SearchResult {
    private int p;
    private int i;
    private boolean found;
    private int recordAddress;

    /**
     * Cria um resultado de busca sem endereço de registro associado.
     *
     * @param p identificador do nó relacionado ao resultado.
     * @param i índice interno relacionado ao resultado.
     * @param found indica se a chave foi encontrada.
     */
    public SearchResult(int p, int i, boolean found) {
        this(p, i, found, 0);
    }

    /**
     * Cria um resultado de busca completo.
     *
     * @param p identificador do nó relacionado ao resultado.
     * @param i índice interno relacionado ao resultado.
     * @param found indica se a chave foi encontrada.
     * @param recordAddress endereço do registro associado quando a chave é encontrada.
     */
    public SearchResult(int p, int i, boolean found, int recordAddress) {
        this.p = p;
        this.i = i;
        this.found = found;
        this.recordAddress = recordAddress;
    }

    /**
     * Retorna o identificador do nó relacionado ao resultado.
     *
     * @return id do nó.
     */
    public int getP() {
        return p;
    }

    /**
     * Retorna o índice interno relacionado ao resultado.
     *
     * @return índice da chave encontrada ou do caminho de descida.
     */
    public int getI() {
        return i;
    }

    /**
     * Informa se a busca encontrou a chave.
     *
     * @return {@code true} se a chave foi encontrada.
     */
    public boolean getFound() {
        return found;
    }

    /**
     * Retorna o endereço do registro associado à chave encontrada.
     *
     * @return endereço do registro, ou {@code 0} quando inexistente.
     */
    public int getRecordAddress() {
        return recordAddress;
    }
}
