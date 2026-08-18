package f95gm.server.persistence.filters.alias

internal object FilterAliasRules {
    val transliterations = mapOf(
        'æ' to "ae", 'œ' to "oe", 'ß' to "ss", 'ð' to "d", 'þ' to "th",
        'đ' to "d", 'ħ' to "h", 'ı' to "i", 'ĸ' to "k", 'ł' to "l",
        'ŋ' to "n", 'ø' to "o", 'ŧ' to "t", 'ƒ' to "f", 'ə' to "e",
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "shch", 'ъ' to "", 'ы' to "y", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya", 'є' to "ye", 'і' to "i",
        'ї' to "yi", 'ґ' to "g", 'ў' to "u", 'љ' to "lj", 'њ' to "nj",
        'џ' to "dz", 'ђ' to "dj", 'ћ' to "c", 'ј' to "j", 'ќ' to "kj",
        'ѓ' to "gj", 'ѕ' to "dz",
        'α' to "a", 'β' to "b", 'γ' to "g", 'δ' to "d", 'ε' to "e",
        'ζ' to "z", 'η' to "i", 'θ' to "th", 'ι' to "i", 'κ' to "k",
        'λ' to "l", 'μ' to "m", 'ν' to "n", 'ξ' to "x", 'ο' to "o",
        'π' to "p", 'ρ' to "r", 'σ' to "s", 'ς' to "s", 'τ' to "t",
        'υ' to "y", 'φ' to "f", 'χ' to "ch", 'ψ' to "ps", 'ω' to "o"
    )
}
