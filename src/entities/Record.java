package entities;

public class Record
{
    private final int key;
    private final String name;
    private final short age;

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

    public int getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public short getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "Record{" +
                "id=" + key +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
