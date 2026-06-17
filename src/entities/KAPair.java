package entities;

/**
 * Representa uma tripla usada nos nós da árvore B.
 *
 * <p>A tripla armazena a chave {@code k}, o endereço do filho à direita
 * {@code a} e o endereço do registro no arquivo principal {@code b}.</p>
 */
public class KAPair {
    private int k;
    private int a;
    private int b;

    /**
     * Cria uma tripla vazia com todos os campos inicializados com zero.
     */
    public KAPair() {}

    /**
     * Cria uma tripla com chave e endereço de filho.
     *
     * @param k chave armazenada.
     * @param a endereço do filho à direita da chave.
     */
    public KAPair(int k, int a) {
        this(k, a, 0);
    }

    /**
     * Cria uma tripla completa com chave, endereço de filho e endereço de registro.
     *
     * @param k chave armazenada.
     * @param a endereço do filho à direita da chave.
     * @param b endereço do registro associado à chave.
     */
    public KAPair(int k, int a, int b) {
        this.k = k;
        this.a = a;
        this.b = b;
    }

    /**
     * Retorna a chave da tripla.
     *
     * @return valor da chave.
     */
    public int getK() {
        return k;
    }

    /**
     * Define a chave da tripla.
     *
     * @param k novo valor da chave.
     */
    public void setK(int k) {
        this.k = k;
    }

    /**
     * Retorna o endereço do filho à direita da chave.
     *
     * @return endereço do filho.
     */
    public int getA() {
        return a;
    }

    /**
     * Define o endereço do filho à direita da chave.
     *
     * @param a novo endereço do filho.
     */
    public void setA(int a) {
        this.a = a;
    }

    /**
     * Retorna o endereço do registro associado à chave.
     *
     * @return endereço do registro.
     */
    public int getB() {
        return b;
    }

    /**
     * Define o endereço do registro associado à chave.
     *
     * @param b novo endereço do registro.
     */
    public void setB(int b) {
        this.b = b;
    }

    /**
     * Retorna a representação textual da tripla.
     *
     * @return string no formato {@code (k, a, b)}.
     */
    @Override
    public String toString() {
        return "(" + k + ", " + a + ", " + b + ")";
    }
}
