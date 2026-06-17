package entities;

/**
 * Representa um registro de dados armazenado no arquivo principal.
 *
 * <p>Cada registro possui uma chave inteira, um nome com tamanho máximo de
 * 80 caracteres e uma idade armazenada como {@code short}.</p>
 */
public class Record
{
    private final int key;
    private final String name;
    private final short age;

    /**
     * Cria um registro de dados.
     *
     * @param key chave única do registro.
     * @param name nome associado ao registro, com no máximo 80 caracteres.
     * @param age idade associada ao registro.
     * @throws IllegalArgumentException se o nome for nulo ou exceder 80 caracteres.
     */
    public Record(int key, String name, short age) {
        if (name == null) {
            throw new IllegalArgumentException("name não pode ser null");
        }

        if (name.length() > 80) {
            throw new IllegalArgumentException("name deve ter no máximo 80 caracteres");
        }

        this.key = key;
        this.name = name;
        this.age = age;
    }

    /**
     * Retorna a chave do registro.
     *
     * @return chave inteira do registro.
     */
    public int getKey() {
        return key;
    }

    /**
     * Retorna o nome armazenado no registro.
     *
     * @return nome do registro.
     */
    public String getName() {
        return name;
    }

    /**
     * Retorna a idade armazenada no registro.
     *
     * @return idade do registro.
     */
    public short getAge() {
        return age;
    }

    /**
     * Retorna a representação textual do registro.
     *
     * @return string contendo chave, nome e idade.
     */
    @Override
    public String toString() {
        return "Record{" +
                "key=" + key +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
