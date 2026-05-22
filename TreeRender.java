import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Тестовое задание
 */
public class TreeRender {

    /**
     * Узел дерева
     */
    static class Node {
        // NEW: замена на string, value может принимать любое числовое значение в дес формате
        final String value;
        final List<Node> children = new LinkedList<>();

        Node(String value) {
            this.value = value;
        }

        Node() {
            // root
            this.value = null;
        }
    }

    /**
     * Парсер
     */
    static class Parser {
        private final String input;
        private int pos;
        private int vertex;

        Parser(String input) {
            this.input = input;
        }

        /**
         * Парсит дерево из строки.
         */
        public Node parse() {
            pos = 0;
            vertex = 0;
            if (input.charAt(pos) != '(') {
                throw new IllegalArgumentException("Дерево должно начинаться с '('");
            }

            var root = new Node();
            parseNode(root);
            if (vertex != 0) {
                throw new IllegalArgumentException("Выражение неверно: нарушена вложенность в выражении");
            }
            return root;
        }

        /**
         * Парсит узел и его детей.
         */
        private void parseNode(Node parent) {
            if (parent == null) {
                throw new IllegalArgumentException("Дерево должно иметь корневой каталог");
            }

            StringBuilder sb = new StringBuilder();

            while (pos < input.length()) {
                char ch = input.charAt(pos);
                if (pos == 0 && ch != '(') {
                    throw new IllegalArgumentException("Дерево должно начинаться с '('");
                }
                pos++;
                if (ch == '(') {
                    // входим во вложенный парсинг
                    vertex++;

                    // возможно здесь стойт проверять sb на непустое, если по контракту не обязателен разделяющий символд пробела между числом и раскрывающейся скобкой
                    if(sb.length() > 0) {
                        // если sb не пустое, необходимо создать ноду
                        parent.children.add(extractNodeFromSb(sb));
                    }

                    if (parent.children.isEmpty()) {
                        // корневой каталог не имеет value
                        parseNode(parent);
                    } else {
                        // парсим детей последнего добавленного нода
                        parseNode(parent.children.get(parent.children.size() - 1));
                    }
                } else if (ch == ')') {
                    if(sb.length() > 0) {
                        // если sb не пустое, необходимо создать ноду
                        parent.children.add(extractNodeFromSb(sb));
                    }
                    // выходим из вложенного парсинга штатно
                    vertex--;
                    break;
                } else if (Character.isDigit(ch)) {
                    // цифры, не разделенные пробелом, складываем в число
                    sb.append(ch);
                } else if (Character.isSpaceChar(ch)) {
                    // пробел означает завершение числа
                    if(sb.length() > 0) {
                        // если sb не пустое, необходимо создать ноду
                        parent.children.add(extractNodeFromSb(sb));
                    }
                } else {
                    throw new IllegalArgumentException("Неожиданный символ: " + ch);
                }
            }
        }
    }

    /**
     * Получает из строки sb чило, также очищает sb
     */
    private static Node extractNodeFromSb(StringBuilder sb) {
        var node = new Node(sb.toString());
        sb.setLength(0);
        return node;
    }

    /**
     * Визуализатор дерева в псевдо-графическом формате.
     */
    static class Renderer {
        // Размер отсупа для дочерних элементов по умолчанию
        private static final int DEFAULT_INDENT = 3;
        private final List<String> lines = new LinkedList<>();

        /**
         * Возвращает строку - предтавление структуры root
         */
        String render(Node root) {
            lines.clear();
            renderNode(root, "", 0, true);
            return lines.stream().collect(Collectors.joining(System.lineSeparator()));
        }

        /**
         * Рекурсивно отрисовывает узел.
         */
        private void renderNode(Node node, String prefix, int indent, boolean isLast) {
            // Корневой узел не рисуем
            if (node.value != null) {
                // Рисуем текущий узел
                StringBuilder nodeLine = new StringBuilder();

                nodeLine.append(prefix);

                nodeLine.append(node.value);
                if(!node.children.isEmpty()) {
                    nodeLine.append(String.join("", Collections.nCopies(indent - node.value.length(), "-")));
                    nodeLine.append("+");
                }
                lines.add(nodeLine.toString());
            }

            // NEW: расчитать размер отступа для выравнивания
            int childIndent = node.children.stream()
                    .map(n -> n.value)
                    .filter(Objects::nonNull)
                    .map(String::length)
                    .reduce(Integer::max)
                    .orElse(1) + DEFAULT_INDENT;

            // Отрисовываем детей
            Iterator<Node> iterator = node.children.iterator();
            while (iterator.hasNext()) {
                Node child = iterator.next();

                StringBuilder childPrefix = new StringBuilder(prefix);
                if (isLast) {
                    childPrefix.append(String.join("", Collections.nCopies(indent, " ")));
                } else {
                    childPrefix.append("|");
                    childPrefix.append(String.join("", Collections.nCopies(indent - 1, " ")));
                }

                boolean lastChild = !iterator.hasNext();

                renderNode(child, childPrefix.toString(), childIndent, lastChild);
            }
        }
    }

    /**
     * Читает содержимое файла.
     */
    static String readFile(String fileName) throws IOException {
        return Files.readString(Paths.get(fileName));
    }

    /**
     * Записывает содержимое в файл.
     */
    static void writeFile(String fileName, String content) throws IOException {
        Files.writeString(Paths.get(fileName), content);
    }

    /**
     * Ф-я запуска приложения
     * @param args параметры командной строки: должно быть 2 имени файла
     */
    public static void main(String[] args) {
        // Проверка аргументов командной строки
        if (args.length != 2) {
            System.err.println("Ошибка: требуется два аргумента командной строки.");
            System.err.println("Использование: java --source 11 TreeRender.java <input-file> <output-file>");
            System.exit(1);
        }

        String inputFileName = args[0];
        String outputFileName = args[1];

        try {
            // Чтение входного файла
            String input = readFile(inputFileName);
            if (input.isBlank()) {
                throw new IllegalArgumentException("Входной файл пуст");
            }

            // Парсинг дерева
            Parser parser = new Parser(input);
            Node root = parser.parse();

            // Визуализация
            Renderer renderer = new Renderer();
            String output = renderer.render(root);

            // Запись выходного файла
            writeFile(outputFileName, output);

        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка формата входных данных: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Ошибка ввода-вывода: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // примитивный тест, чтобы не тянуть зависимости JUnit
    static class Tests {

        public static void main(String[] args) throws IOException {
            Path inputFile = Path.of("input.txt");
            Path outputFile = Path.of("output.txt");

            testValidTree(inputFile, outputFile);

            System.out.println("Тест завершился успешно");
        }

        private static void testValidTree(Path inputFile, Path outputFile) throws IOException {
            String input = "(1 (2 (4 5 6 (7) 108 (9)) 3))";
            String expected =
                    "1---+\n" +
                    "    2---+\n" +
                    "    |   4\n" +
                    "    |   5\n" +
                    "    |   6-----+\n" +
                    "    |   |     7\n" +
                    "    |   108---+\n" +
                    "    |         9\n" +
                    "    3";

            Files.writeString(inputFile, input);

            TreeRender.main(new String[]{inputFile.toString(), outputFile.toString()});

            String actual = Files.readString(outputFile);
            assertEquals(expected, actual);
        }

        private static void assertEquals(String expected, String actual) {
            if (!Objects.equals(expected, actual)) {
                throw new RuntimeException("Тест завершился неудачей");
            }
        }
    }
}
