package main;

import entities.BTree;
import entities.MainFile;
import entities.Node;
import entities.Record;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementa uma camada simples de consulta sobre uma {@link BTree}.
 *
 * <p>Esta classe suporta consultas baseadas exclusivamente no campo {@code key},
 * pois a árvore B funciona como índice ordenado pela chave dos registros.</p>
 *
 * <p>Consultas por igualdade usam diretamente a busca indexada da árvore B.
 * Consultas por intervalo percorrem a árvore B em ordem e visitam apenas os ramos
 * que podem conter chaves dentro da condição configurada.</p>
 *
 * <p>Exemplos de uso:</p>
 *
 * <pre>
 * Query query = new Query(btree);
 *
 * Record record = query
 *         .select()
 *         .whereKeyEquals(10)
 *         .executeOne();
 *
 * List&lt;Record&gt; records = query
 *         .select()
 *         .whereKeyBetween(10, 30)
 *         .execute();
 * </pre>
 */
public class Query {
    private final BTree btree;

    private Integer equalsKey;

    private Integer minKey;
    private boolean includeMin;

    private Integer maxKey;
    private boolean includeMax;

    /**
     * Cria uma consulta associada a uma árvore B.
     *
     * @param btree árvore B usada como índice.
     * @throws IllegalArgumentException se {@code btree} for {@code null}.
     */
    public Query(BTree btree) {
        if (btree == null) {
            throw new IllegalArgumentException("btree não pode ser null");
        }

        this.btree = btree;
        clearConditions();
    }

    /**
     * Inicia uma nova consulta do tipo SELECT.
     *
     * <p>Este método limpa qualquer condição anterior definida nesta instância.</p>
     *
     * @return a própria consulta.
     */
    public Query select() {
        clearConditions();
        return this;
    }

    /**
     * Define a condição:
     *
     * <pre>
     * WHERE key = value
     * </pre>
     *
     * <p>Consulta que usa diretamente a busca indexada
     * da árvore B.</p>
     *
     * @param key chave procurada.
     * @return a própria consulta.
     */
    public Query whereKeyEquals(int key) {
        clearConditions();

        this.equalsKey = key;

        return this;
    }

    /**
     * Define a condição:
     *
     * <pre>
     * WHERE key BETWEEN minKey AND maxKey
     * </pre>
     *
     * <p>O intervalo é fechado, isto é, as extremidades são incluídas:</p>
     *
     * <pre>
     * minKey <= key <= maxKey
     * </pre>
     *
     * <p>Portanto, se a consulta for:</p>
     *
     * <pre>
     * whereKeyBetween(10, 30)
     * </pre>
     *
     * <p>serão retornados registros com chave {@code 10}, {@code 30} e qualquer
     * chave entre elas.</p>
     *
     * @param minKey menor chave aceita, inclusive.
     * @param maxKey maior chave aceita, inclusive.
     * @return a própria consulta.
     * @throws IllegalArgumentException se {@code minKey > maxKey}.
     */
    public Query whereKeyBetween(int minKey, int maxKey) {
        if (minKey > maxKey) {
            throw new IllegalArgumentException("minKey não pode ser maior que maxKey");
        }

        clearConditions();

        this.minKey = minKey;
        this.maxKey = maxKey;
        this.includeMin = true;
        this.includeMax = true;

        return this;
    }

    /**
     * Define a condição:
     *
     * <pre>
     * WHERE key > value
     * </pre>
     *
     * <p>O limite inferior é aberto, ou seja, a própria chave {@code value}
     * não é incluída no resultado.</p>
     *
     * @param key limite inferior exclusivo.
     * @return a própria consulta.
     */
    public Query whereKeyGreaterThan(int key) {
        clearConditions();

        this.minKey = key;
        this.includeMin = false;

        return this;
    }

    /**
     * Define a condição:
     *
     * <pre>
     * WHERE key >= value
     * </pre>
     *
     * <p>O limite inferior é fechado, ou seja, a própria chave {@code value}
     * é incluída no resultado se existir.</p>
     *
     * @param key limite inferior inclusivo.
     * @return a própria consulta.
     */
    public Query whereKeyGreaterOrEqual(int key) {
        clearConditions();

        this.minKey = key;
        this.includeMin = true;

        return this;
    }

    /**
     * Define a condição:
     *
     * <pre>
     * WHERE key < value
     * </pre>
     *
     * <p>O limite superior é aberto, ou seja, a própria chave {@code value}
     * não é incluída no resultado.</p>
     *
     * @param key limite superior exclusivo.
     * @return a própria consulta.
     */
    public Query whereKeyLessThan(int key) {
        clearConditions();

        this.maxKey = key;
        this.includeMax = false;

        return this;
    }

