package utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeUtils {

	public enum DateTimeZone {
		IST("Asia/Kolkata"), GMT("GMT");

		public final String value;

		DateTimeZone(String value) {
			this.value = value;
		}

	}

	public enum DateTimeFormat {

		yyyy_MM_dd_HH_mm_ss_SSS_GMT_z("yyyy-MM-dd HH:mm:ss.SSS z"),
		yyyy_MM_dd_T_HH_mm_ss_SSSZ("yyyy-MM-dd'T'HH:mm:ss.SSSZ"), yyyy_MM_dd("yyyy-MM-dd"), yyyy_MM("yyyy-MM"),
		MMM_dd_yyyy("MMM dd, yyyy"), E("E"), hh_mm("hh:mm"), HH_mm("HH:mm"), MMM("MMM"), MM("MM"), dd("dd"),
		yyyy("yyyy"), EEE_MMM_d("EEE, MMM d"), d_MMM("d MMM"), MMM_d("MMM d"), MMM_d_hh_mm_a("MMM d, hh:mm a"),
		MMM_d_yyyy("MMM d, yyyy"), MMM_yyyy("MMM-yyyy"), hh_mm_a("hh:mm a"), MMM_dd_yyyy_h_mm_a("MMM dd, yyyy h:mm a"),
		yyyy_MM_dd_HH_mm("yyyy-MM-dd HH:mm"), h_mm_a("h:mm a"), dd_MM_yyyy("dd-MM-yyyy"),
		dd_MM_yyyy_slash("dd/MM/yyyy"), dd_M_yyyy_slash("dd/M/yyyy"), yyyy_MM_dd_HH_mm_ss("yyyy-MM-dd HH:mm:ss"),
		dd_MM_yyyy_HH_mm_ss("dd-MM-yyyy HH:mm:ss"), dd_MM_yyyy_hh_mm_a("dd-MM-yyyy hh:mm a"), d_EEE_yyyy("d MMM, yyyy"),
		d_EEE_yyyy_hh_mm_a("d MMM, yyyy hh:mm a");

		public final String value;

		DateTimeFormat(String value) {
			this.value = value;
		}
	}

	public static LocalDateTime getCurrentDateTimeInIST() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimeFormat.yyyy_MM_dd_HH_mm_ss.value);
		String formatedDateTime = LocalDateTime.now(ZoneId.of(DateTimeZone.IST.value)).format(formatter);
		return LocalDateTime.parse(formatedDateTime, formatter);
	}

	public static LocalDateTime getCurrentDateTime(DateTimeZone dateTimeZone, DateTimeFormat dateTimeFormat) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat.value);
		String formatedDateTime = LocalDateTime.now(ZoneId.of(dateTimeZone.value)).format(formatter);
		return LocalDateTime.parse(formatedDateTime);
	}

	public static LocalDateTime getCurrentDateTimeByAddingHours(int hours, DateTimeFormat dateTimeFormat,
			DateTimeZone dateTimeZone) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat.value);
		String formatedDateTime = LocalDateTime.now(ZoneId.of(dateTimeZone.value)).plusHours(hours).format(formatter);
		return LocalDateTime.parse(formatedDateTime);
	}

}
