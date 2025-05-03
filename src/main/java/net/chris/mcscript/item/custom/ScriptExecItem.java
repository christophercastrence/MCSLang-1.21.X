package net.chris.mcscript.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ScriptExecItem extends Item {

    public ScriptExecItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        // Sends message to the chat
        if (!pLevel.isClientSide) {
            pPlayer.sendSystemMessage(Component.literal("This works"));
        }

        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }
}