    /**
     * Define a condição:
     *
     * <pre>
     * WHERE key <= value
     * </pre>
     *
     * <p>O limite superior é fechado, ou seja, a própria chave {@code value}
     * é incluída no resultado se existir.</p>
     *
     * @param key limite superior inclusivo.
     * @return a própria consulta.
     */
    public Query whereKeyLessOrEqual(int key) {
        clearConditions();

        this.maxKey = key;
        this.includeMax = true;

        return this;
    }

    /**
     * Executa a consulta e retorna todos os registros encontrados.
     *
     * <p>Se nenhuma condição {@code where} for definida, todos os registros ativos
     * indexados pela árvore B serão retornados em ordem crescente de chave.</p>
     *
     * @return lista de registros que satisfazem a condição configurada.
     */
    public List<Record> execute() {
        if (equalsKey != null) {
            Record record = btree.searchRecord(equalsKey);

            if (record == null) {
                return List.of();
            }

            return List.of(record);
        }

        List<Record> records = new ArrayList<>();

        collectByRange(btree.getRoot(), records);

        return records;
    }

    /**
     * Executa a consulta esperando no máximo um registro.
     *
     * <p>Este método é recomendado principalmente para consultas por igualdade:</p>
     *
     * <pre>
     * query.select().whereKeyEquals(10).executeOne();
     * </pre>
     *
     * @return o registro encontrado, ou {@code null} se nenhum registro satisfizer a consulta.
     * @throws IllegalStateException se a consulta retornar mais de um registro.
     */
    public Record executeOne() {
        List<Record> records = execute();

        if (records.isEmpty()) {
            return null;
        }

        if (records.size() > 1) {
            throw new IllegalStateException("A consulta retornou mais de um registro");
        }

        return records.getFirst();
    }

    /**
     * Percorre a árvore B em ordem, coletando os registros cujas chaves satisfazem
     * a condição configurada.
     *
     * @param nodeId identificador do nó atual.
     * @param records lista acumuladora de registros.
     */
    private void collectByRange(int nodeId, List<Record> records) {
        if (nodeId == 0) {
            return;
        }

        Node node = btree.readNode(nodeId);

        if (node == null) {
            return;
        }

        int n = node.getN();

        for (int i = 0; i <= n; i++) {
            if (shouldVisitChild(node, i)) {
                collectByRange(node.getA(i), records);
            }

            if (i < n) {
                int key = node.getK(i + 1);

                if (matchesRange(key)) {
                    Record record = readRecordFromNode(node, i + 1);

                    if (record != null) {
                        records.add(record);
                    }
                }
            }
        }
    }

    /**
     * Lê o registro do arquivo principal a partir do endereço armazenado no nó da árvore B.
     *
     * @param node nó que contém o endereço do registro.
     * @param keyIndex posição lógica da chave no nó, iniciando em {@code 1}.
     * @return registro lido do arquivo principal.
     * @throws IllegalStateException se a árvore B não possuir arquivo principal associado.
     */
    private Record readRecordFromNode(Node node, int keyIndex) {
        MainFile mainFile = btree.getMainFile();

        if (mainFile == null) {
            throw new IllegalStateException("Arquivo principal não foi associado à BTree");
        }

        return mainFile.readRecord(node.getB(keyIndex));
    }

    /**
     * Decide se um filho pode conter alguma chave dentro do intervalo configurado.
     *
     * <p>Esta verificação evita visitar ramos que certamente estão fora da condição
     * da consulta. Ela é usada apenas para consultas por intervalo.</p>
     *
     * @param node nó atual.
     * @param childIndex índice do filho no vetor de ponteiros do nó.
     * @return {@code true} se o filho deve ser visitado.
     */
    private boolean shouldVisitChild(Node node, int childIndex) {
        if (minKey == null && maxKey == null) {
            return true;
        }

        int n = node.getN();

        Integer lowerBound = null;
        Integer upperBound = null;

        if (childIndex > 0) {
            lowerBound = node.getK(childIndex);
        }

        if (childIndex < n) {
            upperBound = node.getK(childIndex + 1);
        }

        if (maxKey != null && lowerBound != null && lowerBound >= maxKey) {
            return false;
        }

        if (minKey != null && upperBound != null && upperBound <= minKey) {
            return false;
        }

        return true;
    }

    /**
     * Verifica se uma chave satisfaz a condição configurada.
     *
     * @param key chave a testar.
     * @return {@code true} se a chave pertence ao resultado da consulta.
     */
    private boolean matchesRange(int key) {
        if (minKey != null) {
            if (key < minKey) {
                return false;
            }

            if (!includeMin && key == minKey) {
                return false;
            }
        }

        if (maxKey != null) {
            if (key > maxKey) {
                return false;
            }

            if (!includeMax && key == maxKey) {
                return false;
            }
        }

        return true;
    }

    /**
     * Remove todas as condições configuradas na consulta.
     */
    private void clearConditions() {
        equalsKey = null;

        minKey = null;
        includeMin = true;

        maxKey = null;
        includeMax = true;
    }
}