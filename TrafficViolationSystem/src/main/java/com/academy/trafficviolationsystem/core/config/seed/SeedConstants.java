package com.academy.trafficviolationsystem.core.config.seed;

/**
 * Shared sample-data pools used across all seeders.
 * Keeping these in one place avoids duplicated arrays and makes it easy
 * to add more variety later without touching every seeder file.
 */
public final class SeedConstants {

    private SeedConstants() {}

    public static final String[] MALE_FIRST_NAMES = {
            "Amar", "Adnan", "Emir", "Haris", "Kenan", "Nedim", "Tarik", "Damir",
            "Denis", "Edin", "Faruk", "Ibrahim", "Jasmin", "Mirza", "Nihad",
            "Samir", "Vedran", "Zlatan", "Armin", "Elvir"
    };

    public static final String[] FEMALE_FIRST_NAMES = {
            "Amra", "Aida", "Emina", "Lejla", "Nejra", "Selma", "Amila", "Dina",
            "Ena", "Ilma", "Jasmina", "Merima", "Nina", "Sara", "Vildana",
            "Zana", "Adna", "Berina", "Elma", "Maja"
    };

    public static final String[] LAST_NAMES = {
            "Hodžić", "Mujić", "Kovačević", "Softić", "Delić", "Halilović",
            "Begić", "Karić", "Zukić", "Hasanović", "Suljić", "Alić",
            "Bajramović", "Duraković", "Fazlić", "Gogić", "Hadžić", "Imamović",
            "Jusić", "Kadrić", "Mehić", "Osmić", "Pašić", "Ramić", "Salihović"
    };

    public static final String[] CITIES = {
            "Sarajevo", "Mostar", "Banja Luka", "Tuzla", "Zenica", "Bihać",
            "Prijedor", "Brčko", "Trebinje", "Bijeljina", "Travnik", "Cazin"
    };

    public static final String[] STREETS = {
            "Zmaja od Bosne", "Maršala Tita", "Alipašina", "Kranjčevićeva",
            "Ferhadija", "Đure Đakovića", "Kolodvorska", "Ive Andrića",
            "Envera Šehovića", "Bulevar Meše Selimovića", "Titova",
            "Kralja Tvrtka", "Bosanska", "Save Kovačevića"
    };

    /** City registration plate prefixes, loosely modeled on BiH plates. */
    public static final String[] PLATE_CITY_CODES = {
            "A01", "T21", "M04", "K51", "J23", "E30", "Z11", "B45", "D09", "S77"
    };

    public static final String[] VEHICLE_MAKES = {
            "Volkswagen", "Škoda", "Audi", "BMW", "Mercedes-Benz", "Opel",
            "Renault", "Peugeot", "Fiat", "Toyota", "Ford", "Hyundai", "Kia", "Dacia"
    };

    public static final String[] VEHICLE_MODELS = {
            "Golf", "Passat", "Octavia", "Fabia", "A4", "3 Series", "C-Class",
            "Astra", "Clio", "308", "Punto", "Corolla", "Focus", "i30", "Sportage", "Duster"
    };

    public static final String[] VEHICLE_COLORS = {
            "Bijela", "Crna", "Siva", "Srebrena", "Plava", "Crvena", "Zelena", "Smeđa"
    };

    public static final String[] LICENSE_CATEGORIES = {
            "B", "B", "B", "B", "A", "BE", "C", "D"
    };

    /** Officer badge prefix. */
    public static final String BADGE_PREFIX = "OFF-";

    public static final String DEFAULT_CURRENCY = "BAM";
}