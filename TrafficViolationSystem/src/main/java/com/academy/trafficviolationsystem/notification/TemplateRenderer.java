package com.academy.trafficviolationsystem.notification;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders notification templates by replacing {{variableName}} placeholders
 * with values from the provided variables map.
 *
 * Example:
 *   template: "Dear {{driverName}}, your fine {{fineNumber}} is due on {{dueDate}}."
 *   variables: {driverName="John Doe", fineNumber="F-001", dueDate="31.12.2025"}
 *   result:   "Dear John Doe, your fine F-001 is due on 31.12.2025."
 *
 * Unknown placeholders (no matching key in variables) are left as-is.
 * Null template input returns an empty string.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Replaces all {{key}} placeholders in the template with values from the map.
     *
     * @param template  the raw template string (may be null)
     * @param variables map of placeholder keys to replacement values
     * @return rendered string, or "" if template is null
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) return "";
        if (variables == null || variables.isEmpty()) return template;

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key         = matcher.group(1);
            String replacement = variables.getOrDefault(key, matcher.group(0)); // leave unknown as-is
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}