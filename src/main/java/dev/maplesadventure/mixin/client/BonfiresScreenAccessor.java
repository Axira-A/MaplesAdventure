package dev.maplesadventure.mixin.client;

import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Version-sensitive Bonfires UI details are intentionally isolated here. */
@Pseudo
@Mixin(targets = "wehavecookies56.bonfires.client.gui.BonfireScreen", remap = false)
public interface BonfiresScreenAccessor {
    @Accessor("travelOpen") boolean maplesadventure$isTravelOpen();
    @Accessor("travel") Button maplesadventure$travelButton();
    @Accessor("reinforce") Button maplesadventure$reinforceButton();
    @Accessor("leave") Button maplesadventure$leaveButton();
}
