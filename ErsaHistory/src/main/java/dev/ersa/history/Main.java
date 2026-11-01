package dev.ersa.history;

import dev.ersa.api.ErsaAC;
import dev.ersa.api.ErsaAPI;
import dev.ersa.api.events.impl.ErsaLoadedEvent;
import dev.ersa.api.exception.ErsaNotInitializedException;
import dev.ersa.history.command.HistoryCommand;
import dev.ersa.history.config.HistoryConfig;
import dev.ersa.history.listener.HistoryListener;
import dev.ersa.history.manager.HistoryManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class Main extends JavaPlugin implements Listener {
    private ErsaAPI ersaAPI;
    private HistoryManager manager;
    private HistoryListener listener;
    private volatile boolean hooked = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        HistoryConfig config = new HistoryConfig(getConfig());
        this.manager = new HistoryManager(this, config);
        this.manager.init();

        startRetentionTask();

        if (ErsaAC.isInitialized()) hookErsa();
        else Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void startRetentionTask() {
        HistoryConfig config = manager.getConfig();
        if (config.retentionDays() > 0) {
            long intervalTicks = TimeUnit.MINUTES.toSeconds(config.retentionIntervalMinutes()) * 20L;
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(config.retentionDays());
                int purged = manager.purgeOlderThan(cutoff);
                if (purged > 0) getLogger().info("Purged " + purged + " old history records");
            }, intervalTicks, intervalTicks);
        }
    }

    @EventHandler
    public void onErsaLoaded(ErsaLoadedEvent event) {
        hookErsa();
    }

    private void hookErsa() {
        if (hooked) return;
        hooked = true;
        try {
            this.ersaAPI = ErsaAC.get();
        } catch (ErsaNotInitializedException ex) {
            getLogger().severe("ErsaAC is not initialized: " + ex.getMessage());
            return;
        }
        this.listener = new HistoryListener(manager);
        ersaAPI.eventManager().registerEvent(listener);
        ersaAPI.commandManager().registerCommand(new HistoryCommand(this, manager));
        getLogger().info("Hooked into ErsaAC.");
    }

    @Override
    public void onDisable() {
        if (ersaAPI != null) {
            try { ersaAPI.commandManager().unregisterCommand("history"); } catch (Throwable ignored) {}
            if (listener != null) {
                try { ersaAPI.eventManager().unregisterEvent(listener); } catch (Throwable ignored) {}
            }
        }
        if (manager != null) manager.shutdown();
    }
}
