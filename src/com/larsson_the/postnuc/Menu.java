package com.larsson_the.postnuc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides easy menu creation in few steps. Much easier to use with builder.
 * TODO: add customizing of back button
 * TODO: add localization support(somehow later)
 */
class Menu {
    private String message;
    private Map<String, String> choices;
    private Map<String, MenuChoiceTask> tasks;
    private boolean backChoice;
    private LastChoice lastChoice;

    /**
     * @param message Message to show in menu (for ex. 'Choose your hero'). ':' sign will be added automaticly
     * @param choices Key-Value pairs where [Key = which text triggers choice],  [Value = text of choice]
     * @param tasks Key-Value pairs where [Key = which text triggers choice(same as in choices)],
     *              [Value = action(lambda with 1 parameter) performed when u have triggered Key]
     * @param backChoice if true - menu generates additional choice 'b' to switch back.
     */
    public Menu(String message, Map<String, String> choices,
                Map <String, MenuChoiceTask> tasks, boolean backChoice) {
        this.message = message;
        this.choices = choices;
        this.tasks = tasks;
        this.backChoice = backChoice;
        this.lastChoice = LastChoice.NONE;

        prepare();
    }

    /**
     * Preparations before showing the menu.
     */
    private void prepare() {
        for (Map.Entry<String, String> e : choices.entrySet())
            choices.put(e.getKey().toLowerCase(), e.getValue());

        if (backChoice) {
            choices.put("b", "Назад");

            tasks.computeIfAbsent("b", k -> (c) -> {});
        }
    }

    /**
     * Shows the menu to user
     */
    public void show() {
        String choice;

        while (true) {
            System.out.println(this.message + ":");
            for (Map.Entry<String, String> e : this.choices.entrySet())
                System.out.printf("\t%s - %s\n", e.getValue(), e.getKey());

            choice = Game.sc.nextLine().toLowerCase();
            if (choices.get(choice) != null) {
                lastChoice = choice.equals("b") ? LastChoice.BACK : LastChoice.MENU_BUTTON;
                break;
            }

            System.out.println("Неверный формат ввода. Введите правильную команду из меню.");

        }

        tasks.get(choice).execute(choice);
    }

    /**
     * @return true if back has been pressed
     */
    public boolean isBackPressed() {
        return lastChoice == LastChoice.BACK;
    }

    public enum LastChoice { NONE, MENU_BUTTON, BACK }

    /**
     * This class provides easy 'on the fly' creating menus
     */
    public static class Builder {
        private String message;
        private Map<String, String> choices;
        private Map<String, MenuChoiceTask> tasks;
        private boolean backChoice = false;
        private int menuIdx;


        /** Starting point of menu creating.
         *
         * <h4>Usage example 1:</h4>
         * <pre>
         *     new Menu.Builder("Main menu")
         *          .choice("New game", (c) -> newGame()) // New game - 1
         *          .choice("Exit", (c) -> { // Exit - 2
         *              System.out.println("Bye.");
         *              System.exit(0);
         *          })
         *          .show();
         * </pre>
         *
         * <h4>Usage example 2:</h4>
         * <pre>
         *     new Menu.Builder("Choose your hero")
         *          .choices(heroes) // overloaded for any sequence of Strings
         *          .forAny((c) -> { // 'c' will contain number(String)(starting from 1) of selected choice in menu
         *              System.out.println("You have selected " + heroes[Integer.parseInt(c)-1]);
         *          }
         *          .show();
         * </pre>
         *
         * <h4>Usage example 3:</h4>
         * <pre>
         *     Menu menu = new Menu.Builder("Enter command")
         *          .choice("help", "Help", (c) -> System.out.println("Still no helpful info there :(")
         *          .choice("about", "About", (c) -> {
         *              System.out.println("About me: my name is Robin.");
         *              System.out.println("I think it's enough for start.");
         *              System.out.println("Try to ask for 'help' next time.");
         *          })
         *          .backChoice(true)
         *          .build();
         *     menu.show();
         *
         *     if (menu.isBackPressed()) {
         *         System.out.println("Ok, it's time to go back. See ya!");
         *         return 0;
         *     }
         * </pre>
         * @param message Header of menu. Parameter example: 'Choose your hero' or 'Main menu'
         */
        public Builder(String message) {
            this.message = message;

            choices = new LinkedHashMap<>();
            tasks = new LinkedHashMap<>();
            menuIdx = 0;
        }

        public Builder choice(String choiceCommand, String choiceCaption) {
            return choice(choiceCommand, choiceCaption, null);
        }

        public Builder choice(String choiceCommand, String choiceCaption, MenuChoiceTask task) {
            this.choices.put(choiceCommand, choiceCaption);
            this.tasks.put(choiceCommand, task);

            ++menuIdx;
            return this;
        }

        public Builder choice(String choiceCaption) {
            return choice(choiceCaption, (MenuChoiceTask)null);
        }

        public Builder choice(String choiceCaption, MenuChoiceTask task) {
            String choiceCommand = String.valueOf((++menuIdx));

            this.choices.put(choiceCommand, choiceCaption);
            this.tasks.put(choiceCommand, task);

            return this;
        }

        public Builder forAny(MenuChoiceTask task) {
            this.tasks = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : this.choices.entrySet()) {
                tasks.put(e.getKey(), task);
            }

            return this;
        }

        public Builder choices(List<String> choices) {
            this.choices = new LinkedHashMap<>();
            this.tasks = new LinkedHashMap<>();
            menuIdx = 0;

            for (String choice : choices) {
                String choiceCommand = String.valueOf(++menuIdx);

                this.choices.put(choiceCommand, choice);
                this.tasks.put(choiceCommand, null);
            }

            return this;
        }

        public Builder choices(String[] choices) {
            this.choices = new LinkedHashMap<>();
            this.tasks = new LinkedHashMap<>();
            menuIdx = 0;

            for (String choice : choices) {
                String choiceCommand = String.valueOf(++menuIdx);

                this.choices.put(choiceCommand, choice);
                this.tasks.put(choiceCommand, null);
            }

            return this;
        }

        public Builder choices(Map<String, String> choices) {
            this.choices = new LinkedHashMap<>(choices);
            for (String key : choices.keySet())
                this.tasks.put(key, null);

            return this;
        }

        public Builder choices(LinkedHashMap<String, String> choices) {
            this.choices = choices;
            for (String key : choices.keySet())
                this.tasks.put(key, null);

            return this;
        }

        public Builder backTask(MenuChoiceTask task) {
            this.tasks.put("b", task);
            return this;
        }

        public Builder backChoice(boolean show) {
            this.backChoice = show;
            return this;
        }

        public Menu build() {
            return new Menu(message, choices, tasks, backChoice);
        }

        public void show() {
            build().show();
        }
    }

    @FunctionalInterface
    public interface MenuChoiceTask {
        public void execute(String chosenCommand);
    }
}
