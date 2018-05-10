package hu.bme.aut.cv4sclient2.controller;

public class StringHandler {
    public static String formatToDisplay(String in)
    {
        return in.replace("\\", "").replace("\"{", "{").replace("}\"", "}")
                .replace(",", ",\n").replace("[", "\n[\n").replace("]", "\n]\n").replace("{", "{\n").replace("}", "\n}");
    }


    public static String formatToParse(String in) {
        in=in.replace("\\\"", "'").replace("\\\\", "\\");
        return in.substring(1, in.length() - 1);
    }
}
