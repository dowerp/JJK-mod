package com.dowerp.sukuna.item;

import com.dowerp.sukuna.Fx;
import com.dowerp.sukuna.Players;
import com.dowerp.sukuna.SukunaState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class FingerItem extends Item {
    public FingerItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity p) || !(world instanceof ServerWorld w)) {
            return ActionResult.SUCCESS;
        }
        int fingers = p.getAttachedOrElse(Players.FINGERS, 1);
        if (fingers >= 20) {
            p.sendMessage(Text.translatable("sukuna.msg.max_fingers"), true);
            return ActionResult.FAIL;
        }
        p.setAttached(Players.FINGERS, fingers + 1);
        user.getStackInHand(hand).decrementUnlessCreative(1, user);
        SukunaState s = Players.get(p);
        s.fingers = fingers + 1;
        s.ce = s.maxCe();
        s.initialized = true;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
        Fx.far(w, ParticleTypes.SCULK_SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.6, 0.9, 0.6, 0.05);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.6F, 1.5F);
        if (fingers + 1 == SukunaState.OPEN_DOMAIN_FINGERS) {
            p.sendMessage(Text.translatable("sukuna.msg.open_ready"), false);
        }
        if (fingers == 0) {
            p.sendMessage(Text.translatable("sukuna.msg.awakened"), false);
        } else {
            p.sendMessage(Text.translatable("sukuna.msg.finger", fingers + 1), true);
        }
        return ActionResult.SUCCESS;
    }
}
