package entities;

/**
 * Representa o resultado da divisão de um nó da árvore B após overflow.
 *
 * <p>Contém o nó esquerdo, a chave promovida, o endereço de registro promovido
 * e o nó direito gerado pela divisão.</p>
 */
public class SplitResult {
    private final Node p;
    private final int promotedKey;
    private final int promotedRecordAddress;
    private final Node q;

    /**
     * Cria o resultado de uma divisão de nó.
     *
     * @param p nó esquerdo resultante da divisão.
     * @param promotedKey chave promovida ao nó pai.
     * @param promotedRecordAddress endereço de registro associado à chave promovida.
     * @param q nó direito resultante da divisão.
     */
    public SplitResult(Node p, int promotedKey, int promotedRecordAddress, Node q) {
        this.p = p;
        this.promotedKey = promotedKey;
        this.promotedRecordAddress = promotedRecordAddress;
        this.q = q;
    }

    /**
     * Retorna o nó esquerdo resultante da divisão.
     *
     * @return nó esquerdo.
     */
    public Node getP() {
        return p;
    }

    /**
     * Retorna a chave promovida ao pai.
     *
     * @return chave promovida.
     */
    public int getPromotedKey() {
        return promotedKey;
    }

    /**
     * Retorna o endereço de registro associado à chave promovida.
     *
     * @return endereço de registro promovido.
     */
    public int getPromotedRecordAddress() {
        return promotedRecordAddress;
    }

    /**
     * Retorna o nó direito resultante da divisão.
     *
     * @return nó direito.
     */
    public Node getQ() {
        return q;
    }
}
