package main;

import entities.BTree;
import entities.Record;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class QueryApp extends JFrame {
    private BTree btree;

    private Path selectedDirectory;

    private final Random random;

    private final JTextField directoryField;
    private final JTextField indexFileNameField;
    private final JTextField mainFileNameField;

    private final JTextField orderField;
    private final JTextField recordAmountField;

    private final JComboBox<String> operatorComboBox;
    private final JTextField keyField;
    private final JTextField secondKeyField;

    private final JTextField insertKeyField;
    private final JTextField insertNameField;
    private final JTextField insertAgeField;
    private final JTextField removeKeyField;

    private final JToggleButton randomInsertToggle;
    private final JToggleButton randomRemoveToggle;
    private final JCheckBox randomBatchCheckBox;
    private final JTextField randomBatchAmountField;

    private final DefaultTableModel tableModel;
    private final JTable resultTable;

    private final JTextArea outputArea;
    private final JLabel statusLabel;

    public QueryApp() {
        super("Consulta em Árvore B");

        this.random = new Random();

        selectedDirectory = getDefaultDocumentsDirectory();

        directoryField = new JTextField(selectedDirectory.toAbsolutePath().toString(), 35);
        directoryField.setEditable(false);

        indexFileNameField = new JTextField("index.bin", 12);
        mainFileNameField = new JTextField("main.bin", 12);

        orderField = new JTextField("3", 5);
        recordAmountField = new JTextField("30", 6);

        operatorComboBox = new JComboBox<>(new String[]{
                "ALL",
                "=",
                "BETWEEN",
                ">",
                ">=",
                "<",
                "<="
        });

        keyField = new JTextField(8);
        secondKeyField = new JTextField(8);

        insertKeyField = new JTextField(7);
        insertNameField = new JTextField(14);
        insertAgeField = new JTextField(5);
        removeKeyField = new JTextField(7);

        randomInsertToggle = new JToggleButton("Inserir aleatório");
        randomRemoveToggle = new JToggleButton("Remover aleatório");

        randomInsertToggle.setSelected(true);

        ButtonGroup randomActionGroup = new ButtonGroup();
        randomActionGroup.add(randomInsertToggle);
        randomActionGroup.add(randomRemoveToggle);

        randomBatchCheckBox = new JCheckBox("Batch");
        randomBatchAmountField = new JTextField("10", 5);
        randomBatchAmountField.setEnabled(false);

        tableModel = new DefaultTableModel(
                new Object[]{"Key", "Name", "Age"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultTable = new JTable(tableModel);

        outputArea = new JTextArea(9, 80);
        outputArea.setEditable(false);

        statusLabel = new JLabel("Nenhuma árvore carregada.");

        configureWindow();
    }

    private Path getDefaultDocumentsDirectory() {
        Path documents = Path.of(System.getProperty("user.home"), "Documents");

        if (Files.exists(documents)) {
            return documents;
        }

        return FileSystemView
                .getFileSystemView()
                .getDefaultDirectory()
                .toPath();
    }

    private void configureWindow() {
        setLayout(new BorderLayout(8, 8));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JPanel filePanel = createFilePanel();
        JPanel configPanel = createConfigPanel();
        JPanel queryPanel = createQueryPanel();
        JPanel mutationPanel = createMutationPanel();

        JPanel upperPanel = new JPanel(new GridLayout(4, 1));
        upperPanel.add(filePanel);
        upperPanel.add(configPanel);
        upperPanel.add(queryPanel);
        upperPanel.add(mutationPanel);

        topPanel.add(upperPanel, BorderLayout.CENTER);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Registros selecionados"));
        centerPanel.add(new JScrollPane(resultTable));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Saída"));
        bottomPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        operatorComboBox.addActionListener(event -> updateSecondFieldState());
        randomBatchCheckBox.addActionListener(event -> updateRandomBatchFieldState());

        updateSecondFieldState();
        updateRandomBatchFieldState();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 780);
        setLocationRelativeTo(null);
    }

    private JPanel createFilePanel() {
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.setBorder(BorderFactory.createTitledBorder("Arquivos"));

        JButton chooseDirectoryButton = new JButton("Escolher pasta");
        chooseDirectoryButton.addActionListener(event -> chooseDirectory());

        filePanel.add(new JLabel("Pasta:"));
        filePanel.add(directoryField);
        filePanel.add(chooseDirectoryButton);

        filePanel.add(new JLabel("Índice:"));
        filePanel.add(indexFileNameField);

        filePanel.add(new JLabel("Principal:"));
        filePanel.add(mainFileNameField);

        return filePanel;
    }

    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuração da árvore"));

        configPanel.add(new JLabel("m:"));
        configPanel.add(orderField);

        configPanel.add(new JLabel("Registros iniciais:"));
        configPanel.add(recordAmountField);

        JButton createTreeButton = new JButton("Gerar árvore aleatória");
        createTreeButton.addActionListener(event -> createRandomTree());
        configPanel.add(createTreeButton);

        return configPanel;
    }

    private JPanel createQueryPanel() {
        JPanel queryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queryPanel.setBorder(BorderFactory.createTitledBorder("Consulta"));

        queryPanel.add(new JLabel("WHERE key"));
        queryPanel.add(operatorComboBox);

        queryPanel.add(new JLabel("Valor 1:"));
        queryPanel.add(keyField);

        queryPanel.add(new JLabel("Valor 2:"));
        queryPanel.add(secondKeyField);

        JButton executeButton = new JButton("Executar query");
        executeButton.addActionListener(event -> executeQuery());
        queryPanel.add(executeButton);

        JButton plotButton = new JButton("Visualizar árvore");
        plotButton.addActionListener(event -> showTreePlot());
        queryPanel.add(plotButton);

        JButton reportButton = new JButton("Relatório de acessos");
        reportButton.addActionListener(event -> showDiskAccessReport());
        queryPanel.add(reportButton);

        return queryPanel;
    }

    private JPanel createMutationPanel() {
        JPanel mutationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mutationPanel.setBorder(BorderFactory.createTitledBorder("Inserção e remoção"));

        mutationPanel.add(new JLabel("Inserir key:"));
        mutationPanel.add(insertKeyField);

        mutationPanel.add(new JLabel("Nome:"));
        mutationPanel.add(insertNameField);

        mutationPanel.add(new JLabel("Idade:"));
        mutationPanel.add(insertAgeField);

        JButton insertButton = new JButton("Inserir");
        insertButton.addActionListener(event -> insertManualRecord());
        mutationPanel.add(insertButton);

        mutationPanel.add(new JLabel("Remover key:"));
        mutationPanel.add(removeKeyField);

        JButton removeButton = new JButton("Remover");
        removeButton.addActionListener(event -> removeManualRecord());
        mutationPanel.add(removeButton);

        mutationPanel.add(new JLabel("Aleatório:"));
        mutationPanel.add(randomInsertToggle);
        mutationPanel.add(randomRemoveToggle);

        mutationPanel.add(randomBatchCheckBox);

        mutationPanel.add(new JLabel("Qtd:"));
        mutationPanel.add(randomBatchAmountField);

        JButton executeRandomButton = new JButton("Executar aleatório");
        executeRandomButton.addActionListener(event -> executeRandomMutation());
        mutationPanel.add(executeRandomButton);

        return mutationPanel;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Escolha a pasta onde os arquivos serão salvos");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(selectedDirectory.toFile());

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = chooser.getSelectedFile().toPath();
            directoryField.setText(selectedDirectory.toAbsolutePath().toString());

            statusLabel.setText("Pasta selecionada: " + selectedDirectory.toAbsolutePath());
        }
    }

    private void createRandomTree() {
        try {
            int m = parseOrder();
            int recordAmount = parseRecordAmount();

            Path indexFilePath = getIndexFilePath();
            Path mainFilePath = getMainFilePath();

            Files.createDirectories(selectedDirectory);

            int minKey = 1;
            int maxKey = Math.max(999, recordAmount * 10);

            btree = Tests.createRandomBTree(
                    indexFilePath.toString(),
                    mainFilePath.toString(),
                    m,
                    recordAmount,
                    minKey,
                    maxKey,
                    true,
                    System.currentTimeMillis()
            );

            clearTable();

            List<Record> records = loadAllRecords();
            updateTable(records);

            outputArea.setText("");
            outputArea.append("Nova árvore B aleatória criada.\n\n");

            outputArea.append("Arquivo índice:\n");
            outputArea.append(indexFilePath.toAbsolutePath().toString());
            outputArea.append("\n\n");

            outputArea.append("Arquivo principal:\n");
            outputArea.append(mainFilePath.toAbsolutePath().toString());
            outputArea.append("\n\n");

            outputArea.append("Ordem m = ");
            outputArea.append(String.valueOf(m));
            outputArea.append("\n");

            outputArea.append("Quantidade de registros iniciais = ");
            outputArea.append(String.valueOf(recordAmount));
            outputArea.append("\n");

            outputArea.append("Chaves aleatórias entre ");
            outputArea.append(String.valueOf(minKey));
            outputArea.append(" e ");
            outputArea.append(String.valueOf(maxKey));
            outputArea.append("\n");

            outputArea.append("Reaproveitamento = true\n\n");

            outputArea.append("Registros carregados na tabela: ");
            outputArea.append(String.valueOf(records.size()));
            outputArea.append("\n");

            statusLabel.setText("Árvore carregada. Arquivos salvos em: " + selectedDirectory.toAbsolutePath());
        } catch (Exception ex) {
            showError("Erro ao criar árvore aleatória", ex);
        }
    }

    private void insertManualRecord() {
        if (!hasTree()) {
            return;
        }

        try {
            int key = parseTextFieldAsInt(insertKeyField, "key do registro a inserir");
            String name = insertNameField.getText().trim();
            short age = parseTextFieldAsShort(insertAgeField, "idade");

            if (name.isEmpty()) {
                throw new IllegalArgumentException("Informe o nome do registro.");
            }

            Query query = new Query(btree);
            boolean inserted = query.insert(key, name, age);

            List<Record> records = loadAllRecords();
            updateTable(records);

            outputArea.setText("");

            if (inserted) {
                outputArea.append("Registro inserido com sucesso.\n\n");
                outputArea.append("INSERT record:\n");
                outputArea.append("key = " + key + "\n");
                outputArea.append("name = " + name + "\n");
                outputArea.append("age = " + age + "\n");
            } else {
                outputArea.append("A inserção falhou.\n\n");
                outputArea.append("Já existe um registro com key = ");
                outputArea.append(String.valueOf(key));
                outputArea.append(".\n");
            }

            appendCurrentState(records);
        } catch (Exception ex) {
            showError("Erro ao inserir registro", ex);
        }
    }

    private void removeManualRecord() {
        if (!hasTree()) {
            return;
        }

        try {
            int key = parseTextFieldAsInt(removeKeyField, "key do registro a remover");

            Query query = new Query(btree);
            boolean removed = query.removeByKey(key);

            List<Record> records = loadAllRecords();
            updateTable(records);

            outputArea.setText("");

            if (removed) {
                outputArea.append("Registro removido com sucesso.\n\n");
                outputArea.append("REMOVE WHERE key = ");
                outputArea.append(String.valueOf(key));
                outputArea.append("\n");
            } else {
                outputArea.append("Nenhum registro foi removido.\n\n");
                outputArea.append("Não existe registro com key = ");
                outputArea.append(String.valueOf(key));
                outputArea.append(".\n");
            }

            appendCurrentState(records);
        } catch (Exception ex) {
            showError("Erro ao remover registro", ex);
        }
    }

    private void executeRandomMutation() {
        if (!hasTree()) {
            return;
        }

        try {
            int amount = randomBatchCheckBox.isSelected()
                    ? parseRandomBatchAmount()
                    : 1;

            if (randomInsertToggle.isSelected()) {
                insertRandomRecords(amount);
            } else {
                removeRandomRecords(amount);
            }
        } catch (Exception ex) {
            showError("Erro ao executar operação aleatória", ex);
        }
    }

    private void insertRandomRecords(int amount) {
        Query query = new Query(btree);

        Set<Integer> usedKeys = loadCurrentKeys();
        List<Record> insertedRecords = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            int key = generateUnusedRandomKey(usedKeys);
            String name = "Pessoa " + key;
            short age = (short) random.nextInt(100);

            Record record = new Record(key, name, age);
            boolean inserted = query.insert(record);

            if (inserted) {
                insertedRecords.add(record);
                usedKeys.add(key);
            }
        }

        List<Record> records = loadAllRecords();
        updateTable(records);

        outputArea.setText("");
        outputArea.append("Inserção aleatória concluída.\n\n");

        outputArea.append("Quantidade solicitada: ");
        outputArea.append(String.valueOf(amount));
        outputArea.append("\n");

        outputArea.append("Quantidade inserida: ");
        outputArea.append(String.valueOf(insertedRecords.size()));
        outputArea.append("\n\n");

        outputArea.append("Registros inseridos:\n");

        for (Record record : insertedRecords) {
            outputArea.append(record.toString());
            outputArea.append("\n");
        }

        appendCurrentState(records);
    }

    private void removeRandomRecords(int amount) {
        List<Record> availableRecords = new ArrayList<>(loadAllRecords());

        if (availableRecords.isEmpty()) {
            outputArea.setText("Não há registros para remover.\n");
            statusLabel.setText("Árvore vazia.");
            clearTable();
            return;
        }

        List<Record> removedRecords = new ArrayList<>();

        for (int i = 0; i < amount && !availableRecords.isEmpty(); i++) {
            int index = random.nextInt(availableRecords.size());
            Record record = availableRecords.remove(index);

            boolean removed = new Query(btree).removeByKey(record.getKey());

            if (removed) {
                removedRecords.add(record);
            }
        }

        List<Record> records = loadAllRecords();
        updateTable(records);

        outputArea.setText("");
        outputArea.append("Remoção aleatória concluída.\n\n");

        outputArea.append("Quantidade solicitada: ");
        outputArea.append(String.valueOf(amount));
        outputArea.append("\n");

        outputArea.append("Quantidade removida: ");
        outputArea.append(String.valueOf(removedRecords.size()));
        outputArea.append("\n\n");

        outputArea.append("Registros removidos:\n");

        for (Record record : removedRecords) {
            outputArea.append(record.toString());
            outputArea.append("\n");
        }

        appendCurrentState(records);
    }

    private Set<Integer> loadCurrentKeys() {
        Set<Integer> keys = new HashSet<>();

        for (Record record : loadAllRecords()) {
            keys.add(record.getKey());
        }

        return keys;
    }

    private int generateUnusedRandomKey(Set<Integer> usedKeys) {
        int upperLimit = Math.max(999, (usedKeys.size() + 100) * 10);

        for (int attempt = 0; attempt < 10_000; attempt++) {
            int key = 1 + random.nextInt(upperLimit);

            if (!usedKeys.contains(key)) {
                return key;
            }
        }

        int key = upperLimit + 1;

        while (usedKeys.contains(key)) {
            key++;
        }

        return key;
    }

    private void executeQuery() {
        if (!hasTree()) {
            return;
        }

        try {
            String operator = (String) operatorComboBox.getSelectedItem();

            if (operator == null) {
                return;
            }

            Query query = new Query(btree).select();

            switch (operator) {
                case "ALL" -> {
                    List<Record> records = query.execute();
                    updateTable(records);
                    showQuerySummary("SELECT *", records);
                }

                case "=" -> {
                    int key = parseFirstKey();

                    List<Record> records = query
                            .whereKeyEquals(key)
                            .execute();

                    updateTable(records);
                    showQuerySummary("SELECT * WHERE key = " + key, records);
                }

                case "BETWEEN" -> {
                    int minKey = parseFirstKey();
                    int maxKey = parseSecondKey();

                    List<Record> records = query
                            .whereKeyBetween(minKey, maxKey)
                            .execute();

                    updateTable(records);
                    showQuerySummary(
                            "SELECT * WHERE key BETWEEN " + minKey + " AND " + maxKey +
                                    "\nIntervalo fechado: " + minKey + " <= key <= " + maxKey,
                            records
                    );
                }

                case ">" -> {
                    int key = parseFirstKey();

                    List<Record> records = query
                            .whereKeyGreaterThan(key)
                            .execute();

                    updateTable(records);
                    showQuerySummary("SELECT * WHERE key > " + key, records);
                }

                case ">=" -> {
                    int key = parseFirstKey();

                    List<Record> records = query
                            .whereKeyGreaterOrEqual(key)
                            .execute();

                    updateTable(records);
                    showQuerySummary("SELECT * WHERE key >= " + key, records);
                }

                case "<" -> {
                    int key = parseFirstKey();

                    List<Record> records = query
                            .whereKeyLessThan(key)
                            .execute();

                    updateTable(records);
                    showQuerySummary("SELECT * WHERE key < " + key, records);
                }

                case "<=" -> {
                    int key = parseFirstKey();

                    List<Record> records = query
                            .whereKeyLessOrEqual(key)
                            .execute();

                    updateTable(records);
                    showQuerySummary("SELECT * WHERE key <= " + key, records);
                }

                default -> throw new IllegalStateException("Operador inválido: " + operator);
            }
        } catch (Exception ex) {
            showError("Erro ao executar query", ex);
        }
    }

    private boolean hasTree() {
        if (btree != null) {
            return true;
        }

        JOptionPane.showMessageDialog(
                this,
                "Nenhuma árvore foi carregada. Clique em 'Gerar árvore aleatória' primeiro.",
                "Erro",
                JOptionPane.ERROR_MESSAGE
        );

        return false;
    }

    private List<Record> loadAllRecords() {
        return new Query(btree)
                .select()
                .execute();
    }

    private int parseOrder() {
        String text = orderField.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("Informe o valor de m.");
        }

        int m = Integer.parseInt(text);

        if (m < 3) {
            throw new IllegalArgumentException("O valor de m deve ser maior ou igual a 3.");
        }

        return m;
    }

    private int parseRecordAmount() {
        String text = recordAmountField.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("Informe o número de registros.");
        }

        int amount = Integer.parseInt(text);

        if (amount <= 0) {
            throw new IllegalArgumentException("O número de registros deve ser maior que zero.");
        }

        return amount;
    }

    private int parseRandomBatchAmount() {
        int amount = parseTextFieldAsInt(randomBatchAmountField, "quantidade do batch");

        if (amount <= 0) {
            throw new IllegalArgumentException("A quantidade do batch deve ser maior que zero.");
        }

        return amount;
    }

    private int parseFirstKey() {
        return parseTextFieldAsInt(keyField, "Valor 1");
    }

    private int parseSecondKey() {
        return parseTextFieldAsInt(secondKeyField, "Valor 2");
    }

    private int parseTextFieldAsInt(JTextField field, String fieldName) {
        String text = field.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("Informe o campo " + fieldName + ".");
        }

        return Integer.parseInt(text);
    }

    private short parseTextFieldAsShort(JTextField field, String fieldName) {
        int value = parseTextFieldAsInt(field, fieldName);

        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Valor inválido para " + fieldName + ".");
        }

        return (short) value;
    }

    private Path getIndexFilePath() {
        String fileName = indexFileNameField.getText().trim();

        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome do arquivo de índice.");
        }

        return selectedDirectory.resolve(fileName);
    }

    private Path getMainFilePath() {
        String fileName = mainFileNameField.getText().trim();

        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome do arquivo principal.");
        }

        return selectedDirectory.resolve(fileName);
    }

    private void updateTable(List<Record> records) {
        clearTable();

        for (Record record : records) {
            tableModel.addRow(new Object[]{
                    record.getKey(),
                    record.getName(),
                    record.getAge()
            });
        }

        statusLabel.setText("Registros na tabela: " + records.size());
    }

    private void clearTable() {
        tableModel.setRowCount(0);
    }

    private void showQuerySummary(String queryText, List<Record> records) {
        outputArea.setText("");
        outputArea.append("Query executada:\n");
        outputArea.append(queryText);
        outputArea.append("\n\n");

        outputArea.append("Registros encontrados: ");
        outputArea.append(String.valueOf(records.size()));
        outputArea.append("\n");

        appendCurrentState(records);
    }

    private void appendCurrentState(List<Record> records) {
        outputArea.append("\n");
        outputArea.append("Total de registros ativos exibidos: ");
        outputArea.append(String.valueOf(records.size()));
        outputArea.append("\n\n");

        if (btree != null) {
            outputArea.append("Arquivo índice:\n");
            outputArea.append(btree.getFilePath());
            outputArea.append("\n\n");

            outputArea.append("Arquivo principal:\n");
            outputArea.append(btree.getMainFile().getFilePath());
            outputArea.append("\n\n");

            outputArea.append("Acessos a disco acumulados:\n");
            outputArea.append(btree.getDiskAccessCounter().report());
        }
    }

    private void showTreePlot() {
        if (!hasTree()) {
            return;
        }

        outputArea.setText("");
        outputArea.append("PLOT DA ÁRVORE B:\n\n");
        outputArea.append(btree.plot());
        outputArea.append("\n\nDUMP DOS NÓS:\n\n");
        outputArea.append(btree.dump());
    }

    private void showDiskAccessReport() {
        if (!hasTree()) {
            return;
        }

        outputArea.setText("");
        outputArea.append(btree.getDiskAccessCounter().report());
    }

    private void updateSecondFieldState() {
        String operator = (String) operatorComboBox.getSelectedItem();

        boolean usesFirstKey = !"ALL".equals(operator);
        boolean usesSecondKey = "BETWEEN".equals(operator);

        keyField.setEnabled(usesFirstKey);
        secondKeyField.setEnabled(usesSecondKey);

        if (!usesFirstKey) {
            keyField.setText("");
        }

        if (!usesSecondKey) {
            secondKeyField.setText("");
        }
    }

    private void updateRandomBatchFieldState() {
        randomBatchAmountField.setEnabled(randomBatchCheckBox.isSelected());

        if (!randomBatchCheckBox.isSelected()) {
            randomBatchAmountField.setText("1");
        } else if (randomBatchAmountField.getText().trim().isEmpty()
                || "1".equals(randomBatchAmountField.getText().trim())) {
            randomBatchAmountField.setText("10");
        }
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                message + ":\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QueryApp app = new QueryApp();
            app.setVisible(true);
        });
    }
}