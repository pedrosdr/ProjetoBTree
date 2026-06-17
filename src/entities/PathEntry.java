package entities;

/**
 * Armazena uma entrada do caminho percorrido durante buscas e remoções na árvore B.
 *
 * <p>Cada entrada contém o identificador persistente do nó e a instância em memória
 * lida do arquivo.</p>
 */
public class PathEntry {
    private final int id;
    private final Node node;

    /**
     * Cria uma entrada de caminho para um nó da árvore B.
     *
     * @param id identificador persistente do nó.
     * @param node instância do nó lida do arquivo.
     */
    public PathEntry(int id, Node node) {
        this.id = id;
        this.node = node;
    }

    /**
     * Retorna o identificador persistente do nó.
     *
     * @return id do nó.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o nó associado à entrada de caminho.
     *
     * @return instância do nó.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Retorna a representação textual da entrada de caminho.
     *
     * @return string contendo id e nó associado.
     */
    @Override
    public String toString() {
        return "PathEntry{" + getId() + ", " + getNode() + "}";
    }
}
