package models;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.json.JSONException;
import org.json.JSONObject;

import utils.BasicUtils;
import utils.Constants;
import utils.DateTimeUtils;

@Entity
public class Address {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(updatable = false, nullable = false)
	public String id = null;
	public String city = null;
	public String street = null;
	public String state = null;
	public String country = "India";
	public String postalCode = null;
	public String mobile = null;
	public String phone = null;
	
	@Column(columnDefinition = "JSON")
	public String raw;

	@Column(columnDefinition = "DATETIME", updatable = false, nullable = false)
	public String createDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	@Column(columnDefinition = "DATETIME")
	public String updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();

	public Address() {
	}

	public Address(ResultSet resultSet) throws SQLException {

		id = resultSet.getString("id");
		street = resultSet.getString("street");
		city = resultSet.getString("city");
		state = resultSet.getString("state");
		postalCode = resultSet.getString("postalCode");
		country = resultSet.getString("country");
		mobile = resultSet.getString("mobile");
		phone = resultSet.getString("phone");
		createDatetime = resultSet.getString("createDatetime");
		updateDatetime = resultSet.getString("updateDatetime");

	}

	public Address(JSONObject jsonObject) throws JSONException {

		city = jsonObject.optString("city", Constants.NA);
		street = jsonObject.optString("street", Constants.NA);
		state = jsonObject.optString("state", Constants.NA);
		country = jsonObject.optString("country", "India");
		postalCode = jsonObject.optString("postalCode", Constants.NA);

	}

	public JSONObject toJson() throws JSONException {

		JSONObject userObject = new JSONObject();

		userObject.put("city", city);
		userObject.put("street", street);
		userObject.put("state", state);
		userObject.put("country", country);
		userObject.put("postalCode", postalCode);

		return userObject;

	}

	public boolean isValid() {
		return (hasData(city) && hasData(street) && hasData(state) && hasData(postalCode));
	}

	private boolean hasData(String value) {
		return (!value.equalsIgnoreCase(Constants.NA) && !value.isEmpty());
	}

	public String getAddress() {

		StringBuilder sb = new StringBuilder("");

		if (BasicUtils.isNotNullOrNA(street))
			sb.append(street + ", ");
		if (BasicUtils.isNotNullOrNA(city))
			sb.append(city + ", ");
		if (BasicUtils.isNotNullOrNA(state))
			sb.append(state + ", ");
		if (BasicUtils.isNotNullOrNA(country))
			sb.append(country + ", ");
		if (BasicUtils.isNotNullOrNA(postalCode))
			sb.append(postalCode);

		if (sb.toString().isEmpty())
			sb.append("Not Available!");

		return sb.toString();
	}
	
	public void updateInfo(Address address) {
		
		street = address.street;
		city = address.city;
		state = address.state;
		postalCode = address.postalCode;
		country = address.country;
		updateDatetime = DateTimeUtils.getCurrentDateTimeInIST();
		
	}

}
