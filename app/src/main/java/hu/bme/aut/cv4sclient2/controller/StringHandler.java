package hu.bme.aut.cv4sclient2.controller;

/**
 * Created by Martin on 2018. 05. 03..
 */

public class StringHandler {
    public static String formatToDisplay(String in)
    {
        return in.replace("\\", "").replace("\"{", "{").replace("}\"", "}")
            .replace(",", ",\n").replace("[", "\n[\n").replace("]", "\n]\n").replace("{", "{\n").replace("}", "\n}");
    }

    public static String formatToParse(String in)
    {
        in=in.replace("\\\"", "'");
        return in.substring(1, in.length() - 1);
    }
}
