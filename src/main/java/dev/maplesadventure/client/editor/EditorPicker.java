package dev.maplesadventure.client.editor;

import java.util.*;

/** Search is over author labels, not hidden technical identifiers. */
public final class EditorPicker {
    public static List<RuleEditor.Choice> filter(List<RuleEditor.Choice> choices,String query){String lower=query.toLowerCase(Locale.ROOT);return choices.stream().filter(c->c.label().toLowerCase(Locale.ROOT).contains(lower)).toList();}
    private EditorPicker(){}
}
