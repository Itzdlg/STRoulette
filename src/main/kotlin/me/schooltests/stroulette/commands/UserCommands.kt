package me.schooltests.stroulette.commands

import cloud.commandframework.CommandManager
import cloud.commandframework.arguments.standard.IntegerArgument
import me.rayzr522.jsonmessage.JSONMessage
import me.schooltests.stroulette.RoulettePlugin
import me.schooltests.stroulette.chance.ChanceManager
import me.schooltests.stroulette.chance.KeyType
import me.schooltests.stroulette.chance.RouletteType
import me.schooltests.stroulette.chance.SpawnerType
import me.schooltests.stroulette.gui.MainRouletteGUI
import me.schooltests.stroulette.gui.animations.DuplicateAnyGUI
import me.schooltests.stroulette.gui.animations.RerollAnyGUI
import me.schooltests.stroulette.prefix
import me.schooltests.stroulette.util.send
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

object UserCommands {
    fun rerollRollable(player: Player, type: RouletteType, amount: Int? = 1) {
        if (type.numberOf(player) < amount ?: 1) {
            player.send("$prefix &cYou do not have enough ${type.displayName} ${type.abstractType}(s)")
            return
        }

        val config = RoulettePlugin.instance!!.config
        val singlePrice = config.getDouble("prices.reroll-${type.abstractType}")
        val totalPrice: Double = singlePrice * (amount ?: 1).toDouble()
        val transaction = RoulettePlugin.economy!!.withdrawPlayer(player, totalPrice)
        if (!transaction.transactionSuccess()) {
            player.send("$prefix &cYou do not have enough money to perform this roll!")
            return
        }

        val isAllowedMore = config.getBoolean("allow-roll-many")
        val chanceLose = config.getInt("chance-to-lose.reroll")
        if (isAllowedMore && (amount ?: 1) > 1) {
            val winnings = mutableMapOf<String, Int>()

            var timesLost = 0
            type.take(player, amount ?: 1)
            for (i in 1..(amount ?: 1)) {
                if (chanceLose > Random().nextInt(100)) {
                    timesLost += 1
                    continue
                }

                val gui = RerollAnyGUI(player, type).apply {
                    handler { type, amount ->
                        type.give(player, amount, false)
                        winnings[type.displayName] = (winnings[type.displayName] ?: 0) + amount
                    }
                }

                gui.forceEnd(false)
            }

            var winningsString = ""
            for (item in winnings.entries) {
                winningsString = "$winningsString ${item.value}x ${item.key},"
            }

            if (timesLost > 0) {
                player.send("$prefix You lost ${timesLost}x ${type.displayName} ${type.abstractType}(s)")
            }

            if (timesLost < (amount ?: 1)) {
                player.send("$prefix You won &6${winningsString.substring(1, winningsString.length - 1)} &7${type.abstractType}(s)")
            }
        } else {
            type.take(player, 1)
            if (chanceLose > Random().nextInt(100)) {
                player.send("$prefix You lost 1 ${type.displayName} ${type.abstractType}. Try again next time!")
                return
            }

            RerollAnyGUI(player, type)
        }
    }

    fun duplicateRollable(player: Player, type: RouletteType, amount: Int? = 1) {
        if (type.numberOf(player) < amount ?: 1) {
            player.send("$prefix &cYou do not have enough ${type.displayName} ${type.abstractType}(s)")
            return
        }

        val config = RoulettePlugin.instance!!.config
        val singlePrice = config.getDouble("prices.dupe-${type.abstractType}")
        val totalPrice: Double = singlePrice * (amount ?: 1).toDouble()
        val transaction = RoulettePlugin.economy!!.withdrawPlayer(player, totalPrice)
        if (!transaction.transactionSuccess()) {
            player.send("$prefix &cYou do not have enough money to perform this roll!")
            return
        }

        val isAllowedMore = config.getBoolean("allow-roll-many")
        val chanceLose = config.getInt("chance-to-lose.dupe")
        if (isAllowedMore && (amount ?: 1) > 1) {
            val winnings = mutableMapOf<String, Int>()

            var timesLost = 0
            for (i in 1..(amount ?: 1)) {
                if (chanceLose > Random().nextInt(100)) {
                    type.take(player, 1, false)
                    timesLost += 1
                    continue
                }

                val gui = DuplicateAnyGUI(player, type).apply {
                    handler { type, amount ->
                        type.give(player, amount, false)
                        winnings[type.displayName] = (winnings[type.displayName] ?: 0) + amount
                    }
                }

                gui.forceEnd(false)
            }

            var winningsString = ""
            for (item in winnings.entries) {
                winningsString = "$winningsString ${item.value}x ${item.key},"
            }

            if (timesLost > 0) {
                player.send("$prefix You lost ${timesLost}x ${type.displayName} ${type.abstractType}(s)")
            }

            if (timesLost < (amount ?: 1)) {
                player.send("$prefix You won &6${winningsString.substring(1, winningsString.length - 1)} &7${type.abstractType}(s)")
            }
        } else {
            if (chanceLose > Random().nextInt(100)) {
                type.take(player, 1, false)
                player.send("$prefix You lost 1 ${type.displayName} ${type.abstractType}. Try again next time!")
                return
            }

            DuplicateAnyGUI(player, type)
        }
    }

