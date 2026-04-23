package com.aggrotag;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AggroTagPluginTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(AggroTagPlugin.class);
        RuneLite.main(args);
    }
}