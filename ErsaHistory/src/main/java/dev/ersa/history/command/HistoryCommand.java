package dev.ersa.history.command;

import dev.ersa.api.commands.base.ErsaCommand;
import dev.ersa.api.commands.base.context.CommandContext;

import dev.ersa.history.config.HistoryConfig;
import dev.ersa.history.manager.HistoryManager;
import dev.ersa.history.model.ViolationRecord;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class HistoryCommand extends ErsaCommand {
    private final Plugin plugin;
    private final HistoryManager manager;

    public HistoryCommand(Plugin plugin, HistoryManager manager) {
        super("history");
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getDescription() { return "Shows, clears or reloads alert history"; }
    @Override public String getUsage() { return "/ersa history <player> [page] | clear <player> | reload"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 2; }
    @Override public boolean noConsole() { return false; }

    @Override
    public boolean onCommand(CommandContext context) {
        CommandSender sender = context.getCommandSender();
        String[] args = context.getArgs();
        HistoryConfig config = manager.getConfig();

        if (args[0].equalsIgnoreCase("reload")) {
            if (args.length != 1) {
                sender.sendMessage(color(config, config.msgUsage()));
                return true;
            }
            plugin.reloadConfig();
            final HistoryConfig newConfig = new HistoryConfig(plugin.getConfig());

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                manager.reload(newConfig);
                boolean ok = manager.isLoaded();
                sendMain(sender, color(newConfig, ok
                        ? newConfig.prefix() + " &aИстория успешно перезагружена."
                        : newConfig.msgLoadFailure()));
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (args.length != 2) {
                sender.sendMessage(color(config, config.msgClearUsage()));
                return true;
            }
            handleClear(sender, args[1]);
            return true;
        }

        String targetName = args[0];
        int page;
        try {
            page = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
            if (page < 1) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            sender.sendMessage(color(config, config.msgInvalidPage()));
            return true;
        }
        handleHistory(sender, targetName, page);
        return true;
    }

    private void handleHistory(CommandSender sender, String targetName, int page) {
        HistoryConfig config = manager.getConfig();
        if (!config.enabled()) { sender.sendMessage(color(config, config.msgHistoryDisabled())); return; }
        if (!manager.isLoaded()) { sender.sendMessage(color(config, config.msgLoadFailure())); return; }

        final int entriesPerPage = config.entriesPerPage();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
                sendMain(sender, color(config, config.msgPlayerNotFound()));
                return;
            }
            UUID uuid = target.getUniqueId();
            String displayName = target.getName() != null ? target.getName() : targetName;

            int count = manager.getLogCount(uuid);
            if (!manager.isLoaded()) {
                sendMain(sender, color(config, config.msgLoadFailure()));
                return;
            }
            if (count == 0) {
                sendMain(sender, color(config, config.msgNoViolations().replace("%player%", displayName)));
                return;
            }
            int maxPages = Math.max(1, (int) Math.ceil((double) count / entriesPerPage));
            int finalPage = Math.min(page, maxPages);

            List<ViolationRecord> records = manager.getViolations(uuid, finalPage, entriesPerPage);
            if (!manager.isLoaded()) {
                sendMain(sender, color(config, config.msgLoadFailure()));
                return;
            }

            List<String> lines = new ArrayList<>(records.size() + 1);
            lines.add(color(config, config.msgHeader()
                    .replace("%player%", displayName)
                    .replace("%page%", String.valueOf(finalPage))
                    .replace("%maxPages%", String.valueOf(maxPages))));

            for (int i = records.size() - 1; i >= 0; i--) {
                ViolationRecord r = records.get(i);
                lines.add(color(config, config.msgEntry()
                        .replace("%check%", r.checkName())
                        .replace("%vl%", String.valueOf(r.vl()))
                        .replace("%verbose%", r.verbose())
                        .replace("%timeago%", timeAgo(r.createdAt()))
                        .replace("%brand%", r.clientBrand())
                        .replace("%clientVersion%", r.clientVersion())
                        .replace("%sensX%", String.valueOf(r.sensitivityX()))
                        .replace("%sensY%", String.valueOf(r.sensitivityY()))
                        .replace("%ping%", String.valueOf(r.ping()))
                ));
            }
            sendMain(sender, lines.toArray(new String[0]));
        });
    }

    private void handleClear(CommandSender sender, String targetName) {
        HistoryConfig config = manager.getConfig();
        if (!manager.isLoaded()) { sender.sendMessage(color(config, config.msgLoadFailure())); return; }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
                sendMain(sender, color(config, config.msgPlayerNotFound()));
                return;
            }
            int removed = manager.clear(target.getUniqueId());
            if (!manager.isLoaded()) {
                sendMain(sender, color(config, config.msgLoadFailure()));
                return;
            }
            sendMain(sender, color(config, config.msgCleared()
                    .replace("%player%", target.getName() != null ? target.getName() : targetName)
                    .replace("%count%", String.valueOf(removed))));
        });
    }

    private void sendMain(CommandSender sender, String... messages) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (String msg : messages) sender.sendMessage(msg);
        });
    }

    private String color(HistoryConfig config, String input) {
        return input.replace("%prefix%", config.prefix()).replace('&', '\u00A7');
    }

    private String timeAgo(long timestamp) {
        long ms = System.currentTimeMillis() - timestamp;
        if (ms < 0) return "0s";
        long days = TimeUnit.MILLISECONDS.toDays(ms); ms -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(ms); ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms); ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        String[] args = context.getArgs();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>(matchingPlayers(prefix));
            if ("reload".startsWith(prefix)) suggestions.add("reload");
            if ("clear".startsWith(prefix)) suggestions.add("clear");
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            return matchingPlayers(args[1].toLowerCase(Locale.ROOT));
        }

        return List.of();
    }

    private List<String> matchingPlayers(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) names.add(name);
        }
        return names;
    }
}