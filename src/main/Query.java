package main;

import entities.BTree;
import entities.MainFile;
import entities.Node;
import entities.Record;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementa uma camada simples de consulta e modificação sobre uma {@link BTree}.
 *
 * <p>Esta classe trabalha apenas com operações baseadas em {@code key}, pois a
 * árvore B é o índice primário da aplicação.</p>
 *
 * <p>Operações suportadas:</p>
 *
 * <pre>
 * SELECT *;
 * SELECT * WHERE key = x;
 * SELECT * WHERE key BETWEEN min AND max;
 * SELECT * WHERE key > x;
 * SELECT * WHERE key >= x;
 * SELECT * WHERE key < x;
 * SELECT * WHERE key <= x;
 *
 * INSERT record;
 * REMOVE WHERE key = x;
 * UPDATE WHERE key = x;
 * INSERT OR UPDATE record;
 * </pre>
 *
 * <p>O método {@link #whereKeyBetween(int, int)} usa intervalo fechado:</p>
 *
 * <pre>
 * minKey <= key <= maxKey
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
     * <p>O intervalo é fechado, ou seja:</p>
     *
     * <pre>
     * minKey <= key <= maxKey
     * </pre>
     *
     * @param minKey menor chave aceita, inclusive.
     * @param maxKey maior chave aceita, inclusive.
     * @return a própria consulta.
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
     * Insere um novo registro na árvore B e no arquivo principal.
     *
     * <p>Se já existir um registro com a mesma chave, a inserção falha e retorna
     * {@code false}.</p>
     *
     * @param record registro a inserir.
     * @return {@code true} se o registro foi inserido; {@code false} caso a chave já exista.
     */
    public boolean insert(Record record) {
        if (record == null) {
            throw new IllegalArgumentException("record não pode ser null");
        }

        return btree.insertRecord(record);
    }

    /**
     * Insere um novo registro na árvore B e no arquivo principal.
     *
     * @param key chave do registro.
     * @param name nome do registro.
     * @param age idade do registro.
     * @return {@code true} se o registro foi inserido; {@code false} caso a chave já exista.
     */
    public boolean insert(int key, String name, short age) {
        return insert(new Record(key, name, age));
    }

    /**
     * Insere um registro se a chave ainda não existir.
     * Caso a chave já exista, atualiza o registro existente.
     *
     * <p>Esta operação equivale a um UPSERT:</p>
     *
     * <pre>
     * INSERT OR UPDATE
     * </pre>
     *
     * @param record registro a inserir ou atualizar.
     * @return {@code true} se a operação foi concluída.
     */
    public boolean insertOrUpdate(Record record) {
        if (record == null) {
            throw new IllegalArgumentException("record não pode ser null");
        }

        Record existing = btree.searchRecord(record.getKey());

        if (existing == null) {
            return btree.insertRecord(record);
        }

        return updateByKey(record.getKey(), record);
    }

    /**
     * Atualiza o registro selecionado por:
     *
     * <pre>
     * WHERE key = x
     * </pre>
     *
     * <p>Exemplo:</p>
     *
     * <pre>
     * query.whereKeyEquals(10).update(new Record(10, "Ana", (short) 20));
     * </pre>
     *
     * <p>Se o novo registro tiver uma chave diferente da chave antiga, a operação
     * funciona como troca de chave. Nesse caso, a nova chave não pode existir na árvore.</p>
     *
     * @param newRecord novo registro.
     * @return {@code true} se o registro foi atualizado; {@code false} se a chave antiga não existir.
     */
    public boolean update(Record newRecord) {
        if (equalsKey == null) {
            throw new IllegalStateException(
                    "update() exige uma condição WHERE key = x. Use whereKeyEquals(x)."
            );
        }

        return updateByKey(equalsKey, newRecord);
    }

    /**
     * Atualiza o registro selecionado por {@code WHERE key = x}, mantendo a mesma chave.
     *
     * <p>Exemplo:</p>
     *
     * <pre>
     * query.whereKeyEquals(10).update("Ana Silva", (short) 21);
     * </pre>
     *
     * @param name novo nome.
     * @param age nova idade.
     * @return {@code true} se o registro foi atualizado; {@code false} se a chave não existir.
     */
    public boolean update(String name, short age) {
        if (equalsKey == null) {
            throw new IllegalStateException(
                    "update() exige uma condição WHERE key = x. Use whereKeyEquals(x)."
            );
        }

        return updateByKey(equalsKey, new Record(equalsKey, name, age));
    }

    /**
     * Atualiza diretamente o registro associado a uma chave.
     *
     * @param key chave antiga do registro.
     * @param newRecord novo registro.
     * @return {@code true} se o registro foi atualizado; {@code false} se a chave antiga não existir.
     */
    public boolean updateByKey(int key, Record newRecord) {
        if (newRecord == null) {
            throw new IllegalArgumentException("newRecord não pode ser null");
        }

        Record oldRecord = btree.searchRecord(key);

        if (oldRecord == null) {
            return false;
        }

        boolean changesKey = newRecord.getKey() != key;

        if (changesKey && btree.searchRecord(newRecord.getKey()) != null) {
            return false;
        }

        boolean removed = btree.removeRecord(key);

        if (!removed) {
            return false;
        }

        boolean inserted = btree.insertRecord(newRecord);

        if (!inserted) {
            boolean rollback = btree.insertRecord(oldRecord);

            if (!rollback) {
                throw new IllegalStateException(
                        "Falha ao atualizar registro e falha ao restaurar registro antigo"
                );
            }

            return false;
        }

        return true;
    }

    /**
     * Remove o registro selecionado por:
     *
     * <pre>
     * WHERE key = x
     * </pre>
     *
     * <p>Exemplo:</p>
     *
     * <pre>
     * query.whereKeyEquals(10).remove();
     * </pre>
     *
     * @return {@code true} se o registro foi removido; {@code false} se a chave não existir.
     */
    public boolean remove() {
        if (equalsKey == null) {
            throw new IllegalStateException(
                    "remove() exige uma condição WHERE key = x. Use whereKeyEquals(x)."
            );
        }

        return btree.removeRecord(equalsKey);
    }

    /**
     * Remove diretamente o registro associado a uma chave.
     *
     * @param key chave do registro a remover.
     * @return {@code true} se o registro foi removido; {@code false} se a chave não existir.
     */
    public boolean removeByKey(int key) {
        return btree.removeRecord(key);
    }

    /**
     * Executa a consulta e retorna todos os registros encontrados.
     *
     * <p>Se nenhuma condição {@code where} for definida, todos os registros ativos
     * indexados pela árvore B são retornados em ordem crescente de chave.</p>
     *
     * @return lista de registros encontrados.
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
     * @return registro encontrado, ou {@code null} se não houver resultado.
     */
    public Record executeOne() {
        List<Record> records = execute();

        if (records.isEmpty()) {
            return null;
        }

        if (records.size() > 1) {
            throw new IllegalStateException("A consulta retornou mais de um registro");
        }

        return records.get(0);
    }

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

    private Record readRecordFromNode(Node node, int keyIndex) {
        MainFile mainFile = btree.getMainFile();

        if (mainFile == null) {
            throw new IllegalStateException("Arquivo principal não foi associado à BTree");
        }

        return mainFile.readRecord(node.getB(keyIndex));
    }

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

    private void clearConditions() {
        equalsKey = null;

        minKey = null;
        includeMin = true;

        maxKey = null;
        includeMax = true;
    }
}