    fun requestKeys(player: Player, subCommand: String) {
        val json = JSONMessage.create("Chaos")
            .color(ChatColor.YELLOW)
            .then("Roulette")
            .color(ChatColor.GOLD)
            .then("\n------------")
            .color(ChatColor.GRAY);
        for (key in ChanceManager.keyChances.keys) {
            val type = KeyType(key)
            val name = type.displayName
            val amount = type.numberOf(player)
            if (amount < 1) continue

            json.then("\n${name}: $amount").color(ChatColor.GOLD).suggestCommand("/roulette $subCommand $name $amount")
        }
        json.send(player)
    }

    fun requestSpawners(player: Player, subCommand: String) {
        val json = JSONMessage.create("Chaos")
            .color(ChatColor.YELLOW)
            .then("Roulette")
            .color(ChatColor.GOLD)
            .then("\n------------")
            .color(ChatColor.GRAY);
        for (key in ChanceManager.spawnerChances.keys) {
            val type = SpawnerType(key)
            val name = type.displayName
            val amount = type.numberOf(player)
            if (amount < 1) continue

            json.then("\n${name}: $amount").color(ChatColor.GOLD).suggestCommand("/roulette $subCommand ${type.type} $amount")
        }
        json.send(player)
    }

    fun registerCommands(manager: CommandManager<CommandSender>) {
        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .handler {
                    MainRouletteGUI(it.sender as Player).open()
                }
        )

        registerRerollCommands(manager)
        // registerDuplicationCommands(manager)
    }

    private fun registerRerollCommands(manager: CommandManager<CommandSender>) {
        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("rerollkeys")
                .handler {
                    requestKeys(it.sender as Player, "rerollkeys")
                }
        )

        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("rerollspawners")
                .handler {
                    requestSpawners(it.sender as Player, "rerollspawners")
                }
        )

        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("rerollkeys")
                .argument(KeyTypeArgument.new("type"))
                .argument(IntegerArgument.of("amount"))
                .handler {
                    rerollRollable(it.sender as Player, it.get("type"), it.get("amount"))
                }
        )

        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("rerollspawners")
                .argument(SpawnerTypeArgument.new("type"))
                .argument(IntegerArgument.of("amount"))
                .handler {
                    rerollRollable(it.sender as Player, it.get("type"), it.get("amount"))
                }
        )
    }

    private fun registerDuplicationCommands(manager: CommandManager<CommandSender>) {
        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("dupekeys")
                .handler {
                    requestKeys(it.sender as Player, "dupekeys")
                }
        )

        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("dupespawners")
                .handler {
                    requestSpawners(it.sender as Player, "dupespawners")
                }
        )

        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("dupekeys")
                .argument(KeyTypeArgument.new("type"))
                .argument(IntegerArgument.of("amount"))
                .handler {
                    duplicateRollable(it.sender as Player, it.get("type"), it.get("amount"))
                }
        )

        manager.command(
            manager.commandBuilder("roulette")
                .senderType(Player::class.java)
                .literal("dupespawners")
                .argument(SpawnerTypeArgument.new("type"))
                .argument(IntegerArgument.of("amount"))
                .handler {
                    duplicateRollable(it.sender as Player, it.get("type"), it.get("amount"))
                }
        )
    }
}