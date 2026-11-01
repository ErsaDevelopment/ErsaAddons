package dev.ersa.history.listener;

import dev.ersa.api.check.ICheck;
import dev.ersa.api.events.impl.ErsaCommandExecuteEvent;
import dev.ersa.api.user.ErsaUser;
import dev.ersa.history.config.HistoryConfig;
import dev.ersa.history.manager.HistoryManager;
import dev.ersa.history.model.ViolationRecord;
import dev.onix.EventHandler;

public final class HistoryListener {
    private final HistoryManager manager;

    public HistoryListener(HistoryManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onCommand(ErsaCommandExecuteEvent event) {
        HistoryConfig config = manager.getConfig();

        if (!config.commandMarker().equalsIgnoreCase(event.command())) return;

        ICheck check = event.check();
        ErsaUser player = event.player();

        if (check == null || player == null) return;

        final ViolationRecord record = new ViolationRecord(
                config.serverName(),
                player.uuid(),
                check.getName(),
                event.verbose(),
                (int) check.getViolations(),
                System.currentTimeMillis(),
                String.valueOf(player.brand()),
                String.valueOf(player.version()),
                player.getHumanSensitivityX(),
                player.getHumanSensitivityY(),
                player.ping()
        );

        manager.logAlert(record);
    }
}