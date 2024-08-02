package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.opencsv.bean.CsvBindByName;

import utils.DateTimeUtils;

@Entity
@Table(name = "`ROIReprice`")
public class ROIReprice {

	@Id
	@GeneratedValue(generator = "UUID")
	@CsvBindByName
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id = null;

	@CsvBindByName
	public int userId = -1;
	@CsvBindByName
	public String name = null;
	@CsvBindByName
	public String loanAccountNumber = null;

	@CsvBindByName
	public String branch = null;
	@CsvBindByName
	public String currentROI = null;
	@CsvBindByName
	public String newROI = null;
	@CsvBindByName
	public String loanType = null;
	@CsvBindByName
	public boolean isEligible = false;
	@CsvBindByName
	public boolean isPaid = false;
	@CsvBindByName
	public int paymentId = -1;
	@CsvBindByName
	@Column(nullable = false)
	public Double paymentAmount = 0.0;

	@CsvBindByName
	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@CsvBindByName
	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@CsvBindByName
	@Column(columnDefinition = "DATETIME")
	public String eligibleDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	public ROIReprice() {

	}
}
