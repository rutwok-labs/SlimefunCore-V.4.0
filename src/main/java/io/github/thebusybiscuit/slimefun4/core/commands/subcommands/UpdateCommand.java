package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.core.services.UpdaterService.UpdateStatus;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

class UpdateCommand extends SubCommand {

    @ParametersAreNonnullByDefault
    UpdateCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "update", false);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!(sender.hasPermission("slimefun.command.update") || sender instanceof ConsoleCommandSender)) {
            Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
            return;
        }

        sender.sendMessage(ChatColors.color("&7Checking Slimefun updates..."));
        Slimefun.getUpdater().checkNow(status -> sendStatus(sender, status));
    }

    private void sendStatus(@Nonnull CommandSender sender, @Nonnull UpdateStatus status) {
        sender.sendMessage("");
        sender.sendMessage(ChatColors.color("&aSlimefun Update Status"));
        sender.sendMessage(ChatColors.color("&7Branch: &f" + status.branch().name()));
        sender.sendMessage(ChatColors.color("&7Current: &f" + status.currentVersion()));

        if (status.latestVersion() != null) {
            sender.sendMessage(ChatColors.color("&7Latest: &f" + status.latestVersion()));
        }

        if (status.latestAsset() != null) {
            sender.sendMessage(ChatColors.color("&7Latest file: &f" + status.latestAsset()));
        }

        sender.sendMessage(ChatColors.color("&7Update checks: &f" + (status.checksEnabled() ? "enabled" : "disabled")));
        sender.sendMessage(ChatColors.color("&7Auto-download: &f" + (status.autoDownloadEnabled() ? "enabled" : "disabled")));

        if (status.lastError() != null) {
            sender.sendMessage(ChatColors.color("&cLast check failed: &4" + status.lastError()));
        } else if (!status.checked()) {
            sender.sendMessage(ChatColors.color("&eNo update result is cached yet."));
        } else if (status.updateAvailable()) {
            sender.sendMessage(ChatColors.color("&6A new update is available."));
            if (status.projectUrl() != null) {
                sender.sendMessage(ChatColors.color("&7Download: &f" + status.projectUrl()));
            }
        } else {
            sender.sendMessage(ChatColors.color("&aThis build is up to date."));
        }
    }
}
