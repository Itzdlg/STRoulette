package me.schooltests.stroulette.commands

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import me.schooltests.stroulette.chance.ChanceManager
import me.schooltests.stroulette.chance.KeyType
import java.util.*
import java.util.function.BiFunction

class KeyTypeArgument<C>(
    required: Boolean,
    name: String,
    defaultValue: String,
    suggestionsProvider: (BiFunction<CommandContext<C>, String, List<String>>)?,
    defaultDescription: ArgumentDescription
): CommandArgument<C, KeyType>(
    required, name, KeyTypeParser<C>(), defaultValue, KeyType::class.java, suggestionsProvider, defaultDescription
) {
    companion object {
        fun <C> new(name: String): Builder<C> {
            return Builder(name)
        }
    }

    class Builder<C>(name: String): CommandArgument.Builder<C, KeyType>(KeyType::class.java, name) {
        override fun build(): CommandArgument<C, KeyType> {
            return KeyTypeArgument(this.isRequired, this.name, this.defaultValue, this.suggestionsProvider, this.defaultDescription)
        }
    }

    class KeyTypeParser<C>: ArgumentParser<C, KeyType> {
        override fun parse(context: CommandContext<C>, queue: Queue<String>): ArgumentParseResult<KeyType> {
            val input = queue.peek()
                ?: return ArgumentParseResult.failure(NoInputProvidedException(
                    KeyTypeParser::class.java, context
                ))
            val filter = ChanceManager.keyChances.keys.find { it.equals(input, ignoreCase = true) }
            if (filter != null) {
                queue.remove()
                return ArgumentParseResult.success(KeyType(filter))
            }

            return ArgumentParseResult.failure(IllegalArgumentException())
        }
    }
}