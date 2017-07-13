package org.jenkinsci.plugins.createandrunjobtrigger;

public class Credentials {
    private static final String extensionSeperator = "_";
    private static final String replacement = "-";


    public static String generateId(String service, String... extensions) {
        StringBuffer buf = new StringBuffer();
        buf.append(service.toUpperCase());

        for (String extension : extensions) {
            buf.append(extensionSeperator + cleanExtension(extension));
        }
        return buf.toString();
    }

    private static String cleanExtension(String extension) {
        return extension
                .toLowerCase()
                .replaceAll("://", replacement)
                .replaceAll(":", replacement)
                .replaceAll(extensionSeperator, replacement)
                .split("/")[0];
    }

}
