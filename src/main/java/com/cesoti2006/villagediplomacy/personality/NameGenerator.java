package com.cesoti2006.villagediplomacy.personality;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Name generator based on biome
 */
public class NameGenerator {
    
    // NAMES BY BIOME
    private static final Map<String, String[]> MALE_NAMES = new HashMap<>();
    private static final Map<String, String[]> FEMALE_NAMES = new HashMap<>();
    
    static {
        // PLAINS (English names) - 40 male, 40 female
        MALE_NAMES.put("plains", new String[]{
            "John", "William", "James", "Robert", "Michael", "David",
            "Richard", "Joseph", "Thomas", "Charles", "Daniel", "Matthew",
            "Christopher", "Andrew", "George", "Edward", "Henry", "Arthur",
            "Benjamin", "Samuel", "Alexander", "Peter", "Frederick", "Oliver",
            "Jack", "Luke", "Mark", "Paul", "Stephen", "Timothy",
            "Theodore", "Vincent", "Walter", "Albert", "Oscar", "Simon",
            "Philip", "Gregory", "Francis", "Leonard"
        });
        FEMALE_NAMES.put("plains", new String[]{
            "Mary", "Elizabeth", "Sarah", "Anna", "Margaret", "Emma",
            "Emily", "Hannah", "Grace", "Helen", "Sophia", "Clara",
            "Charlotte", "Alice", "Lucy", "Rose", "Eleanor", "Catherine",
            "Victoria", "Julia", "Rebecca", "Lillian", "Abigail", "Caroline",
            "Dorothy", "Evelyn", "Florence", "Harriet", "Isabel", "Jane",
            "Katherine", "Louise", "Martha", "Naomi", "Olivia", "Patricia",
            "Rachel", "Ruth", "Susan", "Violet"
        });
        
        // DESERT (Middle Eastern names) - 40 male, 40 female
        MALE_NAMES.put("desert", new String[]{
            "Rashid", "Omar", "Khalil", "Hassan", "Tariq", "Malik",
            "Samir", "Rahim", "Faisal", "Aziz", "Karim", "Jamal",
            "Ahmed", "Ali", "Bilal", "Fahd", "Hamza", "Ibrahim",
            "Jalal", "Kareem", "Mahmoud", "Nasir", "Qasim", "Rami",
            "Salim", "Taher", "Umar", "Walid", "Yasir", "Zain",
            "Abbas", "Dawud", "Faris", "Hakim", "Imran", "Kamil",
            "Latif", "Mustafa", "Naveed", "Rafiq"
        });
        FEMALE_NAMES.put("desert", new String[]{
            "Zahra", "Amira", "Layla", "Fatima", "Nadia", "Samira",
            "Yasmin", "Zara", "Aisha", "Leila", "Soraya", "Dalila",
            "Aaliyah", "Basma", "Dalia", "Farah", "Hana", "Iman",
            "Jamila", "Karima", "Lina", "Maryam", "Noura", "Rania",
            "Sabrina", "Salma", "Tahira", "Wafa", "Yara", "Zeinab",
            "Amani", "Bushra", "Durra", "Fadila", "Halima", "Inaya",
            "Khadija", "Malika", "Najma", "Safiya"
        });
        
        // TAIGA/SNOW (Nordic names) - 40 male, 40 female
        MALE_NAMES.put("taiga", new String[]{
            "Bjorn", "Erik", "Olaf", "Sven", "Thor", "Ragnar",
            "Leif", "Harald", "Magnus", "Ivar", "Ulf", "Gunnar",
            "Arne", "Bor", "Dag", "Egil", "Finn", "Gorm",
            "Haakon", "Ingvar", "Jarl", "Knut", "Lars", "Njal",
            "Orm", "Pal", "Rolf", "Sigurd", "Torsten", "Viggo",
            "Asger", "Balder", "Canute", "Einar", "Frode", "Gunnolf",
            "Harek", "Jorund", "Ketil", "Sigmund"
        });
        FEMALE_NAMES.put("taiga", new String[]{
            "Astrid", "Freya", "Ingrid", "Sigrid", "Helga", "Brynhild",
            "Liv", "Solveig", "Greta", "Eira", "Ylva", "Runa",
            "Anja", "Bodil", "Dagny", "Elin", "Frida", "Gudrun",
            "Hilda", "Inga", "Johanna", "Karin", "Linnea", "Maren",
            "Nora", "Ragnhild", "Signe", "Thora", "Ulla", "Vigdis",
            "Alva", "Bergit", "Dagmar", "Embla", "Freja", "Gisela",
            "Hedda", "Idun", "Kaia", "Saga"
        });
        
        // SAVANNA (African names) - 40 male, 40 female
        MALE_NAMES.put("savanna", new String[]{
            "Kofi", "Jabari", "Kwame", "Tau", "Amari", "Zuberi",
            "Sekou", "Juma", "Bakari", "Kito", "Ade", "Nuru",
            "Akil", "Bandele", "Chike", "Daren", "Efosa", "Femi",
            "Gamba", "Hasani", "Imamu", "Jengo", "Kamau", "Lumo",
            "Makena", "Nkrumah", "Obi", "Paki", "Rudo", "Simba",
            "Themba", "Udo", "Vusi", "Wekesa", "Yasir", "Zaki",
            "Amadi", "Bomani", "Chinua", "Dalmar"
        });
        FEMALE_NAMES.put("savanna", new String[]{
            "Nia", "Zuri", "Amara", "Kaya", "Asha", "Zahara",
            "Zola", "Imani", "Sanaa", "Adanna", "Kamaria", "Thandiwe",
            "Ayana", "Bahati", "Chiamaka", "Desta", "Eshe", "Folami",
            "Hadiya", "Ife", "Jamila", "Kesi", "Lulu", "Makena",
            "Nala", "Ode", "Panya", "Ramla", "Safiya", "Tamika",
            "Urbi", "Wanja", "Yaa", "Zaina", "Abeni", "Binta",
            "Chipo", "Dalila", "Femi", "Habiba"
        });
        
        // JUNGLE (Tropical names) - 40 male, 40 female
        MALE_NAMES.put("jungle", new String[]{
            "Mateo", "Diego", "Santiago", "Alejandro", "Javier", "Marco",
            "Paulo", "Leonardo", "Ricardo", "Rodrigo", "Emilio", "Dante",
            "Andres", "Carlos", "Eduardo", "Felipe", "Gabriel", "Hugo",
            "Ignacio", "Julio", "Luis", "Manuel", "Nicolas", "Oscar",
            "Pedro", "Rafael", "Sergio", "Tomas", "Vicente", "Xavier",
            "Alberto", "Bruno", "Cesar", "Dario", "Esteban", "Fernando",
            "Gustavo", "Hector", "Ivan", "Jorge"
        });
        FEMALE_NAMES.put("jungle", new String[]{
            "Isabella", "Valentina", "Camila", "Gabriela", "Fernanda", "Luna",
            "Maya", "Selena", "Esmeralda", "Marina", "Paloma", "Rosalía",
            "Adriana", "Beatriz", "Carmen", "Daniela", "Elena", "Francisca",
            "Gloria", "Ines", "Julieta", "Karla", "Liliana", "Marcela",
            "Natalia", "Olivia", "Paula", "Raquel", "Sofia", "Teresa",
            "Ursula", "Veronica", "Ximena", "Yolanda", "Zoe", "Alicia",
            "Bianca", "Catalina", "Diana", "Emilia"
        });
        
        // SWAMP (Slavic names) - 40 male, 40 female
        MALE_NAMES.put("swamp", new String[]{
            "Igor", "Dimitri", "Nikolai", "Boris", "Yuri", "Viktor",
            "Alexei", "Sergei", "Pavel", "Vladimir", "Ivan", "Maxim",
            "Andrei", "Anton", "Artem", "Denis", "Evgeny", "Fyodor",
            "Grigory", "Ilya", "Konstantin", "Leonid", "Mikhail", "Oleg",
            "Roman", "Stanislav", "Timur", "Vadim", "Yaroslav", "Zakhar",
            "Anatoly", "Bogdan", "Daniil", "Eduard", "Fedor", "Gleb",
            "Kirill", "Matvey", "Nikita", "Ruslan"
        });
        FEMALE_NAMES.put("swamp", new String[]{
            "Natasha", "Svetlana", "Irina", "Olga", "Anya", "Katya",
            "Tatiana", "Sasha", "Elena", "Mila", "Vera", "Zoya",
            "Anastasia", "Daria", "Ekaterina", "Galina", "Inna", "Julia",
            "Kira", "Larisa", "Marina", "Nadezhda", "Polina", "Raisa",
            "Sofia", "Tamara", "Ulyana", "Valentina", "Yelena", "Zlata",
            "Alina", "Bella", "Diana", "Eva", "Faina", "Lyudmila",
            "Nina", "Oxana", "Rimma", "Varvara"
        });
    }
    
