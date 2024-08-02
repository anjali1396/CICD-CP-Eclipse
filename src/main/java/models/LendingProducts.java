package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "`LendingProducts`")
public class LendingProducts {

	@Id
	@Column(updatable = false, nullable = false)
	public String id;

	public String sfId;
	public String name;

	@ColumnDefault("0")
	public boolean isActive = false;
	
	@ColumnDefault("0")
	public boolean isTopup = false;	

}
