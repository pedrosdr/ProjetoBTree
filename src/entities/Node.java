package entities;

/**
 * Representa um nó em memória de uma árvore B de ordem {@code m}.
 *
 * <p>O nó armazena até {@code m - 1} chaves, {@code m} endereços de filhos e,
 * para cada chave, um endereço opcional de registro no arquivo principal.</p>
 */
public class Node {
    private final int m;
    private int n;
    private final int[] A;
    private final int[] K;
    private final int[] B;

    /**
     * Cria um nó vazio para uma árvore B de ordem {@code m}.
     *
     * @param m ordem da árvore B.
     * @throws IllegalArgumentException se {@code m < 3}.
     */
    public Node(int m) {
        if (m < 3) {
            throw new IllegalArgumentException("m deve ser pelo menos 3");
        }

        this.m = m;
        this.n = 0;
        this.A = new int[m + 1];
        this.K = new int[m];
        this.B = new int[m];

        this.A[0] = 0;
    }

    /**
     * Retorna a quantidade atual de chaves no nó.
     *
     * @return número de chaves armazenadas.
     */
    public int getN() {
        return n;
    }

    /**
     * Retorna a ordem da árvore B associada ao nó.
     *
     * @return valor de {@code m}.
     */
    public int getM() {
        return m;
    }

    /**
     * Define o endereço do filho mais à esquerda do nó.
     *
     * @param value endereço do filho em {@code A[0]}.
     */
    public void setA0(int value) {
        A[0] = value;
    }

    /**
     * Retorna o endereço de filho armazenado no índice informado.
     *
     * @param i índice do endereço no vetor {@code A}.
     * @return endereço de filho.
     */
    public int getA(int i) {
        return A[i];
    }

    /**
     * Define um endereço de filho no índice informado.
     *
     * @param i índice do vetor {@code A}.
     * @param value endereço a armazenar.
     */
    private void setA(int i, int value) {
        A[i] = value;
    }

    /**
     * Retorna a chave na posição lógica informada.
     *
     * @param i posição lógica da chave, iniciando em {@code 1}.
     * @return chave armazenada na posição.
     */
    public int getK(int i) {
        return K[i - 1];
    }

    /**
     * Define uma chave na posição lógica informada.
     *
     * @param i posição lógica da chave, iniciando em {@code 1}.
     * @param value chave a armazenar.
     */
    private void setK(int i, int value) {
        K[i - 1] = value;
    }

    /**
     * Retorna o endereço de registro associado à chave na posição informada.
     *
     * @param i posição lógica da chave, iniciando em {@code 1}.
     * @return endereço de registro associado.
     */
    public int getB(int i) {
        return B[i - 1];
    }

    /**
     * Define o endereço de registro associado a uma chave.
     *
     * @param i posição lógica da chave, iniciando em {@code 1}.
     * @param value endereço de registro a armazenar.
     */
    private void setB(int i, int value) {
        B[i - 1] = value;
    }

    /**
     * Insere uma chave e um endereço de filho no nó.
     *
     * @param key chave a inserir.
     * @param address endereço do filho à direita da chave.
     */
    public void addKAPair(int key, int address) {
        addKAPair(key, address, 0);
    }

    /**
     * Insere uma chave, um endereço de filho e um endereço de registro no nó.
     *
     * @param key chave a inserir.
     * @param address endereço do filho à direita da chave.
     * @param recordAddress endereço do registro associado.
     */
    public void addKAPair(int key, int address, int recordAddress) {
        int i = findInsertIndex(key);
        addKAPair(i, key, address, recordAddress);
    }

    /**
     * Insere uma tripla {@code (key, address, recordAddress)} em uma posição lógica.
     *
     * @param i posição lógica de inserção, iniciando em {@code 1}.
     * @param key chave a inserir.
     * @param address endereço do filho à direita.
     * @param recordAddress endereço do registro associado.
     * @throws IllegalStateException se o nó estiver cheio.
     * @throws IndexOutOfBoundsException se a posição for inválida.
     */
    private void addKAPair(int i, int key, int address, int recordAddress) {
        if (n >= m) {
            throw new IllegalStateException("Nó cheio");
        }

        if (i <= 0 || i > n + 1) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        for (int j = n; j >= i; j--) {
            setK(j + 1, getK(j));
            setA(j + 1, getA(j));
            setB(j + 1, getB(j));
        }

        setK(i, key);
        setA(i, address);
        setB(i, recordAddress);
        n++;
    }

