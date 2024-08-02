package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "BreachedPassword", schema = "HomeFirstCustomerPortal")

public class BreachedPassword {
	
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id", updatable = false, nullable = true)
	public String id;
	
	@Column(name = "password",unique = true)
	public String password = null;
	
	@Column(name = "encryptedPassword")
	public String encryptedPassword = null;

	
	
}