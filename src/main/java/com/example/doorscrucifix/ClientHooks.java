package com.example.doorscrucifix;

import net.minecraft.client.Minecraft;

public class ClientHooks {
    public static void openBible() {
        Minecraft.getInstance().displayGuiScreen(new BibleScreen());
    }
}