    /**
     * Verifica se o nó é folha.
     *
     * @return {@code true} se o endereço do filho mais à esquerda for zero.
     */
    public boolean isLeaf() {
        return A[0] == 0;
    }

    /**
     * Verifica se o nó respeita a quantidade mínima de chaves.
     *
     * @return {@code true} se o nó possui ao menos o mínimo permitido.
     */
    public boolean hasMinimumKeys() {
        return n >= (int) Math.ceil(m / 2.0) - 1;
    }

    /**
     * Verifica se o nó pode emprestar uma chave a um irmão em underflow.
     *
     * @return {@code true} se o nó possui mais chaves que o mínimo obrigatório.
     */
    public boolean canLend() {
        return n > (int) Math.ceil(m / 2.0) - 1;
    }

    /**
     * Redistribui chaves emprestando a primeira chave do irmão direito.
     *
     * @param parent nó pai que contém a chave separadora.
     * @param parentKeyIndex posição da chave separadora no pai.
     * @param right irmão direito do nó atual.
     */
    public void borrowFromRight(Node parent, int parentKeyIndex, Node right) {
        int parentKey = parent.getK(parentKeyIndex);
        int parentRecordAddress = parent.getB(parentKeyIndex);

        int rightFirstKey = right.getK(1);
        int rightFirstRecordAddress = right.getB(1);
        int rightFirstAddress = right.getA(0);

        addKAPair(parentKey, rightFirstAddress, parentRecordAddress);

        parent.replaceKAPair(parentKeyIndex, rightFirstKey, rightFirstRecordAddress);
        right.removeFirstKAPairAndShiftA0();
    }

    /**
     * Redistribui chaves emprestando a última chave do irmão esquerdo.
     *
     * @param parent nó pai que contém a chave separadora.
     * @param parentKeyIndex posição da chave separadora no pai.
     * @param left irmão esquerdo do nó atual.
     */
    public void borrowFromLeft(Node parent, int parentKeyIndex, Node left) {
        int parentKey = parent.getK(parentKeyIndex);
        int parentRecordAddress = parent.getB(parentKeyIndex);
        int oldA0 = getA(0);

        KAPair leftLast = left.removeLastKAPair();

        addFirstKAPair(parentKey, leftLast.getA(), oldA0, parentRecordAddress);
        parent.replaceKAPair(parentKeyIndex, leftLast.getK(), leftLast.getB());
    }

    /**
     * Concatena o nó atual com seu irmão direito usando uma chave separadora.
     *
     * @param separatorKey chave separadora proveniente do pai.
     * @param right irmão direito a concatenar.
     */
    public void mergeWithRight(int separatorKey, Node right) {
        mergeWithRight(separatorKey, 0, right);
    }

    /**
     * Concatena o nó atual com seu irmão direito usando chave e endereço de registro separadores.
     *
     * @param separatorKey chave separadora proveniente do pai.
     * @param separatorRecordAddress endereço de registro associado à chave separadora.
     * @param right irmão direito a concatenar.
     */
    public void mergeWithRight(int separatorKey, int separatorRecordAddress, Node right) {
        addKAPair(separatorKey, right.getA(0), separatorRecordAddress);

        for (int i = 1; i <= right.getN(); i++) {
            addKAPair(right.getK(i), right.getA(i), right.getB(i));
        }
    }

