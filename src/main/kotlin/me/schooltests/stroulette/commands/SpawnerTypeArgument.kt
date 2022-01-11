package me.schooltests.stroulette.commands

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import me.schooltests.stroulette.chance.ChanceManager
import me.schooltests.stroulette.chance.SpawnerType
import java.util.*
import java.util.function.BiFunction

class SpawnerTypeArgument<C>(
    required: Boolean,
    name: String,
    defaultValue: String,
    suggestionsProvider: (BiFunction<CommandContext<C>, String, List<String>>)?,
    defaultDescription: ArgumentDescription
): CommandArgument<C, SpawnerType>(
    required, name, SpawnerTypeParser<C>(), defaultValue, SpawnerType::class.java, suggestionsProvider, defaultDescription
) {
    companion object {
        fun <C> new(name: String): Builder<C> {
            return Builder(name)
        }
    }

    class Builder<C>(name: String): CommandArgument.Builder<C, SpawnerType>(SpawnerType::class.java, name) {
        override fun build(): CommandArgument<C, SpawnerType> {
            return SpawnerTypeArgument(this.isRequired, this.name, this.defaultValue, this.suggestionsProvider, this.defaultDescription)
        }
    }

    class SpawnerTypeParser<C>: ArgumentParser<C, SpawnerType> {
        override fun parse(context: CommandContext<C>, queue: Queue<String>): ArgumentParseResult<SpawnerType> {
            val input = queue.peek()
                ?: return ArgumentParseResult.failure(NoInputProvidedException(
                    SpawnerTypeParser::class.java, context
                ))
            val filter = ChanceManager.spawnerChances.keys.find { it.equals(input, ignoreCase = true) }
            if (filter != null) {
                queue.remove()
                return ArgumentParseResult.success(SpawnerType(filter))
            }

            return ArgumentParseResult.failure(IllegalArgumentException())
        }
    }
}