    /**
     * Genera un nombre apropiado para el bioma
     */
    public static String generateName(String biomeType, boolean isMale, Random random) {
        // Normalizar tipo de bioma
        String normalizedBiome = normalizeBiome(biomeType);
        
        // Seleccionar lista de nombres
        Map<String, String[]> nameList = isMale ? MALE_NAMES : FEMALE_NAMES;
        String[] names = nameList.getOrDefault(normalizedBiome, nameList.get("plains"));
        
        // Retornar nombre aleatorio
        return names[random.nextInt(names.length)];
    }
    
    /**
     * Converts Minecraft biome ID to our type
     */
    private static String normalizeBiome(String biomeId) {
        String lower = biomeId.toLowerCase();
        
        if (lower.contains("desert")) return "desert";
        if (lower.contains("taiga") || lower.contains("snow") || lower.contains("ice")) return "taiga";
        if (lower.contains("savanna")) return "savanna";
        if (lower.contains("jungle")) return "jungle";
        if (lower.contains("swamp") || lower.contains("bog")) return "swamp";
        
        // Default: plains
        return "plains";
    }
    
    /**
     * Generates a nickname based on profession and level
     */
    public static String generateNickname(String profession, int level, Random random) {
        if (level < 4) return ""; // Only experts get nicknames
        
        switch (profession.toLowerCase()) {
            case "librarian":
                return random.nextBoolean() ? "the Wise" : "the Scholar";
            case "weaponsmith":
                return random.nextBoolean() ? "the Smith" : "the Warsmith";
            case "armorer":
                return random.nextBoolean() ? "the Anvil" : "the Armorer";
            case "toolsmith":
                return random.nextBoolean() ? "the Craftsman" : "the Artisan";
            case "farmer":
                return random.nextBoolean() ? "the Farmer" : "the Sower";
            case "fisherman":
                return random.nextBoolean() ? "the Fisher" : "the Sailor";
            case "shepherd":
                return random.nextBoolean() ? "the Shepherd" : "the Herder";
            case "fletcher":
                return random.nextBoolean() ? "the Archer" : "the Bowyer";
            case "butcher":
                return random.nextBoolean() ? "the Butcher" : "the Cook";
            case "leatherworker":
                return random.nextBoolean() ? "the Tanner" : "the Leatherworker";
            case "mason":
                return random.nextBoolean() ? "the Mason" : "the Builder";
            case "cartographer":
                return random.nextBoolean() ? "the Cartographer" : "the Explorer";
            case "cleric":
                return random.nextBoolean() ? "the Healer" : "the Priest";
            default:
                return "the Expert";
        }
    }
}
