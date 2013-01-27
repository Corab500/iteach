package net.iteach.service.db;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

public final class SQLUtils {

	private SQLUtils() {
	}
	
	public static String dateToDB (LocalDate date) {
		return date.toString();
	}

	public static LocalDate dateFromDB(String str) {
		return LocalDate.parse(str);
	}
	
	public static String timeToDB (LocalTime time) {
		return time.toString("HH:mm");
	}

	public static LocalTime timeFromDB(String str) {
		return LocalTime.parse(str);
	}

	public static DateTime now() {
		return DateTime.now(DateTimeZone.UTC);
	}

	public static Timestamp toTimestamp(DateTime dateTime) {
		return new Timestamp(dateTime.getMillis());
	}

	public static DateTime getDateTime(ResultSet rs, String columnName) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(columnName);
		return getDateTime(timestamp);
	}

	public static DateTime getDateTime(Timestamp timestamp) {
		return timestamp != null ? new DateTime(timestamp.getTime(), DateTimeZone.UTC) : null;
	}

	public static <E extends Enum<E>> E getEnum(Class<E> enumClass, ResultSet rs, String columnName) throws SQLException {
		String value = rs.getString(columnName);
		if (value == null) {
			return null;
		} else {
			return Enum.valueOf(enumClass, value);
		}
	}

	public static Money moneyFromDB(ResultSet rs, String column) throws SQLException {
		BigDecimal amount = rs.getBigDecimal(column);
		if (amount == null) {
			amount = BigDecimal.ZERO;
		}
		return Money.of(CurrencyUnit.EUR, amount);
	}

}
