package com.larsson_the.postnuc;

import java.util.LinkedHashMap;
import java.util.Map;

public class Character {
    private Character.Basics basics;
    private Character.SPECIAL special;
    private Character.Skills skills;
    private Character.Traits traits;
    private Character.Perks perks;
    private Character.Inventory inventory;

    Character(Map<String, String> basics, Map<String, Integer> special) throws SPECIAL.IllegalSpecialStatException {
        this.basics = new Character.Basics(basics);
        this.special = new Character.SPECIAL(special);

        skills = new Character.Skills();
        traits = new Character.Traits();
        perks = new Character.Perks();
        inventory = new Character.Inventory();
    }

    public String getBasic(String key) {
        return basics.get(key);
    }
    public void setBasic(String k, String v) {
        basics.put(k, v);
    }
    public Map<String, String> getBasics() {
        return basics.get();
    }

    public int getSpecial(String k) {
        return special.get(k);
    }
    public void setSpecial(String k, int v) throws SPECIAL.IllegalSpecialStatException {
        special.put(k, v);
    }
    public Map<String, Integer> getSpecial() {
        return special.get();
    }

    public class Basics {
        private Map<String, String> map;

        Basics(Map<String, String> map) {
            this.map = map;
        }

        public String get(String key) {
            return map.get(key);
        }

        public Map<String, String> get() {
            return map;
        }

        public String put(String key, String value) {
            return map.put(key, value);
        }
    }

    public class SPECIAL {
        private Map<String, Integer> map;

        SPECIAL(Map<String, Integer> special) throws IllegalSpecialStatException {
            if (!special.containsKey("s") || !special.containsKey("p") || !special.containsKey("e") ||
                    !special.containsKey("c") || !special.containsKey("i") || !special.containsKey("a") ||
                    !special.containsKey("l"))
            {
                throw new NullPointerException("Missing stat(-s) for SPECIAL!");
            }
            map = new LinkedHashMap<>();

            put("s", special.get("s"));
            put("p", special.get("p"));
            put("e", special.get("e"));
            put("c", special.get("c"));
            put("i", special.get("i"));
            put("a", special.get("a"));
            put("l", special.get("l"));
        }

        public Map<String, Integer> get() {
            return map;
        }

        public int get(String k) {
            return map.get(k);
        }

        public void put(String k, int v) throws IllegalSpecialStatException {
            if (v < 1 || v > 10)
                throw new IllegalSpecialStatException("Характеристика должна быть в пределах от 1 до 10!");

            map.put(k, v);
        }

        public void inc(String k) throws CannotIncrementSpecialStatException {
            int v = map.get(k);

            if (v == 10)
                throw new CannotIncrementSpecialStatException("Характеристика уже 10, повысить невозможно!");

            map.put(k, v+1);
        }

        public void dec(String k) throws CannotDecrementSpecialStatException {
            int v = map.get(k);

            if (v == 1)
                throw new CannotDecrementSpecialStatException("Характеристика уже 1, понизить невозможно!");

            map.put(k, v-1);
        }

        public class CannotIncrementSpecialStatException extends Throwable {
            public CannotIncrementSpecialStatException(String s) {
                super(s);
            }
        }

        public class CannotDecrementSpecialStatException extends Throwable {
            public CannotDecrementSpecialStatException(String s) {
                super(s);
            }
        }

        public class IllegalSpecialStatException extends Throwable {
            public IllegalSpecialStatException(String s) {
                super(s);
            }
        }
    }

    class Skills {
    }
    class Traits {
    }
    class Perks {
    }

    class Inventory {
    }

}

