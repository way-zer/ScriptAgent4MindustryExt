package wayzer.user.ext

import coreLibrary.lib.PlaceHoldString
import coreLibrary.lib.with
import mindustry.gen.Player
import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile
import java.time.LocalDate

object RankData : IdTable<Int>("RankData") {
    class CurrentWeek : Function<LocalDate>(JavaLocalDateColumnType()) {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
            +"current_week()"
        }
    }

    override val id: Column<EntityID<Int>> = reference("id", PlayerProfile.T, ReferenceOption.CASCADE)
    val name = varchar("name", 36).default("UNKNOWN")
    val onlineTime = integer("onlineTime").default(0)
    val exp = integer("exp").default(0)
    val played = integer("played").default(0)
    val win = integer("win").default(0)
    val week = date("weekUpdate").defaultExpression(CurrentWeek())
    val weekValue = arrayOf(onlineTime, exp, played, win).associateWith {
        registerColumn<Int>(it.name + "Week", it.columnType).default(0)
    }

    override fun createStatement(): List<String> {
        return mutableListOf(utilFunction) + super.createStatement() + triggerStatement + weekTrigger()
    }

    @Language("PostgreSQL")
    val utilFunction = """
        create or replace function current_week() returns date as $$
            begin 
            return current_date - (extract(DOW from current_date)-1||'day')::interval;
            end;
        $$ language plpgsql
    """.trimIndent()

    @Language("PostgreSQL")
    val triggerStatement = """
        create or replace function updateRank() returns trigger as $$
            begin
            if not exists(select * from rankData where id=new.id) then
                insert into rankData(id) values (new.id);
            end if;
            update rankData set 
                name = coalesce(new."lastName",'UNKNOWN'),
                exp = new."totalExp",
                "onlineTime" = new."totalTime"
            where id = new.id;
            return new;
            end
        $$ language plpgsql;
        drop trigger if exists updateRank on playerProfile;
        create trigger updateRank after insert or update on playerProfile
        for each row execute procedure updateRank();
    """.trimIndent()

    fun weekTrigger(): String {
        //language=PostgreSQL
        return """
        create or replace function updateRankWeek() returns trigger as $$
        declare 
            week date := current_week();
        begin
            if new."weekUpdate" != week then
                new."weekUpdate" := week;
                ${
            weekValue.values.joinToString("\n") {
                """new."${it.name}" :=0;"""
            }
        }
            end if;
            ${
            weekValue.entries.joinToString("\n") { (k, v) ->
                """new."${v.name}" := old."${v.name}" + new."${k.name}" - old."${k.name}";"""
            }
        }
            return new;
        end
        $$ language plpgsql;
        drop trigger if exists updateRankWeek on rankData;
        create trigger updateRankWeek before update on rankData
        for each row execute procedure updateRankWeek();
    """.trimIndent()
    }

    class RankFunction(private val field: Expression<Int>, private val order: SortOrder = SortOrder.DESC) :
        Function<Int>(IntegerColumnType()) {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
            append("rank() over (order by ")
            append(field)
            append(' ')
            append(order.name)
            append(')')
        }
    }

    class RankStatement(
        private val field: Column<Int>,
        private val checkWeek: Boolean,
        private val p: Player?,
        private val valueMap: (Int) -> Any
    ) :
        Statement<List<PlaceHoldString>>(StatementType.SELECT, emptyList()) {
        override fun arguments(): Iterable<Iterable<Pair<IColumnType, Any>>> {
            val profile = p?.let { PlayerData[it.uuid()].profile?.id?.value } ?: -1
            return listOf(listOf(IntegerColumnType() to profile))
        }

        override fun prepareSQL(transaction: Transaction) = QueryBuilder(true).apply {
            append("with t as (select ")
            listOf(RankData.id, RankData.name, field, RankFunction(field)).appendTo { +it }
            append(" from ")
            RankData.describe(transaction, this)
            if (checkWeek) {
                append(" where ")
                +SqlExpressionBuilder.run {
                    RankData.week eq RankData.CurrentWeek()
                }
            }
            append(')')

            append("(select * from t limit 10)")
            if (p == null) return@apply
            append("union all")
            append("(select * from t where t.id = ?)")
        }.toString()

        override fun PreparedStatementApi.executeInternal(transaction: Transaction): List<PlaceHoldString> {
            val set = executeQuery()
            return mutableListOf<PlaceHoldString>().apply {
                while (set.next()) {
                    val rank = set.getInt("rank").toString().padStart(2, ' ')
                    val name = set.getString("name")
                    val value = valueMap(set.getInt(field.name))
                    add("{rank}: {name} {value}".with("rank" to rank, "name" to name, "value" to value))
                }
            }
        }

    }
}