    /**
     * Substitui a chave armazenada em uma posição lógica.
     *
     * @param i posição lógica da chave, iniciando em {@code 1}.
     * @param key nova chave.
     * @throws IndexOutOfBoundsException se a posição for inválida.
     */
    public void replaceKey(int i, int key) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        setK(i, key);
    }

    /**
     * Substitui a chave e o endereço de registro de uma posição lógica.
     *
     * @param i posição lógica da chave, iniciando em {@code 1}.
     * @param key nova chave.
     * @param recordAddress novo endereço de registro associado.
     * @throws IndexOutOfBoundsException se a posição for inválida.
     */
    public void replaceKAPair(int i, int key, int recordAddress) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        setK(i, key);
        setB(i, recordAddress);
    }

    /**
     * Procura o índice de um endereço de filho no vetor {@code A}.
     *
     * @param address endereço de filho a localizar.
     * @return índice em que o endereço foi encontrado.
     * @throws IllegalStateException se o endereço não existir no nó.
     */
    public int findChildAddressIndex(int address) {
        for (int i = 0; i <= n; i++) {
            if (A[i] == address) {
                return i;
            }
        }

        throw new IllegalStateException("Endereço " + address + " não encontrado no nó");
    }

    /**
     * Determina o índice do filho que deve ser seguido para uma chave.
     *
     * @param key chave pesquisada.
     * @return índice do vetor {@code A} correspondente ao próximo filho.
     */
    public int findChildIndex(int key) {
        int i = 0;

        while (i < n && key >= getK(i + 1)) {
            i++;
        }

        return i;
    }

    /**
     * Determina a posição lógica em que uma chave deve ser inserida.
     *
     * @param key chave a inserir.
     * @return posição lógica de inserção, iniciando em {@code 1}.
     */
    private int findInsertIndex(int key) {
        return findChildIndex(key) + 1;
    }

    /**
     * Remove a tripla associada à posição lógica informada.
     *
     * @param i posição lógica a remover, iniciando em {@code 1}.
     * @throws IndexOutOfBoundsException se a posição for inválida.
     */
    public void removeKAPairAt(int i) {
        if (i <= 0 || i > n) {
            throw new IndexOutOfBoundsException("Índice inválido: " + i);
        }

        int index = i - 1;

        for (int j = index; j < n - 1; j++) {
            K[j] = K[j + 1];
            B[j] = B[j + 1];
            A[j + 1] = A[j + 2];
        }

        K[n - 1] = 0;
        B[n - 1] = 0;
        A[n] = 0;
        n--;
    }

    /**
     * Remove a primeira tripla do nó e ajusta {@code A[0]} para o antigo {@code A[1]}.
     *
     * @return tripla removida.
     * @throws IllegalStateException se o nó estiver vazio.
     */
    public KAPair removeFirstKAPairAndShiftA0() {
        if (n == 0) {
            throw new IllegalStateException("Nó vazio");
        }

        KAPair removed = new KAPair(getK(1), getA(1), getB(1));

        A[0] = A[1];

        for (int i = 1; i < n; i++) {
            setK(i, getK(i + 1));
            setA(i, getA(i + 1));
            setB(i, getB(i + 1));
        }

        K[n - 1] = 0;
        B[n - 1] = 0;
        A[n] = 0;
        n--;

        return removed;
    }

    /**
     * Remove a última tripla do nó.
     *
     * @return tripla removida.
     * @throws IllegalStateException se o nó estiver vazio.
     */
    public KAPair removeLastKAPair() {
        if (n == 0) {
            throw new IllegalStateException("Nó vazio");
        }

        KAPair removed = new KAPair(getK(n), getA(n), getB(n));

        K[n - 1] = 0;
        B[n - 1] = 0;
        A[n] = 0;
        n--;

        return removed;
    }

    /**
     * Insere uma chave no início do nó sem endereço de registro associado.
     *
     * @param key chave a inserir.
     * @param leftAddress endereço do filho à esquerda da chave.
     * @param rightAddress endereço do filho à direita da chave.
     */
    public void addFirstKAPair(int key, int leftAddress, int rightAddress) {
        addFirstKAPair(key, leftAddress, rightAddress, 0);
    }

    /**
     * Insere uma chave no início do nó com endereços de filhos e de registro.
     *
     * @param key chave a inserir.
     * @param leftAddress endereço do filho à esquerda da chave.
     * @param rightAddress endereço do filho à direita da chave.
     * @param recordAddress endereço do registro associado.
     * @throws IllegalStateException se o nó estiver cheio.
     */
    public void addFirstKAPair(int key, int leftAddress, int rightAddress, int recordAddress) {
        if (n >= m) {
            throw new IllegalStateException("Nó cheio");
        }

        for (int j = n; j >= 1; j--) {
            setK(j + 1, getK(j));
            setA(j + 1, getA(j));
            setB(j + 1, getB(j));
        }

        A[0] = leftAddress;
        setK(1, key);
        setA(1, rightAddress);
        setB(1, recordAddress);
        n++;
    }

    /**
     * Retorna a representação compacta do nó.
     *
     * @return string com quantidade de chaves, ponteiro inicial e triplas armazenadas.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(n);
        sb.append(",");
        sb.append(A[0]);

        for (int i = 1; i <= n; i++) {
            sb.append(",(");
            sb.append(getK(i));
            sb.append(",");
            sb.append(A[i]);
            sb.append(",");
            sb.append(B[i - 1]);
            sb.append(")");
        }

        return sb.toString();
    }
}
