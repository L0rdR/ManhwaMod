package com.TaylorBros.ManhwaMod;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.List;

public class ManhwaCommandSuggestions {

    public static final SuggestionProvider<CommandSourceStack> SKILL_RECIPE =
            (CommandContext<CommandSourceStack> context,
             com.mojang.brigadier.suggestion.SuggestionsBuilder builder) -> {
                String remRaw = builder.getRemaining();
                String rem = remRaw.toUpperCase();

                // If user is typing the display name portion, we can optionally suggest example names
                if (rem.contains("|")) {
                    // Everything after the first pipe is the display name portion
                    int pipe = rem.indexOf('|');
                    String left = rem.substring(0, pipe); // SHAPE:ELEMENT:MODIFIER
                    String rightRaw = remRaw.substring(pipe + 1); // keep original case for name part

                    // If they haven't typed a display name yet, suggest a few examples
                    if (rightRaw.isBlank()) {
                        List<String> names = List.of(
                                left + "|Dismantle",
                                left + "|Cleave",
                                left + "|Barrage",
                                left + "|Dragon Fang",
                                left + "|Flash Step"
                        );
                        return SharedSuggestionProvider.suggest(names, builder);
                    }

                    // Otherwise don’t fight the user while they type their name
                    return builder.buildFuture();
                }

                // Only parse the "tag" half (no display name yet)
                String[] parts = rem.split(":", -1); // keep empties

                // 0 colons typed yet (or empty)
                // Suggest SHAPE:
                if (parts.length == 1) {
                    List<String> out = new ArrayList<>();
                    for (SkillTags.Shape s : SkillTags.Shape.values()) {
                        out.add(s.name() + ":");
                    }
                    return SharedSuggestionProvider.suggest(out, builder);
                }

                // 1 colon typed -> we are choosing ELEMENT
                // rem like "SLASH:" or "SLA"
                if (parts.length == 2) {
                    String shape = parts[0].trim();
                    // If they haven't completed a known shape yet, suggest shapes that match
                    if (!isValidShape(shape)) {
                        List<String> out = new ArrayList<>();
                        for (SkillTags.Shape s : SkillTags.Shape.values()) {
                            if (s.name().startsWith(shape)) out.add(s.name() + ":");
                        }
                        return SharedSuggestionProvider.suggest(out, builder);
                    }

                    // If they typed "SLASH:" (ends with colon), suggest elements as "SLASH:ELEMENT:"
                    if (rem.endsWith(":")) {
                        List<String> out = new ArrayList<>();
                        for (SkillTags.Element e : SkillTags.Element.values()) {
                            out.add(shape + ":" + e.name() + ":");
                        }
                        return SharedSuggestionProvider.suggest(out, builder);
                    }

                    // Otherwise let Brigadier filter as they type
                    List<String> out = new ArrayList<>();
                    for (SkillTags.Element e : SkillTags.Element.values()) {
                        out.add(shape + ":" + e.name() + ":");
                    }
                    return SharedSuggestionProvider.suggest(out, builder);
                }

                // 2 colons typed -> we are choosing MODIFIER
                if (parts.length >= 3) {
                    String shape = parts[0].trim();
                    String element = parts[1].trim();

                    // Validate shape/element; if incomplete, offer best-effort completion
                    if (!isValidShape(shape) || !isValidElement(element)) {
                        // Offer full combos only after a valid shape is chosen (avoids spam)
                        List<String> out = new ArrayList<>();
                        for (SkillTags.Shape s : SkillTags.Shape.values()) {
                            if (!s.name().startsWith(shape)) continue;
                            for (SkillTags.Element e : SkillTags.Element.values()) {
                                if (!element.isEmpty() && !e.name().startsWith(element)) continue;
                                out.add(s.name() + ":" + e.name() + ":");
                            }
                        }
                        return SharedSuggestionProvider.suggest(out, builder);
                    }

                    // If user is at "SHAPE:ELEMENT:" (ends with colon), suggest modifiers with trailing pipe
                    if (rem.endsWith(":")) {
                        List<String> out = new ArrayList<>();
                        for (SkillTags.Modifier m : SkillTags.Modifier.values()) {
                            out.add(shape + ":" + element + ":" + m.name() + "|");
                        }
                        return SharedSuggestionProvider.suggest(out, builder);
                    }

                    // If they started typing modifier letters, suggest matching modifiers
                    String modPrefix = parts[2].trim(); // may be partial
                    List<String> out = new ArrayList<>();
                    for (SkillTags.Modifier m : SkillTags.Modifier.values()) {
                        if (m.name().startsWith(modPrefix)) {
                            out.add(shape + ":" + element + ":" + m.name() + "|");
                        }
                    }
                    return SharedSuggestionProvider.suggest(out, builder);
                }

                return builder.buildFuture();
            };

    private static boolean isValidShape(String s) {
        if (s == null || s.isEmpty()) return false;
        for (SkillTags.Shape sh : SkillTags.Shape.values()) {
            if (sh.name().equals(s)) return true;
        }
        return false;
    }

    private static boolean isValidElement(String e) {
        if (e == null || e.isEmpty()) return false;
        for (SkillTags.Element el : SkillTags.Element.values()) {
            if (el.name().equals(e)) return true;
        }
        return false;
    }
}
