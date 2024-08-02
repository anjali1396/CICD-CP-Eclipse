package v1.repository;

import org.hibernate.Session;
import utils.Constants.CredType;
import utils.HibernateUtil;
import utils.LoggerUtils;
import models.Creds;

public class CommonRepository {

	public Creds findCredsByPartnerName(String partnerName, CredType type) {
		
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			
			return session
					.createQuery(
							"from Creds where partnerName = :partnerName and credType = :credType and isValid = true",
							Creds.class)
					.setParameter("partnerName", partnerName)
					.setParameter("credType", type.value)
					.getSingleResult();

		} catch (Exception e) {

			LoggerUtils.log("findCredsByPartnerName - Error : " + e.getMessage());
			return null;

		}

	}
	
}
