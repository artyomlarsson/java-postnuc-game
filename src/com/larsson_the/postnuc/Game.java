package com.larsson_the.postnuc;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class Game {
    private final static String SAVES_FOLDER = "saves";
    private final static String DATA_FOLDER = "data";
    private final static int SAME_SAVES_LIMIT = 256;

    private Map<String, String> saveInfo;
    private Map<String, String> characterBasic;
    private Map<String, Integer> characterSpecial;
    private List<Character.Skill> characterSkills;
    private List<Character.Trait> characterTraits;
    private List<Character.InventoryItem> characterInventory;

    private static Logger log = Logger.getLogger(Game.class.getName());
    private boolean debug = true;
    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        new Game();
    }

    public Game() {
        this(true);
    }
    public Game(boolean debug) {
        this.debug = debug;

        init();
        start();
    }

    /**
     * Preparation actions.
     */
    private void init() {}

    /**
     * Main game logic starts from there. Main menu
     */
    private void start() {
        while (true) {
            Menu.Builder mainMenu = new Menu.Builder("Главное меню")
                    .choice("Новая игра", (c) -> newGame());

            try {
                if (Utils.getSaves().length > 0)
                    mainMenu = mainMenu.choice("Загрузить игру", (c) -> loadGame());
            } catch (DirectoryCreatingException | InvalidPathException e) {
                if (debug) e.printStackTrace();
                System.out.println("Ошибка: " + e.getMessage());
            }

            mainMenu = mainMenu.choice("Настройки", (c) -> System.out.println("TODO: SETTINGS"))
                    .choice("О авторах", (c) -> System.out.println("TODO: ABOUT"))
                    .choice("Выход", (c) -> {
                        onExit();
                        System.exit(0);
                    });

            mainMenu.show();
        }

    }

    /**
     * Calls before exit
     */
    private void onExit() {
        System.out.println("Bye.");
        // TODO: autosave?
    }

    /**
     * Save the game
     * @throws MissingArgumentException Throws when some required data to save is missing
     */
    private void save() throws MissingArgumentException {
        saveInfo = (saveInfo == null) ? new HashMap<>() : saveInfo;

        if (characterBasic.get("name") == null) {
            throw new MissingArgumentException("Невозможно сохранить игру без имени!");
        }


        if (saveInfo.get("savename") == null) {
            log.info("No savename for this character. Creating new..");
            log.info("Format of savename(examples): Bob_1, Bob_2, Julia_1...");

            int i = 0;
            while ((new File(Utils.getSavesDir() + '/'
                    + characterBasic.get("name") + "_" + (++i) + ".json").exists())) {
                if (i == SAME_SAVES_LIMIT) {
                    String tmp;
                    System.out.println("Достигнут лимит сохранений с вашим именем(" + characterBasic.get("name") + ")!");
                    do {
                        System.out.print("Перезаписать последнее сохранение(Y/n)?: ");
                    } while (!(tmp = sc.nextLine().toLowerCase()).equals("y") && !tmp.equals("n"));

                    if (tmp.equals("n")) {
                        System.out.println("Возвращение в главное меню...");
                        return;
                    }

                    System.out.println("Перезаписывание последнего сохранения...");
                    break;
                }
            }

            saveInfo.put("savename", characterBasic.get("name") + "_" + i);
            log.info("Created savename " + saveInfo.get("savename"));
        }

        try(Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(Utils.getSavePath(saveInfo.get("savename")))))) {
            JSONObject save = new JSONObject();
            JSONObject saveData = new JSONObject();
            JSONObject charData = new JSONObject();
            JSONObject charDataBasic = new JSONObject();
            JSONObject charDataSpecial = new JSONObject();
            JSONArray charDataInventory = new JSONArray();
            JSONArray charDataSkills = new JSONArray();
            JSONArray charDataTraits = new JSONArray();
            JSONArray charDataEffects = new JSONArray();

            save.put("save", saveData);
            saveData.put("savetime", Utils.getCurrentTimeStamp());
            for (Map.Entry<String, String> entry : saveInfo.entrySet()) {
                saveData.put(entry.getKey(), saveInfo.get(entry.getKey()));
            }
            save.put("character", charData);
            charData.put("basic", charDataBasic);
            for (Map.Entry<String, String> entry : characterBasic.entrySet()) {
                charDataBasic.put(entry.getKey(), characterBasic.get(entry.getKey()));
            }
            charData.put("special", charDataSpecial);
            for (Map.Entry<String, Integer> entry : characterSpecial.entrySet()) {
                charDataSpecial.put(entry.getKey(), characterSpecial.get(entry.getKey()));
            }

            writer.write(save.toString(2));
        } catch (IOException e) {
            if (debug) e.printStackTrace();
            System.out.println("Ошибка: " + e.getMessage());
        }

        log.fine("Game saved successfully as " + saveInfo.get("savename"));
    }

    // Main menu methods

    /**
     * Creating a character and starting from beginning
     * TODO: continue the game after creating a character
     * @return 0 if returning back
     */
    private int newGame() {
        System.out.println("    Добро пожаловать в постъядерный мир. Мир, спустя N лет после того, как в");
        System.out.println("воздух взмыли ядерные боеголовки большинства государств мира. Вам повезло:");
        System.out.println("ваши родители оказались участниками эксперимента \"Убежище\".");

        setupBasic();
        setupSpecial();
        setupSkills();

        try {
            save();
        } catch (MissingArgumentException e) {
            if (debug) e.printStackTrace();
            System.out.println("Ошибка: " + e.getMessage());
            System.out.println("Возвращение в главное меню...");
            return 0;
        }

        // TODO: Продолжить игру

        // TODO: 0 only if back but we have to go further
        return 0;
    }

    /**
     * Loading the game
     * TODO: continue the game after loading a character
     * @return 0 if returning back
     */
    private int loadGame() {
        final String[] saves;
        try {
            saves = Utils.getSaves();
        } catch (DirectoryCreatingException | InvalidPathException e) {
            if (debug) e.printStackTrace();
            System.out.println("Ошибка: " + e.getMessage());
            return 0;
        }

        Menu menu = new Menu.Builder("Выберите сохранение")
                .choices(saves)
                .forAny((c) -> {
                    int choice = Integer.parseInt(c);

                    String savePath = Utils.getSavePath(saves[choice-1], "");
                    JSONObject json = new JSONObject(Utils.readFile(savePath));
                    JSONObject jCharacter = json.getJSONObject("character");
                    JSONObject jCharacterSpecial = jCharacter.getJSONObject("special");
                    JSONObject jCharacterBasic = jCharacter.getJSONObject("basic");
                    JSONObject jSave = json.getJSONObject("save");

                    characterSpecial = new LinkedHashMap<>();
                    for (String key : jCharacterSpecial.keySet())
                        characterSpecial.put(key, jCharacterSpecial.getInt(key));

                    characterBasic = new LinkedHashMap<>();
                    for (String key : jCharacterBasic.keySet())
                        characterBasic.put(key, jCharacterBasic.getString(key));

                    saveInfo = new LinkedHashMap<>();
                    for (String key : jSave.keySet())
                        saveInfo.put(key, jSave.getString(key));
                })
                .backChoice(true)
                .build();
        menu.show();

        if (menu.isBackPressed()) {
            return 0;
        }

        log.info("SUCCESSFULLY GATHERED SAVE INFO:");

        log.info("\tcharacterSpecial:");
        for (String key : characterSpecial.keySet())
            log.info("\t\t" + characterSpecial.get(key));

        log.info("\tcharacterBasic:");
        for (String key : characterBasic.keySet())
            log.info("\t\t" + characterBasic.get(key));

        log.info("save:");
        for (String key : saveInfo.keySet())
            log.info("\t\t" + saveInfo.get(key));

        // TODO: 0 only if back but we have to go further
        return 0;
    }

    // Methods of new game

    /**
     * Setup of basic character stats: gender, name, race<br>
     * All basic stats are stored in <u>characterBasic</u>
     */
    private void setupBasic() {
        String tmp;
        characterBasic = new LinkedHashMap<>();

        new Menu.Builder("Укажите ваш пол")
                .choice("Мужской", (c) -> characterBasic.put("gender", "male"))
                .choice("Женский", (c) -> characterBasic.put("gender", "female"))
                .show();

        System.out.print("Укажите ваше имя: ");
        while (!Utils.isAlpha(tmp = sc.nextLine()) || tmp.length() < 3 || tmp.length() > 15) {
            if (tmp.equals("")) continue;

            System.out.println("Неверный формат! Только латинские буквы(от 3 до 15 символов)");
            System.out.print("Укажите ваше имя: ");
        }
        characterBasic.put("name", Utils.capitalize(tmp));

        new Menu.Builder("Ваша раса")
                .choice("Афроамериканец", (c) -> characterBasic.put("race", "american"))
                .choice("Европеоид", (c) -> characterBasic.put("race", "europeoid"))
                .choice("Азиат", (c) -> characterBasic.put("race", "asian"))
                .choice("Бурят", (c) -> characterBasic.put("race", "buryat"))
                .choice("Казах", (c) -> characterBasic.put("race", "kazakh"))
                .choice("Славянин", (c) -> characterBasic.put("race", "slav"))
                .show();
    }

    /**
     * Setup of SPECIAL character stats: S,P,E,C,I,A,L<br>
     * All basic stats are stored in <u>characterSpecial</u>
     */
    private void setupSpecial() {
        characterSpecial = new LinkedHashMap<>();

        Map<String, Integer> tmpSpecial = new LinkedHashMap<>();
        String line = null;
        int pointsLeft = 5;

        tmpSpecial.put("s", 5);
        tmpSpecial.put("p", 5);
        tmpSpecial.put("e", 5);
        tmpSpecial.put("c", 5);
        tmpSpecial.put("i", 5);
        tmpSpecial.put("a", 5);
        tmpSpecial.put("l", 5);

        System.out.println("Настало время распределить очки SPECIAL.");
        System.out.println("SPECIAL - это система, состоящая из 7 характеристик:");
        System.out.println("  S - Strength(Сила)");
        System.out.println("  P - Perception(Восприятие)");
        System.out.println("  E - Endurance(Выносливость)");
        System.out.println("  C - Charisma(Харизма)");
        System.out.println("  I - Intelligence(Интелект)");
        System.out.println("  A - Agility(Ловкость)");
        System.out.println("  L - Luck(Удача)");
        System.out.println("Каждая хар-ка может иметь значение от 1 до 10.");
        System.out.println("Примеры указания хар-ки: 's=2'(без кавычек), 'P = 3'");
        System.out.println("Когда очков останется 0 и вы уверены в выборе - напишите 'ok'(без кавычек)");
        for (Map.Entry<String, Integer> entry : tmpSpecial.entrySet())
            System.out.println("  " + entry.getKey() + " = " + entry.getValue() + " ");
        System.out.print("Введите команду(или 'help'): ");

        do {
            if (line == null || line.equals("")) continue;

            line = line.trim().replaceAll(" +"," ").toLowerCase();

            if (line.startsWith("help")) {
                String[] lineWords = line.split(" ");

                if (lineWords.length == 1) {
                    System.out.println(" У персонажа есть семь основополагающих характеристик системы SPECIAL.");
                    System.out.println(" Для более подробной справки про каждую из характеристик - введите");
                    System.out.println(" 'help X'(без кавычек), где X - название(или первая буква) хар-ки");
                }
                if (lineWords.length == 2) {
                    switch (lineWords[1]) {
                        case "s": case "strength": case "сила":
                            System.out.println(" Сила(S) - физическая сила персонажа. От неё зависит максимальный вес");
                            System.out.println(" снаряжения, которое он может нести, урон, наносимый руками и ногами,");
                            System.out.println(" холодным и рукопашным оружием.");
                            System.out.println(" Служит для расчёта следующих производных характеристик: максимальный");
                            System.out.println(" груз, урон холодным оружием и здоровье. Кроме того, у многих видов");
                            System.out.println(" оружия есть требования к силе персонажа. За каждую недостающую");
                            System.out.println(" единицу силы шанс попадания снижается на 20 %.");
                            break;
                        case "p": case "perception": case "восприятие":
                            System.out.println(" Восприятие(P) - острота слуха и зрения. Высокий уровень этой");
                            System.out.println(" характеристики необходим снайперу.");
                            System.out.println(" Служит для расчёта производной характеристики «Реакция» и");
                            System.out.println(" ряда навыков: «Взлом», «Ловушки», «Первая помощь» и «Доктор».");
                            System.out.println(" От этой величины зависит эффективная дальность стрельбы, шанс");
                            System.out.println(" заметить мелкие детали и ловушки, а также расстояние до противника ");
                            System.out.println(" в начале случайной встречи.");
                        case "e": case "endurance": case "выносливость":
                            System.out.println(" Выносливость(E) - способность переносить ранения и травмы.");
                            System.out.println(" Используется для расчёта следующих производных характеристик:");
                            System.out.println(" здоровье, сопротивляемость ядам, сопротивляемость радиации,");
                            System.out.println(" скорость восстановления здоровья и уровень навыка «Натуралист».");
                            System.out.println(" На основании этой характеристики компьютер определяет, сможет ли");
                            System.out.println(" персонаж перенести некоторые критические удары, например, в голову,");
                            System.out.println(" без потери сознания.");
                            break;
                        case "c": case "charisma": case "харизма":
                            System.out.println(" Харизма(C) - способность очаровывать благодаря привлекательной");
                            System.out.println(" внешности. Высокая привлекательность поможет найти общий язык со");
                            System.out.println(" многими персонажами. Характеристика влияет на отношение других");
                            System.out.println(" персонажей при знакомстве. Собеседники с высокой привлекательностью");
                            System.out.println(" более устойчивы к обаянию персонажа, от нее зависят навыки «Бартер»");
                            System.out.println(" и «Красноречие».");
                            break;
                        case "i": case "intelligence": case "интеллект":
                            System.out.println(
                                      " Интеллект(I) - ум и сообразительность. От этой характеристики зависит\n"
                                    + " количество очков навыков при переходе на следующий уровень. Не влияет\n"
                                    + " на производные характеристики, но служит основой для множества небоевых навыков.\n"
                                    + " Имеет определяющее значение в разговорах: от величины интеллекта зависит\n"
                                    + " количество доступных реплик. Персонажи с высоким интеллектом говорят разумнее\n"
                                    + " и задают больше вопросов, а также получают больше способов прохождения\n"
                                    + " некоторых квестов."
                            );
                            break;
                        case "a": case "agility": case "ловкость":
                            System.out.println(
                                      " Ловкость(A) - быстрота передвижения. От неё зависит скорость\n"
                                    + " перемещения в бою и множество других физических навыков. Влияет на\n"
                                    + " производные характеристики «Класс брони» и число очков действия, а также\n"
                                    + " на большинство навыков, особенно боевых. Чем выше ловкость персонажа, тем\n"
                                    + " лучше у него координация движений, а значит, выше шанс обойти ловушку,\n"
                                    + " вскрыть замок и т.д."
                            );
                            break;
                        case "l": case "luck": case "удача":
                            System.out.println(
                                    "Удача(L) - самая необычная из основных характеристик, определяет\n"
                                    + " исход самых разных событий. Влияет на производную характеристику\n"
                                    + " «Шанс на критическое попадание» и навык «Азартные игры». Чем выше значение\n"
                                    + " удачи, тем чаще персонаж наносит критические удары и тем больше урона они\n"
                                    + " причиняют. Удача также влияет на исход многих событий и вероятность\n"
                                    + " специальных встреч."
                            );
                            break;
                        default:
                            System.out.println("Неизвестная опция - " + lineWords[1]);
                    }
                }
                System.out.print("Введите команду(или help): ");
                continue;
            }

            String[] kv = line.replaceAll("\\s+", "").split("=");
            if (kv.length != 2) {
                System.out.println("Неверный формат ввода!");
            } else {
                String k = kv[0], v = kv[1];
                if (tmpSpecial.get(k) == null) {
                    System.out.println("Неизвестная характеристика - " + k);
                    continue;
                }

                int oldKey = tmpSpecial.get(k), newKey;
                try {
                    newKey = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    System.out.println("Значение характеристики - не цифра! А должно быть цифрой!");
                    continue;
                }

                if (newKey > 10 || newKey < 1) {
                    System.out.println("Значение характеристики должно быть в пределах от 1 до 10!");
                    continue;
                }

                if (pointsLeft+oldKey-newKey < 0) {
                    System.out.println("Попытка установить очков больше чем возможно!");
                    continue;
                }

                pointsLeft += oldKey - newKey;
                tmpSpecial.put(k, newKey);
            }

            System.out.println("Осталось очков SPECIAL для распределения - " + pointsLeft);
            for (Map.Entry<String, Integer> entry : tmpSpecial.entrySet())
                System.out.println("  " + entry.getKey().toUpperCase() + " = " + entry.getValue() + " ");
            if (pointsLeft == 0) {
                System.out.println("Введите 'ok' для подтверждения очков если вы уверены.");
            }
            System.out.print("Введите команду(или help): ");
        } while (!(line = sc.nextLine()).equals("ok") || pointsLeft > 0);

        System.out.println("SPECIAL установлены:");
        for (Map.Entry<String, Integer> entry : tmpSpecial.entrySet()) {
            System.out.println("\t" + entry.getKey() + " = " + entry.getValue());
        }

        characterSpecial = tmpSpecial;
    }

    /**
     * Setup of character skills: TODO: SKILLS_DESCRIPTION<br>
     * All basic stats are stored in <u>characterSkills</u>
     */
    private void setupSkills() {
        characterSkills = new ArrayList<>();
        // TODO: do this
    }


    /**
     * This class gathers static methods from Game.java
     * TODO: refactor(somehow later)
     */
    static class Utils {
        public static String[] getSaves() throws DirectoryCreatingException, InvalidPathException {
            File saveDir = new File(getSavesDir());

            if (saveDir.exists() && !saveDir.isDirectory())
                throw new InvalidPathException("Папка для сохранений является файлом!");
            if (!saveDir.exists() && !saveDir.mkdir())
                throw new DirectoryCreatingException("Невозможно создать папку сохранений(возможно, дело в правах доступа)");

            return saveDir.list();
        }

        private static String getGameDir() {
            try {
                return Game.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI().getPath();
            } catch (URISyntaxException e) {
                return null;
            }
        }

        private static String getSavesDir() {
            return getGameDir() + '/' + SAVES_FOLDER;
        }

        private static String getSavePath(String savename) {
            return getSavePath(savename, ".json");
        }

        private static String getSavePath(String savename, String extension) {
            return getSavesDir() + '/' + savename + extension;
        }

        public static boolean isAlpha(String string) {
            return string.matches("[a-zA-Z]+");
        }

        public static String getCurrentTimeStamp() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }

        public static String capitalize(String string) {
            return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
        }

        public static String readFile(String filename) {
            String result = "";

            try {
                BufferedReader br = new BufferedReader(new FileReader(filename));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }

                result = sb.toString();
            } catch(Exception e) {
                e.printStackTrace();
            }

            return result;
        }

    }
}


