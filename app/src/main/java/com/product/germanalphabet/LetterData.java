package com.product.germanalphabet;

/**
 * LetterData
 * ----------
 * Holds all information for a single German alphabet letter.
 *
 * Fields:
 *   letter        - the letter character shown on the tile  e.g. "A"
 *   pronunciation - how to say the letter name             e.g. "Ah"
 *   germanWord    - a German word starting with this letter e.g. "Apfel"
 *   englishMeaning- English translation of the word        e.g. "apple"
 *   emoji         - Unicode emoji for visual association   e.g. "🍎"
 *   tileColor     - fill color for the tile when tapped    (set from COLORS array)
 */
public class LetterData {

    public final String letter;
    public final String pronunciation;
    public final String germanWord;
    public final String englishMeaning;
    public final String emoji;
    public int tileColor;   // assigned at runtime from color palette

    public LetterData(String letter, String pronunciation,
                      String germanWord, String englishMeaning, String emoji) {
        this.letter         = letter;
        this.pronunciation  = pronunciation;
        this.germanWord     = germanWord;
        this.englishMeaning = englishMeaning;
        this.emoji          = emoji;
    }

    // ── Full 30-letter German alphabet ────────────────────────────────────────

    public static LetterData[] ALL = {
            new LetterData("A",  "Ah",       "Apfel",         "apple",      "🍎"),
            new LetterData("B",  "Bay",       "Ball",          "ball",       "⚽"),
            new LetterData("C",  "Tsay",      "Computer",      "computer",   "💻"),
            new LetterData("D",  "Day",       "Dinosaurier",   "dinosaur",   "🦕"),
            new LetterData("E",  "Ay",        "Elefant",       "elephant",   "🐘"),
            new LetterData("F",  "Eff",       "Frosch",        "frog",       "🐸"),
            new LetterData("G",  "Gay",       "Giraffe",       "giraffe",    "🦒"),
            new LetterData("H",  "Hah",       "Hund",          "dog",        "🐶"),
            new LetterData("I",  "Ee",        "Igel",          "hedgehog",   "🦔"),
            new LetterData("J",  "Yot",       "Jäger",         "hunter",     "🏹"),
            new LetterData("K",  "Kah",       "Katze",         "cat",        "🐱"),
            new LetterData("L",  "Ell",       "Löwe",          "lion",       "🦁"),
            new LetterData("M",  "Emm",       "Mond",          "moon",       "🌙"),
            new LetterData("N",  "Enn",       "Nase",          "nose",       "👃"),
            new LetterData("O",  "Oh",        "Orange",        "orange",     "🍊"),
            new LetterData("P",  "Pay",       "Pinguin",       "penguin",    "🐧"),
            new LetterData("Q",  "Koo",       "Qualle",        "jellyfish",  "🪼"),
            new LetterData("R",  "Err",       "Regenbogen",    "rainbow",    "🌈"),
            new LetterData("S",  "Ess",       "Sonne",         "sun",        "☀️"),
            new LetterData("T",  "Tay",       "Tiger",         "tiger",      "🐯"),
            new LetterData("U",  "Oo",        "Uhu",           "owl",        "🦉"),
            new LetterData("V",  "Fow",       "Vogel",         "bird",       "🐦"),
            new LetterData("W",  "Vay",       "Wolke",         "cloud",      "☁️"),
            new LetterData("X",  "Iks",       "Xylophon",      "xylophone",  "🎵"),
            new LetterData("Y",  "Üpsilon",   "Yacht",         "yacht",      "⛵"),
            new LetterData("Z",  "Tset",      "Zebra",         "zebra",      "🦓"),
            new LetterData("Ä",  "Eh",        "Äpfel",         "apples",     "🍏"),
            new LetterData("Ö",  "Oe",        "Öl",            "oil",        "🫙"),
            new LetterData("Ü",  "Ue",        "Überraschung",  "surprise",   "🎁"),
            new LetterData("ß",  "Ess-tset",  "Straße",        "street",     "🛣️"),
    };
}
