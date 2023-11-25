package com.elitemastereric.examplemod.mixin;

import com.elitemastereric.helloworld.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Main.class)
public class MainMixin {
    @Inject(method = "getGreeting*", at = @At("RETURN"), cancellable = true)
    public void injectGetGreeting(CallbackInfoReturnable<String> callbackInfo) {
        callbackInfo.setReturnValue("Goodbye World!");
    }
}
