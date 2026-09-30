package dev.maplesadventure.api.editor.logic;
import dev.maplesadventure.api.editor.ComponentDescriptor;
/** Experimental common descriptor; an integration emits this event through MaplesAuthoringApi. */
public record EventType<T>(ComponentDescriptor<T> schema, boolean suppliesPlayer, boolean requiresVolume) {}
